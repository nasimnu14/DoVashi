package com.example.dovashiapp.audio

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.AVFAudio.AVAudioRecorder
import platform.AVFAudio.AVAudioRecorderDelegateProtocol
import platform.AVFAudio.AVEncoderBitRateKey
import platform.AVFAudio.AVFormatIDKey
import platform.AVFAudio.AVNumberOfChannelsKey
import platform.AVFAudio.AVSampleRateKey
import platform.CoreAudioTypes.kAudioFormatMPEG4AAC
import platform.Foundation.NSDate
import platform.Foundation.NSError
import platform.Foundation.NSNumber
import platform.Foundation.NSURL
import platform.Foundation.timeIntervalSinceReferenceDate
import platform.darwin.NSObject

/** [AudioRecorder] backed by [AVAudioRecorder]: AAC in MPEG-4 (`.m4a`), mono, 16 kHz, 64 kbps. Call from the main thread. */
@OptIn(ExperimentalForeignApi::class)
class IosAudioRecorder : AudioRecorder {
    private var recorder: AVAudioRecorder? = null
    private var delegate: Delegate? = null
    private var startedAt = 0.0
    private var maxDurationMillis = 0L
    private var maxDurationReached = false

    override fun start(absolutePath: String, maxDurationMillis: Long, onStoppedAutomatically: () -> Unit): Boolean {
        discardCurrent()
        if (!IosAudioSession.activateForRecording()) return false
        val settings: Map<Any?, *> = mapOf(
            AVFormatIDKey to NSNumber(unsignedInt = kAudioFormatMPEG4AAC),
            AVSampleRateKey to NSNumber(double = 16_000.0),
            AVNumberOfChannelsKey to NSNumber(int = 1),
            AVEncoderBitRateKey to NSNumber(int = 64_000),
        )
        val created = memScoped {
            val error = alloc<ObjCObjectVar<NSError?>>()
            try {
                AVAudioRecorder(uRL = NSURL.fileURLWithPath(absolutePath), settings = settings, error = error.ptr)
            } catch (e: Exception) {
                null
            }
        }
        if (created == null) {
            IosAudioSession.deactivate()
            return false
        }
        // The delegate fires when the time limit passes or on an encoder error; a stop of ours detaches it first.
        val newDelegate = Delegate { successfully ->
            if (successfully) maxDurationReached = true
            onStoppedAutomatically()
        }
        created.delegate = newDelegate
        if (!created.prepareToRecord() || !created.recordForDuration(maxDurationMillis / 1000.0)) {
            created.delegate = null
            IosAudioSession.deactivate()
            return false
        }
        recorder = created
        delegate = newDelegate
        this.maxDurationMillis = maxDurationMillis
        maxDurationReached = false
        startedAt = NSDate.timeIntervalSinceReferenceDate
        return true
    }

    override fun stop(): Long? {
        val current = recorder ?: return null
        val elapsed = ((NSDate.timeIntervalSinceReferenceDate - startedAt) * 1000).toLong()
        release(current)
        // A recording cut at the limit is complete; its duration is the limit.
        return if (maxDurationReached) maxDurationMillis else elapsed.takeIf { it > 0 }
    }

    /** Stops a recorder left running, keeping nothing (the caller owns the file). */
    private fun discardCurrent() {
        recorder?.let(::release)
    }

    private fun release(current: AVAudioRecorder) {
        current.delegate = null
        current.stop()
        recorder = null
        delegate = null
        IosAudioSession.deactivate()
    }

    private class Delegate(private val onFinished: (successfully: Boolean) -> Unit) : NSObject(), AVAudioRecorderDelegateProtocol {
        override fun audioRecorderDidFinishRecording(recorder: AVAudioRecorder, successfully: Boolean) = onFinished(successfully)

        override fun audioRecorderEncodeErrorDidOccur(recorder: AVAudioRecorder, error: NSError?) = onFinished(false)
    }
}
