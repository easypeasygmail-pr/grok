package de.odpisz.feature.letter

import de.odpisz.core.model.Letter

data class LetterUiState(
    val status: Status = Status.Loading,
    val letter: Letter? = null,
    val editingIndex: Int? = null,
    val editedDate: String = "",
    val message: String? = null,
) {
    enum class Status { Loading, Empty, Error, Content }
}

sealed interface LetterUiEvent {
    data object Retry : LetterUiEvent

    data class Confirm(
        val index: Int,
    ) : LetterUiEvent

    data class StartCorrect(
        val index: Int,
    ) : LetterUiEvent

    data class DateChanged(
        val value: String,
    ) : LetterUiEvent

    data object SaveDate : LetterUiEvent

    data class Calendar(
        val index: Int,
    ) : LetterUiEvent

    data object Reminders : LetterUiEvent

    data object Reply : LetterUiEvent
}

sealed interface LetterEffect {
    data class OpenReply(
        val id: String,
    ) : LetterEffect

    data class OpenRedCard(
        val id: String,
    ) : LetterEffect

    data class Message(
        val text: String,
    ) : LetterEffect
}
