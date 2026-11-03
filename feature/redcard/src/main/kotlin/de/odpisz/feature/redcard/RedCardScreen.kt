package de.odpisz.feature.redcard

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.odpisz.core.designsystem.BodyText
import de.odpisz.core.designsystem.LoadingPane
import de.odpisz.core.designsystem.MessagePane
import de.odpisz.core.designsystem.OdpiszButton
import de.odpisz.core.designsystem.OdpiszColumn
import de.odpisz.core.designsystem.OdpiszScaffold
import de.odpisz.core.designsystem.OdpiszTheme
import de.odpisz.core.designsystem.SectionLabel
import de.odpisz.core.model.LetterRepository
import de.odpisz.core.model.RiskCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RedCardUiState(
    val status: Status = Status.Loading,
    val category: RiskCategory? = null,
    val sender: String = "",
) {
    enum class Status { Loading, Empty, Error, Content }
}

@HiltViewModel
class RedCardViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repository: LetterRepository,
    ) : ViewModel() {
        private val letterId: String = checkNotNull(savedStateHandle.get<String>("letterId"))
        private val _state = MutableStateFlow(RedCardUiState())
        val state: StateFlow<RedCardUiState> =
            _state.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(STOP_MS),
                RedCardUiState(),
            )

        init {
            viewModelScope.launch {
                runCatching { repository.getLetter(letterId) }
                    .onSuccess { letter ->
                        _state.value =
                            if (letter == null) {
                                RedCardUiState(status = RedCardUiState.Status.Empty)
                            } else {
                                RedCardUiState(
                                    status = RedCardUiState.Status.Content,
                                    category = letter.analysis?.riskCategory,
                                    sender = letter.analysis?.sender ?: letter.title,
                                )
                            }
                    }.onFailure {
                        _state.value = RedCardUiState(status = RedCardUiState.Status.Error)
                    }
            }
        }

        private companion object {
            const val STOP_MS = 5_000L
        }
    }

@Composable
fun RedCardRoute(
    onBack: () -> Unit,
    viewModel: RedCardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RedCardScreen(state = state, onBack = onBack)
}

@Composable
fun RedCardScreen(
    state: RedCardUiState,
    onBack: () -> Unit,
) {
    OdpiszScaffold(title = stringResource(R.string.redcard_title), onBack = onBack) { padding ->
        when (state.status) {
            RedCardUiState.Status.Loading -> {
                LoadingPane(padding)
            }

            RedCardUiState.Status.Empty,
            RedCardUiState.Status.Error,
            -> {
                MessagePane(
                    padding = padding,
                    title = stringResource(R.string.redcard_title),
                    body = stringResource(R.string.redcard_missing),
                    actionLabel = stringResource(R.string.redcard_back),
                    onAction = onBack,
                )
            }

            RedCardUiState.Status.Content -> {
                OdpiszColumn(padding) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = OdpiszTheme.urgency.red,
                                contentColor = OdpiszTheme.urgency.onRed,
                            ),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            modifier = Modifier.padding(OdpiszTheme.spacing.md),
                            tint = OdpiszTheme.urgency.onRed,
                        )
                        Text(
                            text = stringResource(R.string.redcard_lead),
                            modifier = Modifier.padding(OdpiszTheme.spacing.md),
                            style = MaterialTheme.typography.bodyLarge,
                            color = OdpiszTheme.urgency.onRed,
                        )
                    }
                    SectionLabel(text = stringResource(R.string.redcard_why))
                    BodyText(text = state.sender)
                    BodyText(text = reason(state.category))
                    SectionLabel(text = stringResource(R.string.redcard_where))
                    BodyText(text = stringResource(R.string.redcard_where_body))
                    OdpiszButton(text = stringResource(R.string.redcard_back), onClick = onBack)
                }
            }
        }
    }
}

@Composable
private fun reason(category: RiskCategory?): String =
    when (category) {
        RiskCategory.Court -> stringResource(R.string.redcard_court)
        RiskCategory.Enforcement -> stringResource(R.string.redcard_enforcement)
        RiskCategory.PaymentOrder -> stringResource(R.string.redcard_payment)
        RiskCategory.Residence -> stringResource(R.string.redcard_residence)
        RiskCategory.TaxAppeal -> stringResource(R.string.redcard_tax)
        else -> stringResource(R.string.redcard_lead)
    }

@Preview(name = "Jasny")
@Preview(name = "Ciemny", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RedCardPreview() {
    OdpiszTheme {
        RedCardScreen(
            state =
                RedCardUiState(
                    status = RedCardUiState.Status.Content,
                    category = RiskCategory.Court,
                    sender = "Amtsgericht Berlin",
                ),
            onBack = {},
        )
    }
}
