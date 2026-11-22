package com.yg.pvtodo.task

import kotlinx.coroutines.flow.Flow

class TaskRepository(private val dao: TaskDao) {
    fun observeAll(): Flow<List<Task>> = dao.observeAll()
    suspend fun add(title: String, notes: String, due: Long?, priority: Int) {
        dao.insert(Task(title = title, notes = notes, due = due, priority = priority))
    }
    suspend fun toggle(t: Task) = dao.update(t.copy(done = !t.done))
    suspend fun delete(t: Task) = dao.delete(t)
}
