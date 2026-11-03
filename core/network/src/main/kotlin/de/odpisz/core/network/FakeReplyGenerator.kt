package de.odpisz.core.network

import de.odpisz.core.model.ReplyDraft
import de.odpisz.core.model.ReplyPolicy
import de.odpisz.core.model.ReplyScenario

/**
 * Fills a fixed template. The user's facts are quoted, not turned into a legal
 * argument. Phase 0 does not translate those facts; the Polish back-translation
 * explains the German template and repeats the facts the user wrote.
 */
class FakeReplyGenerator : ReplyGenerator {
    override suspend fun generate(request: ReplyRequest): ReplyDraft {
        check(!ReplyPolicy.blocksReply(request.riskCategory)) {
            "Generator odpowiedzi jest zablokowany dla ${request.riskCategory}."
        }
        check(request.scenario in ReplyPolicy.allowedScenarios(request.riskCategory)) {
            "Scenariusz ${request.scenario} nie jest dozwolony dla ${request.riskCategory}."
        }
        val facts = request.polishFacts.trim().ifBlank { "Nie podałem dodatkowych faktów." }
        val template = template(request.scenario)
        return ReplyDraft(
            scenario = request.scenario,
            polishFacts = facts,
            subject = template.subject(request),
            germanBody = template.german(request, facts),
            polishBackTranslation = template.polish(request, facts),
            pdfPath = null,
        )
    }

    private fun template(scenario: ReplyScenario): ReplyTemplate =
        when (scenario) {
            ReplyScenario.Fristverlaengerung -> FristTemplate
            ReplyScenario.RatenzahlungStundung -> RateTemplate
            ReplyScenario.FristwahrenderWiderspruch -> WiderspruchTemplate
            ReplyScenario.Erklaerung -> ErklaerungTemplate
            ReplyScenario.AenderungDerDaten -> DatenTemplate
        }
}

private interface ReplyTemplate {
    fun subject(request: ReplyRequest): String

    fun german(
        request: ReplyRequest,
        facts: String,
    ): String

    fun polish(
        request: ReplyRequest,
        facts: String,
    ): String
}

private fun fileRef(request: ReplyRequest): String = request.aktenzeichen?.let { "Az. $it" } ?: "ohne Aktenzeichen"

private fun factsBlock(facts: String): String =
    """
    Eigene Angaben (vom Nutzer auf Polnisch erfasst).
    Eine spätere Version kann sie übersetzen. Dieser Brief bewertet sie nicht rechtlich:
    "$facts"
    """.trimIndent()

private object FristTemplate : ReplyTemplate {
    override fun subject(request: ReplyRequest): String = "Bitte um Fristverlängerung, ${fileRef(request)}"

    override fun german(
        request: ReplyRequest,
        facts: String,
    ): String =
        """
        Sehr geehrte Damen und Herren,

        hiermit bitte ich um Verlängerung der Frist in der Angelegenheit ${fileRef(request)}.

        ${factsBlock(facts)}

        Ich bitte um eine kurze schriftliche Bestätigung des neuen Termins.

        Mit freundlichen Grüßen
        ${request.senderName.ifBlank { "[USER_NAME]" }}
        """.trimIndent()

    override fun polish(
        request: ReplyRequest,
        facts: String,
    ): String =
        """
        Proszę o przedłużenie terminu w sprawie ${fileRef(request)}.
        Nie podaję podstawy prawnej. Proszę tylko o pisemne potwierdzenie nowego terminu.

        Moje fakty, bez zmian:
        $facts
        """.trimIndent()
}

private object RateTemplate : ReplyTemplate {
    override fun subject(request: ReplyRequest): String = "Bitte um Ratenzahlung oder Stundung, ${fileRef(request)}"

    override fun german(
        request: ReplyRequest,
        facts: String,
    ): String =
        """
        Sehr geehrte Damen und Herren,

        hiermit bitte ich um Ratenzahlung oder Stundung in der Angelegenheit ${fileRef(request)}.

        ${factsBlock(facts)}

        Ich bitte um Mitteilung, ob eine Aufteilung der Zahlung möglich ist.

        Mit freundlichen Grüßen
        ${request.senderName.ifBlank { "[USER_NAME]" }}
        """.trimIndent()

    override fun polish(
        request: ReplyRequest,
        facts: String,
    ): String =
        """
        Proszę o rozłożenie płatności na raty albo o odroczenie terminu w sprawie ${fileRef(request)}.
        To prośba, nie argument prawny.

        Moje fakty, bez zmian:
        $facts
        """.trimIndent()
}

private object WiderspruchTemplate : ReplyTemplate {
    override fun subject(request: ReplyRequest): String =
        "Fristwahrender Widerspruch, Begründung folgt, ${fileRef(request)}"

    override fun german(
        request: ReplyRequest,
        facts: String,
    ): String =
        """
        Sehr geehrte Damen und Herren,

        hiermit lege ich fristwahrend Widerspruch ein.
        Begründung folgt.

        ${factsBlock(facts)}

        Mit freundlichen Grüßen
        ${request.senderName.ifBlank { "[USER_NAME]" }}
        """.trimIndent()

    override fun polish(
        request: ReplyRequest,
        facts: String,
    ): String =
        """
        Składam sprzeciw tylko po to, żeby zachować termin. Begründung folgt — uzasadnienie dopiszę później.
        W tym liście nie ma argumentu prawnego.

        Moje fakty, bez zmian:
        $facts
        """.trimIndent()
}

private object ErklaerungTemplate : ReplyTemplate {
    override fun subject(request: ReplyRequest): String = "Sachverhaltsangaben, ${fileRef(request)}"

    override fun german(
        request: ReplyRequest,
        facts: String,
    ): String =
        """
        Sehr geehrte Damen und Herren,

        zu Ihrem Schreiben teile ich Folgendes mit.

        ${factsBlock(facts)}

        Mit freundlichen Grüßen
        ${request.senderName.ifBlank { "[USER_NAME]" }}
        """.trimIndent()

    override fun polish(
        request: ReplyRequest,
        facts: String,
    ): String =
        """
        Przekazuję tylko opis faktów do pisma ${fileRef(request)}. Bez oceny prawnej.

        Moje fakty, bez zmian:
        $facts
        """.trimIndent()
}

private object DatenTemplate : ReplyTemplate {
    override fun subject(request: ReplyRequest): String = "Änderung meiner Daten, ${fileRef(request)}"

    override fun german(
        request: ReplyRequest,
        facts: String,
    ): String =
        """
        Sehr geehrte Damen und Herren,

        bitte ändern Sie meine gespeicherten Daten wie folgt.

        ${factsBlock(facts)}

        Ich bitte um eine kurze Bestätigung der Änderung.

        Mit freundlichen Grüßen
        ${request.senderName.ifBlank { "[USER_NAME]" }}
        """.trimIndent()

    override fun polish(
        request: ReplyRequest,
        facts: String,
    ): String =
        """
        Proszę o zmianę moich danych w sprawie ${fileRef(request)} i o krótkie potwierdzenie.
        Nowe dane są wyłącznie tym, co sam wpisałem.

        Moje fakty, bez zmian:
        $facts
        """.trimIndent()
}
