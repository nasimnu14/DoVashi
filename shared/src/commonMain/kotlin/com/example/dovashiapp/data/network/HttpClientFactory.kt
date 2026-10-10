package com.example.dovashiapp.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Lenient JSON for provider responses: unknown fields are ignored. */
val networkJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

/** The app's one HTTP client. Status codes are checked by each adapter, so non-2xx responses don't throw. */
fun createHttpClient(engine: HttpClientEngine): HttpClient = HttpClient(engine) {
    expectSuccess = false
    install(ContentNegotiation) { json(networkJson) }
    install(HttpTimeout) {
        connectTimeoutMillis = 15_000
        requestTimeoutMillis = 120_000 // Recording uploads
        socketTimeoutMillis = 120_000
    }
}
