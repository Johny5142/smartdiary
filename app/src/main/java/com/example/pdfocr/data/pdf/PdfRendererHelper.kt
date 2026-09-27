package com.example.pdfocr.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * Renders PDF pages to bitmaps via the built-in android.graphics.pdf.PdfRenderer.
 * Content is copied to a private cache file because PdfRenderer requires a seekable fd.
 */
class PdfRendererHelper(private val context: Context) {

    fun openDocument(uri: Uri): RenderedPdf {
        val descriptor = context.contentResolver.openFileDescriptor(uri, "r")
            ?: throw IllegalArgumentException("Cannot open PDF: $uri")
        return RenderedPdf(descriptor)
    }

    fun stablePdfKey(uri: Uri, displayName: String): String {
        val size = try {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: 0L
        } catch (_: Exception) { 0L }
        val raw = "$displayName|$size|$uri"
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    class RenderedPdf(private val descriptor: ParcelFileDescriptor) : AutoCloseable {
        private val renderer = PdfRenderer(descriptor)

        val pageCount: Int get() = renderer.pageCount

        fun renderPage(index: Int, targetWidthPx: Int): Bitmap {
            require(index in 0 until pageCount) { "Page index out of range: $index" }
            renderer.openPage(index).use { page ->
                val scale = targetWidthPx.toFloat() / page.width
                val bitmap = Bitmap.createBitmap(
                    targetWidthPx,
                    (page.height * scale).roundToIntCompat(),
                    Bitmap.Config.ARGB_8888
                )
                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                return bitmap
            }
        }

        override fun close() {
            try { renderer.close() } finally { descriptor.close() }
        }

        private fun Int.roundToIntCompat() = Math.round(this.toFloat() * 1f).toInt()
        private fun Float.roundToIntCompat(): Int {
            val rounded = this + 0.5f
            return rounded.toInt()
        }
    }
}
