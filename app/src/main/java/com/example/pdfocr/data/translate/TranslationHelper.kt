package com.example.pdfocr.data.translate

import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.tasks.await

class TranslationHelper {
    private var translator: Translator? = null
    private var currentPair: Pair<String, String>? = null

    /** Downloads (if needed) and prepares an on-device translation model. */
    suspend fun prepare(source: String = TranslateLanguage.ENGLISH, target: String = TranslateLanguage.RUSSIAN) {
        val pair = source to target
        if (translator != null && currentPair == pair) return

        translator?.close()
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(source)
            .setTargetLanguage(target)
            .build()
        translator = Translation.getClient(options)
        currentPair = pair

        translator!!.downloadModelIfNeeded(
            DownloadConditions.Builder()
                .requireWifi() // keep mobile data usage predictable
                .build()
        ).await()
    }

    suspend fun translateWord(word: String): String {
        val active = translator
            ?: error("Translation model is not prepared. Call prepare() first.")
        return active.translate(word).await().trim()
    }

    fun close() {
        translator?.close()
        translator = null
        currentPair = null
    }
}
