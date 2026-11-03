package de.odpisz.core.data

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import de.odpisz.core.model.Deadline
import de.odpisz.core.model.ReminderPlanner
import de.odpisz.core.model.ReminderScheduler
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkReminderScheduler
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : ReminderScheduler {
        override fun schedule(
            letterId: String,
            title: String,
            deadlines: List<Deadline>,
        ) {
            val workManager = WorkManager.getInstance(context)
            val zone = ZoneId.systemDefault()
            val now = ZonedDateTime.now(zone)
            deadlines.forEachIndexed { index, deadline ->
                ReminderPlanner.plan(deadline.date, now, zone).forEach { planned ->
                    val delay = Duration.between(Instant.now(), planned.fireAt).coerceAtLeast(Duration.ofSeconds(1))
                    val request =
                        OneTimeWorkRequestBuilder<DeadlineReminderWorker>()
                            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
                            .setInputData(
                                workDataOf(
                                    DeadlineReminderWorker.KEY_TITLE to title,
                                    DeadlineReminderWorker.KEY_MESSAGE to planned.message,
                                ),
                            ).addTag(tag(letterId))
                            .build()
                    workManager.enqueueUniqueWork(
                        "$letterId-$index-${planned.offsetDays}",
                        ExistingWorkPolicy.REPLACE,
                        request,
                    )
                }
            }
        }

        override fun cancelAll() {
            WorkManager.getInstance(context).cancelAllWork()
        }

        private fun tag(letterId: String): String = "deadline-$letterId"
    }
