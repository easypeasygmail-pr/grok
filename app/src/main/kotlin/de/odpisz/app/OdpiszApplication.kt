package de.odpisz.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import de.odpisz.core.data.DeadlineReminderWorker

@HiltAndroidApp
class OdpiszApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DeadlineReminderWorker.ensureChannel(this)
    }
}
