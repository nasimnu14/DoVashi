package com.example.dovashiapp.audio

import kotlinx.coroutines.flow.StateFlow

/** Plays Recordings, one at a time. `audioPath` is a Recording reference as stored on the Message. */
interface AudioPlayer {
    val state: StateFlow<PlaybackState>

    /**
     * Resumes if [audioPath] is paused; otherwise stops whatever is playing and starts [audioPath] from the
     * beginning. Returns false when the Recording can't be played: if its file can't be found, the current
     * playback carries on; if the file exists but won't play, the player ends up [PlaybackState.Idle].
     */
    fun play(audioPath: String): Boolean

    fun pause()

    fun stop()

    /** Frees native resources; the player is [PlaybackState.Idle] and unusable afterwards. */
    fun release()
}

sealed interface PlaybackState {
    data object Idle : PlaybackState
    data class Playing(val audioPath: String) : PlaybackState
    data class Paused(val audioPath: String) : PlaybackState
}
