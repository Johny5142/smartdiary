package com.example.pdfocr.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DictionaryDao {
    @Query("SELECT * FROM dictionary_entries ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DictionaryEntryEntity>>

    @Query("SELECT COUNT(*) FROM dictionary_entries WHERE phrase = :phrase COLLATE NOCASE")
    suspend fun countPhrase(phrase: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: DictionaryEntryEntity): Long

    @Query("DELETE FROM dictionary_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM dictionary_entries")
    suspend fun clear()
}
