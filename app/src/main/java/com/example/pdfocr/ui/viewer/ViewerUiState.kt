package com.example.pdfocr.ui.viewer

import android.graphics.Bitmap
import android.net.Uri
import com.example.pdfocr.data.local.DictionaryEntryEntity
import com.example.pdfocr.data.local.LibraryBookEntity
import com.example.pdfocr.domain.model.Bookmark

data class ViewerUiState(
    val documentUri: Uri? = null,
    val pdfKey: String? = null,
    val displayName: String = "document.pdf",
    val dictionaryName: String? = null,
    val pageCount: Int = 0,
    val currentPage: Int = 0,
    val pageBitmap: Bitmap? = null,
    val renderScale: Float = 1f,
    val zoom: Float = 1f,
    val selectionMode: Boolean = true,
    val isRendering: Boolean = false,
    val isOcrRunning: Boolean = false,
    val bookmarks: List<Bookmark> = emptyList(),
    val library: List<LibraryBookEntity> = emptyList(),
    val dictionary: List<DictionaryEntryEntity> = emptyList(),
    val selectedPhrase: String? = null,
    val translation: String? = null,
    val isTranslating: Boolean = false,
    val isSavingToDictionary: Boolean = false,
    val wasAddedToDictionary: Boolean = false,
    val error: String? = null
) {
    val isCurrentPageBookmarked: Boolean
        get() = bookmarks.any { it.pageIndex == currentPage }
}
