package de.odpisz.core.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplyPolicyTest {
    @Test
    fun blocksCourtEnforcementPaymentOrderResidenceAndTaxAppeal() {
        listOf(
            RiskCategory.Court,
            RiskCategory.Enforcement,
            RiskCategory.PaymentOrder,
            RiskCategory.Residence,
            RiskCategory.TaxAppeal,
        ).forEach { category ->
            assertTrue(category.name, ReplyPolicy.blocksReply(category))
            assertTrue(ReplyPolicy.allowedScenarios(category).isEmpty())
        }
    }

    @Test
    fun taxUsesNeutralTemplatesWithoutWiderspruch() {
        val allowed = ReplyPolicy.allowedScenarios(RiskCategory.Tax)
        assertFalse(ReplyPolicy.blocksReply(RiskCategory.Tax))
        assertFalse(allowed.contains(ReplyScenario.FristwahrenderWiderspruch))
        assertTrue(allowed.contains(ReplyScenario.RatenzahlungStundung))
        assertTrue(allowed.contains(ReplyScenario.Erklaerung))
    }

    @Test
    fun generalAllowsEveryScenario() {
        assertEqualsScenarios(ReplyScenario.entries, ReplyPolicy.allowedScenarios(RiskCategory.General))
    }

    private fun assertEqualsScenarios(
        expected: List<ReplyScenario>,
        actual: List<ReplyScenario>,
    ) {
        org.junit.Assert.assertEquals(expected, actual)
    }
}
