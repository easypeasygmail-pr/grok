package de.odpisz.feature.onboarding

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.odpisz.core.designsystem.BodyText
import de.odpisz.core.designsystem.OdpiszButton
import de.odpisz.core.designsystem.OdpiszColumn
import de.odpisz.core.designsystem.OdpiszOutlinedButton
import de.odpisz.core.designsystem.OdpiszScaffold
import de.odpisz.core.designsystem.OdpiszTheme
import de.odpisz.core.designsystem.SectionLabel

@Composable
fun OnboardingRoute(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.finished.collect { onFinished() }
    }
    OnboardingScreen(state = state, onEvent = viewModel::onEvent)
}

@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    onEvent: (OnboardingUiEvent) -> Unit,
) {
    val pager = rememberPagerState(pageCount = { state.pageCount })
    LaunchedEffect(state.page) {
        if (pager.currentPage != state.page) pager.animateScrollToPage(state.page)
    }
    OdpiszScaffold(title = stringResource(R.string.onboarding_title)) { padding ->
        OdpiszColumn(padding) {
            HorizontalPager(
                state = pager,
                modifier = Modifier.fillMaxWidth(),
                userScrollEnabled = false,
            ) { page ->
                Card {
                    Column(
                        modifier = Modifier.padding(OdpiszTheme.spacing.md),
                        verticalArrangement = Arrangement.spacedBy(OdpiszTheme.spacing.sm),
                    ) {
                        SectionLabel(text = titleFor(page))
                        BodyText(text = bodyFor(page))
                    }
                }
            }
            if (state.page == state.pageCount - 1) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = state.consentChecked,
                        onCheckedChange = { onEvent(OnboardingUiEvent.ConsentChanged(it)) },
                        modifier = Modifier.testTag("onboarding_consent"),
                    )
                    Text(
                        text = stringResource(R.string.onboarding_consent),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            if (state.page > 0) {
                OdpiszOutlinedButton(
                    text = stringResource(R.string.onboarding_back),
                    onClick = { onEvent(OnboardingUiEvent.Back) },
                )
            }
            if (state.page < state.pageCount - 1) {
                OdpiszButton(
                    text = stringResource(R.string.onboarding_next),
                    onClick = { onEvent(OnboardingUiEvent.Next) },
                    modifier = Modifier.testTag("onboarding_next"),
                )
            } else {
                OdpiszButton(
                    text = stringResource(R.string.onboarding_start),
                    onClick = { onEvent(OnboardingUiEvent.Finish) },
                    enabled = state.canFinish,
                    modifier = Modifier.testTag("onboarding_start"),
                )
            }
        }
    }
}

@Composable
private fun titleFor(page: Int): String =
    when (page) {
        0 -> stringResource(R.string.onboarding_card_what_title)
        1 -> stringResource(R.string.onboarding_card_privacy_title)
        else -> stringResource(R.string.onboarding_card_legal_title)
    }

@Composable
private fun bodyFor(page: Int): String =
    when (page) {
        0 -> stringResource(R.string.onboarding_card_what_body)
        1 -> stringResource(R.string.onboarding_card_privacy_body)
        else -> stringResource(R.string.onboarding_card_legal_body)
    }

@Preview(name = "Jasny")
@Preview(name = "Ciemny", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OnboardingPreview() {
    OdpiszTheme {
        OnboardingScreen(
            state = OnboardingUiState(page = 2, consentChecked = true),
            onEvent = {},
        )
    }
}
