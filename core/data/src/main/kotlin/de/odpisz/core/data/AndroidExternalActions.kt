package de.odpisz.core.data

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import de.odpisz.core.model.ExternalActions
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidExternalActions
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : ExternalActions {
        override fun addToCalendar(
            title: String,
            date: LocalDate,
            description: String,
        ) {
            val start = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val intent =
                Intent(Intent.ACTION_INSERT)
                    .setData(CalendarContract.Events.CONTENT_URI)
                    .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
                    .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, start + HOUR_MS)
                    .putExtra(CalendarContract.Events.TITLE, title)
                    .putExtra(CalendarContract.Events.DESCRIPTION, description)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }

        override fun sharePdf(absolutePath: String) {
            val file = File(absolutePath)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent =
                Intent(Intent.ACTION_SEND)
                    .setType("application/pdf")
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(Intent.createChooser(intent, "Wyślij PDF").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }

        private companion object {
            const val HOUR_MS = 60L * 60L * 1000L
        }
    }
