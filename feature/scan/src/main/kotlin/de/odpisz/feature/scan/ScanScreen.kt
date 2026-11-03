package de.odpisz.feature.scan

import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.launch

@Composable
fun ScanRoute(
    onOpenReview: (String) -> Unit,
    bottomBar: @Composable () -> Unit,
    pendingUri: String? = null,
    onPendingConsumed: () -> Unit = {},
    viewModel: ScanViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scanLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult(),
        ) { result ->
            viewModel.onEvent(ScanUiEvent.ScanFinished(result.resultCode, result.data))
        }
    val fileLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) viewModel.onEvent(ScanUiEvent.ImportUri(uri.toString()))
        }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ScanEffect.OpenReview -> onOpenReview(effect.letterId)
            }
        }
    }
    LaunchedEffect(pendingUri) {
        if (pendingUri != null) {
            viewModel.onEvent(ScanUiEvent.ImportUri(pendingUri))
            onPendingConsumed()
        }
    }
    ScanScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onScan = {
            val activity = context as? ComponentActivity ?: return@ScanScreen
            scope.launch {
                runCatching { viewModel.scanIntent(activity) }
                    .onSuccess { sender ->
                        scanLauncher.launch(IntentSenderRequest.Builder(sender).build())
                    }.onFailure {
                        viewModel.onScanUnavailable(
                            context.getString(R.string.scan_scanner_missing),
                        )
                    }
            }
        },
        onPickFile = { fileLauncher.launch(arrayOf("image/*", "application/pdf")) },
        bottomBar = bottomBar,
    )
}

@Composable
fun ScanScreen(
    state: ScanUiState,
    onEvent: (ScanUiEvent) -> Unit,
    onScan: () -> Unit,
    onPickFile: () -> Unit,
    bottomBar: @Composable () -> Unit = {},
) {
    OdpiszScaffold(
        title = stringResource(R.string.scan_title),
        bottomBar = bottomBar,
    ) { padding ->
        when (state.status) {
            ScanUiState.Status.Loading -> {
                LoadingPane(padding)
            }

            ScanUiState.Status.Error -> {
                MessagePane(
                    padding = padding,
                    title = stringResource(R.string.scan_error_title),
                    body = state.message ?: stringResource(R.string.scan_scanner_missing),
                    actionLabel = stringResource(R.string.scan_retry),
                    onAction = { onEvent(ScanUiEvent.Retry) },
                )
            }

            ScanUiState.Status.Empty,
            ScanUiState.Status.Content,
            -> {
                OdpiszColumn(padding) {
                    BodyText(text = stringResource(R.string.scan_intro))
                    OdpiszButton(
                        text = stringResource(R.string.scan_camera),
                        onClick = onScan,
                        modifier = Modifier.testTag("scan_camera"),
                    )
                    OdpiszOutlinedButton(
                        text = stringResource(R.string.scan_file),
                        onClick = onPickFile,
                    )
                    OdpiszOutlinedButton(
                        text = stringResource(R.string.scan_sample_tax),
                        onClick = { onEvent(ScanUiEvent.SampleTax) },
                        modifier = Modifier.testTag("scan_sample_tax"),
                    )
                    OdpiszOutlinedButton(
                        text = stringResource(R.string.scan_sample_court),
                        onClick = { onEvent(ScanUiEvent.SampleCourt) },
                        modifier = Modifier.testTag("scan_sample_court"),
                    )
                    OutlinedTextField(
                        value = state.pasted,
                        onValueChange = { onEvent(ScanUiEvent.PasteChanged(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.scan_paste_label)) },
                        minLines = 4,
                        textStyle = MaterialTheme.typography.bodyLarge,
                    )
                    OdpiszButton(
                        text = stringResource(R.string.scan_paste_action),
                        onClick = { onEvent(ScanUiEvent.ImportPaste) },
                        enabled = state.pasted.isNotBlank(),
                    )
                }
            }
        }
    }
}

@Preview(name = "Jasny")
@Preview(name = "Ciemny", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ScanPreview() {
    OdpiszTheme {
        ScanScreen(state = ScanUiState(), onEvent = {}, onScan = {}, onPickFile = {})
    }
}
