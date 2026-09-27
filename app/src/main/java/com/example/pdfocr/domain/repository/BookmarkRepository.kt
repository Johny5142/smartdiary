package com.example.pdfocr.domain.repository

import com.example.pdfocr.domain.model.Bookmark
import kotlinx.coroutines.flow.Flow

interface BookmarkRepository {
    fun observeBookmarks(pdfKey: String): Flow<List<Bookmark>>
    suspend fun toggleBookmark(
        pdfKey: String,
        uri: String,
        displayName: String,
        pageIndex: Int
    ): Boolean

    suspend fun deleteBookmark(bookmark: Bookmark)
}
