package com.example.myapplication.model

import com.example.myapplication.data.local.db.entity.Task as DbTask
import com.example.myapplication.data.local.db.entity.TaskGroup as DbTaskGroup
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

fun DbTask.toUiTask(): Task {
    val zone = ZoneId.of("UTC")
    return Task(
        id = id,
        title = title,
        dueDate = dueDate?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() },
        priority = priority.toUiPriority(),
        status = status,
        isCompleted = status == 1,
        collectionId = groupId,
        description = description,
        imageUri = imagePath
    )
}

fun Int.toUiPriority(): Priority {
    return when (this) {
        0 -> Priority.LOW
        2 -> Priority.HIGH
        else -> Priority.NORMAL
    }
}

fun Priority.toDbPriority(): Int {
    return when (this) {
        Priority.LOW -> 0
        Priority.NORMAL -> 1
        Priority.HIGH -> 2
    }
}

fun LocalDate.toEpochMillis(): Long {
    return atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
}

fun DbTaskGroup.toUiCollection(): Collection {
    return Collection(
        id = groupId,
        name = groupName,
        color = Color(groupColor),
        icon = Icons.Default.List
    )
}
