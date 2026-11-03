package de.odpisz.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.odpisz.core.model.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class AppStart { Onboarding, History }

data class GateUiState(
    val ready: Boolean = false,
    val start: AppStart = AppStart.Onboarding,
    val locked: Boolean = false,
)

@HiltViewModel
class AppGateViewModel
    @Inject
    constructor(
        settings: SettingsRepository,
        private val session: UnlockSession,
    ) : ViewModel() {
        val gate =
            combine(
                settings.onboardingCompleted,
                settings.biometricLockEnabled,
                session.unlocked,
            ) { onboarded, biometric, unlocked ->
                GateUiState(
                    ready = true,
                    start = if (onboarded) AppStart.History else AppStart.Onboarding,
                    locked = onboarded && biometric && !unlocked,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), GateUiState())

        fun unlock() {
            session.unlock()
        }

        private companion object {
            const val STOP_MS = 5_000L
        }
    }
