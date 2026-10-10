package com.example.dovashiapp.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import java.io.File

/** [AudioRecorder] backed by [MediaRecorder]: AAC in MPEG-4, mono, 16 kHz, 64 kbps. Call from the main thread. */
class AndroidAudioRecorder(private val context: Context) : AudioRecorder {
    private var recorder: MediaRecorder? = null
    private var startedAt = 0L
    private var maxDurationReached = false

    override fun start(absolutePath: String, maxDurationMillis: Long, onStoppedAutomatically: () -> Unit): Boolean {
        discardCurrent()
        val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        return try {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mediaRecorder.setAudioChannels(1)
            mediaRecorder.setAudioSamplingRate(16_000)
            mediaRecorder.setAudioEncodingBitRate(64_000)
            mediaRecorder.setMaxDuration(maxDurationMillis.toInt())
            mediaRecorder.setOutputFile(absolutePath)
            mediaRecorder.setOnInfoListener { _, what, _ ->
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                    maxDurationReached = true
                    onStoppedAutomatically()
                }
            }
            // e.g. the microphone was taken away; stop() then reports nothing usable.
            mediaRecorder.setOnErrorListener { _, _, _ -> onStoppedAutomatically() }
            mediaRecorder.prepare()
            mediaRecorder.start()
            recorder = mediaRecorder
            startedAt = SystemClock.elapsedRealtime()
            maxDurationReached = false
            true
        } catch (e: Exception) {
            mediaRecorder.release()
            File(absolutePath).delete()
            false
        }
    }

    override fun stop(): Long? {
        val mediaRecorder = recorder ?: return null
        recorder = null
        val duration = SystemClock.elapsedRealtime() - startedAt
        return try {
            mediaRecorder.stop()
            duration
        } catch (e: RuntimeException) {
            // stop() throws when no audio was captured; after an automatic stop at max duration the file is complete.
            if (maxDurationReached) duration else null
        } finally {
            mediaRecorder.release()
        }
    }

    /** Stops and releases a recorder left running, keeping nothing (the caller owns the file). */
    private fun discardCurrent() {
        val mediaRecorder = recorder ?: return
        recorder = null
        try {
            mediaRecorder.stop()
        } catch (e: RuntimeException) {
            // Nothing captured; discarding anyway.
        } finally {
            mediaRecorder.release()
        }
    }
}
