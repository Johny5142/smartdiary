package com.example.pdfocr.data.repository

import com.example.pdfocr.data.local.AppDatabase
import com.example.pdfocr.data.local.BookmarkEntity
import com.example.pdfocr.domain.model.Bookmark
import com.example.pdfocr.domain.repository.BookmarkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BookmarkRepositoryImpl(private val database: AppDatabase) : BookmarkRepository {

    private val dao get() = database.bookmarkDao()

    override fun observeBookmarks(pdfKey: String): Flow<List<Bookmark>> =
        dao.observeBookmarks(pdfKey).map { entities ->
            entities.map { it.toDomain() }
        }

    /** @return true if a bookmark now exists for the page. */
    override suspend fun toggleBookmark(
        pdfKey: String,
        uri: String,
        displayName: String,
        pageIndex: Int
    ): Boolean {
        val exists = dao.countForPage(pdfKey, pageIndex) > 0
        if (exists) {
            dao.deleteForPage(pdfKey, pageIndex)
            return false
        }
        dao.insert(
            BookmarkEntity(
                pdfKey = pdfKey,
                uri = uri,
                displayName = displayName,
                pageIndex = pageIndex,
                pageLabel = "Page ${pageIndex + 1}"
            )
        )
        return true
    }

    override suspend fun deleteBookmark(bookmark: Bookmark) {
        dao.deleteById(bookmark.id)
    }

    private fun BookmarkEntity.toDomain() = Bookmark(
        id = id,
        pageIndex = pageIndex,
        pageLabel = pageLabel,
        createdAt = createdAt
    )
}
