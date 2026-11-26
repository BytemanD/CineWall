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

    @Query("SELECT * FROM media_items WHERE isAvailable = 1 AND mediaType = :type ORDER BY title COLLATE NOCASE ASC")
    fun getByType(type: String): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items WHERE isAvailable = 1 ORDER BY title COLLATE NOCASE ASC")
    fun getAll(): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items WHERE id = :id")
    suspend fun getById(id: String): MediaItem?

    /** 查询单条（含不可用记录，用于扫描恢复逻辑） */
    @Query("SELECT * FROM media_items WHERE id = :id")
    suspend fun getByIdIncludeUnavailable(id: String): MediaItem?

    /** 查询全部记录（含不可用记录，用于扫描增量对比） */
    @Query("SELECT * FROM media_items ORDER BY title COLLATE NOCASE ASC")
    suspend fun getAllIncludeUnavailable(): List<MediaItem>

    @Query("SELECT * FROM media_items WHERE isAvailable = 1 AND scraped = 1 ORDER BY title COLLATE NOCASE ASC")
    fun getScraped(): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items WHERE isAvailable = 1 AND scraped = 0 ORDER BY title COLLATE NOCASE ASC")
    fun getUnscraped(): Flow<List<MediaItem>>

    /** 批量标记文件可用性（扫描时未命中标记 false，再次命中恢复 true） */
    @Query("UPDATE media_items SET isAvailable = :available WHERE filePath IN (:paths)")
    suspend fun updateAvailability(paths: List<String>, available: Boolean)

    @Query("SELECT COUNT(*) FROM media_items")
    suspend fun count(): Int

    @Transaction
    suspend fun replaceAll(items: List<MediaItem>) {
        deleteAll()
        insert(items)
    }
}
