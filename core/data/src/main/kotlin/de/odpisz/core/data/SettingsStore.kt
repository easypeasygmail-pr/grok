package de.odpisz.core.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import de.odpisz.core.model.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore by preferencesDataStore(name = "odpisz_settings")

@Singleton
class SettingsStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : SettingsRepository {
        override val onboardingCompleted: Flow<Boolean> =
            context.settingsDataStore.data.map {
                it[ONBOARDING] == true
            }

        override val biometricLockEnabled: Flow<Boolean> =
            context.settingsDataStore.data.map {
                it[BIOMETRIC] == true
            }

        override val art9AcceptedAt: Flow<Long?> =
            context.settingsDataStore.data.map {
                it[ART9]
            }

        override suspend fun completeOnboarding() {
            context.settingsDataStore.edit { prefs ->
                prefs[ONBOARDING] = true
                prefs[ART9] = System.currentTimeMillis()
            }
        }

        override suspend fun setBiometricLock(enabled: Boolean) {
            context.settingsDataStore.edit { prefs -> prefs[BIOMETRIC] = enabled }
        }

        override suspend fun clear() {
            context.settingsDataStore.edit { it.clear() }
        }

        private companion object {
            val ONBOARDING = booleanPreferencesKey("onboarding_done")
            val BIOMETRIC = booleanPreferencesKey("biometric_lock")
            val ART9 = longPreferencesKey("art9_accepted_at")
        }
    }
