package de.odpisz.feature.reply

import de.odpisz.core.model.ReplyDraft
import de.odpisz.core.model.ReplyScenario
import de.odpisz.core.model.RiskCategory

data class ReplyUiState(
    val status: Status = Status.Loading,
    val scenarios: List<ReplyScenario> = emptyList(),
    val selected: ReplyScenario? = null,
    val facts: String = "",
    val draft: ReplyDraft? = null,
    val risk: RiskCategory? = null,
    val message: String? = null,
) {
    enum class Status { Loading, Empty, Error, Content }
}

sealed interface ReplyUiEvent {
    data object Retry : ReplyUiEvent

    data class Select(
        val scenario: ReplyScenario,
    ) : ReplyUiEvent

    data class FactsChanged(
        val value: String,
    ) : ReplyUiEvent

    data object Generate : ReplyUiEvent

    data object Share : ReplyUiEvent
}
