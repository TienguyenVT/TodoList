package com.example.myapplication.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.myapplication.data.local.db.entity.TaskGroup
import com.example.myapplication.data.local.db.relation.GroupWithTasks
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskGroupDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(group: TaskGroup): Long

    @Update
    suspend fun update(group: TaskGroup)

    @Delete
    suspend fun delete(group: TaskGroup)

    @Query("DELETE FROM task_groups WHERE group_id = :groupId")
    suspend fun deleteById(groupId: Int)

    @Query("SELECT * FROM task_groups WHERE group_id = :groupId LIMIT 1")
    suspend fun getById(groupId: Int): TaskGroup?

    @Query("SELECT * FROM task_groups ORDER BY group_name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<TaskGroup>>

    @Transaction
    @Query("SELECT * FROM task_groups ORDER BY group_name COLLATE NOCASE ASC")
    fun observeAllGroupsWithTasks(): Flow<List<GroupWithTasks>>
}
