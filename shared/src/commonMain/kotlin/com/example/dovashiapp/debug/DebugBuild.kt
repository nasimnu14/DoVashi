package com.example.dovashiapp.debug

import androidx.compose.runtime.staticCompositionLocalOf

/** Whether this is a debug build, provided to the UI by the platform's entry point. */
val LocalDebugBuild = staticCompositionLocalOf { false }
