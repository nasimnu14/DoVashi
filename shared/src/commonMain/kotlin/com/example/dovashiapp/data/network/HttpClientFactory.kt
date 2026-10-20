package com.example.dovashiapp.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.LoggingFormat
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Lenient JSON for provider responses: unknown fields are ignored. */
val networkJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

/**
 * The app's one HTTP client. Status codes are checked by each adapter, so non-2xx responses don't throw.
 * With a [trafficLogger] (debug builds only: bodies hold what users said), every request and response is logged,
 * with the `Authorization` header masked and audio uploads summarised as their size.
 */
fun createHttpClient(engine: HttpClientEngine, trafficLogger: Logger? = null): HttpClient = HttpClient(engine) {
    expectSuccess = false
    if (trafficLogger != null) {
        install(Logging) {
            logger = trafficLogger
            level = LogLevel.ALL
            format = LoggingFormat.OkHttp // omits binary bodies (Recordings) instead of dumping them
            sanitizeHeader { it == HttpHeaders.Authorization }
        }
    }
    install(ContentNegotiation) { json(networkJson) }
    install(HttpTimeout) {
        connectTimeoutMillis = 15_000
        requestTimeoutMillis = 120_000 // Recording uploads
        socketTimeoutMillis = 120_000
    }
}
