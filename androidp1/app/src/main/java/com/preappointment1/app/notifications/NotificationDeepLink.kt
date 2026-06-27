package com.preappointment1.app.notifications

import android.content.Context
import android.content.Intent
import com.preappointment1.app.MainActivity

data class NotificationDeepLink(
    val subscriptionId: String,
    val scheduleKey: String?,
    val openMeasurementForm: Boolean
)

object NotificationIntents {
    const val EXTRA_SUBSCRIPTION_ID = "subscription_id"
    const val EXTRA_SCHEDULE_KEY = "schedule_key"
    const val EXTRA_OPEN_MEASUREMENT_FORM = "open_measurement_form"

    fun toMainActivityIntent(
        context: Context,
        subscriptionId: String,
        scheduleKey: String?,
        openMeasurementForm: Boolean = true
    ): Intent {
        return Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_SUBSCRIPTION_ID, subscriptionId)
            scheduleKey?.let { putExtra(EXTRA_SCHEDULE_KEY, it) }
            putExtra(EXTRA_OPEN_MEASUREMENT_FORM, openMeasurementForm)
        }
    }

    fun from(intent: Intent?): NotificationDeepLink? {
        if (intent == null) return null
        val subscriptionId = intent.getStringExtra(EXTRA_SUBSCRIPTION_ID) ?: return null
        return NotificationDeepLink(
            subscriptionId = subscriptionId,
            scheduleKey = intent.getStringExtra(EXTRA_SCHEDULE_KEY),
            openMeasurementForm = intent.getBooleanExtra(EXTRA_OPEN_MEASUREMENT_FORM, true)
        )
    }

    fun activityPendingIntent(
        context: Context,
        requestCode: Int,
        subscriptionId: String,
        scheduleKey: String?,
        openMeasurementForm: Boolean = true
    ): android.app.PendingIntent {
        val launchIntent = toMainActivityIntent(context, subscriptionId, scheduleKey, openMeasurementForm)
        return android.app.PendingIntent.getActivity(
            context,
            requestCode,
            launchIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
    }
}
