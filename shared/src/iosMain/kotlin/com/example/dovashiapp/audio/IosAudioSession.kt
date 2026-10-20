package com.example.dovashiapp.audio

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionDefaultToSpeaker
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation
import platform.AVFAudio.setActive
import platform.Foundation.NSError

/** The shared audio session: claiming it interrupts other apps' audio (iOS's "audio focus"), releasing it hands it back. */
@OptIn(ExperimentalForeignApi::class)
internal object IosAudioSession {
    fun activateForRecording(): Boolean = activate(AVAudioSessionCategoryPlayAndRecord, AVAudioSessionCategoryOptionDefaultToSpeaker)

    fun activateForPlayback(): Boolean = activate(AVAudioSessionCategoryPlayback, 0u)

    fun deactivate() {
        memScoped {
            val error = alloc<ObjCObjectVar<NSError?>>()
            AVAudioSession.sharedInstance().setActive(false, withOptions = AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation, error = error.ptr)
        }
    }

    private fun activate(category: String?, options: ULong): Boolean = memScoped {
        val error = alloc<ObjCObjectVar<NSError?>>()
        val session = AVAudioSession.sharedInstance()
        session.setCategory(category, withOptions = options, error = error.ptr) && session.setActive(true, error = error.ptr)
    }
}
