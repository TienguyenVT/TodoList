package com.example.myapplication.model

import java.time.LocalDate
import java.time.YearMonth

data class Event(val id: String, val title: String, val description: String? = null, val isLunar: Boolean = false)

object FestivalUtils {
    // Solar events (cleared)
    private val solarEvents = emptyList<Pair<Event, Pair<Int, Int>>>()

    // Lunar events (cleared)
    private val lunarEvents = emptyList<Pair<Event, Pair<Int, Int>>>()

    fun getEventsForDate(date: LocalDate): List<Event> {
        // All events cleared intentionally for a clean dataset
        return emptyList()
    }

    fun getEventsForMonth(yearMonth: YearMonth): Map<LocalDate, List<Event>> {
        // Return empty map for all months
        return emptyMap()
    }
}
