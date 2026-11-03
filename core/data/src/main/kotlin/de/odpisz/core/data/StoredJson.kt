package de.odpisz.core.data

import de.odpisz.core.database.LetterEntity
import de.odpisz.core.database.ProfileEntity
import de.odpisz.core.model.Letter
import de.odpisz.core.model.LetterAnalysis
import de.odpisz.core.model.LetterStatus
import de.odpisz.core.model.PiiKind
import de.odpisz.core.model.PiiToken
import de.odpisz.core.model.ReplyDraft
import de.odpisz.core.model.ReplyScenario
import de.odpisz.core.model.RiskCategory
import de.odpisz.core.model.UserProfile
import de.odpisz.core.network.AmountDto
import de.odpisz.core.network.AnalysisDto
import de.odpisz.core.network.AnalysisJson
import de.odpisz.core.network.DeadlineDto
import de.odpisz.core.network.toDomain
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.time.Instant

private val storedJson = Json { ignoreUnknownKeys = false }

@Serializable
private data class ReplyStored(
    val scenario: String,
    val polishFacts: String,
    val subject: String,
    val germanBody: String,
    val polishBackTranslation: String,
    val pdfPath: String? = null,
)

@Serializable
private data class PiiStored(
    val token: String,
    val kind: String,
    val original: String,
)

fun Letter.toEntity(): LetterEntity =
    LetterEntity(
        id = id,
        createdAtEpochMillis = createdAt.toEpochMilli(),
        title = title,
        originalText = originalText,
        maskedText = maskedText,
        sourceLabel = sourceLabel,
        status = status.name,
        analysisJson = analysis?.let { AnalysisJson.encode(it.toDto()) },
        replyJson = reply?.let { storedJson.encodeToString(ReplyStored.serializer(), it.toStored()) },
        piiJson =
            storedJson.encodeToString(
                ListSerializer(PiiStored.serializer()),
                piiTokens.map { PiiStored(it.token, it.kind.name, it.original) },
            ),
        deadlineFlags =
            storedJson.encodeToString(
                ListSerializer(Boolean.serializer()),
                analysis?.deadlines?.map { it.confirmed }.orEmpty(),
            ),
    )

fun LetterEntity.toDomain(): Letter {
    val decoded = analysisJson?.let { AnalysisJson.decode(it).toDomain() }
    val flags = storedJson.decodeFromString(ListSerializer(Boolean.serializer()), deadlineFlags)
    return Letter(
        id = id,
        createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
        title = title,
        originalText = originalText,
        maskedText = maskedText,
        sourceLabel = sourceLabel,
        status = LetterStatus.valueOf(status),
        analysis =
            decoded?.copy(
                deadlines =
                    decoded.deadlines.mapIndexed { index, deadline ->
                        deadline.copy(confirmed = flags.getOrElse(index) { false })
                    },
            ),
        reply = replyJson?.let { storedJson.decodeFromString(ReplyStored.serializer(), it).toDomain() },
        piiTokens =
            storedJson
                .decodeFromString(ListSerializer(PiiStored.serializer()), piiJson)
                .map { PiiToken(it.token, PiiKind.valueOf(it.kind), it.original) },
    )
}

fun ProfileEntity.toDomain(): UserProfile =
    UserProfile(
        fullName = fullName,
        street = street,
        postcode = postcode,
        city = city,
        email = email,
        phone = phone,
    )

fun UserProfile.toEntity(): ProfileEntity =
    ProfileEntity(
        fullName = fullName,
        street = street,
        postcode = postcode,
        city = city,
        email = email,
        phone = phone,
    )

private fun LetterAnalysis.toDto(): AnalysisDto =
    AnalysisDto(
        sender = sender,
        whatTheyWant = whatTheyWant,
        amounts =
            amounts.map {
                AmountDto(
                    value = it.value.toPlainString(),
                    currency = it.currency,
                    purpose = it.purpose,
                    iban = it.iban,
                    dueDate = it.dueDate?.toString(),
                )
            },
        deadlines =
            deadlines.map {
                DeadlineDto(
                    date = it.date.toString(),
                    kind = it.kind.name.uppercase(),
                    sourceQuote = it.sourceQuote,
                    rule = it.rule,
                )
            },
        aktenzeichen = aktenzeichen,
        urgency = urgency.name.uppercase(),
        riskCategory =
            when (riskCategory) {
                RiskCategory.General -> "GENERAL"
                RiskCategory.Court -> "COURT"
                RiskCategory.Enforcement -> "ENFORCEMENT"
                RiskCategory.PaymentOrder -> "PAYMENT_ORDER"
                RiskCategory.Residence -> "RESIDENCE"
                RiskCategory.Tax -> "TAX"
                RiskCategory.TaxAppeal -> "TAX_APPEAL"
            },
    )

private fun ReplyDraft.toStored(): ReplyStored =
    ReplyStored(
        scenario = scenario.name,
        polishFacts = polishFacts,
        subject = subject,
        germanBody = germanBody,
        polishBackTranslation = polishBackTranslation,
        pdfPath = pdfPath,
    )

private fun ReplyStored.toDomain(): ReplyDraft =
    ReplyDraft(
        scenario = ReplyScenario.valueOf(scenario),
        polishFacts = polishFacts,
        subject = subject,
        germanBody = germanBody,
        polishBackTranslation = polishBackTranslation,
        pdfPath = pdfPath,
    )
