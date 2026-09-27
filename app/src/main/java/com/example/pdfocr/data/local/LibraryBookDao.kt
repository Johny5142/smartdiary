package com.example.pdfocr.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryBookDao {
    @Query("SELECT * FROM library_books ORDER BY lastOpenedAt DESC")
    fun observeLibrary(): Flow<List<LibraryBookEntity>>

    @Query("SELECT * FROM library_books WHERE pdfKey = :pdfKey")
    suspend fun findByKey(pdfKey: String): LibraryBookEntity?

    @Upsert
    suspend fun upsert(book: LibraryBookEntity)

    @Query("UPDATE library_books SET lastPageIndex = :pageIndex, lastOpenedAt = :now WHERE pdfKey = :pdfKey")
    suspend fun updateProgress(pdfKey: String, pageIndex: Int, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM library_books WHERE pdfKey = :pdfKey")
    suspend fun delete(pdfKey: String)
}
