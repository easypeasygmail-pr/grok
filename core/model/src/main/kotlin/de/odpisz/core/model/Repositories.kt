package de.odpisz.core.model

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface LetterRepository {
    fun observeLetters(): Flow<List<LetterSummary>>

    suspend fun getLetter(id: String): Letter?

    suspend fun importText(
        text: String,
        sourceLabel: String?,
    ): String

    suspend fun importUri(uri: String): String

    suspend fun analyze(letterId: String): Letter

    suspend fun confirmDeadline(
        letterId: String,
        index: Int,
    )

    suspend fun correctDeadline(
        letterId: String,
        index: Int,
        date: LocalDate,
    )

    suspend fun addDeadline(
        letterId: String,
        deadline: Deadline,
    )

    suspend fun generateReply(
        letterId: String,
        scenario: ReplyScenario,
        polishFacts: String,
    ): Letter

    fun observeProfile(): Flow<UserProfile>

    suspend fun saveProfile(profile: UserProfile)

    suspend fun deleteAll()
}

interface SettingsRepository {
    val onboardingCompleted: Flow<Boolean>

    val biometricLockEnabled: Flow<Boolean>

    val art9AcceptedAt: Flow<Long?>

    suspend fun completeOnboarding()

    suspend fun setBiometricLock(enabled: Boolean)

    suspend fun clear()
}

interface ReminderScheduler {
    fun schedule(
        letterId: String,
        title: String,
        deadlines: List<Deadline>,
    )

    fun cancelAll()
}

interface ExternalActions {
    fun addToCalendar(
        title: String,
        date: LocalDate,
        description: String,
    )

    fun sharePdf(absolutePath: String)
}

interface DocumentTextReader {
    suspend fun read(uri: String): String
}

interface LetterPdfWriter {
    suspend fun write(letter: Din5008Letter): String
}
