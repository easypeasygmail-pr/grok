package de.odpisz.core.model

/**
 * Product rules for phase 0.
 *
 * The generator fills a template or places the user's own facts in it.
 * It does not invent legal argumentation (RDG). Tax letters stay on neutral
 * templates (StBerG): no substantive appeal, no Widerspruch.
 */
object ReplyPolicy {
    fun blocksReply(category: RiskCategory): Boolean =
        when (category) {
            RiskCategory.Court,
            RiskCategory.Enforcement,
            RiskCategory.PaymentOrder,
            RiskCategory.Residence,
            RiskCategory.TaxAppeal,
            -> true

            RiskCategory.General,
            RiskCategory.Tax,
            -> false
        }

    fun allowedScenarios(category: RiskCategory): List<ReplyScenario> {
        if (blocksReply(category)) return emptyList()
        if (category == RiskCategory.Tax) {
            return listOf(
                ReplyScenario.Fristverlaengerung,
                ReplyScenario.RatenzahlungStundung,
                ReplyScenario.Erklaerung,
                ReplyScenario.AenderungDerDaten,
            )
        }
        return ReplyScenario.entries
    }
}
