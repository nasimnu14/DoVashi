package com.example.dovashiapp

import android.app.Application
import android.content.pm.ApplicationInfo
import com.example.dovashiapp.data.debug.DebugSeeder
import com.example.dovashiapp.di.androidPlatformModule
import com.example.dovashiapp.domain.usecase.FailInterruptedMessagesUseCase
import com.example.dovashiapp.di.initKoin
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.runBlocking

class DoVashiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val koin = initKoin(androidPlatformModule(this, openAiApiKey = BuildConfig.OPENAI_API_KEY))
        // Before any screen can start a Recording: Messages a killed process left mid-pipeline become Failed (retryable),
        // cut-off Recordings are removed. Best effort: a failure here (e.g. a full disk) must not block every launch.
        try {
            runBlocking { koin.get<FailInterruptedMessagesUseCase>()() }
        } catch (e: Exception) {
            // Left for the next launch.
        }
        val isDebuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        if (isDebuggable) {
            // Blocking on purpose (debug only): the list must be seeded before the first screen reads it.
            runBlocking { koin.get<DebugSeeder>().seedIfEmpty(writeSampleTone()) }
        }
    }

    /** Debug only: a 1.5 s 440 Hz tone so a seeded Message has a playable Recording. Returns its reference. */
    private fun writeSampleTone(): String? {
        val reference = "audio/seed-tone.wav"
        val file = File(filesDir, reference)
        val bytes = sineWav(frequencyHz = 440.0, seconds = 1.5, sampleRate = 16_000)
        if (file.length() == bytes.size.toLong()) return reference
        return try {
            file.parentFile?.mkdirs()
            // Write then rename, so a kill mid-write never leaves a truncated file behind.
            val partial = File(file.parentFile, "${file.name}.part")
            partial.writeBytes(bytes)
            if (!partial.renameTo(file)) throw IOException("rename failed")
            reference
        } catch (e: IOException) {
            null
        }
    }
}

/** 16-bit mono PCM WAV of a sine tone. */
private fun sineWav(frequencyHz: Double, seconds: Double, sampleRate: Int): ByteArray {
    val samples = (seconds * sampleRate).toInt()
    val dataSize = samples * 2
    val buffer = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)
    buffer.put("RIFF".toByteArray()).putInt(36 + dataSize).put("WAVE".toByteArray())
    buffer.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1)
        .putInt(sampleRate).putInt(sampleRate * 2).putShort(2).putShort(16)
    buffer.put("data".toByteArray()).putInt(dataSize)
    repeat(samples) { i ->
        buffer.putShort((sin(2 * PI * frequencyHz * i / sampleRate) * Short.MAX_VALUE * 0.3).toInt().toShort())
    }
    return buffer.array()
}
