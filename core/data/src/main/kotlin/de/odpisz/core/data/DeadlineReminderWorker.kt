package de.odpisz.core.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class DeadlineReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE) ?: return Result.failure()
        val message = inputData.getString(KEY_MESSAGE) ?: return Result.failure()
        show(applicationContext, title, message, id.hashCode())
        return Result.success()
    }

    companion object {
        const val KEY_TITLE = "title"
        const val KEY_MESSAGE = "message"
        const val CHANNEL_ID = "odpisz_deadlines"

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(NotificationManager::class.java)
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "Terminy pism",
                    NotificationManager.IMPORTANCE_DEFAULT,
                )
            channel.description = "Przypomnienia 7 dni, 2 dni i w dniu terminu."
            manager.createNotificationChannel(channel)
        }

        private fun show(
            context: Context,
            title: String,
            message: String,
            notificationId: Int,
        ) {
            ensureChannel(context)
            val notification =
                NotificationCompat
                    .Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                    .setAutoCancel(true)
                    .build()
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.notify(notificationId, notification)
        }
    }
}
