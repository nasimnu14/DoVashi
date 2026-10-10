package com.example.dovashiapp.permissions

import androidx.compose.runtime.staticCompositionLocalOf

/** Access to the microphone, provided to the UI by the platform's entry point. */
interface MicrophonePermission {
    fun isGranted(): Boolean

    /** Asks the user; [onResult] receives whether access was granted. */
    fun request(onResult: (granted: Boolean) -> Unit)
}

/** For platforms that can't record yet. */
object MicrophonePermissionUnavailable : MicrophonePermission {
    override fun isGranted(): Boolean = false

    override fun request(onResult: (granted: Boolean) -> Unit) = onResult(false)
}

val LocalMicrophonePermission = staticCompositionLocalOf<MicrophonePermission> { MicrophonePermissionUnavailable }
