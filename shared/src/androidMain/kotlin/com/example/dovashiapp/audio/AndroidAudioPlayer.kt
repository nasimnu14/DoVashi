package com.example.dovashiapp.audio

import android.media.AudioAttributes
import android.media.MediaPlayer
import java.io.FileInputStream
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** [AudioPlayer] backed by [MediaPlayer]. Call from the main thread; callbacks arrive on it too. */
class AndroidAudioPlayer(private val fileStorage: FileStorage) : AudioPlayer {
    private val _state = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var player: MediaPlayer? = null
    private var released = false

    override fun play(audioPath: String): Boolean {
        if (released) return false
        val current = _state.value
        if (current is PlaybackState.Paused && current.audioPath == audioPath) {
            return try {
                checkNotNull(player).start()
                _state.value = PlaybackState.Playing(audioPath)
                true
            } catch (e: Exception) {
                stop()
                false
            }
        }
        // Resolve first: a missing file must not interrupt what is already playing.
        val path = fileStorage.resolve(audioPath) ?: return false
        stop()
        val mediaPlayer = MediaPlayer()
        return try {
            mediaPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            // A file descriptor, not the path string, which MediaPlayer would parse as a URI.
            FileInputStream(path).use { mediaPlayer.setDataSource(it.fd) }
            mediaPlayer.setOnCompletionListener { stop() }
            mediaPlayer.setOnErrorListener { _, _, _ -> stop(); true }
            mediaPlayer.prepare() // Local file: synchronous prepare is fast enough.
            mediaPlayer.start()
            player = mediaPlayer
            _state.value = PlaybackState.Playing(audioPath)
            true
        } catch (e: Exception) {
            mediaPlayer.release()
            _state.value = PlaybackState.Idle
            false
        }
    }

    override fun pause() {
        val current = _state.value as? PlaybackState.Playing ?: return
        try {
            player?.pause()
            _state.value = PlaybackState.Paused(current.audioPath)
        } catch (e: IllegalStateException) {
            // An error the player hasn't reported yet; treat it as the end of playback.
            stop()
        }
    }

    override fun stop() {
        player?.release()
        player = null
        _state.value = PlaybackState.Idle
    }

    override fun release() {
        released = true
        stop()
    }
}
