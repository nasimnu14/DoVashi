package com.example.dovashiapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.example.dovashiapp.domain.usecase.VoiceRecordingController
import com.example.dovashiapp.permissions.LocalMicrophonePermission
import org.koin.core.context.GlobalContext
import com.example.dovashiapp.permissions.rememberMicrophonePermission
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            CompositionLocalProvider(LocalMicrophonePermission provides rememberMicrophonePermission()) {
                App()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // In the background Android silences the microphone: stop and process what was said. Rotation keeps recording.
        if (!isChangingConfigurations) GlobalContext.get().get<VoiceRecordingController>().stopAny()
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}