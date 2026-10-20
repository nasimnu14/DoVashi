package com.example.dovashiapp.di

import app.cash.sqldelight.db.SqlDriver
import com.example.dovashiapp.audio.AudioPlayer
import com.example.dovashiapp.audio.AudioRecorder
import com.example.dovashiapp.audio.FileStorage
import com.example.dovashiapp.audio.IosAudioPlayer
import com.example.dovashiapp.audio.IosAudioRecorder
import com.example.dovashiapp.data.database.createIosSqlDriver
import com.example.dovashiapp.data.network.openai.OpenAiConfig
import com.example.dovashiapp.storage.IosAppDirectory
import com.example.dovashiapp.storage.IosFileStorage
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.dsl.module

/** What `sharedModule` needs from the platform, on iOS. */
fun iosPlatformModule(openAiApiKey: String): Module = module {
    single<SqlDriver> { createIosSqlDriver(IosAppDirectory.path) }
    single<CoroutineDispatcher> { Dispatchers.Default } // no IO dispatcher on Native; the database work is short
    single<FileStorage> { IosFileStorage(IosAppDirectory.path) }
    factory<AudioPlayer> { IosAudioPlayer(get()) }
    single<AudioRecorder> { IosAudioRecorder() }
    single<HttpClientEngine> { Darwin.create() }
    single { OpenAiConfig(apiKey = openAiApiKey) }
}
