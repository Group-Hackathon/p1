package com.preappointment1.app.schedule

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object ScheduleDefaults {
    private const val PREFS = "schedule_defaults"
    private const val KEY_SCHEDULE = "default_schedule_json"

    val fallback: Map<String, List<String>> = mapOf(
        "08:00" to listOf("pain", "temperature"),
        "20:00" to listOf("pain", "temperature", "photo")
    )

    fun load(context: Context): Map<String, List<String>> {
        val json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_SCHEDULE, null) ?: return fallback
        return runCatching {
            val type = object : TypeToken<Map<String, List<String>>>() {}.type
            Gson().fromJson<Map<String, List<String>>>(json, type)
        }.getOrDefault(fallback)
    }

    fun save(context: Context, schedule: Map<String, List<String>>) {
        val json = Gson().toJson(schedule)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SCHEDULE, json)
            .apply()
    }
}
