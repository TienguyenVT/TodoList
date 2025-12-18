package com.example.myapplication.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class Collection(
    val id: Int,
    val name: String,
    val color: Color,
    val icon: ImageVector
)

enum class NavigationItem { MY_DAY, CALENDAR, COLLECTIONS, SETTINGS }