package de.odpisz.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.odpisz.core.model.SettingsRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel
    @Inject
    constructor(
        private val settings: SettingsRepository,
    ) : ViewModel() {
        private val _state = MutableStateFlow(OnboardingUiState())
        val state = _state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), OnboardingUiState())
        private val _effects = Channel<Unit>(Channel.BUFFERED)
        val finished = _effects.receiveAsFlow()

        fun onEvent(event: OnboardingUiEvent) {
            when (event) {
                OnboardingUiEvent.Next -> {
                    _state.value =
                        _state.value.copy(
                            page = (_state.value.page + 1).coerceAtMost(_state.value.pageCount - 1),
                        )
                }

                OnboardingUiEvent.Back -> {
                    _state.value =
                        _state.value.copy(
                            page = (_state.value.page - 1).coerceAtLeast(0),
                        )
                }

                is OnboardingUiEvent.ConsentChanged -> {
                    _state.value = _state.value.copy(consentChecked = event.checked)
                }

                OnboardingUiEvent.Finish -> {
                    viewModelScope.launch {
                        if (_state.value.canFinish) {
                            settings.completeOnboarding()
                            _effects.send(Unit)
                        }
                    }
                }
            }
        }

        private companion object {
            const val STOP_MS = 5_000L
        }
    }
