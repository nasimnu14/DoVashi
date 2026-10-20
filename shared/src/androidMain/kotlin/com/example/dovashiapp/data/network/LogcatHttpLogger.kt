package com.example.dovashiapp.data.network

import android.util.Log
import io.ktor.client.plugins.logging.Logger

/** HTTP traffic to logcat under the tag `DoVashi-HTTP` (debug builds only). */
object LogcatHttpLogger : Logger {
    private const val TAG = "DoVashi-HTTP"
    private const val MAX_LINE = 3_000 // logcat truncates longer entries

    override fun log(message: String) {
        message.chunked(MAX_LINE).forEach { Log.d(TAG, it) }
    }
}
