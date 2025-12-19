package com.example.myapplication.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "task_groups"
)
data class TaskGroup(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "group_id")
    val groupId: Int = 0,

    @ColumnInfo(name = "group_name")
    val groupName: String,

    @ColumnInfo(name = "group_color")
    val groupColor: Int,

    @ColumnInfo(name = "description")
    val description: String? = null
)
