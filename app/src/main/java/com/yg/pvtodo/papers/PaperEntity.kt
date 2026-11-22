package com.yg.pvtodo.papers

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "papers")
data class PaperEntity(
    @PrimaryKey val doi: String,
    val title: String,
    val authors: String,
    val abstract: String,
    val published: String,
    val link: String,
    val source: String,
    val journal: String?,
    val tier: String,
    val fav: Boolean = false,
    val fetchedAt: Long = System.currentTimeMillis()
)
