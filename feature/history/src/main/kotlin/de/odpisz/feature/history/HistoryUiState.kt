package de.odpisz.feature.history

import de.odpisz.core.model.LetterSummary

data class HistoryUiState(
    val status: Status = Status.Loading,
    val letters: List<LetterSummary> = emptyList(),
    val query: String = "",
    val message: String? = null,
) {
    enum class Status { Loading, Empty, Error, Content }
}

sealed interface HistoryUiEvent {
    data object Retry : HistoryUiEvent

    data class QueryChanged(
        val query: String,
    ) : HistoryUiEvent

    data class Open(
        val id: String,
    ) : HistoryUiEvent
}
