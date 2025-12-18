package com.example.myapplication.model

import java.time.LocalDate
import java.time.YearMonth

/**
 * Event: mô tả một sự kiện/lễ hội Việt Nam đã được “resolve” ra ngày dương lịch cụ thể.
 *
 * @param title       Tên sự kiện (ví dụ: "Tết Nguyên đán")
 * @param description Mô tả ngắn (tùy chọn)
 * @param isLunar     true nếu bản chất sự kiện là theo âm lịch (dùng để tô màu/hiển thị khác),
 *                    false nếu là sự kiện thuần dương lịch.
 */
data class Event(
    val title: String,
    val description: String? = null,
    val isLunar: Boolean
)

/**
 * FestivalUtils
 *
 * Cung cấp danh sách các ngày lễ, tết và sự kiện lớn của Việt Nam.
 * Hiện tại dữ liệu được hard-code để:
 *  - Không phụ thuộc mạng / API ngoài
 *  - Đơn giản, gọn nhẹ, dễ bảo trì
 * Sau này, nếu cần có thể thay thế bằng nguồn dữ liệu ngoài mà không phải
 * sửa `CalendarScreen` vì API vẫn giữ nguyên.
 */
object FestivalUtils {

    // Định nghĩa cho các sự kiện theo dương lịch (ngày/tháng cố định mỗi năm)
    private data class SolarEventDef(
        val month: Int,
        val day: Int,
        val title: String,
        val description: String? = null
    )

    // Định nghĩa cho các sự kiện theo âm lịch (ngày/tháng âm, có thể nhuận)
    private data class LunarEventDef(
        val lunarMonth: Int,
        val lunarDay: Int,
        val isLeap: Boolean = false,
        val title: String,
        val description: String? = null
    )

    // Một số ngày lễ/sự kiện dương lịch tiêu biểu tại Việt Nam
    private val solarEvents = listOf(
        SolarEventDef(1, 1, "Tết Dương lịch", "Nghỉ lễ đầu năm mới"),
        SolarEventDef(2, 14, "Lễ Tình nhân", "Valentine (không phải ngày nghỉ chính thức)"),
        SolarEventDef(3, 8, "Ngày Quốc tế Phụ nữ", "Tôn vinh phụ nữ"),
        SolarEventDef(4, 30, "Ngày Giải phóng miền Nam", "30/4 - Ngày thống nhất đất nước"),
        SolarEventDef(5, 1, "Ngày Quốc tế Lao động", "01/5 - Nghỉ lễ toàn quốc"),
        SolarEventDef(9, 2, "Quốc khánh Việt Nam", "02/9 - Nghỉ lễ toàn quốc"),
        SolarEventDef(11, 20, "Ngày Nhà giáo Việt Nam", "20/11"),
        SolarEventDef(12, 24, "Đêm Giáng sinh", "24/12 - Christmas Eve"),
        SolarEventDef(12, 25, "Lễ Giáng sinh", "25/12 - Christmas Day")
    )

    // Một số ngày lễ âm lịch lớn (dùng lịch Việt Nam)
    private val lunarEvents = listOf(
        LunarEventDef(1, 1, false, "Tết Nguyên đán", "Mùng 1 Tết âm lịch"),
        LunarEventDef(1, 2, false, "Mùng 2 Tết", "Ngày thứ 2 của Tết Nguyên đán"),
        LunarEventDef(1, 3, false, "Mùng 3 Tết", "Ngày thứ 3 của Tết Nguyên đán"),
        LunarEventDef(1, 15, false, "Rằm tháng Giêng", "Lễ Thượng Nguyên (Tết Nguyên tiêu)"),
        LunarEventDef(3, 10, false, "Giỗ Tổ Hùng Vương", "Lễ hội Đền Hùng"),
        LunarEventDef(5, 5, false, "Tết Đoan Ngọ", "Tết diệt sâu bọ"),
        LunarEventDef(7, 15, false, "Rằm tháng Bảy", "Lễ Vu Lan, Xá tội vong nhân"),
        LunarEventDef(8, 15, false, "Tết Trung Thu", "Rằm tháng Tám"),
        LunarEventDef(12, 23, false, "Ông Công Ông Táo", "Tiễn Táo Quân chầu trời")
    )

    /**
     * Lấy danh sách sự kiện đúng vào một ngày dương lịch cụ thể.
     */
    fun getEventsForDate(date: LocalDate): List<Event> {
        val result = mutableListOf<Event>()

        // 1. Sự kiện thuần dương lịch
        solarEvents
            .filter { it.month == date.monthValue && it.day == date.dayOfMonth }
            .forEach { def ->
                result += Event(
                    title = def.title,
                    description = def.description,
                    isLunar = false
                )
            }

        // 2. Sự kiện gốc âm lịch: chuyển ngày dương -> âm rồi đối chiếu
        try {
            val lunar = LunarUtils.convertSolar2Lunar(date)
            lunarEvents
                .filter {
                    it.lunarMonth == lunar.month &&
                            it.lunarDay == lunar.day &&
                            it.isLeap == lunar.isLeap
                }
                .forEach { def ->
                    result += Event(
                        title = def.title,
                        description = def.description,
                        isLunar = true
                    )
                }
        } catch (_: Exception) {
            // Nếu chuyển đổi âm lịch lỗi thì bỏ qua phần sự kiện âm lịch,
            // vẫn trả về các sự kiện dương lịch (nếu có).
        }

        return result
    }

    /**
     * Lấy toàn bộ sự kiện trong một tháng dương lịch.
     *
     * Trả về map từ ngày dương lịch -> danh sách Event của ngày đó.
     */
    fun getEventsForMonth(yearMonth: YearMonth): Map<LocalDate, List<Event>> {
        val map = mutableMapOf<LocalDate, MutableList<Event>>()
        val daysInMonth = yearMonth.lengthOfMonth()

        for (d in 1..daysInMonth) {
            val date = yearMonth.atDay(d)
            val eventsForDay = getEventsForDate(date)
            if (eventsForDay.isNotEmpty()) {
                map.getOrPut(date) { mutableListOf() }.addAll(eventsForDay)
            }
        }

        return map
    }
}

