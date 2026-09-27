package com.example.pdfocr.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "library_books")
data class LibraryBookEntity(
    @PrimaryKey val pdfKey: String,
    val uri: String,
    val displayName: String,
    val pageCount: Int,
    val lastPageIndex: Int,
    val lastOpenedAt: Long = System.currentTimeMillis()
)
