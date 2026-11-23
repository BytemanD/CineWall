package com.cinewall.app.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cinewall.app.model.MediaItem
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(items: List<MediaItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: MediaItem)

    @Query("DELETE FROM media_items")
    suspend fun deleteAll()

    @Delete
    suspend fun delete(item: MediaItem)

    @Query("SELECT * FROM media_items WHERE mediaType = :type ORDER BY title COLLATE NOCASE ASC")
    fun getByType(type: String): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items ORDER BY title COLLATE NOCASE ASC")
    fun getAll(): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items WHERE id = :id")
    suspend fun getById(id: String): MediaItem?

    @Query("SELECT * FROM media_items WHERE scraped = 1 ORDER BY title COLLATE NOCASE ASC")
    fun getScraped(): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items WHERE scraped = 0 ORDER BY title COLLATE NOCASE ASC")
    fun getUnscraped(): Flow<List<MediaItem>>

    @Query("SELECT COUNT(*) FROM media_items")
    suspend fun count(): Int

    @Transaction
    suspend fun replaceAll(items: List<MediaItem>) {
        deleteAll()
        insert(items)
    }
}
