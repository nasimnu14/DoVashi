package com.example.dovashiapp.audio

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioPlayerDelegateProtocol
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.darwin.NSObject

/** [AudioPlayer] backed by [AVAudioPlayer]. Call from the main thread; callbacks arrive on it too. */
@OptIn(ExperimentalForeignApi::class)
class IosAudioPlayer(private val fileStorage: FileStorage) : AudioPlayer {
    private val _state = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var player: AVAudioPlayer? = null
    private var delegate: Delegate? = null
    private var released = false

    override fun play(audioPath: String): Boolean {
        if (released) return false
        val current = _state.value
        if (current is PlaybackState.Paused && current.audioPath == audioPath) {
            if (player?.play() == true) {
                _state.value = PlaybackState.Playing(audioPath)
                return true
            }
            stop()
            return false
        }
        // Resolve first: a missing file must not interrupt what is already playing.
        val path = fileStorage.resolve(audioPath) ?: return false
        stop()
        if (!IosAudioSession.activateForPlayback()) return false
        val created = memScoped {
            val error = alloc<ObjCObjectVar<NSError?>>()
            try {
                AVAudioPlayer(contentsOfURL = NSURL.fileURLWithPath(path), error = error.ptr)
            } catch (e: Exception) {
                null // the file exists but won't decode
            }
        }
        val newDelegate = Delegate(onEnded = ::stop)
        if (created == null || !run { created.delegate = newDelegate; created.prepareToPlay() } || !created.play()) {
            created?.delegate = null
            IosAudioSession.deactivate()
            _state.value = PlaybackState.Idle
            return false
        }
        player = created
        delegate = newDelegate
        _state.value = PlaybackState.Playing(audioPath)
        return true
    }

    override fun pause() {
        val current = _state.value as? PlaybackState.Playing ?: return
        player?.pause()
        _state.value = PlaybackState.Paused(current.audioPath)
    }

    override fun stop() {
        val ended = player
        player = null
        delegate = null
        ended?.delegate = null
        ended?.stop()
        _state.value = PlaybackState.Idle
        if (ended != null) IosAudioSession.deactivate()
    }

    override fun release() {
        released = true
        stop()
    }

    private class Delegate(private val onEnded: () -> Unit) : NSObject(), AVAudioPlayerDelegateProtocol {
        override fun audioPlayerDidFinishPlaying(player: AVAudioPlayer, successfully: Boolean) = onEnded()

        override fun audioPlayerDecodeErrorDidOccur(player: AVAudioPlayer, error: NSError?) = onEnded()
    }
}
