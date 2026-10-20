package com.example.dovashiapp.data.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

/** The app's SQLite database in [directory], with foreign keys on (as on Android). */
fun createIosSqlDriver(directory: String): SqlDriver =
    NativeSqliteDriver(
        schema = DoVashiDatabase.Schema,
        name = "dovashi.db",
        onConfiguration = { config ->
            config.copy(extendedConfig = config.extendedConfig.copy(basePath = directory, foreignKeyConstraints = true))
        },
    )
