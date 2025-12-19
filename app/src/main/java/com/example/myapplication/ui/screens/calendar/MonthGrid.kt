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
import androidx.compose.material3.MaterialTheme
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
import com.example.myapplication.model.Event
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
    val isDebuggable = remember(context) {
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }
    val firstOfMonth = yearMonth.atDay(1)
    val daysInMonth = yearMonth.lengthOfMonth()
    val startOffset = (firstOfMonth.dayOfWeek.value + 6) % 7 // Monday=0 .. Sunday=6
    val dayList = (1..daysInMonth).toList()

    val indicators by remember(tasksByDate, yearMonth) {
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

    val eventsMap by remember(yearMonth) {
        derivedStateOf { FestivalUtils.getEventsForMonth(yearMonth).mapKeys { entry -> entry.key.dayOfMonth } }
    }

    val lunarMap by remember(yearMonth) {
        derivedStateOf {
            val m = mutableMapOf<Int, LunarUtils.LunarDate?>()
            for (d in 1..daysInMonth) {
                val date = yearMonth.atDay(d)
                try {
                    m[d] = LunarUtils.convertSolar2Lunar(date)
                } catch (ex: IllegalArgumentException) {
                    Log.e("CalendarScreen", "Lunar conversion failed for date=$date: ${ex.message}", ex)
                    m[d] = null
                } catch (ex: ArithmeticException) {
                    Log.e("CalendarScreen", "Lunar conversion arithmetic error for date=$date: ${ex.message}", ex)
                    m[d] = null
                } catch (ex: Exception) {
                    Log.e("CalendarScreen", "Unexpected error converting lunar for date=$date", ex)
                    if (isDebuggable) throw ex else m[d] = null
                }
            }
            m
        }
    }

    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
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

        Spacer(modifier = Modifier.height(8.dp))

        val totalCells = startOffset + daysInMonth
        val rows = (totalCells + 6) / 7 // integer ceil
        val cellSize = 44.dp
        val verticalSpacing = 4.dp
        val gridHeight = (cellSize * rows) + (verticalSpacing * (rows - 1)) + 8.dp

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.height(gridHeight),
            horizontalArrangement = Arrangement.spacedBy(verticalSpacing),
            verticalArrangement = Arrangement.spacedBy(verticalSpacing),
            content = {
                val padList = List(startOffset) { it }
                items(padList, key = { "pad_$it" }) { Box(Modifier.size(cellSize)) }

                items(items = dayList, key = { d -> yearMonth.atDay(d).toEpochDay() }) { day ->
                    val date = yearMonth.atDay(day)
                    val isSelected = date == selectedDate
                    val isToday = date == LocalDate.now()
                    val priorities = indicators[day] ?: emptySet()

                    CalendarDayCell(
                        day = day,
                        cellSize = cellSize,
                        isSelected = isSelected,
                        isToday = isToday,
                        priorities = priorities,
                        events = eventsMap[day] ?: emptyList(),
                        lunar = lunarMap[day],
                        onClick = { onSelectDate(date) }
                    )
                }
            }
        )
    }
}

@Composable
fun CalendarDayCell(
    day: Int,
    cellSize: androidx.compose.ui.unit.Dp,
    isSelected: Boolean,
    isToday: Boolean,
    priorities: Set<Priority>,
    events: List<Event>,
    lunar: LunarUtils.LunarDate?,
    onClick: () -> Unit
) {
    val accent = NeumorphicColors.accentBlue
    val containerColor = when {
        isSelected -> accent
        isToday -> NeumorphicColors.accentMint.copy(0.12f)
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
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = day.toString(),
                fontSize = 13.sp,
                fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else NeumorphicColors.textPrimary
            )

            lunar?.let {
                val lm = if (it.isLeap) "L${it.month}" else it.month.toString()
                Text(
                    text = "${it.day}/${lm}",
                    fontSize = 9.sp,
                    color = NeumorphicColors.textSecondary
                )
            }
        }

        if (priorities.isNotEmpty() || events.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 15.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (Priority.LOW in priorities) {
                    Dot(color = NeumorphicColors.priorityLow)
                }
                if (Priority.NORMAL in priorities) {
                    Dot(color = NeumorphicColors.priorityNormal)
                }
                if (Priority.HIGH in priorities) {
                    Dot(color = NeumorphicColors.priorityHigh)
                }
                if (events.isNotEmpty()) {
                    Dot(color = NeumorphicColors.accentBlue)
                }
            }
        }
    }
}
