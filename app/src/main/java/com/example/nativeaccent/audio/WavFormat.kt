package com.example.nativeaccent.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max

/**
 * Canonical 44-byte RIFF/WAVE header for uncompressed PCM, plus a helper for
 * reading 16-bit little-endian samples. Pure Kotlin so it is unit-testable.
 */
object WavFormat {
    const val HEADER_SIZE = 44

    fun header(
        dataLength: Long,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int,
    ): ByteArray {
        val blockAlign = channels * bitsPerSample / 8
        val byteRate = sampleRate * blockAlign
        return ByteBuffer.allocate(HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt((36 + dataLength).toInt())
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16)                       // PCM fmt chunk size
            putShort(1)                      // audio format: PCM
            putShort(channels.toShort())
            putInt(sampleRate)
            putInt(byteRate)
            putShort(blockAlign.toShort())
            putShort(bitsPerSample.toShort())
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(dataLength.toInt())
        }.array()
    }

    /** Largest absolute sample value in the first [length] bytes of 16-bit LE PCM (0..32768). */
    fun peakAmplitude(pcm16le: ByteArray, length: Int = pcm16le.size): Int {
        var peak = 0
        var i = 0
        while (i + 1 < length) {
            val sample = (pcm16le[i + 1].toInt() shl 8) or (pcm16le[i].toInt() and 0xFF)
            peak = max(peak, abs(sample))
            i += 2
        }
        return peak
    }
}
