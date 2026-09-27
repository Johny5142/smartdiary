package com.example.pdfocr.data.repository

import android.content.Context
import android.net.Uri
import com.example.pdfocr.data.local.DictionaryDao
import com.example.pdfocr.data.local.DictionaryEntryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * User dictionary backed by a chosen TXT file.
 * Supported line formats: "phrase - translation", "phrase: translation", "phrase<TAB>translation".
 */
class DictionaryRepository(
    private val context: Context,
    private val dao: DictionaryDao
) {
    var fileUri: Uri? = null

    fun observeAll(): Flow<List<DictionaryEntryEntity>> = dao.observeAll()

    suspend fun createFile(uri: Uri) {
        withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri, "w")?.use { stream ->
                stream.write(ByteArray(0))
            } ?: error("Cannot create dictionary file")
        }
        fileUri = uri
        dao.clear()
    }

    suspend fun importFile(uri: Uri) {
        fileUri = uri
        dao.clear()
        val entries = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)
                ?.useLines { lines ->
                    lines.mapNotNull(::parseLine).toList()
                } ?: emptyList()
        }
        entries.forEach { dao.insert(it) }
    }

    suspend fun add(phrase: String, translation: String) {
        val trimmedPhrase = phrase.trim()
        val trimmedTranslation = translation.trim()
        if (trimmedPhrase.isEmpty() || trimmedTranslation.isEmpty()) return
        if (dao.countPhrase(trimmedPhrase) == 0) {
            dao.insert(DictionaryEntryEntity(phrase = trimmedPhrase, translation = trimmedTranslation))
        }
        val uri = fileUri ?: return
        withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri, "wa")?.use { stream ->
                stream.write("$trimmedPhrase - $trimmedTranslation\n".toByteArray(Charsets.UTF_8))
            }
        }
    }

    suspend fun delete(id: Long) = dao.deleteById(id)

    private fun parseLine(line: String): DictionaryEntryEntity? {
        val text = line.trim()
        if (text.isEmpty() || text.startsWith("#")) return null
        val parts = listOf(" - ", " – ", ": ", "\t")
            .firstNotNullOfOrNull { sep ->
                text.indexOf(sep).takeIf { it > 0 }?.let { idx ->
                    text.substring(0, idx).trim() to text.substring(idx + sep.length).trim()
                }
            } ?: return null
        val (phrase, translation) = parts
        return if (translation.isEmpty()) null
        else DictionaryEntryEntity(phrase = phrase, translation = translation)
    }
}
