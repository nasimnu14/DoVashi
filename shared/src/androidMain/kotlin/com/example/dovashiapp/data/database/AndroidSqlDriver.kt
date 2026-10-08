package com.example.dovashiapp.data.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

fun createAndroidSqlDriver(context: Context): SqlDriver =
    AndroidSqliteDriver(
        schema = DoVashiDatabase.Schema,
        context = context,
        name = "dovashi.db",
        callback = object : AndroidSqliteDriver.Callback(DoVashiDatabase.Schema) {
            override fun onConfigure(db: SupportSQLiteDatabase) {
                super.onConfigure(db)
                db.setForeignKeyConstraintsEnabled(true)
            }
        },
    )
