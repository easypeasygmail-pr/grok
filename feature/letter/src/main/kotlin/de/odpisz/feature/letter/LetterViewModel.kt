package de.odpisz.feature.letter

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.odpisz.core.model.ExternalActions
import de.odpisz.core.model.LetterRepository
import de.odpisz.core.model.ReminderScheduler
import de.odpisz.core.model.ReplyPolicy
import de.odpisz.feature.letter.LetterUiState.Status
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeParseException
import javax.inject.Inject

@HiltViewModel
class LetterViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repository: LetterRepository,
        private val reminders: ReminderScheduler,
        private val actions: ExternalActions,
    ) : ViewModel() {
        private val letterId: String = checkNotNull(savedStateHandle.get<String>("letterId"))
        private val _state = MutableStateFlow(LetterUiState())
        val state = _state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), LetterUiState())
        private val _effects = Channel<LetterEffect>(Channel.BUFFERED)
        val effects = _effects.receiveAsFlow()

        init {
            reload()
        }

        fun onEvent(event: LetterUiEvent) {
            when (event) {
                LetterUiEvent.Retry -> {
                    reload()
                }

                is LetterUiEvent.Confirm -> {
                    update { repository.confirmDeadline(letterId, event.index) }
                }

                is LetterUiEvent.StartCorrect -> {
                    _state.update {
                        it.copy(editingIndex = event.index, editedDate = "", message = null)
                    }
                }

                is LetterUiEvent.DateChanged -> {
                    _state.update { it.copy(editedDate = event.value) }
                }

                LetterUiEvent.SaveDate -> {
                    saveDate()
                }

                is LetterUiEvent.Calendar -> {
                    addToCalendar(event.index)
                }

                LetterUiEvent.Reminders -> {
                    scheduleReminders()
                }

                LetterUiEvent.Reply -> {
                    openReply()
                }
            }
        }

        private fun reload() {
            viewModelScope.launch {
                _state.update { it.copy(status = Status.Loading, message = null) }
                runCatching { repository.getLetter(letterId) }
                    .onSuccess { letter ->
                        _state.update {
                            if (letter == null) {
                                it.copy(status = Status.Empty, letter = null)
                            } else {
                                it.copy(status = Status.Content, letter = letter)
                            }
                        }
                    }.onFailure { error ->
                        _state.update { it.copy(status = Status.Error, message = error.message) }
                    }
            }
        }

        private fun update(block: suspend () -> Unit) {
            viewModelScope.launch {
                runCatching { block() }
                    .onSuccess { reload() }
                    .onFailure { error -> _state.update { it.copy(status = Status.Error, message = error.message) } }
            }
        }

        private fun saveDate() {
            val index = _state.value.editingIndex ?: return
            val date =
                try {
                    LocalDate.parse(_state.value.editedDate.trim())
                } catch (_: DateTimeParseException) {
                    _state.update { it.copy(message = "bad-date") }
                    return
                }
            viewModelScope.launch {
                runCatching { repository.correctDeadline(letterId, index, date) }
                    .onSuccess {
                        _state.update { it.copy(editingIndex = null, editedDate = "") }
                        reload()
                    }.onFailure { error -> _state.update { it.copy(status = Status.Error, message = error.message) } }
            }
        }

        private fun addToCalendar(index: Int) {
            val letter = _state.value.letter ?: return
            val deadline = letter.analysis?.deadlines?.getOrNull(index) ?: return
            runCatching {
                actions.addToCalendar(
                    title = "Odpisz.de: ${letter.title}",
                    date = deadline.date,
                    description = deadline.sourceQuote,
                )
            }.onFailure { error ->
                _state.update { it.copy(message = error.message) }
            }
        }

        private fun scheduleReminders() {
            val letter = _state.value.letter ?: return
            val deadlines = letter.analysis?.deadlines.orEmpty()
            reminders.schedule(letter.id, letter.title, deadlines)
            viewModelScope.launch { _effects.send(LetterEffect.Message("reminders")) }
        }

        private fun openReply() {
            val letter = _state.value.letter ?: return
            val category = letter.analysis?.riskCategory
            viewModelScope.launch {
                if (category != null && ReplyPolicy.blocksReply(category)) {
                    _effects.send(LetterEffect.OpenRedCard(letterId))
                } else {
                    _effects.send(LetterEffect.OpenReply(letterId))
                }
            }
        }

        private companion object {
            const val STOP_MS = 5_000L
        }
    }
