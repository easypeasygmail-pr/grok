package de.odpisz.feature.scan

import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.odpisz.core.model.LetterRepository
import de.odpisz.core.model.SampleLetters
import de.odpisz.core.ocr.PlayDocumentScanner
import de.odpisz.feature.scan.ScanUiState.Status
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScanViewModel
    @Inject
    constructor(
        private val repository: LetterRepository,
        private val scanner: PlayDocumentScanner,
    ) : ViewModel() {
        private val _state = MutableStateFlow(ScanUiState())
        val state = _state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), ScanUiState())
        private val _effects = Channel<ScanEffect>(Channel.BUFFERED)
        val effects = _effects.receiveAsFlow()

        fun onEvent(event: ScanUiEvent) {
            when (event) {
                ScanUiEvent.SampleTax -> {
                    import(SampleLetters.finanzamt, "przyklad-finanzamt")
                }

                ScanUiEvent.SampleCourt -> {
                    import(SampleLetters.court, "przyklad-sad")
                }

                is ScanUiEvent.PasteChanged -> {
                    _state.update { it.copy(pasted = event.value, status = Status.Content) }
                }

                ScanUiEvent.ImportPaste -> {
                    import(_state.value.pasted, "wklejony-tekst")
                }

                is ScanUiEvent.ImportUri -> {
                    importUri(event.uri)
                }

                is ScanUiEvent.ScanFinished -> {
                    val uri = scanner.resultUri(event.resultCode, event.data)
                    if (uri == null) {
                        _state.update { it.copy(status = Status.Empty) }
                    } else {
                        importUri(uri)
                    }
                }

                ScanUiEvent.Retry -> {
                    _state.update { it.copy(status = Status.Empty, message = null) }
                }
            }
        }

        suspend fun scanIntent(activity: ComponentActivity) = scanner.scanIntent(activity)

        fun onScanUnavailable(message: String) {
            _state.update { it.copy(status = Status.Error, message = message) }
        }

        private fun import(
            text: String,
            label: String,
        ) {
            if (text.isBlank()) return
            viewModelScope.launch {
                _state.update { it.copy(status = Status.Loading) }
                runCatching { repository.importText(text, label) }
                    .onSuccess { id ->
                        _state.update { it.copy(status = Status.Empty, pasted = "") }
                        _effects.send(ScanEffect.OpenReview(id))
                    }.onFailure { error ->
                        _state.update { it.copy(status = Status.Error, message = error.message) }
                    }
            }
        }

        private fun importUri(uri: String) {
            viewModelScope.launch {
                _state.update { it.copy(status = Status.Loading) }
                runCatching { repository.importUri(uri) }
                    .onSuccess { id ->
                        _state.update { it.copy(status = Status.Empty) }
                        _effects.send(ScanEffect.OpenReview(id))
                    }.onFailure { error ->
                        _state.update { it.copy(status = Status.Error, message = error.message) }
                    }
            }
        }

        private companion object {
            const val STOP_MS = 5_000L
        }
    }
