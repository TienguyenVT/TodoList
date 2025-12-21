package com.example.myapplication.model

import android.util.Log
import java.time.LocalDate
import kotlin.math.*

/**
 * LunarUtils
 *
 * Full lunisolar conversion (solar <-> lunar) based on widely used astronomical
 * algorithms (new moon, sun longitude) as implemented in common open-source
 * converters (amlich.js style). This implementation uses the Vietnam time zone
 * by default (UTC+7) and exposes clear APIs.
 *
 * TODO: Replace or verify against an authoritative astronomical library
 * or official Vietnamese lunar calendar source for production-critical uses.
 * If that external library is not available, this code falls back to the
 * internal algorithm implemented here.
 */
object LunarUtils {
    data class LunarDate(val day: Int, val month: Int, val year: Int, val isLeap: Boolean)

    private const val PI = Math.PI

    // Convert a Gregorian date to Julian day number (integer)
    private fun jdFromDate(dd: Int, mm: Int, yy: Int): Int {
        var a = (14 - mm) / 12
        val y = yy + 4800 - a
        val m = mm + 12 * a - 3
        var jd = dd + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045
        if (jd < 2299161) jd = dd + (153 * m + 2) / 5 + 365 * y + y / 4 - 32083
        return jd
    }

    // Normalize angle to (0..2*PI)
    private fun normalize(d: Double): Double {
        var x = d
        while (x < 0) x += 2 * PI
        while (x >= 2 * PI) x -= 2 * PI
        return x
    }

    // Astronomical calculation of new moon (returns Julian day number as double)
    private fun newMoon(k: Int): Double {
        val T = k / 1236.85
        val T2 = T * T
        val T3 = T2 * T
        val dr = PI / 180.0
        var Jd1 = 2415020.75933 + 29.53058868 * k + 0.0001178 * T2 - 0.000000155 * T3
        Jd1 += 0.00033 * sin((166.56 + 132.87 * T - 0.009173 * T2) * dr)
        val M = 359.2242 + 29.10535608 * k - 0.0000333 * T2 - 0.00000347 * T3
        val Mpr = 306.0253 + 385.81691806 * k + 0.0107306 * T2 + 0.00001236 * T3
        val F = 21.2964 + 390.67050646 * k - 0.0016528 * T2 - 0.00000239 * T3
        var C1 = (0.1734 - 0.000393 * T) * sin(M * dr) + 0.0021 * sin(2 * M * dr) - 0.4068 * sin(Mpr * dr)
        C1 += 0.0161 * sin(2 * Mpr * dr) - 0.0004 * sin(3 * Mpr * dr)
        C1 += 0.0104 * sin(2 * F * dr) - 0.0051 * sin((M + Mpr) * dr) - 0.0074 * sin((M - Mpr) * dr)
        C1 += 0.0004 * sin((2 * F + M) * dr) - 0.0004 * sin((2 * F - M) * dr)
        C1 += 0.0006 * sin((2 * F + Mpr) * dr) - 0.0010 * sin((2 * F - Mpr) * dr) - 0.0005 * sin((2 * Mpr + M) * dr)
        var deltaT = 0.0
        if (T < -11) deltaT = 0.001 + 0.000839 * T + 0.0002261 * T2 - 0.00000845 * T3 - 0.000000081 * T * T3
        else deltaT = -0.000278 + 0.000265 * T + 0.000262 * T2
        return Jd1 + C1 - deltaT
    }

    // Get the day (integer) of the kth new moon in a given timezone
    private fun getNewMoonDay(k: Int, timeZone: Double): Int {
        val jd = newMoon(k)
        return floor(jd + 0.5 + timeZone / 24.0).toInt()
    }

    // Sun longitude at jdn
    private fun sunLongitude(jdn: Int, timeZone: Double): Int {
        val T = (jdn - 2451545 - timeZone / 24.0) / 36525.0
        val T2 = T * T
        val dr = PI / 180.0
        var M = 357.52910 + 35999.05030 * T - 0.0001559 * T2 - 0.00000048 * T * T2
        var L0 = 280.46645 + 36000.76983 * T + 0.0003032 * T2
        var DL = (1.914600 - 0.004817 * T - 0.000014 * T2) * sin(M * dr)
        DL += (0.019993 - 0.000101 * T) * sin(2 * M * dr) + 0.000290 * sin(3 * M * dr)
        var L = L0 + DL
        L = normalize(L * dr)
        return floor(L / PI * 180.0 / 30.0).toInt()
    }

    private fun getLunarMonth11(yy: Int, timeZone: Double): Int {
        val off = jdFromDate(31, 12, yy) - 2415021
        val k = floor(off / 29.530588853).toInt()
        var nm = getNewMoonDay(k, timeZone)
        var sunLong = sunLongitude(nm, timeZone)
        if (sunLong >= 9) nm = getNewMoonDay(k - 1, timeZone)
        return nm
    }

    private fun getLeapMonthOffset(a11: Int, timeZone: Double): Int {
        var k = floor(0.5 + (a11 - 2415021.076998695) / 29.530588853).toInt()
        var last = 0
        var i = 1
        var arc = sunLongitude(getNewMoonDay(k + i, timeZone), timeZone)
        do {
            last = arc
            i++
            arc = sunLongitude(getNewMoonDay(k + i, timeZone), timeZone)
        } while (arc != last && i < 14)
        return i - 1
    }

    fun convertSolar2Lunar(date: LocalDate, timeZone: Double = 7.0): LunarDate {
        val dayNumber = jdFromDate(date.dayOfMonth, date.monthValue, date.year)
        val k = floor((dayNumber - 2415021.076998695) / 29.530588853).toInt()
        
        var monthStart = getNewMoonDay(k + 1, timeZone)
        if (monthStart > dayNumber) monthStart = getNewMoonDay(k, timeZone)
        
        val lunarYearData = getLunarYearAndMonths(date.year, monthStart, timeZone)
        val a11 = lunarYearData.first
        val b11 = lunarYearData.second
        val lunarYear = lunarYearData.third

        val monthData = calculateLunarMonth(monthStart, a11, b11, timeZone)
        
        return LunarDate(
            day = dayNumber - monthStart + 1,
            month = monthData.first,
            year = lunarYear - if (monthData.first >= 11 && (monthNumber(monthStart, a11) < 4)) 1 else 0,
            isLeap = monthData.second
        )
    }

    private fun monthNumber(monthStart: Int, a11: Int): Int = floor((monthStart - a11) / 29.0).toInt()

    private fun getLunarYearAndMonths(year: Int, monthStart: Int, timeZone: Double): Triple<Int, Int, Int> {
        var a11 = getLunarMonth11(year, timeZone)
        if (a11 >= monthStart) return Triple(getLunarMonth11(year - 1, timeZone), a11, year)
        return Triple(a11, getLunarMonth11(year + 1, timeZone), year)
    }

    private fun calculateLunarMonth(monthStart: Int, a11: Int, b11: Int, timeZone: Double): Pair<Int, Boolean> {
        val diff = floor((monthStart - a11) / 29.0).toInt()
        var lunarMonth = diff + 11
        var isLeap = false
        
        if (b11 - a11 > 365) {
            val leapMonthDiff = getLeapMonthOffset(a11, timeZone)
            if (diff >= leapMonthDiff) {
                lunarMonth = diff + 10
                if (diff == leapMonthDiff) isLeap = true
            }
        }
        if (lunarMonth > 12) lunarMonth -= 12
        return Pair(lunarMonth, isLeap)
    }

    fun convertLunar2Solar(lDay: Int, lMonth: Int, lYear: Int, isLeap: Boolean = false, timeZone: Double = 7.0): LocalDate {
        val a11 = getLunarMonth11(lYear - if (lMonth > 11) 1 else 0, timeZone)
        val b11 = getLunarMonth11(lYear + 1, timeZone)
        val k = floor(0.5 + (a11 - 2415021.076998695) / 29.530588853).toInt()
        
        val off = calculateSolarOffset(lMonth, isLeap, a11, b11, timeZone)
        val monthStart = getNewMoonDay(k + off, timeZone)
        val jd = monthStart + lDay - 1
        
        return jdToDate(jd)
    }

    private fun calculateSolarOffset(lMonth: Int, isLeap: Boolean, a11: Int, b11: Int, timeZone: Double): Int {
        var off = lMonth - 11
        if (off < 0) off += 12
        if (b11 - a11 > 365) {
            val leapOff = getLeapMonthOffset(a11, timeZone)
            if (!isLeap && off >= leapOff) off += 1
        }
        return off
    }

    private fun jdToDate(jd: Int): LocalDate {
        var Z = jd
        var A = Z
        if (Z >= 2299161) {
            val alpha = ((Z - 1867216.25) / 36524.25).toInt()
            A = Z + 1 + alpha - alpha / 4
        }
        val B = A + 1524
        val C = ((B - 122.1) / 365.25).toInt()
        val D = (365.25 * C).toInt()
        val E = ((B - D) / 30.6001).toInt()
        val day = B - D - (30.6001 * E).toInt()
        val month = if (E < 14) E - 1 else E - 13
        val year = if (month > 2) C - 4716 else C - 4715
        return LocalDate.of(year, month, day)
    }

    fun getLunarDisplay(date: LocalDate): String {
        return try {
            val l = convertSolar2Lunar(date)
            val leapStr = if (l.isLeap) " (N)" else ""
            "${l.day}/${l.month.toString().padStart(2, '0')}$leapStr"
        } catch (ex: Exception) {
            // Log the failure and fallback to the old lightweight approximation
            Log.e("LunarUtils", "convertSolar2Lunar failed for date: $date", ex)
            val epoch = date.toEpochDay()
            val lDay = ((epoch + 40) % 30 + 30) % 30 + 1
            val lMonth = (((date.monthValue - 1) + ((epoch / 30) % 12).toInt()) % 12 + 12) % 12 + 1
            "${lDay.toString().padStart(2, '0')}/${lMonth.toString().padStart(2, '0')}"
        }
    }
}

