package com.example.pdfocr.data.repository

import com.example.pdfocr.data.local.LibraryBookDao
import com.example.pdfocr.data.local.LibraryBookEntity
import kotlinx.coroutines.flow.Flow

class LibraryRepository(private val dao: LibraryBookDao) {

    fun observeLibrary(): Flow<List<LibraryBookEntity>> = dao.observeLibrary()

    suspend fun upsertBook(
        pdfKey: String,
        uri: String,
        displayName: String,
        pageCount: Int,
        lastPageIndex: Int = 0
    ) {
        val existing = dao.findByKey(pdfKey)
        val lastPage = existing?.lastPageIndex?.coerceIn(0, (pageCount - 1).coerceAtLeast(0)) ?: lastPageIndex
        dao.upsert(
            LibraryBookEntity(
                pdfKey = pdfKey,
                uri = uri,
                displayName = displayName,
                pageCount = pageCount,
                lastPageIndex = lastPage
            )
        )
    }

    suspend fun saveProgress(pdfKey: String, pageIndex: Int) {
        dao.updateProgress(pdfKey, pageIndex)
    }

    suspend fun delete(pdfKey: String) = dao.delete(pdfKey)
}
