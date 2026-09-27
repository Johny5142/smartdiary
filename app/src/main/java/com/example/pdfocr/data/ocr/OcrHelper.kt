package com.example.pdfocr.data.ocr

import android.graphics.Bitmap
import com.example.pdfocr.domain.model.OcrLine
import com.example.pdfocr.domain.model.OcrWord
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.math.roundToInt
import kotlinx.coroutines.tasks.await

class OcrHelper {
    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun recognize(bitmap: Bitmap): List<OcrLine> {
        val image = InputImage.fromBitmap(bitmap, 0)
        val visionText = recognizer.process(image).await()
        val width = bitmap.width.toFloat()
        val height = bitmap.height.toFloat()

        return visionText.textBlocks.flatMap { block ->
            block.lines.map { line ->
                OcrLine(
                    line.elements.map { element ->
                        OcrWord(
                            text = element.text,
                            left = element.boundingBox?.left?.div(width) ?: 0f,
                            top = element.boundingBox?.top?.div(height) ?: 0f,
                            right = element.boundingBox?.right?.div(width) ?: 0f,
                            bottom = element.boundingBox?.bottom?.div(height) ?: 0f
                        )
                    }.filter { it.right > it.left && it.bottom > it.top }
                )
            }.filter { it.words.isNotEmpty() }
        }.filter { it.words.isNotEmpty() }
    }

    fun close() = recognizer.close()
}
