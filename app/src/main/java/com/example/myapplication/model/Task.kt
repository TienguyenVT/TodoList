package com.example.myapplication.model

import java.time.LocalDate

enum class Priority { LOW, NORMAL, HIGH }

data class Task(
    val id: Int,
    val title: String,
    val dueDate: LocalDate? = null,
    val priority: Priority = Priority.NORMAL,
    var isCompleted: Boolean = false,
    val collectionId: Int? = null,
    val description: String? = null,
    val imageUri: String? = null
)