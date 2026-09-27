package com.example.pdfocr.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pdfKey: String,          // sha256(uri + size + displayName), persisted across re-picks
    val uri: String,
    val displayName: String,
    val pageIndex: Int,
    val pageLabel: String,
    val createdAt: Long = System.currentTimeMillis()
)
