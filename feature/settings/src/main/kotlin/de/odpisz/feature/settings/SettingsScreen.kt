package de.odpisz.feature.settings

import android.content.res.Configuration
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
import de.odpisz.core.model.ReminderScheduler
import de.odpisz.core.model.SettingsRepository
import de.odpisz.core.model.UserProfile
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val status: Status = Status.Loading,
    val profile: UserProfile = UserProfile(),
    val biometric: Boolean = false,
    val message: String? = null,
) {
    enum class Status { Loading, Empty, Error, Content }
}

sealed interface SettingsUiEvent {
    data class Name(
        val value: String,
    ) : SettingsUiEvent

    data class Street(
        val value: String,
    ) : SettingsUiEvent

    data class Postcode(
        val value: String,
    ) : SettingsUiEvent

    data class City(
        val value: String,
    ) : SettingsUiEvent

    data class Email(
        val value: String,
    ) : SettingsUiEvent

    data class Phone(
        val value: String,
    ) : SettingsUiEvent

    data object SaveProfile : SettingsUiEvent

    data class Biometric(
        val enabled: Boolean,
    ) : SettingsUiEvent

    data object DeleteAll : SettingsUiEvent

    data object Retry : SettingsUiEvent
}

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val settings: SettingsRepository,
        private val letters: LetterRepository,
        private val reminders: ReminderScheduler,
    ) : ViewModel() {
        private val draft = MutableStateFlow(UserProfile())
        private val status = MutableStateFlow(SettingsUiState.Status.Loading)
        private val message = MutableStateFlow<String?>(null)
        private val wiped = Channel<Unit>(Channel.BUFFERED)
        val wipedEvents = wiped.receiveAsFlow()

        val state =
            combine(
                draft,
                settings.biometricLockEnabled,
                status,
                message,
            ) { profile, biometric, current, note ->
                SettingsUiState(
                    status = current,
                    profile = profile,
                    biometric = biometric,
                    message = note,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), SettingsUiState())

        init {
            viewModelScope.launch {
                letters.observeProfile().collect { profile ->
                    draft.value = profile
                    status.value = SettingsUiState.Status.Content
                }
            }
        }

        fun onEvent(event: SettingsUiEvent) {
            when (event) {
                is SettingsUiEvent.Name -> {
                    draft.value = draft.value.copy(fullName = event.value)
                }

                is SettingsUiEvent.Street -> {
                    draft.value = draft.value.copy(street = event.value)
                }

                is SettingsUiEvent.Postcode -> {
                    draft.value = draft.value.copy(postcode = event.value)
                }

                is SettingsUiEvent.City -> {
                    draft.value = draft.value.copy(city = event.value)
                }

                is SettingsUiEvent.Email -> {
                    draft.value = draft.value.copy(email = event.value)
                }

                is SettingsUiEvent.Phone -> draft.value = draft.value.copy(phone = event.value)

                SettingsUiEvent.SaveProfile ->
                    viewModelScope.launch {
                        runCatching { letters.saveProfile(draft.value) }
                            .onSuccess { message.value = "saved" }
                            .onFailure { message.value = it.message }
                    }

                is SettingsUiEvent.Biometric ->
                    viewModelScope.launch {
                        settings.setBiometricLock(event.enabled)
                    }

                SettingsUiEvent.DeleteAll ->
                    viewModelScope.launch {
                        runCatching {
                            reminders.cancelAll()
                            letters.deleteAll()
                            settings.clear()
                        }.onSuccess { wiped.send(Unit) }
                            .onFailure {
                                status.value = SettingsUiState.Status.Error
                                message.value = it.message
                            }
                    }

                SettingsUiEvent.Retry -> status.value = SettingsUiState.Status.Content
            }
        }

        private companion object {
            const val STOP_MS = 5_000L
        }
    }

@Composable
fun SettingsRoute(
    onWiped: () -> Unit,
    bottomBar: @Composable () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(viewModel) {
        viewModel.wipedEvents.collect { onWiped() }
    }
    SettingsScreen(state = state, onEvent = viewModel::onEvent, bottomBar = bottomBar)
}

@Composable
fun LockScreen(onUnlocked: () -> Unit) {
    val context = LocalContext.current
    val activity = context as FragmentActivity
    val title = stringResource(R.string.settings_lock_title)
    val subtitle = stringResource(R.string.settings_lock_subtitle)
    val prompt =
        remember(activity) {
            BiometricPrompt(
                activity,
                ContextCompat.getMainExecutor(activity),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        onUnlocked()
                    }
                },
            )
        }

    fun authenticate() {
        val info =
            BiometricPrompt.PromptInfo
                .Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL,
                ).build()
        prompt.authenticate(info)
    }
    OdpiszScaffold(title = stringResource(R.string.settings_title)) { padding ->
        OdpiszColumn(padding) {
            BodyText(text = subtitle)
            OdpiszButton(text = stringResource(R.string.settings_lock_retry), onClick = { authenticate() })
        }
    }
    androidx.compose.runtime.LaunchedEffect(Unit) { authenticate() }
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
    bottomBar: @Composable () -> Unit = {},
) {
    var confirmDelete by remember { mutableStateOf(false) }
    OdpiszScaffold(title = stringResource(R.string.settings_title), bottomBar = bottomBar) { padding ->
        when (state.status) {
            SettingsUiState.Status.Loading -> {
                LoadingPane(padding)
            }

            SettingsUiState.Status.Error -> {
                MessagePane(
                    padding = padding,
                    title = stringResource(R.string.settings_title),
                    body = state.message ?: stringResource(R.string.settings_error),
                    actionLabel = stringResource(R.string.settings_cancel),
                    onAction = { onEvent(SettingsUiEvent.Retry) },
                )
            }

            SettingsUiState.Status.Empty,
            SettingsUiState.Status.Content,
            -> {
                OdpiszColumn(padding) {
                    SectionLabel(text = stringResource(R.string.settings_profile))
                    ProfileField(state.profile.fullName, stringResource(R.string.settings_name)) {
                        onEvent(SettingsUiEvent.Name(it))
                    }
                    ProfileField(state.profile.street, stringResource(R.string.settings_street)) {
                        onEvent(SettingsUiEvent.Street(it))
                    }
                    ProfileField(state.profile.postcode, stringResource(R.string.settings_postcode)) {
                        onEvent(SettingsUiEvent.Postcode(it))
                    }
                    ProfileField(state.profile.city, stringResource(R.string.settings_city)) {
                        onEvent(SettingsUiEvent.City(it))
                    }
                    ProfileField(state.profile.email, stringResource(R.string.settings_email)) {
                        onEvent(SettingsUiEvent.Email(it))
                    }
                    ProfileField(state.profile.phone, stringResource(R.string.settings_phone)) {
                        onEvent(SettingsUiEvent.Phone(it))
                    }
                    OdpiszButton(
                        text = stringResource(R.string.settings_save_profile),
                        onClick = { onEvent(SettingsUiEvent.SaveProfile) },
                    )
                    if (state.message == "saved") {
                        BodyText(text = stringResource(R.string.settings_saved))
                    }
                    SectionLabel(text = stringResource(R.string.settings_biometric))
                    BodyText(text = stringResource(R.string.settings_biometric_body))
                    Switch(
                        checked = state.biometric,
                        onCheckedChange = { onEvent(SettingsUiEvent.Biometric(it)) },
                    )
                    OdpiszButton(
                        text = stringResource(R.string.settings_delete),
                        onClick = { confirmDelete = true },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SectionLabel(text = stringResource(R.string.settings_impressum_title))
                    BodyText(text = stringResource(R.string.settings_impressum_body))
                    SectionLabel(text = stringResource(R.string.settings_privacy_title))
                    BodyText(text = stringResource(R.string.settings_privacy_body))
                }
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.settings_delete_title)) },
            text = { Text(stringResource(R.string.settings_delete_body), style = MaterialTheme.typography.bodyLarge) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onEvent(SettingsUiEvent.DeleteAll)
                    },
                ) { Text(stringResource(R.string.settings_delete_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.settings_cancel))
                }
            },
        )
    }
}

@Composable
private fun ProfileField(
    value: String,
    label: String,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge,
    )
}

@Preview(name = "Jasny")
@Preview(name = "Ciemny", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsPreview() {
    OdpiszTheme {
        SettingsScreen(
            state = SettingsUiState(status = SettingsUiState.Status.Content),
            onEvent = {},
        )
    }
}
