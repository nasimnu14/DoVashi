package com.example.dovashiapp.permissions

import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionRecordPermissionGranted
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/** Microphone access through the shared audio session. The usage text is `NSMicrophoneUsageDescription` in Info.plist. */
object IosMicrophonePermission : MicrophonePermission {
    override fun isGranted(): Boolean = AVAudioSession.sharedInstance().recordPermission == AVAudioSessionRecordPermissionGranted

    override fun request(onResult: (granted: Boolean) -> Unit) {
        AVAudioSession.sharedInstance().requestRecordPermission { granted ->
            // The system answers on an arbitrary queue; the UI wants the main thread.
            dispatch_async(dispatch_get_main_queue()) { onResult(granted) }
        }
    }
}
