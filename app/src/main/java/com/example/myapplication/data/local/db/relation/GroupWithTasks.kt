package com.example.myapplication.data.local.db.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.myapplication.data.local.db.entity.Task
import com.example.myapplication.data.local.db.entity.TaskGroup

data class GroupWithTasks(
    @Embedded
    val group: TaskGroup,

    @Relation(
        parentColumn = "group_id",
        entityColumn = "group_id"
    )
    val tasks: List<Task>
)
