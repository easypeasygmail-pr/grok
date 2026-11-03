package de.odpisz.feature.reply

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.odpisz.core.designsystem.BodyText
import de.odpisz.core.designsystem.LoadingPane
import de.odpisz.core.designsystem.MessagePane
import de.odpisz.core.designsystem.OdpiszButton
import de.odpisz.core.designsystem.OdpiszColumn
import de.odpisz.core.designsystem.OdpiszScaffold
import de.odpisz.core.designsystem.OdpiszTheme
import de.odpisz.core.designsystem.SectionLabel
import de.odpisz.core.model.ReplyDraft
import de.odpisz.core.model.ReplyScenario
import de.odpisz.core.model.RiskCategory

@Composable
fun ReplyRoute(
    onBack: () -> Unit,
    onBlocked: (String) -> Unit,
    letterId: String,
    viewModel: ReplyViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.status, state.message) {
        if (state.message == "blocked") onBlocked(letterId)
    }
    ReplyScreen(state = state, onBack = onBack, onEvent = viewModel::onEvent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReplyScreen(
    state: ReplyUiState,
    onEvent: (ReplyUiEvent) -> Unit,
    onBack: () -> Unit,
) {
    OdpiszScaffold(title = stringResource(R.string.reply_title), onBack = onBack) { padding ->
        when (state.status) {
            ReplyUiState.Status.Loading -> {
                LoadingPane(padding)
            }

            ReplyUiState.Status.Error -> {
                MessagePane(
                    padding = padding,
                    title = stringResource(R.string.reply_error_title),
                    body =
                        if (state.message == "blocked") {
                            stringResource(R.string.reply_blocked)
                        } else {
                            state.message.orEmpty()
                        },
                    actionLabel = stringResource(R.string.reply_retry),
                    onAction = { onEvent(ReplyUiEvent.Retry) },
                )
            }

            ReplyUiState.Status.Empty -> {
                MessagePane(
                    padding = padding,
                    title = stringResource(R.string.reply_error_title),
                    body = stringResource(R.string.reply_blocked),
                )
            }

            ReplyUiState.Status.Content -> {
                OdpiszColumn(padding) {
                    BodyText(text = stringResource(R.string.reply_lead))
                    if (state.risk == RiskCategory.Tax) {
                        BodyText(text = stringResource(R.string.reply_tax_note))
                    }
                    state.scenarios.forEach { scenario ->
                        FilterChip(
                            selected = state.selected == scenario,
                            onClick = { onEvent(ReplyUiEvent.Select(scenario)) },
                            label = { Text(scenarioLabel(scenario), style = MaterialTheme.typography.bodyLarge) },
                            modifier = Modifier.testTag("scenario_${scenario.name}"),
                        )
                    }
                    OutlinedTextField(
                        value = state.facts,
                        onValueChange = { onEvent(ReplyUiEvent.FactsChanged(it)) },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .testTag("reply_facts"),
                        label = { Text(stringResource(R.string.reply_facts)) },
                        minLines = 4,
                        textStyle = MaterialTheme.typography.bodyLarge,
                    )
                    OdpiszButton(
                        text = stringResource(R.string.reply_generate),
                        onClick = { onEvent(ReplyUiEvent.Generate) },
                        enabled = state.selected != null,
                        modifier = Modifier.testTag("reply_generate"),
                    )
                    state.draft?.let { draft ->
                        SectionLabel(text = stringResource(R.string.reply_german))
                        Paper(draft.subject + "\n\n" + draft.germanBody)
                        SectionLabel(text = stringResource(R.string.reply_polish))
                        Paper(draft.polishBackTranslation)
                        if (draft.pdfPath != null) {
                            OdpiszButton(
                                text = stringResource(R.string.reply_share),
                                onClick = { onEvent(ReplyUiEvent.Share) },
                                modifier = Modifier.testTag("reply_share"),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Paper(text: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = text,
            modifier = Modifier.padding(OdpiszTheme.spacing.md),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun scenarioLabel(scenario: ReplyScenario): String =
    when (scenario) {
        ReplyScenario.Fristverlaengerung -> stringResource(R.string.reply_scenario_frist)
        ReplyScenario.RatenzahlungStundung -> stringResource(R.string.reply_scenario_rate)
        ReplyScenario.FristwahrenderWiderspruch -> stringResource(R.string.reply_scenario_widerspruch)
        ReplyScenario.Erklaerung -> stringResource(R.string.reply_scenario_erklaerung)
        ReplyScenario.AenderungDerDaten -> stringResource(R.string.reply_scenario_daten)
    }

@Preview(name = "Jasny")
@Preview(name = "Ciemny", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ReplyPreview() {
    OdpiszTheme {
        ReplyScreen(
            state =
                ReplyUiState(
                    status = ReplyUiState.Status.Content,
                    scenarios = listOf(ReplyScenario.Fristverlaengerung),
                    selected = ReplyScenario.Fristverlaengerung,
                    facts = "Dostałem pismo wczoraj.",
                    risk = RiskCategory.Tax,
                    draft =
                        ReplyDraft(
                            scenario = ReplyScenario.Fristverlaengerung,
                            polishFacts = "Dostałem pismo wczoraj.",
                            subject = "Bitte um Fristverlängerung",
                            germanBody = "hiermit bitte ich um Verlängerung der Frist.",
                            polishBackTranslation = "Proszę o przedłużenie terminu.",
                            pdfPath = "/tmp/list.pdf",
                        ),
                ),
            onEvent = {},
            onBack = {},
        )
    }
}
