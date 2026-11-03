package de.odpisz.feature.reply

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.odpisz.core.model.ExternalActions
import de.odpisz.core.model.LetterRepository
import de.odpisz.core.model.ReplyPolicy
import de.odpisz.feature.reply.ReplyUiState.Status
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReplyViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repository: LetterRepository,
        private val actions: ExternalActions,
    ) : ViewModel() {
        private val letterId: String = checkNotNull(savedStateHandle.get<String>("letterId"))
        private val _state = MutableStateFlow(ReplyUiState())
        val state = _state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), ReplyUiState())

        init {
            reload()
        }

        fun onEvent(event: ReplyUiEvent) {
            when (event) {
                ReplyUiEvent.Retry -> reload()
                is ReplyUiEvent.Select -> _state.update { it.copy(selected = event.scenario) }
                is ReplyUiEvent.FactsChanged -> _state.update { it.copy(facts = event.value) }
                ReplyUiEvent.Generate -> generate()
                ReplyUiEvent.Share -> share()
            }
        }

        private fun reload() {
            viewModelScope.launch {
                _state.update { it.copy(status = Status.Loading) }
                runCatching { repository.getLetter(letterId) }
                    .onSuccess { letter ->
                        val category = letter?.analysis?.riskCategory
                        if (letter == null || category == null) {
                            _state.update { it.copy(status = Status.Empty) }
                            return@onSuccess
                        }
                        if (ReplyPolicy.blocksReply(category)) {
                            _state.update { it.copy(status = Status.Error, message = "blocked", risk = category) }
                            return@onSuccess
                        }
                        val scenarios = ReplyPolicy.allowedScenarios(category)
                        _state.update {
                            it.copy(
                                status = Status.Content,
                                scenarios = scenarios,
                                selected = it.selected ?: scenarios.firstOrNull(),
                                draft = letter.reply,
                                risk = category,
                                facts = letter.reply?.polishFacts ?: it.facts,
                            )
                        }
                    }.onFailure { error ->
                        _state.update { it.copy(status = Status.Error, message = error.message) }
                    }
            }
        }

        private fun generate() {
            val scenario = _state.value.selected ?: return
            viewModelScope.launch {
                _state.update { it.copy(status = Status.Loading) }
                runCatching { repository.generateReply(letterId, scenario, _state.value.facts) }
                    .onSuccess { letter ->
                        _state.update { it.copy(status = Status.Content, draft = letter.reply) }
                    }.onFailure { error ->
                        _state.update { it.copy(status = Status.Error, message = error.message) }
                    }
            }
        }

        private fun share() {
            val path = _state.value.draft?.pdfPath ?: return
            runCatching { actions.sharePdf(path) }
                .onFailure { error -> _state.update { it.copy(message = error.message) } }
        }

        private companion object {
            const val STOP_MS = 5_000L
        }
    }
