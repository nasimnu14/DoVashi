package com.example.dovashiapp.data.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.dovashiapp.domain.model.MessageStatus
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals

/** Doc 10: the tables hold exactly the specified columns, and the statuses are the specified five. */
class SchemaConformanceTest {
    private val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY, Properties().apply { put("foreign_keys", "true") })
        .also { DoVashiDatabase.Schema.create(it) }

    private fun <T> rows(sql: String, read: (SqlCursor) -> T): List<T> = driver.executeQuery(null, sql, { cursor ->
        val out = mutableListOf<T>()
        while (cursor.next().value) out += read(cursor)
        QueryResult.Value(out)
    }, 0).value

    private fun columns(table: String): List<String> = rows("PRAGMA table_info($table)") { it.getString(1)!! }

    @Test
    fun conversationTableHasTheSpecifiedColumns() = assertEquals(
        listOf("id", "title", "language1Code", "language2Code", "createdAt", "updatedAt"),
        columns("Conversation"),
    )

    @Test
    fun messageTableHasTheSpecifiedColumns() = assertEquals(
        listOf(
            "id", "conversationId", "sourceLanguage", "targetLanguage", "audioPath",
            "transcribedText", "translatedText", "reading", "status", "createdAt",
        ),
        columns("Message"),
    )

    @Test
    fun messageReferencesItsConversation() = assertEquals(
        listOf("Conversation"),
        rows("PRAGMA foreign_key_list(Message)") { it.getString(2)!! },
    )

    @Test
    fun statusValuesAreTheSpecifiedFive() = assertEquals(
        listOf("RECORDING", "TRANSCRIBING", "TRANSLATING", "COMPLETED", "FAILED"),
        MessageStatus.entries.map { it.name },
    )
}
