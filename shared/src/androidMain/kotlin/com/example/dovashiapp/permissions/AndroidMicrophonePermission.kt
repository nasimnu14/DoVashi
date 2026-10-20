package com.example.dovashiapp.permissions

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/** The Android microphone permission, backed by the Activity Result API. Provide it through [LocalMicrophonePermission]. */
@Composable
fun rememberMicrophonePermission(): MicrophonePermission {
    val context = LocalContext.current
    val pending = remember { arrayOfNulls<(Boolean) -> Unit>(1) }
    // A result arriving after a configuration change finds no pending callback; the user simply taps again.
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        pending[0]?.invoke(granted)
        pending[0] = null
    }
    return remember(context, launcher) {
        object : MicrophonePermission {
            override fun isGranted(): Boolean =
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

            override fun request(onResult: (granted: Boolean) -> Unit) {
                if (pending[0] != null) return // a double tap: the dialog is already up
                pending[0] = onResult
                launcher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }
}
