package com.example.pdfocr

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pdfocr.data.local.DictionaryEntryEntity
import com.example.pdfocr.data.local.LibraryBookEntity
import com.example.pdfocr.domain.model.Bookmark
import com.example.pdfocr.ui.theme.PdfOcrReaderTheme
import com.example.pdfocr.ui.viewer.PdfViewerViewModel
import com.example.pdfocr.ui.viewer.ViewerUiState
import kotlinx.coroutines.launch

class PdfViewerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PdfOcrReaderTheme { ViewerScreen() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen() {
    var showBookmarks by remember { mutableStateOf(false) }
    var showLibrary by remember { mutableStateOf(false) }
    var showDictionary by remember { mutableStateOf(false) }
    val viewModel: PdfViewerViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    val documentPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let(viewModel::openDocument) }

    val dictionaryPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let(viewModel::importDictionary) }

    val dictionaryCreator = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? -> uri?.let(viewModel::createDictionary) }

    LaunchedEffect(state.documentUri, state.library.size) {
        if (state.documentUri == null && state.library.isEmpty()) {
            documentPicker.launch(arrayOf("application/pdf"))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { showLibrary = true }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Library")
                    }
                },
                title = {
                    Column {
                        Text(state.displayName, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "Page ${state.currentPage + 1}/${state.pageCount}" +
                                if (state.isOcrRunning) " · OCR…" else "",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { dictionaryPicker.launch(arrayOf("text/plain")) }) {
                        Icon(Icons.Default.Description, contentDescription = "Choose dictionary TXT")
                    }
                    IconButton(onClick = { showDictionary = true }) {
                        Icon(Icons.Default.List, contentDescription = "Dictionary")
                    }
                    IconButton(onClick = { documentPicker.launch(arrayOf("application/pdf")) }) {
                        Icon(Icons.Default.Add, contentDescription = "Open another PDF")
                    }
                    IconButton(onClick = viewModel::toggleBookmark) {
                        Icon(
                            imageVector = if (state.isCurrentPageBookmarked) Icons.Default.Bookmark
                            else Icons.Default.BookmarkBorder,
                            contentDescription = "Toggle bookmark"
                        )
                    }
                    IconButton(onClick = { showBookmarks = true }) {
                        Icon(Icons.Default.MenuBook, contentDescription = "Bookmarks")
                    }
                }
            )
        },
        bottomBar = {
            if (state.pageCount > 0) {
                BottomAppBar {
                    IconButton(onClick = viewModel::previousPage, enabled = state.currentPage > 0) {
                        Text("‹")
                    }
                    IconButton(
                        onClick = viewModel::nextPage,
                        enabled = state.currentPage < state.pageCount - 1
                    ) { Text("›") }
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            when {
                state.pageBitmap != null -> ZoomablePage(
                    state = state,
                    onPhraseSelected = viewModel::onPhraseSelected,
                    onZoom = viewModel::setZoom,
                    modifier = Modifier.fillMaxSize().padding(padding)
                )

                state.library.isNotEmpty() -> Box(Modifier.fillMaxSize().padding(padding)) {
                    LibraryList(
                        library = state.library,
                        onOpen = viewModel::openLibraryBook,
                        onDelete = viewModel::removeLibraryBook
                    )
                }

                else -> Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

        }
    }

    if (showBookmarks) {
        BookmarksSheet(
            state = state,
            onDismiss = { showBookmarks = false },
            onGoTo = { index ->
                showBookmarks = false
                viewModel.goToPage(index)
            },
            onDelete = viewModel::deleteBookmark
        )
    }

    if (showLibrary) {
        LibrarySheet(
            library = state.library,
            onDismiss = { showLibrary = false },
            onOpen = { book ->
                showLibrary = false
                viewModel.openLibraryBook(book)
            },
            onDelete = viewModel::removeLibraryBook,
            onOpenNew = {
                showLibrary = false
                documentPicker.launch(arrayOf("application/pdf"))
            }
        )
    }

    if (showDictionary) {
        DictionarySheet(
            dictionary = state.dictionary,
            dictionaryName = state.dictionaryName,
            onDismiss = { showDictionary = false },
            onDelete = viewModel::deleteDictionaryEntry,
            onChooseFile = {
                showDictionary = false
                dictionaryPicker.launch(arrayOf("text/plain"))
            },
            onCreateFile = {
                showDictionary = false
                dictionaryCreator.launch("dictionary.txt")
            }
        )
    }

    state.selectedPhrase?.let { phrase ->
        TranslationPopup(
            phrase = phrase,
            translation = state.translation,
            isLoading = state.isTranslating,
            error = state.error,
            canSaveToDictionary = state.dictionaryName != null && !state.wasAddedToDictionary,
            isSaving = state.isSavingToDictionary,
            onDismiss = viewModel::dismissTranslation,
            onSaveToDictionary = viewModel::addCurrentTranslationToDictionary
        )
    }

    state.error?.let { message ->
        LaunchedEffect(message) {
            scope.launch {
                SnackbarHostState().showSnackbar(message)
                viewModel.clearError()
            }
        }
    }
}


@Composable
fun ZoomablePage(
    state: ViewerUiState,
    onPhraseSelected: (Float, Float, Float, Float) -> Unit,
    onZoom: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val bitmap = state.pageBitmap ?: return
    var offset by remember(bitmap) { mutableStateOf(Offset.Zero) }
    var selectionStart by remember(bitmap) { mutableStateOf<Offset?>(null) }
    var selectionEnd by remember(bitmap) { mutableStateOf<Offset?>(null) }

    Box(
        modifier
            .clipToBounds()
            // Pinch zoom + pan
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newZoom = (state.zoom * zoom).coerceIn(1f, 5f)
                    if (zoom != 1f) onZoom(newZoom)
                    offset = Offset(
                        (offset.x + pan.x).coerceIn(-2000f, 2000f),
                        (offset.y + pan.y).coerceIn(-2000f, 2000f)
                    )
                }
            }
            // Single-finger pan
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        if (event.changes.size == 1) {
                            val change = event.changes.first()
                            if (change.pressed && selectionStart == null) {
                                offset = Offset(
                                    (offset.x + change.positionChange().x).coerceIn(-2000f, 2000f),
                                    (offset.y + change.positionChange().y).coerceIn(-2000f, 2000f)
                                )
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            // Long-press + drag = word/phrase selection
            .pointerInput(bitmap) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { position ->
                        selectionStart = position
                        selectionEnd = position
                    },
                    onDrag = { change, _ ->
                        selectionEnd = change.position
                    },
                    onDragEnd = {
                        val start = selectionStart
                        val end = selectionEnd
                        if (start != null && end != null) {
                            val x1 = minOf(start.x, end.x) / size.width
                            val x2 = maxOf(start.x, end.x) / size.width
                            val y1 = minOf(start.y, end.y) / size.height
                            val y2 = maxOf(start.y, end.y) / size.height
                            if (x2 - x1 > 0.005f || y2 - y1 > 0.005f) {
                                onPhraseSelected(x1, y1, x2, y2)
                            }
                        }
                        selectionStart = null
                        selectionEnd = null
                    }
                )
            }
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "PDF page",
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer {
                    translationX = offset.x
                    translationY = offset.y
                    scaleX = state.zoom
                    scaleY = state.zoom
                }
                .fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        selectionStart?.let { start ->
            selectionEnd?.let { end ->
                SelectionOverlay(start = start, end = end, modifier = Modifier.fillMaxSize())
            }
        }

        if (state.isRendering) {
            LinearProgressIndicator(Modifier.align(Alignment.TopCenter).fillMaxWidth())
        }
    }
}

@Composable
private fun SelectionOverlay(start: Offset, end: Offset, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val left = minOf(start.x, end.x)
        val top = minOf(start.y, end.y)
        val width = kotlin.math.abs(end.x - start.x)
        val height = kotlin.math.abs(end.y - start.y)
        drawRect(
            color = Color(0x33448AFF),
            topLeft = Offset(left, top),
            size = Size(width, height)
        )
        drawRect(
            color = Color(0xAA448AFF),
            topLeft = Offset(left, top),
            size = Size(width, height),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationPopup(
    phrase: String,
    translation: String?,
    isLoading: Boolean,
    error: String?,
    canSaveToDictionary: Boolean,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSaveToDictionary: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(phrase, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            Spacer(Modifier.height(8.dp))
            when {
                isLoading || isSaving -> CircularProgressIndicator()
                error != null -> Text(error, color = MaterialTheme.colorScheme.error)
                translation != null -> Text(translation, style = MaterialTheme.typography.titleMedium)
            }
            if (canSaveToDictionary && translation != null) {
                Spacer(Modifier.height(16.dp))
                Button(onClick = onSaveToDictionary) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Add to dictionary")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksSheet(
    state: ViewerUiState,
    onDismiss: () -> Unit,
    onGoTo: (Int) -> Unit,
    onDelete: (Bookmark) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(20.dp)) {
            Text("Bookmarks", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            if (state.bookmarks.isEmpty()) {
                Text("No bookmarks yet. Tap the bookmark icon to save this page.")
            } else {
                state.bookmarks.forEach { bookmark ->
                    ListItem(
                        headlineContent = { Text(bookmark.pageLabel) },
                        supportingContent = { Text("Tap to jump to page") },
                        trailingContent = {
                            IconButton(onClick = { onDelete(bookmark) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete bookmark")
                            }
                        },
                        modifier = Modifier.clickable { onGoTo(bookmark.pageIndex) }
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibrarySheet(
    library: List<LibraryBookEntity>,
    onDismiss: () -> Unit,
    onOpen: (LibraryBookEntity) -> Unit,
    onDelete: (LibraryBookEntity) -> Unit,
    onOpenNew: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Library", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onOpenNew) { Text("Open new") }
            }
            Spacer(Modifier.height(12.dp))
            if (library.isEmpty()) {
                Text("No books in library yet.")
            } else {
                library.forEach { book ->
                    ListItem(
                        headlineContent = { Text(book.displayName) },
                        supportingContent = { Text("Page ${book.lastPageIndex + 1} of ${book.pageCount}") },
                        trailingContent = {
                            IconButton(onClick = { onDelete(book) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove from library")
                            }
                        },
                        modifier = Modifier.clickable { onOpen(book) }
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionarySheet(
    dictionary: List<DictionaryEntryEntity>,
    dictionaryName: String?,
    onDismiss: () -> Unit,
    onDelete: (DictionaryEntryEntity) -> Unit,
    onChooseFile: () -> Unit,
    onCreateFile: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Dictionary", style = MaterialTheme.typography.titleLarge)
                    dictionaryName?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                }
                TextButton(onClick = onChooseFile) { Text("Choose TXT") }
                TextButton(onClick = onCreateFile) { Text("Create TXT") }
            }
            Spacer(Modifier.height(12.dp))
            if (dictionary.isEmpty()) {
                Text("No dictionary entries yet. Choose a TXT file or add words while reading.")
            } else {
                LazyColumn {
                    items(dictionary, key = { it.id }) { entry ->
                        ListItem(
                            headlineContent = { Text(entry.phrase) },
                            supportingContent = { Text(entry.translation) },
                            trailingContent = {
                                IconButton(onClick = { onDelete(entry) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete entry")
                                }
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun LibraryList(
    library: List<LibraryBookEntity>,
    onOpen: (LibraryBookEntity) -> Unit,
    onDelete: (LibraryBookEntity) -> Unit
) {
    LazyColumn(Modifier.fillMaxSize()) {
        items(library, key = { it.pdfKey }) { book ->
            ListItem(
                headlineContent = { Text(book.displayName) },
                supportingContent = { Text("Page ${book.lastPageIndex + 1} of ${book.pageCount}") },
                trailingContent = {
                    IconButton(onClick = { onDelete(book) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove from library")
                    }
                },
                modifier = Modifier.clickable { onOpen(book) }
            )
        }
    }
}
