package de.odpisz.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.odpisz.core.model.LetterRepository
import de.odpisz.feature.history.HistoryUiState.Status
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel
    @Inject
    constructor(
        repository: LetterRepository,
    ) : ViewModel() {
        private val query = MutableStateFlow("")
        private val attempts = MutableStateFlow(0)

        val state =
            attempts
                .flatMapLatest {
                    repository
                        .observeLetters()
                        .map { letters ->
                            val q = query.value
                            val filtered =
                                if (q.isBlank()) {
                                    letters
                                } else {
                                    letters.filter { letter ->
                                        letter.title.contains(q, ignoreCase = true) ||
                                            letter.sender?.contains(q, ignoreCase = true) == true
                                    }
                                }
                            HistoryUiState(
                                status = if (filtered.isEmpty()) Status.Empty else Status.Content,
                                letters = filtered,
                                query = q,
                            )
                        }.catch { error ->
                            emit(
                                HistoryUiState(
                                    status = Status.Error,
                                    query = query.value,
                                    message = error.message ?: "Błąd odczytu.",
                                ),
                            )
                        }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), HistoryUiState())

        fun onEvent(event: HistoryUiEvent) {
            when (event) {
                HistoryUiEvent.Retry -> {
                    attempts.update { it + 1 }
                }

                is HistoryUiEvent.QueryChanged -> {
                    query.value = event.query
                    attempts.update { it + 1 }
                }

                is HistoryUiEvent.Open -> {
                    Unit
                }
            }
        }

        private companion object {
            const val STOP_MS = 5_000L
        }
    }
