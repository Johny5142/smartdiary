package com.example.pdfocr.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dictionary_entries")
data class DictionaryEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phrase: String,
    val translation: String,
    val createdAt: Long = System.currentTimeMillis()
)
