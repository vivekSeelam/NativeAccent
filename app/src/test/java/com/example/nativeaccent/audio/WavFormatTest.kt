package com.example.nativeaccent.audio

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WavFormatTest {

    @Test
    fun `header describes 16 kHz mono 16-bit PCM`() {
        val dataLength = 32_000L // one second
        val header = WavFormat.header(dataLength, sampleRate = 16_000, channels = 1, bitsPerSample = 16)
        val buf = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

        assertEquals(44, header.size)
        assertEquals("RIFF", String(header, 0, 4, Charsets.US_ASCII))
        assertEquals(36 + dataLength, buf.getInt(4).toLong())
        assertEquals("WAVE", String(header, 8, 4, Charsets.US_ASCII))
        assertEquals("fmt ", String(header, 12, 4, Charsets.US_ASCII))
        assertEquals(16, buf.getInt(16))            // fmt chunk size
        assertEquals(1, buf.getShort(20).toInt())    // PCM
        assertEquals(1, buf.getShort(22).toInt())    // mono
        assertEquals(16_000, buf.getInt(24))         // sample rate
        assertEquals(32_000, buf.getInt(28))         // byte rate
        assertEquals(2, buf.getShort(32).toInt())    // block align
        assertEquals(16, buf.getShort(34).toInt())   // bits per sample
        assertEquals("data", String(header, 36, 4, Charsets.US_ASCII))
        assertEquals(dataLength, buf.getInt(40).toLong())
    }

    @Test
    fun `peak amplitude reads signed little-endian samples`() {
        val samples = shortArrayOf(0, 1200, -3000, 250, Short.MIN_VALUE)
        val bytes = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
            .apply { samples.forEach { putShort(it) } }.array()

        assertEquals(32768, WavFormat.peakAmplitude(bytes))
        assertEquals(3000, WavFormat.peakAmplitude(bytes, length = 8)) // first four samples only
    }

    @Test
    fun `silence has zero peak`() {
        assertEquals(0, WavFormat.peakAmplitude(ByteArray(1024)))
    }
}
