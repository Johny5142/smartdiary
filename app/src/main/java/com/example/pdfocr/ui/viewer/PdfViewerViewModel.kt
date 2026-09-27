package com.example.pdfocr.ui.viewer

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pdfocr.data.local.AppDatabase
import com.example.pdfocr.data.local.DictionaryEntryEntity
import com.example.pdfocr.data.local.LibraryBookEntity
import com.example.pdfocr.data.ocr.OcrHelper
import com.example.pdfocr.data.pdf.PdfRendererHelper
import com.example.pdfocr.data.repository.BookmarkRepositoryImpl
import com.example.pdfocr.data.repository.DictionaryRepository
import com.example.pdfocr.data.repository.LibraryRepository
import com.example.pdfocr.data.translate.TranslationHelper
import com.example.pdfocr.domain.model.Bookmark
import com.example.pdfocr.domain.model.OcrLine
import com.example.pdfocr.domain.usecase.DeleteBookmarkUseCase
import com.example.pdfocr.domain.usecase.FindWordsInSelectionUseCase
import com.example.pdfocr.domain.usecase.ObserveBookmarksUseCase
import com.example.pdfocr.domain.usecase.ToggleBookmarkUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PdfViewerViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.get(application)
    private val pdfHelper = PdfRendererHelper(application)
    private val ocrHelper = OcrHelper()
    private val translationHelper = TranslationHelper()
    private val bookmarkRepository = BookmarkRepositoryImpl(database)
    private val libraryRepository = LibraryRepository(database.libraryDao())
    private val dictionaryRepository = DictionaryRepository(application, database.dictionaryDao())
    private val findWordsInSelection = FindWordsInSelectionUseCase()

    private val observeBookmarks = ObserveBookmarksUseCase(bookmarkRepository)
    private val toggleBookmarkUseCase = ToggleBookmarkUseCase(bookmarkRepository)
    private val deleteBookmarkUseCase = DeleteBookmarkUseCase(bookmarkRepository)

    private val _uiState = MutableStateFlow(ViewerUiState())
    val uiState: StateFlow<ViewerUiState> = _uiState.asStateFlow()

    private var document: PdfRendererHelper.RenderedPdf? = null
    private var ocrCache = mutableMapOf<Int, List<OcrLine>>()
    private var bookmarksJob: Job? = null
    private var libraryJob: Job? = null
    private var dictionaryJob: Job? = null
    private var saveJob: Job? = null

    init {
        libraryJob = viewModelScope.launch {
            libraryRepository.observeLibrary().collect { books ->
                _uiState.update { it.copy(library = books) }
            }
        }
        dictionaryJob = viewModelScope.launch {
            dictionaryRepository.observeAll().collect { entries ->
                _uiState.update { it.copy(dictionary = entries) }
            }
        }
        prepareTranslation()
    }

    fun openDocument(uri: Uri) {
        if (uiState.value.documentUri == uri) return
        saveProgressNow()
        closeDocument()
        _uiState.update {
            ViewerUiState(
                documentUri = uri,
                zoom = it.zoom,
                selectionMode = it.selectionMode,
                dictionaryName = it.dictionaryName,
                dictionary = it.dictionary
            )
        }
        viewModelScope.launch {
            try {
                document = withContext(Dispatchers.IO) { pdfHelper.openDocument(uri) }
                val name = queryDisplayName(uri)
                val key = pdfHelper.stablePdfKey(uri, name)
                val pageCount = document!!.pageCount
                val saved = database.libraryDao().findByKey(key)
                libraryRepository.upsertBook(key, uri.toString(), name, pageCount)
                bookmarksJob?.cancel()
                bookmarksJob = viewModelScope.launch {
                    observeBookmarks(key).collect { list ->
                        _uiState.update { it.copy(bookmarks = list) }
                    }
                }
                _uiState.update { it.copy(pdfKey = key, displayName = name, pageCount = pageCount) }
                renderPage(saved?.lastPageIndex ?: 0)
            } catch (t: Throwable) {
                _uiState.update { it.copy(error = "Failed to open PDF: ${t.message}") }
            }
        }
    }

    fun openLibraryBook(book: LibraryBookEntity) = openDocument(Uri.parse(book.uri))

    fun removeLibraryBook(book: LibraryBookEntity) {
        viewModelScope.launch { libraryRepository.delete(book.pdfKey) }
    }

    fun renderPage(index: Int) {
        val doc = document ?: return
        if (index !in 0 until doc.pageCount) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRendering = true) }
            try {
                val scale = uiState.value.renderScale
                val bitmap = withContext(Dispatchers.IO) {
                    doc.renderPage(index, (BASE_RENDER_WIDTH * scale).toInt())
                }
                _uiState.update {
                    it.copy(currentPage = index, pageBitmap = bitmap, isRendering = false, error = null)
                }
                saveProgressDebounced()
                runOcrIfNeeded(index)
            } catch (t: Throwable) {
                _uiState.update { it.copy(isRendering = false, error = t.message) }
            }
        }
    }

    fun setZoom(zoom: Float) {
        val clamped = zoom.coerceIn(1f, 5f)
        if (clamped == uiState.value.zoom) return
        _uiState.update { it.copy(zoom = clamped) }
        if (clamped > 2f && uiState.value.renderScale < 2f) {
            _uiState.update { it.copy(renderScale = 2f) }
            renderPage(uiState.value.currentPage)
        }
    }

    fun setSelectionMode(enabled: Boolean) {
        _uiState.update { it.copy(selectionMode = enabled) }
    }

    fun onPhraseSelected(left: Float, top: Float, right: Float, bottom: Float) {
        if (!uiState.value.selectionMode) return
        val state = uiState.value
        val lines = ocrCache[state.currentPage] ?: return
        val phrase = findWordsInSelection(lines, left, top, right, bottom)
        if (phrase.isBlank()) return

        val cached = state.dictionary.firstOrNull {
            it.phrase.equals(phrase, ignoreCase = true)
        }
        _uiState.update {
            it.copy(
                selectedPhrase = phrase,
                translation = cached?.translation,
                isTranslating = cached == null,
                wasAddedToDictionary = false,
                error = null
            )
        }
        if (cached == null) translate(phrase)
    }

    private fun translate(phrase: String) {
        viewModelScope.launch {
            try {
                val translation = translationHelper.translateWord(phrase)
                _uiState.update { it.copy(translation = translation, isTranslating = false) }
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        translation = null,
                        isTranslating = false,
                        error = "Translation failed: ${t.message}"
                    )
                }
            }
        }
    }

    fun importDictionary(uri: Uri) {
        viewModelScope.launch {
            try {
                dictionaryRepository.importFile(uri)
                _uiState.update { it.copy(dictionaryName = queryDisplayName(uri), error = null) }
            } catch (t: Throwable) {
                _uiState.update { it.copy(error = "Failed to import dictionary: ${t.message}") }
            }
        }
    }

    fun createDictionary(uri: Uri) {
        viewModelScope.launch {
            try {
                dictionaryRepository.createFile(uri)
                _uiState.update { it.copy(dictionaryName = queryDisplayName(uri), error = null) }
            } catch (t: Throwable) {
                _uiState.update { it.copy(error = "Failed to create dictionary: ${t.message}") }
            }
        }
    }

    fun addCurrentTranslationToDictionary() {
        val state = uiState.value
        val phrase = state.selectedPhrase ?: return
        val translation = state.translation ?: return
        _uiState.update { it.copy(isSavingToDictionary = true) }
        viewModelScope.launch {
            try {
                dictionaryRepository.add(phrase, translation)
                _uiState.update { it.copy(isSavingToDictionary = false, wasAddedToDictionary = true) }
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(isSavingToDictionary = false, error = "Could not save to dictionary: ${t.message}")
                }
            }
        }
    }

    fun deleteDictionaryEntry(entry: DictionaryEntryEntity) {
        viewModelScope.launch { dictionaryRepository.delete(entry.id) }
    }

    private suspend fun runOcrIfNeeded(page: Int) {
        if (ocrCache.containsKey(page)) return
        val bitmap = uiState.value.pageBitmap ?: return
        try {
            _uiState.update { it.copy(isOcrRunning = true) }
            val lines = withContext(Dispatchers.Default) { ocrHelper.recognize(bitmap) }
            ocrCache[page] = lines
            _uiState.update { it.copy(isOcrRunning = false) }
        } catch (t: Throwable) {
            _uiState.update { it.copy(isOcrRunning = false) }
        }
    }

    private fun prepareTranslation() {
        viewModelScope.launch {
            try {
                translationHelper.prepare()
            } catch (_: Exception) {
            }
        }
    }

    fun dismissTranslation() {
        _uiState.update {
            it.copy(
                selectedPhrase = null,
                translation = null,
                isTranslating = false,
                wasAddedToDictionary = false,
                error = null
            )
        }
    }

    fun toggleBookmark() {
        val state = uiState.value
        val key = state.pdfKey ?: return
        val uri = state.documentUri ?: return
        viewModelScope.launch {
            try {
                toggleBookmarkUseCase(key, uri.toString(), state.displayName, state.currentPage)
            } catch (t: Throwable) {
                _uiState.update { it.copy(error = "Bookmark failed: ${t.message}") }
            }
        }
    }

    fun deleteBookmark(bookmark: Bookmark) {
        viewModelScope.launch { deleteBookmarkUseCase(bookmark) }
    }

    fun goToPage(index: Int) {
        if (index in 0 until (document?.pageCount ?: 0)) renderPage(index)
    }

    fun nextPage() = goToPage(uiState.value.currentPage + 1)
    fun previousPage() = goToPage(uiState.value.currentPage - 1)

    fun clearError() = _uiState.update { it.copy(error = null) }

    private fun saveProgressDebounced() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            saveProgressNow()
        }
    }

    private fun saveProgressNow() {
        val state = uiState.value
        val key = state.pdfKey ?: return
        viewModelScope.launch { libraryRepository.saveProgress(key, state.currentPage) }
    }

    private fun queryDisplayName(uri: Uri): String =
        getApplication<Application>().contentResolver
            .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
            } ?: "file.txt"

    override fun onCleared() {
        saveProgressNow()
        libraryJob?.cancel()
        dictionaryJob?.cancel()
        bookmarksJob?.cancel()
        document?.close()
        ocrHelper.close()
        translationHelper.close()
        super.onCleared()
    }

    private fun closeDocument() {
        document?.close()
        document = null
        ocrCache.clear()
        bookmarksJob?.cancel()
    }

    companion object {
        const val BASE_RENDER_WIDTH = 1600
    }
}
