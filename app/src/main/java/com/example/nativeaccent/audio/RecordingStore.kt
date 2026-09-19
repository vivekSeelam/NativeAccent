package com.example.nativeaccent.audio

import java.io.File

/**
 * Owns the on-disk lifetime of attempt recordings.
 *
 * Everything lives under `cacheDir/attempts`, and starting a new attempt for an
 * item wipes that item's previous takes first, so the cache holds at most one
 * file per practice item.
 */
class RecordingStore(cacheDir: File) {

    private val dir: File = File(cacheDir, DIR_NAME)

    /** Deletes any earlier take for [itemId] and returns a fresh, unique target file. */
    fun newAttemptFile(itemId: String): File {
        dir.mkdirs()
        deleteAttemptsFor(itemId)
        return File(dir, "${prefixFor(itemId)}${System.currentTimeMillis()}.m4a")
    }

    fun deleteAttemptsFor(itemId: String) {
        val prefix = prefixFor(itemId)
        dir.listFiles()?.forEach { file ->
            if (file.name.startsWith(prefix)) file.delete()
        }
    }

    private fun prefixFor(itemId: String) = "attempt_${itemId.sanitized()}_"

    private fun String.sanitized() = replace(Regex("[^A-Za-z0-9_-]"), "_")

    private companion object {
        const val DIR_NAME = "attempts"
    }
}
