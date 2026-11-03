package de.odpisz.feature.review

import de.odpisz.core.model.PiiToken

data class ReviewUiState(
    val status: Status = Status.Loading,
    val maskedText: String = "",
    val originalText: String = "",
    val tokens: List<PiiToken> = emptyList(),
    val showOriginal: Boolean = false,
    val message: String? = null,
) {
    enum class Status { Loading, Empty, Error, Content }
}

sealed interface ReviewUiEvent {
    data object Retry : ReviewUiEvent

    data object ToggleOriginal : ReviewUiEvent

    data object Analyze : ReviewUiEvent
}

sealed interface ReviewEffect {
    data class OpenLetter(
        val id: String,
    ) : ReviewEffect
}
