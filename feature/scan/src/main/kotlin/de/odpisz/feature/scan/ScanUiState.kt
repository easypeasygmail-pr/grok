package de.odpisz.feature.scan

import android.content.Intent

data class ScanUiState(
    val status: Status = Status.Empty,
    val pasted: String = "",
    val message: String? = null,
) {
    enum class Status { Loading, Empty, Error, Content }
}

sealed interface ScanUiEvent {
    data object SampleTax : ScanUiEvent

    data object SampleCourt : ScanUiEvent

    data class PasteChanged(
        val value: String,
    ) : ScanUiEvent

    data object ImportPaste : ScanUiEvent

    data class ImportUri(
        val uri: String,
    ) : ScanUiEvent

    data class ScanFinished(
        val resultCode: Int,
        val data: Intent?,
    ) : ScanUiEvent

    data object Retry : ScanUiEvent
}

sealed interface ScanEffect {
    data class OpenReview(
        val letterId: String,
    ) : ScanEffect
}
