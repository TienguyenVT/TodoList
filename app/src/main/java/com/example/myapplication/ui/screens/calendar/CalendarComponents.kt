package com.example.myapplication.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.model.Event
import com.example.myapplication.model.LunarUtils
import com.example.myapplication.model.Priority
import com.example.myapplication.model.Task
import com.example.myapplication.ui.theme.NeumorphicColors

data class DayState(
    val isSelected: Boolean,
    val isToday: Boolean,
    val priorities: Set<Priority>,
    val events: List<Event>,
    val lunar: LunarUtils.LunarDate?
)

@Composable
fun DayIndicators(dayState: DayState) {
    if (dayState.priorities.isNotEmpty() || dayState.events.isNotEmpty()) {
        Row(
            modifier = Modifier.padding(bottom = 15.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (Priority.LOW in dayState.priorities) Dot(color = NeumorphicColors.priorityLow)
            if (Priority.NORMAL in dayState.priorities) Dot(color = NeumorphicColors.priorityNormal)
            if (Priority.HIGH in dayState.priorities) Dot(color = NeumorphicColors.priorityHigh)
            if (dayState.events.isNotEmpty()) Dot(color = NeumorphicColors.accentBlue)
        }
    }
}

@Composable
fun CalendarDayNumber(day: Int, dayState: DayState) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = day.toString(),
            fontSize = 13.sp,
            fontWeight = if (dayState.isToday) FontWeight.SemiBold else FontWeight.Normal,
            color = if (dayState.isSelected) MaterialTheme.colorScheme.onPrimary else NeumorphicColors.textPrimary
        )

        dayState.lunar?.let {
            val lm = if (it.isLeap) "L${it.month}" else it.month.toString()
            Text(
                text = "${it.day}/${lm}",
                fontSize = 9.sp,
                color = NeumorphicColors.textSecondary
            )
        }
    }
}

@Composable
fun Dot(color: androidx.compose.ui.graphics.Color) {
    Box(
        Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
fun CompactTaskItem(
    task: Task,
    collectionNameMap: Map<Int, String>,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.all_day),
            fontSize = 12.sp,
            color = NeumorphicColors.textSecondary,
            modifier = Modifier.width(52.dp),
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(task.title, fontSize = 14.sp, color = NeumorphicColors.textPrimary, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(2.dp))
            val meta = buildString {
                val priorityLabel = when (task.priority) {
                    Priority.HIGH -> stringResource(R.string.priority_high)
                    Priority.NORMAL -> stringResource(R.string.priority_normal)
                    Priority.LOW -> stringResource(R.string.priority_low)
                }
                append(priorityLabel)
                task.collectionId?.let { cid ->
                    val name = collectionNameMap[cid] ?: cid.toString()
                    append(" • ")
                    append(stringResource(R.string.collection_prefix, name))
                }
            }
            Text(meta, fontSize = 12.sp, color = NeumorphicColors.textSecondary)
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(
                modifier = Modifier.size(28.dp),
                shape = RoundedCornerShape(6.dp),
                colors = CardDefaults.cardColors(containerColor = if (task.isCompleted) NeumorphicColors.accentMint else NeumorphicColors.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onToggle() },
                    contentAlignment = Alignment.Center
                ) {
                    if (task.isCompleted) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = stringResource(R.string.cd_done),
                            tint = NeumorphicColors.textPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.cd_delete),
                    tint = NeumorphicColors.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
