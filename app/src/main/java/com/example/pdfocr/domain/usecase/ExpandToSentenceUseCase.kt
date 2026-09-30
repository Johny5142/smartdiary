package com.example.pdfocr.domain.usecase

import com.example.pdfocr.domain.model.OcrLine
import javax.inject.Inject

/** Expands a word/phrase selection to the full sentence(s) it intersects. */
class ExpandToSentenceUseCase @Inject constructor() {
    operator fun invoke(lines: List<OcrLine>, left: Float, top: Float, right: Float, bottom: Float): String {
        val y1 = minOf(top, bottom)
        val y2 = maxOf(top, bottom)
        val selected = lines.filter { line ->
            line.words.any { word -> word.bottom >= y1 && word.top <= y2 }
        }
        return selected
            .flatMap { it.words }
            .sortedWith(compareBy({ it.top }, { it.left }))
            .joinToString(" ") { it.text }
            .trim()
    }
}
