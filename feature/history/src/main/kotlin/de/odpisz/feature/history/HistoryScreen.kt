package de.odpisz.feature.history

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.odpisz.core.designsystem.LoadingPane
import de.odpisz.core.designsystem.MessagePane
import de.odpisz.core.designsystem.OdpiszColumn
import de.odpisz.core.designsystem.OdpiszScaffold
import de.odpisz.core.designsystem.OdpiszTheme
import de.odpisz.core.model.LetterStatus
import de.odpisz.core.model.LetterSummary
import de.odpisz.core.model.Urgency
import java.time.Instant
import java.time.LocalDate

@Composable
fun HistoryRoute(
    onOpen: (String) -> Unit,
    bottomBar: @Composable () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HistoryScreen(
        state = state,
        onEvent = { event ->
            if (event is HistoryUiEvent.Open) onOpen(event.id) else viewModel.onEvent(event)
        },
        bottomBar = bottomBar,
    )
}

@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onEvent: (HistoryUiEvent) -> Unit,
    bottomBar: @Composable () -> Unit = {},
) {
    OdpiszScaffold(
        title = stringResource(R.string.history_title),
        bottomBar = bottomBar,
    ) { padding ->
        when (state.status) {
            HistoryUiState.Status.Loading -> {
                LoadingPane(padding)
            }

            HistoryUiState.Status.Error -> {
                MessagePane(
                    padding = padding,
                    title = stringResource(R.string.history_error_title),
                    body = state.message.orEmpty(),
                    actionLabel = stringResource(R.string.history_retry),
                    onAction = { onEvent(HistoryUiEvent.Retry) },
                )
            }

            HistoryUiState.Status.Empty -> {
                OdpiszColumn(padding) {
                    SearchField(state.query) { onEvent(HistoryUiEvent.QueryChanged(it)) }
                    Text(
                        text = stringResource(R.string.history_empty_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text =
                            if (state.query.isBlank()) {
                                stringResource(R.string.history_empty_body)
                            } else {
                                stringResource(R.string.history_empty_search)
                            },
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            HistoryUiState.Status.Content -> {
                OdpiszColumn(padding) {
                    SearchField(state.query) { onEvent(HistoryUiEvent.QueryChanged(it)) }
                    state.letters.forEach { letter ->
                        LetterRow(letter) { onEvent(HistoryUiEvent.Open(letter.id)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onChange,
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag("history_search"),
        label = { Text(stringResource(R.string.history_search)) },
        leadingIcon = {
            Icon(Icons.Filled.Search, contentDescription = null)
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge,
    )
}

@Composable
private fun LetterRow(
    letter: LetterSummary,
    onClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(OdpiszTheme.spacing.md)) {
            Text(text = letter.title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = statusLabel(letter.status),
                style = MaterialTheme.typography.bodyLarge,
            )
            letter.urgency?.let { urgency ->
                Text(
                    text = urgencyLabel(urgency),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            letter.nearestDeadline?.let { date ->
                Text(
                    text = date.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun statusLabel(status: LetterStatus): String =
    when (status) {
        LetterStatus.Draft -> stringResource(R.string.history_status_draft)
        LetterStatus.Analyzed -> stringResource(R.string.history_status_analyzed)
        LetterStatus.ReplyReady -> stringResource(R.string.history_status_reply)
        LetterStatus.NeedsAdvisor -> stringResource(R.string.history_status_advisor)
    }

@Composable
private fun urgencyLabel(urgency: Urgency): String =
    when (urgency) {
        Urgency.Green -> stringResource(R.string.history_urgency_green)
        Urgency.Yellow -> stringResource(R.string.history_urgency_yellow)
        Urgency.Red -> stringResource(R.string.history_urgency_red)
    }

@Preview(name = "Jasny")
@Preview(name = "Ciemny", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HistoryPreview() {
    OdpiszTheme {
        HistoryScreen(
            state =
                HistoryUiState(
                    status = HistoryUiState.Status.Content,
                    letters =
                        listOf(
                            LetterSummary(
                                id = "1",
                                createdAt = Instant.parse("2026-11-03T10:00:00Z"),
                                title = "Finanzamt Berlin Mitte",
                                sender = "Finanzamt Berlin Mitte",
                                status = LetterStatus.Analyzed,
                                urgency = Urgency.Yellow,
                                nearestDeadline = LocalDate.of(2026, 11, 20),
                            ),
                        ),
                ),
            onEvent = {},
        )
    }
}
