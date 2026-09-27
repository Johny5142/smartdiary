package com.example.pdfocr.domain.usecase

import com.example.pdfocr.domain.model.Bookmark
import com.example.pdfocr.domain.repository.BookmarkRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveBookmarksUseCase @Inject constructor(
    private val repository: BookmarkRepository
) {
    operator fun invoke(pdfKey: String): Flow<List<Bookmark>> = repository.observeBookmarks(pdfKey)
}

class ToggleBookmarkUseCase @Inject constructor(
    private val repository: BookmarkRepository
) {
    suspend operator fun invoke(
        pdfKey: String, uri: String, displayName: String, pageIndex: Int
    ): Boolean = repository.toggleBookmark(pdfKey, uri, displayName, pageIndex)
}

class DeleteBookmarkUseCase @Inject constructor(
    private val repository: BookmarkRepository
) {
    suspend operator fun invoke(bookmark: Bookmark) = repository.deleteBookmark(bookmark)
}
