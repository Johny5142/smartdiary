package com.example.pdfocr.domain.usecase

import com.example.pdfocr.domain.model.OcrLine
import com.example.pdfocr.domain.model.OcrWord
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.min

/** Finds the OCR word closest to a normalized tap position. */
class FindWordAtTapUseCase @Inject constructor() {
    operator fun invoke(lines: List<OcrLine>, tapX: Float, tapY: Float): OcrWord? {
        var best: OcrWord? = null
        var bestScore = Float.MAX_VALUE
        for (line in lines) for (word in line.words) {
            val inBox = tapX in word.left..word.right && tapY in word.top..word.bottom
            val dx = if (inBox) 0f else abs(tapX - word.centerX)
            val dy = if (inBox) 0f else abs(tapY - word.centerY)
            // Strongly prefer vertical proximity so taps on another line never win.
            val score = dy * 10f + dx
            if (score < bestScore) { bestScore = score; best = word }
        }
        return best?.takeIf { bestScore < 0.30f } // ~30% page width tolerance
    }
}
