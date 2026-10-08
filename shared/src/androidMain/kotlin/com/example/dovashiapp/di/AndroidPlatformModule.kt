package com.example.dovashiapp.di

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import com.example.dovashiapp.data.database.createAndroidSqlDriver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.dsl.module

fun androidPlatformModule(context: Context): Module = module {
    single<SqlDriver> { createAndroidSqlDriver(context) }
    single<CoroutineDispatcher> { Dispatchers.IO }
}
