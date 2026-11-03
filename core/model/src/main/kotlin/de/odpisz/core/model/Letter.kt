package de.odpisz.core.model

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

enum class LetterStatus {
    Draft,
    Analyzed,
    ReplyReady,
    NeedsAdvisor,
}

enum class Urgency {
    Green,
    Yellow,
    Red,
}

/**
 * Risk classes that decide whether a template reply is allowed.
 *
 * Court, enforcement (Gerichtsvollzieher), payment order (Mahnbescheid),
 * residence matters and substantive tax appeals never reach the reply generator.
 * Ordinary tax letters may use neutral templates only (StBerG).
 */
enum class RiskCategory {
    General,
    Court,
    Enforcement,
    PaymentOrder,
    Residence,
    Tax,
    TaxAppeal,
}

enum class DeadlineKind {
    Response,
    Payment,
    Hearing,
    Other,
}

enum class ReplyScenario {
    Fristverlaengerung,
    RatenzahlungStundung,
    FristwahrenderWiderspruch,
    Erklaerung,
    AenderungDerDaten,
}

enum class PiiKind {
    Iban,
    SteuerId,
    Phone,
    Email,
    Postcode,
    UserName,
    UserAddress,
}

data class Deadline(
    val date: LocalDate,
    val kind: DeadlineKind,
    val sourceQuote: String,
    val rule: String,
    val confirmed: Boolean,
)

data class Amount(
    val value: BigDecimal,
    val currency: String,
    val purpose: String,
    val iban: String?,
    val dueDate: LocalDate?,
)

data class LetterAnalysis(
    val sender: String,
    val whatTheyWant: String,
    val amounts: List<Amount>,
    val deadlines: List<Deadline>,
    val aktenzeichen: String?,
    val urgency: Urgency,
    val riskCategory: RiskCategory,
)

data class ReplyDraft(
    val scenario: ReplyScenario,
    val polishFacts: String,
    val subject: String,
    val germanBody: String,
    val polishBackTranslation: String,
    val pdfPath: String?,
)

data class PiiToken(
    val token: String,
    val kind: PiiKind,
    val original: String,
)

data class UserProfile(
    val fullName: String = "",
    val street: String = "",
    val postcode: String = "",
    val city: String = "",
    val email: String = "",
    val phone: String = "",
) {
    val addressLine: String
        get() =
            listOf(street, listOf(postcode, city).filter { it.isNotBlank() }.joinToString(" "))
                .filter { it.isNotBlank() }
                .joinToString(", ")
}

data class Letter(
    val id: String,
    val createdAt: Instant,
    val title: String,
    val originalText: String,
    val maskedText: String,
    val sourceLabel: String?,
    val status: LetterStatus,
    val analysis: LetterAnalysis?,
    val reply: ReplyDraft?,
    val piiTokens: List<PiiToken>,
)

data class LetterSummary(
    val id: String,
    val createdAt: Instant,
    val title: String,
    val sender: String?,
    val status: LetterStatus,
    val urgency: Urgency?,
    val nearestDeadline: LocalDate?,
)

fun Letter.toSummary(): LetterSummary =
    LetterSummary(
        id = id,
        createdAt = createdAt,
        title = title,
        sender = analysis?.sender,
        status = status,
        urgency = analysis?.urgency,
        nearestDeadline = analysis?.deadlines?.minOfOrNull { it.date },
    )
