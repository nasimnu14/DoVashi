package com.example.dovashiapp

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.ComposeUIViewController
import com.example.dovashiapp.di.initKoin
import com.example.dovashiapp.di.iosPlatformModule
import com.example.dovashiapp.domain.usecase.FailInterruptedMessagesUseCase
import com.example.dovashiapp.domain.usecase.VoiceRecordingController
import com.example.dovashiapp.permissions.IosMicrophonePermission
import com.example.dovashiapp.permissions.LocalMicrophonePermission
import kotlinx.coroutines.runBlocking
import org.koin.core.Koin
import platform.Foundation.NSBundle
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplicationDidEnterBackgroundNotification

fun MainViewController() = ComposeUIViewController {
    startApp()
    CompositionLocalProvider(LocalMicrophonePermission provides IosMicrophonePermission) { App() }
}

private var koin: Koin? = null

/** Starts Koin once per process, before any screen reads from it. */
private fun startApp() {
    if (koin != null) return
    val started = initKoin(iosPlatformModule(openAiApiKey = openAiApiKey()))
    // Messages a killed process left mid-pipeline become Failed (retryable); best effort, as on Android.
    try {
        runBlocking { started.get<FailInterruptedMessagesUseCase>()() }
    } catch (e: Exception) {
        // Left for the next launch.
    }
    // A backgrounded app can't keep the microphone: stop and process what was said.
    NSNotificationCenter.defaultCenter.addObserverForName(UIApplicationDidEnterBackgroundNotification, null, NSOperationQueue.mainQueue) {
        started.get<VoiceRecordingController>().stopAny()
    }
    koin = started
}

/**
 * The key comes from the git-ignored `Secrets.xcconfig` through Info.plist (`OpenAIApiKey`). An unset build setting
 * stays as the literal `$(OPENAI_API_KEY)`, which is not a key; keys are printable ASCII, so anything else is dropped.
 */
private fun openAiApiKey(): String =
    (NSBundle.mainBundle.objectForInfoDictionaryKey("OpenAIApiKey") as? String)
        ?.takeIf { !it.startsWith("$(") }
        ?.filter { it in '!'..'~' }
        .orEmpty()
