package com.preappointment1.app.schedule

import java.time.LocalTime
import java.time.format.DateTimeFormatter

enum class MeasurementStep {
    Pain, Temperature, Photo
}

data class ScheduleSlot(
    val timeKey: String,
    val time: LocalTime,
    val actions: List<String>
)

data class MeasurementContext(
    val dueSlot: ScheduleSlot?,
    val nextSlot: ScheduleSlot?,
    val showStarterCheckIn: Boolean,
    val formScheduleKey: String,
    val formLabelOverride: String?,
    val formStepsOverride: List<MeasurementStep>?
)

object ScheduleLogic {
    const val INITIAL_LABEL = "Initial"
    private const val WINDOW_HOURS = 4L
    private const val STARTER_LOOKAHEAD_MINUTES = 180L

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun parseScheduleSlots(schedule: Map<String, List<String>>): List<ScheduleSlot> {
        return schedule.mapNotNull { (key, actions) ->
            runCatching { ScheduleSlot(key, LocalTime.parse(key), actions) }.getOrNull()
        }.sortedBy { it.time }
    }

    fun isInMeasurementWindow(now: LocalTime, slotTime: LocalTime): Boolean {
        val windowEnd = slotTime.plusHours(WINDOW_HOURS)
        val crossesMidnight = windowEnd.isBefore(slotTime)
        return if (crossesMidnight) {
            !now.isBefore(slotTime) || now.isBefore(windowEnd)
        } else {
            !now.isBefore(slotTime) && now.isBefore(windowEnd)
        }
    }

    fun minutesUntil(target: LocalTime, now: LocalTime): Long {
        val diffSeconds = target.toSecondOfDay() - now.toSecondOfDay()
        return if (diffSeconds >= 0) diffSeconds / 60L else (24 * 60 + diffSeconds / 60L)
    }

    fun nextPendingSlot(
        slots: List<ScheduleSlot>,
        isPending: (ScheduleSlot) -> Boolean,
        now: LocalTime
    ): ScheduleSlot? {
        if (slots.isEmpty()) return null
        slots.firstOrNull { isPending(it) && it.time.isAfter(now) }?.let { return it }
        if (!slots.any(isPending)) return slots.firstOrNull()
        return slots.firstOrNull()
    }

    fun starterActions(
        schedule: Map<String, List<String>>,
        now: LocalTime,
        activeSlot: ScheduleSlot?,
        nextSlot: ScheduleSlot?
    ): List<String> {
        activeSlot?.let { return it.actions }
        nextSlot?.let { slot ->
            if (minutesUntil(slot.time, now) <= STARTER_LOOKAHEAD_MINUTES) {
                return slot.actions
            }
        }
        val enabled = schedule.values.flatten().map { it.lowercase() }.toSet()
        return buildList {
            if ("pain" in enabled) add("pain")
            if ("temperature" in enabled) add("temperature")
        }.ifEmpty { listOf("pain") }
    }

    fun actionsToMeasurementSteps(actions: List<String>): List<MeasurementStep> {
        val ordered = listOf(
            MeasurementStep.Pain to "pain",
            MeasurementStep.Temperature to "temperature",
            MeasurementStep.Photo to "photo"
        )
        val normalized = actions.map { it.lowercase() }.toSet()
        return ordered.filter { (_, key) -> key in normalized }.map { it.first }
            .ifEmpty { listOf(MeasurementStep.Pain) }
    }

    fun resolveMeasurementContext(
        schedule: Map<String, List<String>>,
        slots: List<ScheduleSlot>,
        now: LocalTime,
        isInitial: Boolean,
        isSlotPending: (ScheduleSlot) -> Boolean
    ): MeasurementContext {
        val activeSlot = slots.firstOrNull { isInMeasurementWindow(now, it.time) && isSlotPending(it) }
        val dueSlot = activeSlot

        val nextSlot = if (dueSlot == null) {
            nextPendingSlot(slots, isSlotPending, now)
        } else {
            slots.firstOrNull { slot ->
                slot.time.isAfter(dueSlot.time) && isSlotPending(slot)
            }
        }

        val showStarterCheckIn = isInitial && dueSlot == null

        val formScheduleKey = dueSlot?.timeKey
            ?: nextSlot?.timeKey
            ?: slots.firstOrNull()?.timeKey
            ?: "08:00"

        val formLabelOverride: String?
        val formStepsOverride: List<MeasurementStep>?

        if (showStarterCheckIn) {
            formLabelOverride = INITIAL_LABEL
            formStepsOverride = actionsToMeasurementSteps(
                starterActions(schedule, now, activeSlot, nextSlot)
            )
        } else {
            formLabelOverride = null
            formStepsOverride = null
        }

        return MeasurementContext(
            dueSlot = dueSlot,
            nextSlot = nextSlot,
            showStarterCheckIn = showStarterCheckIn,
            formScheduleKey = formScheduleKey,
            formLabelOverride = formLabelOverride,
            formStepsOverride = formStepsOverride
        )
    }

    fun replaceSlotTime(
        schedule: Map<String, List<String>>,
        oldTimeKey: String,
        newTime: LocalTime
    ): Map<String, List<String>> {
        val newKey = newTime.format(timeFormatter)
        if (oldTimeKey == newKey) return schedule
        val actions = schedule[oldTimeKey] ?: return schedule
        val updated = schedule.toMutableMap()
        updated.remove(oldTimeKey)
        val merged = (updated[newKey].orEmpty() + actions).distinct()
        updated[newKey] = merged
        return updated.toSortedMap(compareBy { LocalTime.parse(it) })
    }

    fun adaptScheduleToNow(
        schedule: Map<String, List<String>>,
        now: LocalTime
    ): Map<String, List<String>> {
        val slots = parseScheduleSlots(schedule)
        if (slots.isEmpty()) return schedule

        val hasUpcoming = slots.any { it.time.isAfter(now) || isInMeasurementWindow(now, it.time) }
        if (hasUpcoming) return schedule

        val rounded = roundToNextHalfHour(now)
        val starter = starterActions(schedule, now, null, null)
        val adapted = schedule.toMutableMap()
        adapted[rounded.format(timeFormatter)] = starter
        return adapted.toSortedMap(compareBy { LocalTime.parse(it) })
    }

    private fun roundToNextHalfHour(now: LocalTime): LocalTime {
        val minutes = now.hour * 60 + now.minute
        val rounded = ((minutes + 29) / 30) * 30
        val hour = (rounded / 60) % 24
        val minute = rounded % 60
        return LocalTime.of(hour, minute)
    }
}
