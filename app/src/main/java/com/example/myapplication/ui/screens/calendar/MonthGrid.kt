package com.example.myapplication.ui.screens.calendar

import android.content.pm.ApplicationInfo
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.model.FestivalUtils
import com.example.myapplication.model.Priority
import com.example.myapplication.model.Task
import com.example.myapplication.model.LunarUtils
import com.example.myapplication.ui.theme.NeumorphicColors
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CompactMonthGrid(
    yearMonth: YearMonth,
    tasksByDate: Map<LocalDate, List<Task>>,
    selectedDate: LocalDate?,
    onSelectDate: (LocalDate) -> Unit,
    onToggleMonth: () -> Unit
) {
    val context = LocalContext.current
    val isDebuggable = remember(context) { (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0 }
    
    val firstOfMonth = yearMonth.atDay(1)
    val daysInMonth = yearMonth.lengthOfMonth()
    val startOffset = (firstOfMonth.dayOfWeek.value + 6) % 7
    val dayList = (1..daysInMonth).toList()

    val indicators by rememberIndicators(tasksByDate, yearMonth, daysInMonth)
    val eventsMap by rememberEvents(yearMonth)
    val lunarMap by rememberLunarMap(yearMonth, daysInMonth, isDebuggable)

    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        MonthWeekdayHeader()
        Spacer(modifier = Modifier.height(8.dp))

        val rows = (startOffset + daysInMonth + 6) / 7
        val cellSize = 44.dp
        val verticalSpacing = 4.dp
        val gridHeight = (cellSize * rows) + (verticalSpacing * (rows - 1)) + 8.dp

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.height(gridHeight),
            horizontalArrangement = Arrangement.spacedBy(verticalSpacing),
            verticalArrangement = Arrangement.spacedBy(verticalSpacing)
        ) {
            val padList = List(startOffset) { it }
            items(padList, key = { "pad_$it" }) { Box(Modifier.size(cellSize)) }

            items(items = dayList, key = { d -> yearMonth.atDay(d).toEpochDay() }) { day ->
                val date = yearMonth.atDay(day)
                val state = DayState(
                    isSelected = date == selectedDate,
                    isToday = date == LocalDate.now(),
                    priorities = indicators[day] ?: emptySet(),
                    events = eventsMap[day] ?: emptyList(),
                    lunar = lunarMap[day]
                )

                CalendarDayCell(
                    day = day,
                    cellSize = cellSize,
                    dayState = state,
                    onClick = { onSelectDate(date) }
                )
            }
        }
    }
}

private const val TAG = "CalendarScreen"

@Composable
private fun rememberIndicators(tasksByDate: Map<LocalDate, List<Task>>, yearMonth: YearMonth, daysInMonth: Int) = remember(tasksByDate, yearMonth) {
    derivedStateOf {
        val map = mutableMapOf<Int, Set<Priority>>()
        for (d in 1..daysInMonth) {
            val date = yearMonth.atDay(d)
            val priorities = tasksByDate[date]?.map { it.priority }?.toSet() ?: emptySet()
            if (priorities.isNotEmpty()) map[d] = priorities
        }
        map
    }
}

@Composable
private fun rememberEvents(yearMonth: YearMonth) = remember(yearMonth) {
    derivedStateOf { FestivalUtils.getEventsForMonth(yearMonth).mapKeys { entry -> entry.key.dayOfMonth } }
}

@Composable
private fun rememberLunarMap(yearMonth: YearMonth, daysInMonth: Int, isDebuggable: Boolean) = remember(yearMonth) {
    derivedStateOf {
        val m = mutableMapOf<Int, LunarUtils.LunarDate?>()
        for (d in 1..daysInMonth) {
            val date = yearMonth.atDay(d)
            try {
                m[d] = LunarUtils.convertSolar2Lunar(date)
            } catch (ex: Exception) {
                // Catching generic Exception because calculation errors might vary
                Log.e(TAG, "Lunar error date=$date", ex)
                if (isDebuggable && ex !is IllegalArgumentException && ex !is ArithmeticException) throw ex
                m[d] = null
            }
        }
        m
    }
}

@Composable
fun MonthWeekdayHeader() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        val weekdayLabels = stringArrayResource(id = R.array.weekday_short)
        weekdayLabels.forEach { dow ->
            Text(
                text = dow,
                fontSize = 12.sp,
                color = NeumorphicColors.textSecondary,
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun CalendarDayCell(
    day: Int,
    cellSize: androidx.compose.ui.unit.Dp,
    dayState: DayState,
    onClick: () -> Unit
) {
    val containerColor = when {
        dayState.isSelected -> NeumorphicColors.accentBlue
        dayState.isToday -> NeumorphicColors.accentMint.copy(0.12f)
        else -> NeumorphicColors.surface
    }

    Box(
        modifier = Modifier
            .size(cellSize)
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.align(Alignment.TopCenter)) {
            CalendarDayNumber(day, dayState)
        }
        Box(Modifier.align(Alignment.BottomCenter)) {
            DayIndicators(dayState)
        }
    }
}
