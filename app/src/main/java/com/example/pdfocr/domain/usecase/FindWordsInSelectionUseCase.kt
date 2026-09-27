package com.example.pdfocr.domain.usecase

import com.example.pdfocr.domain.model.OcrLine
import javax.inject.Inject

/** Collects words intersecting a normalized selection rectangle. */
class FindWordsInSelectionUseCase @Inject constructor() {
    operator fun invoke(
        lines: List<OcrLine>,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float
    ): String {
        val l = left.coerceIn(0f, 1f)
        val r = right.coerceIn(0f, 1f)
        val t = top.coerceIn(0f, 1f)
        val b = bottom.coerceIn(0f, 1f)
        val x1 = minOf(l, r)
        val x2 = maxOf(l, r)
        val y1 = minOf(t, b)
        val y2 = maxOf(t, b)

        return lines.asSequence()
            .flatMap { it.words }
            .filter { word ->
                word.right >= x1 && word.left <= x2 && word.bottom >= y1 && word.top <= y2
            }
            .sortedWith(compareBy({ it.top }, { it.left }))
            .joinToString(" ") { it.text }
            .trim()
    }
}
