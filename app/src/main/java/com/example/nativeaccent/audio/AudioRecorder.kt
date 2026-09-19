package com.example.nativeaccent.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException

/** Outcome of [AudioRecorder.stop], so callers never have to reason about MediaRecorder states. */
sealed interface RecordingResult {
    data class Success(val file: File, val durationMs: Long) : RecordingResult
    data class Failure(val reason: String) : RecordingResult
}

/**
 * Thin, UI-agnostic wrapper around [MediaRecorder].
 *
 * Deliberately knows nothing about Compose or the ViewModel: the future scoring
 * module can reuse it, and it can be faked in tests behind the same two calls.
 */
class AudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var target: File? = null
    private var startedAtMs: Long = 0L

    val isRecording: Boolean get() = recorder != null

    /**
     * Starts capturing to [outputFile]. Caller must already hold RECORD_AUDIO.
     * Returns false if the microphone could not be opened.
     */
    fun start(outputFile: File): Boolean {
        if (isRecording) stop()
        val created = buildRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(BIT_RATE)
            setAudioSamplingRate(SAMPLE_RATE)
            setOutputFile(outputFile.absolutePath)
        }
        return try {
            created.prepare()
            created.start()
            recorder = created
            target = outputFile
            startedAtMs = System.currentTimeMillis()
            true
        } catch (e: IOException) {
            Log.e(TAG, "prepare() failed", e)
            created.releaseQuietly()
            false
        } catch (e: IllegalStateException) {
            Log.e(TAG, "start() failed", e)
            created.releaseQuietly()
            false
        } catch (e: RuntimeException) {
            // Thrown when another app holds the mic, or on some OEMs when the
            // permission was revoked between the check and the start.
            Log.e(TAG, "microphone unavailable", e)
            created.releaseQuietly()
            false
        }
    }

    /**
     * Stops capturing and hands back the finished file.
     *
     * MediaRecorder throws if it is stopped before it has written a usable
     * frame, which happens on a quick double-tap — that becomes a [RecordingResult.Failure]
     * and the half-written file is removed.
     */
    fun stop(): RecordingResult {
        val active = recorder ?: return RecordingResult.Failure("Not recording")
        val file = target
        val durationMs = System.currentTimeMillis() - startedAtMs
        recorder = null
        target = null

        return try {
            active.stop()
            active.releaseQuietly()
            when {
                file == null || !file.exists() || file.length() == 0L ->
                    RecordingResult.Failure("Nothing was recorded")
                else -> RecordingResult.Success(file, durationMs)
            }
        } catch (e: RuntimeException) {
            Log.w(TAG, "stop() failed - recording was too short", e)
            active.releaseQuietly()
            file?.delete()
            RecordingResult.Failure("That was too short — hold on a moment longer")
        }
    }

    /** Aborts an in-flight recording and discards the file. Safe to call when idle. */
    fun cancel() {
        val active = recorder ?: return
        val file = target
        recorder = null
        target = null
        try {
            active.stop()
        } catch (e: RuntimeException) {
            Log.d(TAG, "cancel(): recorder was not running", e)
        }
        active.releaseQuietly()
        file?.delete()
    }

    fun release() = cancel()

    @Suppress("DEPRECATION")
    private fun buildRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()

    private fun MediaRecorder.releaseQuietly() {
        try {
            reset()
            release()
        } catch (e: RuntimeException) {
            Log.d(TAG, "release() failed", e)
        }
    }

    private companion object {
        const val TAG = "AudioRecorder"
        const val BIT_RATE = 128_000
        const val SAMPLE_RATE = 44_100
    }
}
