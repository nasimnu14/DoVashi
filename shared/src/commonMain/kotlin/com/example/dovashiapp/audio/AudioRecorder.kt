package com.example.dovashiapp.audio

/** Records the microphone into a file. Call from the main thread. */
interface AudioRecorder {
    /**
     * Starts recording into [absolutePath]. If recording stops by itself — [maxDurationMillis] reached (the file is
     * kept) or a recorder error — [onStoppedAutomatically] is called on the main thread so the caller can [stop].
     * Returns false if the recorder can't start.
     */
    fun start(absolutePath: String, maxDurationMillis: Long, onStoppedAutomatically: () -> Unit): Boolean

    /** Stops and finalises the file. Returns the recorded duration in milliseconds, or null if nothing usable was recorded. */
    fun stop(): Long?
}
