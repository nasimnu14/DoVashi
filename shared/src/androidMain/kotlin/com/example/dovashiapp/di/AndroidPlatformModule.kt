package com.example.dovashiapp.di

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import com.example.dovashiapp.audio.AndroidAudioPlayer
import com.example.dovashiapp.audio.AndroidAudioRecorder
import com.example.dovashiapp.audio.AudioPlayer
import com.example.dovashiapp.audio.AudioRecorder
import com.example.dovashiapp.audio.FileStorage
import com.example.dovashiapp.data.database.createAndroidSqlDriver
import com.example.dovashiapp.data.network.openai.OpenAiConfig
import com.example.dovashiapp.storage.AndroidFileStorage
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.dsl.module

fun androidPlatformModule(context: Context, openAiApiKey: String): Module = module {
    single<SqlDriver> { createAndroidSqlDriver(context) }
    single<CoroutineDispatcher> { Dispatchers.IO }
    single<FileStorage> { AndroidFileStorage(context.filesDir) }
    factory<AudioPlayer> { AndroidAudioPlayer(get()) }
    single<AudioRecorder> { AndroidAudioRecorder(context) }
    single<HttpClientEngine> { OkHttp.create() }
    single { OpenAiConfig(apiKey = openAiApiKey) }
}
