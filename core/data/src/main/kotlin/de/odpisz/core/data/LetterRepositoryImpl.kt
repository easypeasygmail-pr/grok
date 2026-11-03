package de.odpisz.core.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import de.odpisz.core.database.OdpiszDatabase
import de.odpisz.core.model.Deadline
import de.odpisz.core.model.Din5008Letter
import de.odpisz.core.model.DocumentTextReader
import de.odpisz.core.model.Letter
import de.odpisz.core.model.LetterPdfWriter
import de.odpisz.core.model.LetterRepository
import de.odpisz.core.model.LetterStatus
import de.odpisz.core.model.LetterSummary
import de.odpisz.core.model.ReplyPolicy
import de.odpisz.core.model.ReplyScenario
import de.odpisz.core.model.UserProfile
import de.odpisz.core.model.toSummary
import de.odpisz.core.network.LetterAnalyzer
import de.odpisz.core.network.ReplyGenerator
import de.odpisz.core.network.ReplyRequest
import de.odpisz.core.privacy.PiiMasker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LetterRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val database: OdpiszDatabase,
        private val masker: PiiMasker,
        private val analyzer: LetterAnalyzer,
        private val replies: ReplyGenerator,
        private val reader: DocumentTextReader,
        private val pdfWriter: LetterPdfWriter,
    ) : LetterRepository {
        override fun observeLetters(): Flow<List<LetterSummary>> =
            database.letterDao().observeAll().map { rows -> rows.map { it.toDomain().toSummary() } }

        override suspend fun getLetter(id: String): Letter? =
            withContext(Dispatchers.IO) {
                database.letterDao().get(id)?.toDomain()
            }

        override suspend fun importText(
            text: String,
            sourceLabel: String?,
        ): String =
            withContext(Dispatchers.IO) {
                val profile = currentProfile()
                val masked = masker.mask(text, profile)
                val id = UUID.randomUUID().toString()
                val title =
                    text
                        .lineSequence()
                        .firstOrNull { it.isNotBlank() }
                        ?.trim()
                        ?.take(TITLE_LIMIT) ?: "Pismo"
                val letter =
                    Letter(
                        id = id,
                        createdAt = Instant.now(),
                        title = title,
                        originalText = text,
                        maskedText = masked.masked,
                        sourceLabel = sourceLabel,
                        status = LetterStatus.Draft,
                        analysis = null,
                        reply = null,
                        piiTokens = masked.tokens,
                    )
                database.letterDao().upsert(letter.toEntity())
                id
            }

        override suspend fun importUri(uri: String): String {
            val text = reader.read(uri)
            return importText(text, uri)
        }

        override suspend fun analyze(letterId: String): Letter =
            withContext(Dispatchers.IO) {
                val letter = requireLetter(letterId)
                val analysis = analyzer.analyze(letter.maskedText)
                val status =
                    if (ReplyPolicy.blocksReply(analysis.riskCategory)) {
                        LetterStatus.NeedsAdvisor
                    } else {
                        LetterStatus.Analyzed
                    }
                val updated =
                    letter.copy(
                        analysis = analysis,
                        status = status,
                        title = analysis.sender.ifBlank { letter.title },
                    )
                database.letterDao().upsert(updated.toEntity())
                updated
            }

        override suspend fun confirmDeadline(
            letterId: String,
            index: Int,
        ) = updateDeadline(letterId, index) { copy(confirmed = true) }

        override suspend fun correctDeadline(
            letterId: String,
            index: Int,
            date: LocalDate,
        ) = updateDeadline(letterId, index) {
            copy(
                date = date,
                confirmed = true,
                rule = "Poprawione przez użytkownika.",
            )
        }

        override suspend fun addDeadline(
            letterId: String,
            deadline: Deadline,
        ) = withContext(Dispatchers.IO) {
            val letter = requireLetter(letterId)
            val analysis = letter.analysis ?: error("Najpierw przeanalizuj pismo.")
            val updated = letter.copy(analysis = analysis.copy(deadlines = analysis.deadlines + deadline))
            database.letterDao().upsert(updated.toEntity())
        }

        override suspend fun generateReply(
            letterId: String,
            scenario: ReplyScenario,
            polishFacts: String,
        ): Letter =
            withContext(Dispatchers.IO) {
                val letter = requireLetter(letterId)
                val analysis = letter.analysis ?: error("Najpierw przeanalizuj pismo.")
                if (ReplyPolicy.blocksReply(analysis.riskCategory)) {
                    error("Ta sprawa wymaga poradni. Generator jest zablokowany.")
                }
                val profile = currentProfile()
                val draft =
                    replies.generate(
                        ReplyRequest(
                            scenario = scenario,
                            polishFacts = polishFacts,
                            sender = analysis.sender,
                            aktenzeichen = analysis.aktenzeichen,
                            riskCategory = analysis.riskCategory,
                            senderName = profile.fullName,
                        ),
                    )
                val today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                val place = profile.city.ifBlank { "Ort" }
                val path =
                    pdfWriter.write(
                        Din5008Letter(
                            senderName = profile.fullName,
                            senderStreet = profile.street,
                            senderPostcodeCity =
                                listOf(profile.postcode, profile.city)
                                    .filter { it.isNotBlank() }
                                    .joinToString(" "),
                            recipientLines = listOf(analysis.sender),
                            placeAndDate = "$place, den $today",
                            aktenzeichen = analysis.aktenzeichen,
                            subject = draft.subject,
                            body = draft.germanBody,
                        ),
                    )
                val updated =
                    letter.copy(
                        reply = draft.copy(pdfPath = path),
                        status = LetterStatus.ReplyReady,
                    )
                database.letterDao().upsert(updated.toEntity())
                updated
            }

        override fun observeProfile(): Flow<UserProfile> =
            database.profileDao().observe().map { it?.toDomain() ?: UserProfile() }

        override suspend fun saveProfile(profile: UserProfile) =
            withContext(Dispatchers.IO) {
                database.profileDao().upsert(profile.toEntity())
            }

        override suspend fun deleteAll() =
            withContext(Dispatchers.IO) {
                database.letterDao().deleteAll()
                database.profileDao().deleteAll()
                File(context.filesDir, "letters").deleteRecursively()
                Unit
            }

        private suspend fun updateDeadline(
            letterId: String,
            index: Int,
            transform: Deadline.() -> Deadline,
        ) = withContext(Dispatchers.IO) {
            val letter = requireLetter(letterId)
            val analysis = letter.analysis ?: error("Brak analizy.")
            val deadlines = analysis.deadlines.toMutableList()
            deadlines[index] = deadlines[index].transform()
            database.letterDao().upsert(letter.copy(analysis = analysis.copy(deadlines = deadlines)).toEntity())
        }

        private suspend fun requireLetter(id: String): Letter =
            database.letterDao().get(id)?.toDomain() ?: error("Nie ma takiego pisma.")

        private suspend fun currentProfile(): UserProfile = database.profileDao().get()?.toDomain() ?: UserProfile()

        private companion object {
            const val TITLE_LIMIT = 80
        }
    }
