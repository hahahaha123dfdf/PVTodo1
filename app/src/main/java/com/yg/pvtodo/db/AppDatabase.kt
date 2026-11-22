package com.yg.pvtodo.db

import android.content.Context
import androidx.room.*
import com.yg.pvtodo.papers.PaperDao
import com.yg.pvtodo.papers.PaperEntity
import com.yg.pvtodo.task.Task
import com.yg.pvtodo.task.TaskDao

@Database(entities = [Task::class, PaperEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun paperDao(): PaperDao
    companion object {
        @Volatile private var i: AppDatabase? = null
        fun get(ctx: Context): AppDatabase = i ?: synchronized(this) {
            i ?: Room.databaseBuilder(ctx.applicationContext, AppDatabase::class.java, "pvtodo.db")
                .fallbackToDestructiveMigration()
                .build().also { i = it }
        }
    }
}
