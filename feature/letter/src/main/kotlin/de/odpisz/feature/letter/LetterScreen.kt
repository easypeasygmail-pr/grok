package de.odpisz.feature.letter

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.odpisz.core.designsystem.BodyText
import de.odpisz.core.designsystem.LoadingPane
import de.odpisz.core.designsystem.MessagePane
import de.odpisz.core.designsystem.OdpiszButton
import de.odpisz.core.designsystem.OdpiszColumn
import de.odpisz.core.designsystem.OdpiszOutlinedButton
import de.odpisz.core.designsystem.OdpiszScaffold
import de.odpisz.core.designsystem.OdpiszTheme
import de.odpisz.core.designsystem.SectionLabel
import de.odpisz.core.model.Amount
import de.odpisz.core.model.Deadline
import de.odpisz.core.model.DeadlineKind
import de.odpisz.core.model.Letter
import de.odpisz.core.model.LetterAnalysis
import de.odpisz.core.model.LetterStatus
import de.odpisz.core.model.ReplyPolicy
import de.odpisz.core.model.RiskCategory
import de.odpisz.core.model.Urgency
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@Composable
fun LetterRoute(
    onBack: () -> Unit,
    onReply: (String) -> Unit,
    onRedCard: (String) -> Unit,
    viewModel: LetterViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val remindersDone = stringResource(R.string.letter_reminders_done)
    val context = LocalContext.current
    val permission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) viewModel.onEvent(LetterUiEvent.Reminders)
        }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is LetterEffect.OpenReply -> onReply(effect.id)
                is LetterEffect.OpenRedCard -> onRedCard(effect.id)
                is LetterEffect.Message -> snackbar.showSnackbar(remindersDone)
            }
        }
    }
    LetterScreen(
        state = state,
        snackbarHostState = snackbar,
        onBack = onBack,
        onEvent = { event ->
            if (event is LetterUiEvent.Reminders && needsNotificationPermission(context)) {
                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.onEvent(event)
            }
        },
    )
}

private fun needsNotificationPermission(context: android.content.Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
    ) != PackageManager.PERMISSION_GRANTED
}

@Composable
fun LetterScreen(
    state: LetterUiState,
    onEvent: (LetterUiEvent) -> Unit,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    OdpiszScaffold(
        title = stringResource(R.string.letter_title),
        onBack = onBack,
        snackbarHostState = snackbarHostState,
    ) { padding ->
        when (state.status) {
            LetterUiState.Status.Loading -> {
                LoadingPane(padding)
            }

            LetterUiState.Status.Error -> {
                MessagePane(
                    padding = padding,
                    title = stringResource(R.string.letter_error_title),
                    body = state.message.orEmpty(),
                    actionLabel = stringResource(R.string.letter_retry),
                    onAction = { onEvent(LetterUiEvent.Retry) },
                )
            }

            LetterUiState.Status.Empty -> {
                MessagePane(
                    padding = padding,
                    title = stringResource(R.string.letter_error_title),
                    body = stringResource(R.string.letter_missing),
                )
            }

            LetterUiState.Status.Content -> {
                LetterContent(padding, state, onEvent)
            }
        }
    }
}

@Composable
private fun LetterContent(
    padding: androidx.compose.foundation.layout.PaddingValues,
    state: LetterUiState,
    onEvent: (LetterUiEvent) -> Unit,
) {
    val letter = state.letter ?: return
    val analysis = letter.analysis
    OdpiszColumn(padding) {
        BodyText(text = stringResource(R.string.letter_fake_note))
        analysis?.let { UrgencyBanner(it.urgency) }
        Card(modifier = Modifier.fillMaxWidth()) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.padding(OdpiszTheme.spacing.md),
                verticalArrangement =
                    androidx.compose.foundation.layout.Arrangement
                        .spacedBy(OdpiszTheme.spacing.sm),
            ) {
                Fact(stringResource(R.string.letter_who), analysis?.sender ?: letter.title)
                Fact(
                    stringResource(R.string.letter_what),
                    analysis?.whatTheyWant ?: stringResource(R.string.letter_none),
                )
                Fact(stringResource(R.string.letter_how_much), amounts(analysis?.amounts.orEmpty()))
                Fact(
                    stringResource(R.string.letter_until),
                    analysis?.deadlines?.minOfOrNull { it.date }?.toString()
                        ?: stringResource(R.string.letter_none),
                )
                Fact(
                    stringResource(R.string.letter_file),
                    analysis?.aktenzeichen ?: stringResource(R.string.letter_none),
                )
            }
        }
        SectionLabel(text = stringResource(R.string.letter_deadlines))
        analysis?.deadlines?.forEachIndexed { index, deadline ->
            DeadlineCard(
                deadline = deadline,
                editing = state.editingIndex == index,
                editedDate = state.editedDate,
                dateError = state.message == "bad-date" && state.editingIndex == index,
                onEvent = onEvent,
                index = index,
            )
        }
        val blocked = analysis != null && ReplyPolicy.blocksReply(analysis.riskCategory)
        OdpiszButton(
            text = stringResource(if (blocked) R.string.letter_advisor else R.string.letter_reply),
            onClick = { onEvent(LetterUiEvent.Reply) },
            modifier = Modifier.testTag(if (blocked) "letter_red" else "letter_reply"),
        )
    }
}

@Composable
private fun Fact(
    label: String,
    value: String,
) {
    SectionLabel(text = label)
    Text(text = value, style = MaterialTheme.typography.bodyLarge)
}

@Composable
private fun amounts(amounts: List<Amount>): String {
    if (amounts.isEmpty()) return stringResource(R.string.letter_none)
    return amounts.joinToString("\n") { amount ->
        "${amount.value.toPlainString()} ${amount.currency} — ${amount.purpose}"
    }
}

@Composable
private fun UrgencyBanner(urgency: Urgency) {
    val colors = OdpiszTheme.urgency
    val background: Color
    val content: Color
    val label: String
    when (urgency) {
        Urgency.Green -> {
            background = colors.green
            content = colors.onGreen
            label = stringResource(R.string.letter_urgency_green)
        }

        Urgency.Yellow -> {
            background = colors.yellow
            content = colors.onYellow
            label = stringResource(R.string.letter_urgency_yellow)
        }

        Urgency.Red -> {
            background = colors.red
            content = colors.onRed
            label = stringResource(R.string.letter_urgency_red)
        }
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            androidx.compose.material3.CardDefaults.cardColors(
                containerColor = background,
                contentColor = content,
            ),
    ) {
        Row(
            modifier = Modifier.padding(OdpiszTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (urgency == Urgency.Green) Icons.Filled.Check else Icons.Filled.Warning,
                contentDescription = null,
                tint = content,
            )
            Text(
                text = label,
                modifier = Modifier.padding(start = OdpiszTheme.spacing.sm),
                style = MaterialTheme.typography.bodyLarge,
                color = content,
            )
        }
    }
}

@Composable
private fun DeadlineCard(
    deadline: Deadline,
    editing: Boolean,
    editedDate: String,
    dateError: Boolean,
    index: Int,
    onEvent: (LetterUiEvent) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(OdpiszTheme.spacing.md),
            verticalArrangement =
                androidx.compose.foundation.layout.Arrangement
                    .spacedBy(OdpiszTheme.spacing.sm),
        ) {
            Text(text = deadline.date.toString(), style = MaterialTheme.typography.titleMedium)
            SectionLabel(text = stringResource(R.string.letter_quote))
            Text(text = deadline.sourceQuote, style = MaterialTheme.typography.bodyLarge)
            Text(text = deadline.rule, style = MaterialTheme.typography.bodyMedium)
            if (deadline.confirmed) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Text(
                        text = stringResource(R.string.letter_confirmed),
                        modifier = Modifier.padding(start = OdpiszTheme.spacing.sm),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            OdpiszOutlinedButton(
                text = stringResource(R.string.letter_confirm),
                onClick = { onEvent(LetterUiEvent.Confirm(index)) },
            )
            OdpiszOutlinedButton(
                text = stringResource(R.string.letter_correct),
                onClick = { onEvent(LetterUiEvent.StartCorrect(index)) },
            )
            if (editing) {
                OutlinedTextField(
                    value = editedDate,
                    onValueChange = { onEvent(LetterUiEvent.DateChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.letter_date_label)) },
                    isError = dateError,
                    supportingText =
                        if (dateError) {
                            { Text(stringResource(R.string.letter_bad_date)) }
                        } else {
                            null
                        },
                    singleLine = true,
                )
                OdpiszButton(
                    text = stringResource(R.string.letter_save_date),
                    onClick = { onEvent(LetterUiEvent.SaveDate) },
                )
            }
            OdpiszOutlinedButton(
                text = stringResource(R.string.letter_calendar),
                onClick = { onEvent(LetterUiEvent.Calendar(index)) },
            )
            OdpiszOutlinedButton(
                text = stringResource(R.string.letter_reminders),
                onClick = { onEvent(LetterUiEvent.Reminders) },
                modifier = Modifier.testTag("letter_reminders"),
            )
        }
    }
}

@Preview(name = "Jasny")
@Preview(name = "Ciemny", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LetterPreview() {
    OdpiszTheme {
        LetterScreen(
            state =
                LetterUiState(
                    status = LetterUiState.Status.Content,
                    letter =
                        Letter(
                            id = "1",
                            createdAt = Instant.parse("2026-11-03T10:00:00Z"),
                            title = "Finanzamt Berlin Mitte",
                            originalText = "",
                            maskedText = "",
                            sourceLabel = null,
                            status = LetterStatus.Analyzed,
                            analysis =
                                LetterAnalysis(
                                    sender = "Finanzamt Berlin Mitte",
                                    whatTheyWant = "Prosi o zapłatę.",
                                    amounts =
                                        listOf(
                                            Amount(
                                                BigDecimal("186.40"),
                                                "EUR",
                                                "Podatek",
                                                null,
                                                LocalDate.of(2026, 11, 20),
                                            ),
                                        ),
                                    deadlines =
                                        listOf(
                                            Deadline(
                                                LocalDate.of(2026, 11, 20),
                                                DeadlineKind.Payment,
                                                "Bitte zahlen Sie bis zum 20.11.2026",
                                                "Z pisma",
                                                false,
                                            ),
                                        ),
                                    aktenzeichen = "12/345/67890",
                                    urgency = Urgency.Yellow,
                                    riskCategory = RiskCategory.Tax,
                                ),
                            reply = null,
                            piiTokens = emptyList(),
                        ),
                ),
            onEvent = {},
            onBack = {},
        )
    }
}
