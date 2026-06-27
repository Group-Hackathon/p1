package com.preappointment1.app.notifications

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Tracking Reminder"
        val message = intent.getStringExtra("message") ?: "It's time for your health check-in!"
        val subscriptionId = intent.getStringExtra(NotificationIntents.EXTRA_SUBSCRIPTION_ID)
        val scheduleKey = intent.getStringExtra(NotificationIntents.EXTRA_SCHEDULE_KEY)

        val contentIntent = when {
            subscriptionId != null -> NotificationIntents.activityPendingIntent(
                context = context,
                requestCode = (subscriptionId + (scheduleKey ?: "")).hashCode(),
                subscriptionId = subscriptionId,
                scheduleKey = scheduleKey,
                openMeasurementForm = true
            )
            else -> {
                val activeId = ScheduleReminderManager.getActiveSubscriptionId(context) ?: return
                NotificationIntents.activityPendingIntent(
                    context = context,
                    requestCode = activeId.hashCode(),
                    subscriptionId = activeId,
                    scheduleKey = scheduleKey,
                    openMeasurementForm = true
                )
            }
        }

        val builder = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                notify(NotificationHelper.NOTIFICATION_ID + System.currentTimeMillis().toInt(), builder.build())
            }
        }
    }
}
