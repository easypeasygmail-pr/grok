package de.odpisz.core.network

import de.odpisz.core.model.ReplyScenario
import de.odpisz.core.model.RiskCategory
import de.odpisz.core.model.SampleLetters
import de.odpisz.core.model.Urgency
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.plugin
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FakeAnalyzerTest {
    private val analyzer = FakeLetterAnalyzer(today = { LocalDate.of(2026, 11, 3) })

    @Test
    fun finanzamtSampleRoundTripsThroughJsonSchemaFields() =
        runTest {
            val analysis = analyzer.analyze(SampleLetters.finanzamt)
            val encoded =
                AnalysisJson.encode(
                    AnalysisDto(
                        sender = analysis.sender,
                        whatTheyWant = analysis.whatTheyWant,
                        amounts =
                            listOf(
                                AmountDto(
                                    value =
                                        analysis.amounts
                                            .single()
                                            .value
                                            .toPlainString(),
                                    currency = "EUR",
                                    purpose = analysis.amounts.single().purpose,
                                    iban = analysis.amounts.single().iban,
                                    dueDate =
                                        analysis.amounts
                                            .single()
                                            .dueDate
                                            ?.toString(),
                                ),
                            ),
                        deadlines =
                            analysis.deadlines.map {
                                DeadlineDto(
                                    date = it.date.toString(),
                                    kind = "PAYMENT",
                                    sourceQuote = it.sourceQuote,
                                    rule = it.rule,
                                )
                            },
                        aktenzeichen = analysis.aktenzeichen,
                        urgency = "YELLOW",
                        riskCategory = "TAX",
                    ),
                )

            assertEquals("Finanzamt Berlin Mitte", analysis.sender)
            assertEquals(RiskCategory.Tax, analysis.riskCategory)
            assertEquals(Urgency.Yellow, analysis.urgency)
            assertEquals("12/345/67890", analysis.aktenzeichen)
            assertEquals(
                "186.40",
                analysis.amounts
                    .single()
                    .value
                    .toPlainString(),
            )
            assertEquals(LocalDate.of(2026, 11, 20), analysis.deadlines.single().date)
            assertTrue(
                analysis.deadlines
                    .single()
                    .sourceQuote
                    .contains("20.11.2026"),
            )
            assertTrue(analysis.whatTheyWant.contains("skarbow"))
            assertTrue(encoded.contains("\"urgency\" : \"YELLOW\"") || encoded.contains("\"urgency\": \"YELLOW\""))
            assertFalse(analysis.deadlines.single().confirmed)
        }

    @Test
    fun courtAndEnforcementAreRedAndBlocked() =
        runTest {
            val court = analyzer.analyze(SampleLetters.court)
            val enforcement = analyzer.analyze(SampleLetters.enforcement)
            assertEquals(RiskCategory.Court, court.riskCategory)
            assertEquals(Urgency.Red, court.urgency)
            assertEquals(RiskCategory.Enforcement, enforcement.riskCategory)
            assertTrue(
                court.deadlines
                    .single()
                    .sourceQuote
                    .contains("15.12.2026"),
            )
        }

    @Test
    fun httpClientInstallsTimeoutsAndHasNoApiKey() {
        val client = AnalyzerHttp.client("")
        val timeouts = client.plugin(HttpTimeout)
        assertNotNull(timeouts)
        client.close()
    }
}

class FakeReplyGeneratorTest {
    private val generator = FakeReplyGenerator()

    @Test
    fun widerspruchOnlyPreservesTheDeadline() =
        runTest {
            val draft =
                generator.generate(
                    ReplyRequest(
                        scenario = ReplyScenario.FristwahrenderWiderspruch,
                        polishFacts = "Dostałem pismo wczoraj.",
                        sender = "Amt",
                        aktenzeichen = "1/2",
                        riskCategory = RiskCategory.General,
                        senderName = "Jan",
                    ),
                )
            assertTrue(draft.germanBody.contains("Begründung folgt"))
            assertTrue(draft.germanBody.contains("Dostałem pismo wczoraj."))
            assertTrue(draft.polishBackTranslation.contains("Begründung folgt"))
            assertNoLegalArgument(draft.germanBody)
            assertNoLegalArgument(draft.polishBackTranslation)
        }

    @Test
    fun taxAppealAndWiderspruchOnTaxAreRejected() =
        runTest {
            val blocked =
                runCatching {
                    generator.generate(
                        ReplyRequest(
                            scenario = ReplyScenario.Erklaerung,
                            polishFacts = "fakty",
                            sender = "Finanzamt",
                            aktenzeichen = null,
                            riskCategory = RiskCategory.TaxAppeal,
                            senderName = "Jan",
                        ),
                    )
                }
            assertTrue(blocked.isFailure)
            val taxWiderspruch =
                runCatching {
                    generator.generate(
                        ReplyRequest(
                            scenario = ReplyScenario.FristwahrenderWiderspruch,
                            polishFacts = "fakty",
                            sender = "Finanzamt",
                            aktenzeichen = "9",
                            riskCategory = RiskCategory.Tax,
                            senderName = "Jan",
                        ),
                    )
                }
            assertTrue(taxWiderspruch.isFailure)
        }

    private fun assertNoLegalArgument(text: String) {
        listOf("rechtswidrig", "Anspruchsgrundlage", "Rechtsprechung", "unwirksam", "§").forEach {
            assertFalse(text.contains(it))
        }
    }
}
