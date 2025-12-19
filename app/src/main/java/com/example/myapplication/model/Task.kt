package com.example.myapplication.model

import java.time.LocalDate

enum class Priority { LOW, NORMAL, HIGH }

data class Task(
    val id: Int,
    val title: String,
    val dueDate: LocalDate? = null,
    val priority: Priority = Priority.NORMAL,
    /**
     * Raw status from DB: 0 = uncompleted, 1 = completed, 2 = in-progress.
     * Kept mainly for Kanban grouping; other screens rely on isCompleted.
     */
    val status: Int = 0,
    var isCompleted: Boolean = false,
    val collectionId: Int? = null,
    val description: String? = null,
    val imageUri: String? = null
)