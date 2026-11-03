package de.odpisz.core.network

import de.odpisz.core.model.Amount
import de.odpisz.core.model.Deadline
import de.odpisz.core.model.DeadlineKind
import de.odpisz.core.model.LetterAnalysis
import de.odpisz.core.model.RiskCategory
import de.odpisz.core.model.Urgency
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Wire format for letter analysis. The matching JSON Schema is
 * `schema/letter-analysis.schema.json`. `confirmed` is a local user flag and
 * is intentionally absent here.
 */
@Serializable
data class AnalysisDto(
    val sender: String,
    val whatTheyWant: String,
    val amounts: List<AmountDto>,
    val deadlines: List<DeadlineDto>,
    val aktenzeichen: String?,
    val urgency: String,
    val riskCategory: String,
)

@Serializable
data class AmountDto(
    val value: String,
    val currency: String,
    val purpose: String,
    val iban: String? = null,
    val dueDate: String? = null,
)

@Serializable
data class DeadlineDto(
    val date: String,
    val kind: String,
    val sourceQuote: String,
    val rule: String,
)

object AnalysisJson {
    val format: Json =
        Json {
            ignoreUnknownKeys = false
            explicitNulls = true
            encodeDefaults = true
            prettyPrint = true
        }

    fun encode(dto: AnalysisDto): String = format.encodeToString(AnalysisDto.serializer(), dto)

    fun decode(json: String): AnalysisDto = format.decodeFromString(AnalysisDto.serializer(), json)
}

fun AnalysisDto.toDomain(): LetterAnalysis =
    LetterAnalysis(
        sender = sender,
        whatTheyWant = whatTheyWant,
        amounts = amounts.map { it.toDomain() },
        deadlines = deadlines.map { it.toDomain() },
        aktenzeichen = aktenzeichen,
        urgency = urgencyFromWire(urgency),
        riskCategory = riskFromWire(riskCategory),
    )

private fun urgencyFromWire(value: String): Urgency =
    when (value.uppercase()) {
        "GREEN" -> Urgency.Green
        "YELLOW" -> Urgency.Yellow
        "RED" -> Urgency.Red
        else -> error("Unknown urgency: $value")
    }

private fun riskFromWire(value: String): RiskCategory =
    when (value.uppercase()) {
        "GENERAL" -> RiskCategory.General
        "COURT" -> RiskCategory.Court
        "ENFORCEMENT" -> RiskCategory.Enforcement
        "PAYMENT_ORDER" -> RiskCategory.PaymentOrder
        "RESIDENCE" -> RiskCategory.Residence
        "TAX" -> RiskCategory.Tax
        "TAX_APPEAL" -> RiskCategory.TaxAppeal
        else -> error("Unknown risk category: $value")
    }

private fun AmountDto.toDomain(): Amount =
    Amount(
        value = BigDecimal(value),
        currency = currency,
        purpose = purpose,
        iban = iban,
        dueDate = dueDate?.let(LocalDate::parse),
    )

private fun DeadlineDto.toDomain(): Deadline {
    require(sourceQuote.isNotBlank()) { "sourceQuote is required" }
    return Deadline(
        date = LocalDate.parse(date),
        kind =
            when (kind.uppercase()) {
                "RESPONSE" -> DeadlineKind.Response
                "PAYMENT" -> DeadlineKind.Payment
                "HEARING" -> DeadlineKind.Hearing
                "OTHER" -> DeadlineKind.Other
                else -> error("Unknown deadline kind: $kind")
            },
        sourceQuote = sourceQuote,
        rule = rule,
        confirmed = false,
    )
}
