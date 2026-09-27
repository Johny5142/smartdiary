package com.example.pdfocr.domain.model

data class OcrWord(
    val text: String,
    val left: Float,   // normalized 0..1 relative to page width
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
}

data class OcrLine(val words: List<OcrWord>)

data class PdfPageInfo(val index: Int, val widthPx: Int, val heightPx: Int)

data class Bookmark(
    val id: Long,
    val pageIndex: Int,
    val pageLabel: String,
    val createdAt: Long
)
