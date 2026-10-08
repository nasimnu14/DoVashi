package com.example.dovashiapp

import android.app.Application
import android.content.pm.ApplicationInfo
import com.example.dovashiapp.data.debug.DebugSeeder
import com.example.dovashiapp.di.androidPlatformModule
import com.example.dovashiapp.di.initKoin
import kotlinx.coroutines.runBlocking

class DoVashiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val koin = initKoin(androidPlatformModule(this))
        val isDebuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        if (isDebuggable) {
            // Blocking on purpose (debug only): the list must be seeded before the first screen reads it.
            runBlocking { koin.get<DebugSeeder>().seedIfEmpty() }
        }
    }
}
