package de.odpisz.feature.review

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.odpisz.core.model.LetterRepository
import de.odpisz.feature.review.ReviewUiState.Status
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReviewViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repository: LetterRepository,
    ) : ViewModel() {
        private val letterId: String = checkNotNull(savedStateHandle.get<String>("letterId"))
        private val _state = MutableStateFlow(ReviewUiState())
        val state = _state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), ReviewUiState())
        private val _effects = Channel<ReviewEffect>(Channel.BUFFERED)
        val effects = _effects.receiveAsFlow()

        init {
            reload()
        }

        fun onEvent(event: ReviewUiEvent) {
            when (event) {
                ReviewUiEvent.Retry -> reload()
                ReviewUiEvent.ToggleOriginal -> _state.update { it.copy(showOriginal = !it.showOriginal) }
                ReviewUiEvent.Analyze -> analyze()
            }
        }

        private fun reload() {
            viewModelScope.launch {
                _state.update { it.copy(status = Status.Loading) }
                runCatching { repository.getLetter(letterId) }
                    .onSuccess { letter ->
                        if (letter == null) {
                            _state.update { it.copy(status = Status.Empty) }
                        } else {
                            _state.update {
                                it.copy(
                                    status = Status.Content,
                                    maskedText = letter.maskedText,
                                    originalText = letter.originalText,
                                    tokens = letter.piiTokens,
                                )
                            }
                        }
                    }.onFailure { error ->
                        _state.update { it.copy(status = Status.Error, message = error.message) }
                    }
            }
        }

        private fun analyze() {
            viewModelScope.launch {
                _state.update { it.copy(status = Status.Loading) }
                runCatching { repository.analyze(letterId) }
                    .onSuccess { _effects.send(ReviewEffect.OpenLetter(letterId)) }
                    .onFailure { error ->
                        _state.update { it.copy(status = Status.Error, message = error.message) }
                    }
            }
        }

        private companion object {
            const val STOP_MS = 5_000L
        }
    }
