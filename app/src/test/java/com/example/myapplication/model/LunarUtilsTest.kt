package com.example.myapplication.model

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class LunarUtilsTest {
    @Test
    fun testKnownNewYearDates() {
        // Known Vietnamese Lunar New Year (Tet) dates
        val known = listOf(
            LocalDate.of(2019, 2, 5),  // 1/1/2019 (Lunar)
            LocalDate.of(2020, 1, 25), // 1/1/2020
            LocalDate.of(2021, 2, 12), // 1/1/2021
            LocalDate.of(2022, 2, 1),  // 1/1/2022
            LocalDate.of(2023, 1, 22), // 1/1/2023
            LocalDate.of(2024, 2, 10)  // 1/1/2024
        )

        for (d in known) {
            val lunar = LunarUtils.convertSolar2Lunar(d)
            assertEquals("Solar $d should be lunar 1/1", 1, lunar.day)
            assertEquals("Solar $d should be lunar month 1", 1, lunar.month)
        }
    }

    @Test
    fun testRoundTripConversion() {
        val testDates = listOf(
            LocalDate.of(2018, 12, 31),
            LocalDate.of(2019, 3, 1),
            LocalDate.of(2020, 6, 21),
            LocalDate.of(2021, 9, 10),
            LocalDate.of(2022, 11, 30),
            LocalDate.of(2023, 4, 5)
        )
        for (d in testDates) {
            val lunar = LunarUtils.convertSolar2Lunar(d)
            val solarBack = LunarUtils.convertLunar2Solar(lunar.day, lunar.month, lunar.year, lunar.isLeap)
            assertEquals("Round-trip should return same date for $d", d, solarBack)
        }
    }

    @Test
    fun testEdgeCaseLeapMonthBehavior() {
        // This test checks that the conversion does not throw and produces consistent results
        val d = LocalDate.of(2014, 8, 31) // 2014 had a leap month; usage as a smoke test
        val lunar = LunarUtils.convertSolar2Lunar(d)
        val solarBack = LunarUtils.convertLunar2Solar(lunar.day, lunar.month, lunar.year, lunar.isLeap)
        assertEquals(d, solarBack)
    }
}
