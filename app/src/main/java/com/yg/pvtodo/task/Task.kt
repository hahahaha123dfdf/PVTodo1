package com.yg.pvtodo.task

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val notes: String = "",
    val due: Long? = null,
    val priority: Int = 1,
    val done: Boolean = false,
    val created: Long = System.currentTimeMillis()
)
