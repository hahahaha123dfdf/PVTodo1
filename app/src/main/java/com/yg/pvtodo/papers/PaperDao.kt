package com.yg.pvtodo.papers

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PaperDao {
    @Query("SELECT * FROM papers ORDER BY fav DESC, fetchedAt DESC")
    fun observeAll(): Flow<List<PaperEntity>>
    @Query("SELECT doi FROM papers")
    suspend fun allDois(): List<String>
    @Query("UPDATE papers SET fav = :fav WHERE doi = :doi")
    suspend fun setFav(doi: String, fav: Boolean)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(list: List<PaperEntity>)
    @Query("SELECT COUNT(*) FROM papers")
    suspend fun count(): Int
    @Query("DELETE FROM papers WHERE fav = 0")
    suspend fun clearUnfav()
}
