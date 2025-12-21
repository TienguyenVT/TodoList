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
                }
        }
    }
}
