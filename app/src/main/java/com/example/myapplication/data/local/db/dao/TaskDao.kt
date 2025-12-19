package com.example.myapplication.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.myapplication.data.local.db.entity.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: Task): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tasks: List<Task>): List<Long>

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteById(taskId: Int)

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    suspend fun getById(taskId: Int): Task?

    @Query("SELECT * FROM tasks ORDER BY due_date IS NULL, due_date ASC, id DESC")
    fun observeAll(): Flow<List<Task>>

    @Query(
        "SELECT * FROM tasks " +
            "WHERE title LIKE '%' || :query || '%' " +
            "ORDER BY due_date IS NULL, due_date ASC, id DESC"
    )
    fun searchByTitle(query: String): Flow<List<Task>>

    @Query(
        "SELECT * FROM tasks " +
            "WHERE due_date IS NOT NULL AND due_date BETWEEN :fromMillis AND :toMillis " +
            "ORDER BY due_date ASC"
    )
    fun observeByDueDateRange(fromMillis: Long, toMillis: Long): Flow<List<Task>>

    @Query(
        "SELECT * FROM tasks " +
            "WHERE group_id = :groupId " +
            "ORDER BY due_date IS NULL, due_date ASC, id DESC"
    )
    fun observeByGroup(groupId: Int): Flow<List<Task>>

    @Query(
        "SELECT * FROM tasks " +
            "WHERE group_id IS NULL " +
            "ORDER BY due_date IS NULL, due_date ASC, id DESC"
    )
    fun observeUncategorized(): Flow<List<Task>>
}
