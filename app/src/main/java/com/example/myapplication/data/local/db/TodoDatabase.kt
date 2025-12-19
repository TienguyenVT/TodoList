package com.example.myapplication.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.myapplication.data.local.db.dao.TaskDao
import com.example.myapplication.data.local.db.dao.TaskGroupDao
import com.example.myapplication.data.local.db.entity.Task
import com.example.myapplication.data.local.db.entity.TaskGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TaskGroup::class,
        Task::class
    ],
    version = 1,
    exportSchema = false
)
abstract class TodoDatabase : RoomDatabase() {

    abstract fun taskGroupDao(): TaskGroupDao

    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: TodoDatabase? = null

        fun getInstance(context: Context): TodoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TodoDatabase::class.java,
                    "todo_db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(PrepopulateCallback)
                    .build()

                INSTANCE = instance
                instance
            }
        }

        private object PrepopulateCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                val instance = INSTANCE ?: return

                CoroutineScope(Dispatchers.IO).launch {
                    val groupDao = instance.taskGroupDao()
                    val taskDao = instance.taskDao()

                    val workGroupId = groupDao.upsert(
                        TaskGroup(
                            groupName = "Work Project",
                            groupColor = 0xFF3F51B5.toInt(),
                            description = "Sample group"
                        )
                    ).toInt()

                    groupDao.upsert(
                        TaskGroup(
                            groupName = "Vacation",
                            groupColor = 0xFFFF9800.toInt(),
                            description = null
                        )
                    )

                    taskDao.upsert(
                        Task(
                            title = "Welcome to ZenTask",
                            description = "This is a pre-populated task.",
                            progressNotes = null,
                            dueDate = null,
                            urlLink = null,
                            imagePath = null,
                            status = 0,
                            priority = 1,
                            groupId = workGroupId
                        )
                    )
                }
            }
        }
    }
}
