package com.example.nativeaccent.audio

/**
 * Shared playback rate for both the coach voice and the learner's recording, so
 * the "1x" toggle means the same thing on both buttons.
 */
enum class PlaybackSpeed(val rate: Float, val label: String) {
    NORMAL(1.0f, "1x"),
    HALF(0.5f, "0.5x");

    fun next(): PlaybackSpeed = if (this == NORMAL) HALF else NORMAL
}
