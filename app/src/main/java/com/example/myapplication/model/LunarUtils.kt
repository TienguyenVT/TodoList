package com.example.myapplication.model

import java.time.LocalDate

object LunarUtils {
    // NOTE: This is a lightweight, deterministic approximation for displaying a "lunar" day/month.
    // It is intended as a visual addition only. Replace with a full lunisolar conversion algorithm
    // if you need exact Vietnamese lunisolar calendar dates.

    fun getLunarDisplay(date: LocalDate): String {
        val epoch = date.toEpochDay()
        // approximate lunar day in 1..30
        val lDay = ((epoch + 40) % 30 + 30) % 30 + 1
        // approximate lunar month based on month and epoch
        val lMonth = (((date.monthValue - 1) + ((epoch / 30) % 12).toInt()) % 12 + 12) % 12 + 1
        return String.format("%02d/%02d", lDay, lMonth)
    }
}
