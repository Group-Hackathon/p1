package com.preappointment1.app.notifications

import android.content.Context
import com.preappointment1.app.ui.screens.FollowUpUi
import java.time.LocalTime

object ScheduleReminderManager {
    private const val PREFS = "schedule_reminders"
    private const val KEY_ACTIVE_SUB = "active_subscription_id"
    private const val MAX_SLOTS_PER_SUB = 12

    fun requestCode(subscriptionId: String, slotIndex: Int): Int {
        return (subscriptionId.hashCode() and 0x7FFF) + slotIndex + 1
    }

    fun scheduleForFollowUp(
        context: Context,
        subscriptionId: String,
        title: String,
        schedule: Map<String, List<String>>
    ) {
        cancelForFollowUp(context, subscriptionId)
        NotificationHelper.createNotificationChannel(context)

        val slots = schedule.mapNotNull { (timeKey, actions) ->
            runCatching {
                val time = LocalTime.parse(timeKey)
                Triple(timeKey, time.hour, time.minute) to actions
            }.getOrNull()
        }.sortedBy { it.first.third }.sortedBy { it.first.second }

        slots.forEachIndexed { index, (timeInfo, actions) ->
            val (timeKey, hour, minute) = timeInfo
            val measures = formatActions(actions)
            val notifTitle = "Check-in — $timeKey"
            val message = buildString {
                append("Time for your ")
                append(title)
                append(" check-in at ")
                append(timeKey)
                if (measures.isNotBlank()) {
                    append(" — ")
                    append(measures)
                }
                append('.')
            }
            NotificationHelper.scheduleDailyReminder(
                context = context,
                hour = hour,
                minute = minute,
                requestCode = requestCode(subscriptionId, index),
                title = notifTitle,
                message = message
            )
        }

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_ACTIVE_SUB, subscriptionId)
            .apply()
    }

    fun cancelForFollowUp(context: Context, subscriptionId: String) {
        for (index in 0 until MAX_SLOTS_PER_SUB) {
            NotificationHelper.cancelReminder(context, requestCode(subscriptionId, index))
        }
    }

    fun rescheduleActiveFollowUps(context: Context, followUps: List<FollowUpUi>) {
        val active = followUps
            .filter { it.daysRemaining > 0 && !it.schedule.isNullOrEmpty() }
            .maxByOrNull { it.startsAt }

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val previousId = prefs.getString(KEY_ACTIVE_SUB, null)
        if (previousId != null && previousId != active?.id) {
            cancelForFollowUp(context, previousId)
        }

        if (active != null && active.schedule != null) {
            scheduleForFollowUp(context, active.id, active.title, active.schedule)
        } else if (previousId != null) {
            cancelForFollowUp(context, previousId)
            prefs.edit().remove(KEY_ACTIVE_SUB).apply()
        }
    }

    fun remindersFromSchedule(schedule: Map<String, List<String>>): List<ScheduleReminderUi> {
        return schedule.mapNotNull { (timeKey, actions) ->
            runCatching { LocalTime.parse(timeKey) }.getOrNull()?.let { time ->
                ScheduleReminderUi(
                    timeKey = timeKey,
                    displayTime = formatDisplayTime(time),
                    measures = formatActions(actions)
                )
            }
        }.sortedBy { it.timeKey }
    }

    private fun formatActions(actions: List<String>): String {
        return actions.map { action ->
            when (action.lowercase()) {
                "pain" -> "pain level"
                "temperature" -> "temperature"
                "photo" -> "photo"
                else -> action
            }
        }.joinToString(", ")
    }

    private fun formatDisplayTime(time: LocalTime): String {
        val hour = time.hour
        val minute = time.minute
        val amPm = if (hour < 12) "AM" else "PM"
        val h12 = when (val h = hour % 12) {
            0 -> 12
            else -> h
        }
        return if (minute == 0) "$h12:00 $amPm" else String.format("%d:%02d %s", h12, minute, amPm)
    }
}

data class ScheduleReminderUi(
    val timeKey: String,
    val displayTime: String,
    val measures: String
)
