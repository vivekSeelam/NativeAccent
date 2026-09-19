package com.example.nativeaccent.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import kotlin.concurrent.thread
import kotlin.math.max

/** Outcome of [WavRecorder.stop], so callers never have to reason about AudioRecord states. */
sealed interface RecordingResult {
    /** [peakAmplitude] is 0..32768; near zero means the take is silence. */
    data class Success(val file: File, val durationMs: Long, val peakAmplitude: Int) : RecordingResult
    data class Failure(val reason: String) : RecordingResult
}

/**
 * Captures the microphone as 16 kHz, mono, 16-bit PCM WAV — the format Azure
 * pronunciation assessment reads directly.
 *
 * MediaRecorder cannot produce WAV (every output format it offers is compressed),
 * so this drives [AudioRecord] on a worker thread and writes the RIFF header
 * itself once the final length is known.
 */
class WavRecorder(private val context: Context) {

    private var audioRecord: AudioRecord? = null
    private var writer: Thread? = null
    private var target: File? = null

    @Volatile private var keepRecording = false

    // Written by the worker thread; read only after join(), which publishes them.
    private var bytesWritten = 0L
    private var peak = 0
    private var failure: String? = null

    val isRecording: Boolean get() = audioRecord != null

    /**
     * Starts capturing to [outputFile]. Returns false if the permission is missing
     * or the microphone could not be opened (e.g. another app is holding it).
     */
    fun start(outputFile: File): Boolean {
        if (isRecording) cancel()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }

        val minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, ENCODING)
        if (minBuffer <= 0) {
            Log.e(TAG, "16 kHz mono PCM is not supported on this device ($minBuffer)")
            return false
        }
        // Half a second of headroom so a busy UI thread never makes us drop audio.
        val bufferSize = max(minBuffer, BYTES_PER_SECOND / 2)

        val record = try {
            AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, SAMPLE_RATE, CHANNEL_CONFIG, ENCODING, bufferSize)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "AudioRecord rejected the configuration", e)
            return false
        } catch (e: SecurityException) {
            Log.e(TAG, "RECORD_AUDIO was revoked", e)
            return false
        }
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            return false
        }
        try {
            record.startRecording()
        } catch (e: IllegalStateException) {
            Log.e(TAG, "startRecording() failed", e)
            record.release()
            return false
        }
        if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
            // Another app holds the microphone.
            record.release()
            return false
        }

        audioRecord = record
        target = outputFile
        bytesWritten = 0L
        peak = 0
        failure = null
        keepRecording = true
        writer = thread(name = "wav-writer") { pump(record, outputFile, bufferSize) }
        return true
    }

    /**
     * Stops capturing and hands back the finished WAV. An empty take becomes a
     * [RecordingResult.Failure] and its file is removed.
     */
    fun stop(): RecordingResult {
        val record = audioRecord ?: return RecordingResult.Failure("Not recording")
        val file = target
        finishCapture(record)

        failure?.let { reason ->
            file?.delete()
            return RecordingResult.Failure(reason)
        }
        if (file == null || bytesWritten == 0L) {
            file?.delete()
            return RecordingResult.Failure("Nothing was recorded")
        }
        return RecordingResult.Success(
            file = file,
            durationMs = bytesWritten * 1000 / BYTES_PER_SECOND,
            peakAmplitude = peak,
        )
    }

    /** Aborts an in-flight recording and discards the file. Safe to call when idle. */
    fun cancel() {
        val record = audioRecord ?: return
        val file = target
        finishCapture(record)
        file?.delete()
    }

    fun release() = cancel()

    private fun finishCapture(record: AudioRecord) {
        keepRecording = false
        try {
            record.stop() // also unblocks a pending read()
        } catch (e: IllegalStateException) {
            Log.d(TAG, "stop() on a recorder that was not running", e)
        }
        try {
            writer?.join(JOIN_TIMEOUT_MS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        record.release()
        audioRecord = null
        writer = null
        target = null
    }

    /** Worker thread: stream PCM to disk after a placeholder header, then patch the header. */
    private fun pump(record: AudioRecord, file: File, bufferSize: Int) {
        val buffer = ByteArray(bufferSize)
        var total = 0L
        var loudest = 0
        try {
            RandomAccessFile(file, "rw").use { out ->
                out.setLength(0)
                out.write(ByteArray(WavFormat.HEADER_SIZE))
                while (keepRecording && total < MAX_BYTES) {
                    val read = record.read(buffer, 0, buffer.size)
                    when {
                        read > 0 -> {
                            out.write(buffer, 0, read)
                            total += read
                            loudest = max(loudest, WavFormat.peakAmplitude(buffer, read))
                        }
                        read < 0 -> {
                            failure = "The microphone stopped responding"
                            break
                        }
                    }
                }
                out.seek(0)
                out.write(WavFormat.header(total, SAMPLE_RATE, CHANNELS, BITS_PER_SAMPLE))
            }
        } catch (e: IOException) {
            Log.e(TAG, "could not write $file", e)
            failure = "Couldn't save the recording"
        }
        bytesWritten = total
        peak = loudest
    }

    companion object {
        const val SAMPLE_RATE = 16_000
        private const val CHANNELS = 1
        private const val BITS_PER_SAMPLE = 16
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
        private const val BYTES_PER_SECOND = SAMPLE_RATE * CHANNELS * BITS_PER_SAMPLE / 8

        /** Azure's single-shot recognition handles ~30 s; stop writing past that. */
        private const val MAX_BYTES = BYTES_PER_SECOND * 30L
        private const val JOIN_TIMEOUT_MS = 2_000L
        private const val TAG = "WavRecorder"
    }
}
