package com.example.pdfocr.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE pdfKey = :pdfKey ORDER BY pageIndex ASC, createdAt DESC")
    fun observeBookmarks(pdfKey: String): Flow<List<BookmarkEntity>>

    @Query("SELECT COUNT(*) FROM bookmarks WHERE pdfKey = :pdfKey AND pageIndex = :pageIndex")
    suspend fun countForPage(pdfKey: String, pageIndex: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: BookmarkEntity): Long

    @Query("DELETE FROM bookmarks WHERE pdfKey = :pdfKey AND pageIndex = :pageIndex")
    suspend fun deleteForPage(pdfKey: String, pageIndex: Int)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteById(id: Long)
}
