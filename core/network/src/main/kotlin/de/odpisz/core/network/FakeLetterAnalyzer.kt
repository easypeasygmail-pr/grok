package de.odpisz.core.network

import de.odpisz.core.model.LetterAnalysis
import de.odpisz.core.model.ReplyPolicy
import de.odpisz.core.model.RiskCategory
import de.odpisz.core.model.Urgency
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Offline stand-in for the future model. It classifies the masked text with
 * keywords and pulls dates, amounts and the Aktenzeichen out of the letter.
 * The result is always serialised through [AnalysisDto] so the JSON schema
 * stays the contract. It does not give legal advice.
 */
class FakeLetterAnalyzer(
    private val today: () -> LocalDate = { LocalDate.now() },
) : LetterAnalyzer {
    override suspend fun analyze(maskedText: String): LetterAnalysis {
        val dto = build(maskedText)
        val json = AnalysisJson.encode(dto)
        return AnalysisJson.decode(json).toDomain()
    }

    private fun build(text: String): AnalysisDto {
        val category = classify(text)
        val deadlines = extractDeadlines(text)
        val amounts = extractAmounts(text, deadlines.firstOrNull()?.date)
        return AnalysisDto(
            sender = firstLine(text),
            whatTheyWant = explain(category),
            amounts = amounts,
            deadlines = deadlines,
            aktenzeichen = extractFileNumber(text),
            urgency = urgency(category, deadlines.map { LocalDate.parse(it.date) }, today()).name.uppercase(),
            riskCategory = wireRisk(category),
        )
    }

    private fun classify(text: String): RiskCategory {
        val normalized = text.lowercase()
        return when {
            containsAny(normalized, "gerichtsvollzieher", "zwangsvollstreck") -> {
                RiskCategory.Enforcement
            }

            normalized.contains("mahnbescheid") -> {
                RiskCategory.PaymentOrder
            }

            containsAny(normalized, "ausländer", "auslaender", "aufenthaltstitel", "aufenthaltserlaubnis") -> {
                RiskCategory.Residence
            }

            containsAny(normalized, "einspruch", "widerspruch") &&
                containsAny(normalized, "steuer", "finanzamt", "einkommensteuer") -> {
                RiskCategory.TaxAppeal
            }

            containsAny(normalized, "finanzamt", "steuerbescheid", "einkommensteuer") -> {
                RiskCategory.Tax
            }

            containsAny(normalized, "amtsgericht", "landgericht", "klage", "gericht") -> {
                RiskCategory.Court
            }

            else -> {
                RiskCategory.General
            }
        }
    }

    private fun explain(category: RiskCategory): String =
        when (category) {
            RiskCategory.Tax -> {
                "Urząd skarbowy podaje kwotę i termin zapłaty. To nie jest porada podatkowa."
            }

            RiskCategory.TaxAppeal -> {
                "Pismo wygląda na merytoryczne odwołanie podatkowe. Szablon odpowiedzi jest zablokowany."
            }

            RiskCategory.Court -> {
                "Pismo pochodzi z sądu albo dotyczy pozwu. Odpowiedź musi przygotować człowiek."
            }

            RiskCategory.Enforcement -> {
                "Pismo dotyczy egzekucji (Gerichtsvollzieher). Nie piszemy odpowiedzi z szablonu."
            }

            RiskCategory.PaymentOrder -> {
                "To nakaz zapłaty (Mahnbescheid). Termin jest krótki, a szablon odpowiedzi jest zablokowany."
            }

            RiskCategory.Residence -> {
                "Pismo dotyczy pobytu. Tego nie wolno załatwiać szablonem."
            }

            RiskCategory.General -> {
                "Nadawca opisuje sprawę i prosi o reakcję. Sprawdź termin i cytat w piśmie."
            }
        }

    private fun extractDeadlines(text: String): List<DeadlineDto> {
        val found = linkedMapOf<LocalDate, DeadlineDto>()
        for (match in GERMAN_DATE.findAll(text)) {
            val date = parseGerman(match) ?: continue
            val quote = lineOf(text, match.range.first)
            found.putIfAbsent(
                date,
                DeadlineDto(
                    date = date.toString(),
                    kind = kindFor(quote),
                    sourceQuote = quote,
                    rule = "Data odczytana z pisma. Potwierdź ją albo popraw.",
                ),
            )
        }
        for (match in ISO_DATE.findAll(text)) {
            val date = runCatching { LocalDate.parse(match.value) }.getOrNull() ?: continue
            val quote = lineOf(text, match.range.first)
            found.putIfAbsent(
                date,
                DeadlineDto(
                    date = date.toString(),
                    kind = kindFor(quote),
                    sourceQuote = quote,
                    rule = "Data odczytana z pisma. Potwierdź ją albo popraw.",
                ),
            )
        }
        return found.values.toList()
    }

    private fun extractAmounts(
        text: String,
        due: String?,
    ): List<AmountDto> {
        val values =
            AMOUNT
                .findAll(text)
                .map { it.groupValues[1] }
                .distinct()
                .toList()
        if (values.isEmpty()) return emptyList()
        val ibanToken = IBAN_TOKEN.find(text)?.value
        return values.map { raw ->
            AmountDto(
                value = germanToDot(raw),
                currency = "EUR",
                purpose = "Kwota z pisma",
                iban = ibanToken,
                dueDate = due,
            )
        }
    }

    private fun extractFileNumber(text: String): String? {
        val match = FILE_NUMBER.find(text) ?: return null
        return match.groupValues[1].trim().trimEnd('.', ',', ';')
    }

    private fun urgency(
        category: RiskCategory,
        dates: List<LocalDate>,
        today: LocalDate,
    ): Urgency {
        if (ReplyPolicy.blocksReply(category)) return Urgency.Red
        val nearest = dates.minOrNull() ?: return Urgency.Green
        val days = ChronoUnit.DAYS.between(today, nearest)
        return when {
            days <= WEEK_DAYS -> Urgency.Red
            days <= YELLOW_DAYS -> Urgency.Yellow
            else -> Urgency.Green
        }
    }

    private fun wireRisk(category: RiskCategory): String =
        when (category) {
            RiskCategory.General -> "GENERAL"
            RiskCategory.Court -> "COURT"
            RiskCategory.Enforcement -> "ENFORCEMENT"
            RiskCategory.PaymentOrder -> "PAYMENT_ORDER"
            RiskCategory.Residence -> "RESIDENCE"
            RiskCategory.Tax -> "TAX"
            RiskCategory.TaxAppeal -> "TAX_APPEAL"
        }

    private fun kindFor(quote: String): String {
        val lower = quote.lowercase()
        return when {
            containsAny(lower, "zahl", "eur", "betrag") -> "PAYMENT"
            containsAny(lower, "verhandlung", "termin") -> "HEARING"
            containsAny(lower, "frist", "stellungnahme") -> "RESPONSE"
            else -> "OTHER"
        }
    }

    private fun firstLine(text: String): String =
        text
            .lineSequence()
            .firstOrNull { it.isNotBlank() }
            ?.trim()
            ?.take(MAX_SENDER) ?: "Nieznany nadawca"

    private fun lineOf(
        text: String,
        index: Int,
    ): String {
        val start = text.lastIndexOf('\n', index).let { if (it < 0) 0 else it + 1 }
        val end = text.indexOf('\n', index).let { if (it < 0) text.length else it }
        val line = text.substring(start, end).trim()
        return line.ifBlank { text.trim().take(MAX_QUOTE) }
    }

    private fun parseGerman(match: MatchResult): LocalDate? =
        runCatching {
            LocalDate.of(
                match.groupValues[3].toInt(),
                match.groupValues[2].toInt(),
                match.groupValues[1].toInt(),
            )
        }.getOrNull()

    private fun germanToDot(raw: String): String = raw.replace(".", "").replace(',', '.')

    private fun containsAny(
        text: String,
        vararg needles: String,
    ): Boolean = needles.any { text.contains(it) }

    private companion object {
        const val WEEK_DAYS = 7L
        const val YELLOW_DAYS = 21L
        const val MAX_SENDER = 80
        const val MAX_QUOTE = 160
        val GERMAN_DATE: Regex = Regex("""\b([0-3]?[0-9])\.([01]?[0-9])\.([0-9]{4})\b""")
        val ISO_DATE: Regex = Regex("""\b[0-9]{4}-[0-9]{2}-[0-9]{2}\b""")
        val AMOUNT: Regex = Regex("""([0-9]{1,3}(?:\.[0-9]{3})*,[0-9]{2}|[0-9]+\.[0-9]{2})\s*(?:EUR|€)""")
        val FILE_NUMBER: Regex =
            Regex(
                """(?i)(?:aktenzeichen|geschäftszeichen|geschaeftszeichen|az\.?)\s*[:.]?\s*([A-Za-z0-9][A-Za-z0-9 ./+\-]{2,40})""",
            )
        val IBAN_TOKEN: Regex = Regex("""\[IBAN_[0-9]+]""")
    }
}
