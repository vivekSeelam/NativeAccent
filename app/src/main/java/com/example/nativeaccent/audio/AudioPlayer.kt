package com.example.nativeaccent.audio

import android.media.MediaPlayer
import android.media.PlaybackParams
import android.util.Log
import java.io.File

/**
 * Plays back a recorded attempt at a chosen [PlaybackSpeed].
 *
 * One player at a time: starting a new playback tears the previous one down, so
 * tapping "You" twice never stacks two voices on top of each other.
 */
class AudioPlayer {

    private var player: MediaPlayer? = null

    val isPlaying: Boolean get() = player?.isPlaying == true

    /**
     * Plays [path] at [speed]. [onComplete] fires when playback ends or fails,
     * always on the main thread (MediaPlayer callbacks are posted there).
     */
    fun play(path: String, speed: PlaybackSpeed, onComplete: () -> Unit = {}) {
        stop()
        val file = File(path)
        if (!file.exists() || file.length() == 0L) {
            Log.w(TAG, "recording missing: $path")
            onComplete()
            return
        }
        val created = MediaPlayer()
        try {
            created.setDataSource(path)
            created.prepare()
            // Params must be applied while prepared; MediaPlayer would otherwise
            // reset the rate to 1x on start().
            created.playbackParams = PlaybackParams().setSpeed(speed.rate)
            created.setOnCompletionListener {
                stop()
                onComplete()
            }
            created.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "playback error what=$what extra=$extra")
                stop()
                onComplete()
                true
            }
            created.start()
            player = created
        } catch (e: Exception) {
            Log.e(TAG, "could not play $path", e)
            created.releaseQuietly()
            onComplete()
        }
    }

    fun stop() {
        player?.releaseQuietly()
        player = null
    }

    fun release() = stop()

    private fun MediaPlayer.releaseQuietly() {
        try {
            setOnCompletionListener(null)
            setOnErrorListener(null)
            if (isPlaying) stop()
        } catch (e: IllegalStateException) {
            Log.d(TAG, "stop() on a released player", e)
        } finally {
            release()
        }
    }

    private companion object {
        const val TAG = "AudioPlayer"
    }
}
