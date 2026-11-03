package de.odpisz.core.network

import de.odpisz.core.model.LetterAnalysis
import de.odpisz.core.model.ReplyDraft
import de.odpisz.core.model.ReplyScenario
import de.odpisz.core.model.RiskCategory

interface LetterAnalyzer {
    suspend fun analyze(maskedText: String): LetterAnalysis
}

interface ReplyGenerator {
    suspend fun generate(request: ReplyRequest): ReplyDraft
}

data class ReplyRequest(
    val scenario: ReplyScenario,
    val polishFacts: String,
    val sender: String,
    val aktenzeichen: String?,
    val riskCategory: RiskCategory,
    val senderName: String,
)
