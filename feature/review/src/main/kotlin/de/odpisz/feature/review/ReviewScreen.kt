package de.odpisz.feature.review

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
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
import de.odpisz.core.designsystem.OdpiszOutlinedButton
import de.odpisz.core.designsystem.OdpiszScaffold
import de.odpisz.core.designsystem.OdpiszTheme
import de.odpisz.core.designsystem.SectionLabel
import de.odpisz.core.model.PiiKind
import de.odpisz.core.model.PiiToken

@Composable
fun ReviewRoute(
    onBack: () -> Unit,
    onAnalyzed: (String) -> Unit,
    viewModel: ReviewViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ReviewEffect.OpenLetter -> onAnalyzed(effect.id)
            }
        }
    }
    ReviewScreen(state = state, onEvent = viewModel::onEvent, onBack = onBack)
}

@Composable
fun ReviewScreen(
    state: ReviewUiState,
    onEvent: (ReviewUiEvent) -> Unit,
    onBack: () -> Unit,
) {
    OdpiszScaffold(title = stringResource(R.string.review_title), onBack = onBack) { padding ->
        when (state.status) {
            ReviewUiState.Status.Loading -> {
                LoadingPane(padding)
            }

            ReviewUiState.Status.Error -> {
                MessagePane(
                    padding = padding,
                    title = stringResource(R.string.review_error_title),
                    body = state.message.orEmpty(),
                    actionLabel = stringResource(R.string.review_retry),
                    onAction = { onEvent(ReviewUiEvent.Retry) },
                )
            }

            ReviewUiState.Status.Empty -> {
                MessagePane(
                    padding = padding,
                    title = stringResource(R.string.review_error_title),
                    body = stringResource(R.string.review_missing),
                )
            }

            ReviewUiState.Status.Content -> {
                OdpiszColumn(padding) {
                    BodyText(text = stringResource(R.string.review_lead))
                    SectionLabel(
                        text =
                            if (state.showOriginal) {
                                stringResource(R.string.review_original)
                            } else {
                                stringResource(R.string.review_masked)
                            },
                    )
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if (state.showOriginal) state.originalText else state.maskedText,
                            modifier = Modifier.padding(OdpiszTheme.spacing.md),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    if (state.tokens.isNotEmpty()) {
                        SectionLabel(text = stringResource(R.string.review_tokens))
                        state.tokens.forEach { token ->
                            Text(
                                text = "${token.token} · ${token.kind.name}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    OdpiszOutlinedButton(
                        text =
                            stringResource(
                                if (state.showOriginal) {
                                    R.string.review_hide_original
                                } else {
                                    R.string.review_show_original
                                },
                            ),
                        onClick = { onEvent(ReviewUiEvent.ToggleOriginal) },
                    )
                    OdpiszButton(
                        text = stringResource(R.string.review_analyze),
                        onClick = { onEvent(ReviewUiEvent.Analyze) },
                        modifier = Modifier.testTag("review_analyze"),
                    )
                }
            }
        }
    }
}

@Preview(name = "Jasny")
@Preview(name = "Ciemny", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ReviewPreview() {
    OdpiszTheme {
        ReviewScreen(
            state =
                ReviewUiState(
                    status = ReviewUiState.Status.Content,
                    maskedText = "IBAN [IBAN_1], termin 20.11.2026",
                    originalText = "IBAN DE89…",
                    tokens = listOf(PiiToken("[IBAN_1]", PiiKind.Iban, "DE89")),
                ),
            onEvent = {},
            onBack = {},
        )
    }
}
