package com.livingpatientmemory.worker

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object NotificationScheduler {
    
    /**
     * Schedules a local notification for a specific daily routine.
     * In a full implementation, this would calculate the delay until the next specific hour (e.g. 18:00).
     */
    fun scheduleRoutineReminder(context: Context, title: String, message: String, delayMinutes: Long) {
        val data = Data.Builder()
            .putString("title", title)
            .putString("message", message)
            .build()

        val request = OneTimeWorkRequestBuilder<RoutineNotificationWorker>()
            .setInputData(data)
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueue(request)
    }
}
