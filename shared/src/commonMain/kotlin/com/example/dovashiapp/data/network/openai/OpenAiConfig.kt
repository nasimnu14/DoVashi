package com.example.dovashiapp.data.network.openai

/**
 * Where and how to reach OpenAI. [apiKey] comes from untracked build configuration, never from source.
 * [baseUrl] can point at a backend proxy later without touching the app's logic.
 */
data class OpenAiConfig(val apiKey: String, val baseUrl: String = "https://api.openai.com/v1") {
    override fun toString(): String = "OpenAiConfig(apiKey=<redacted>, baseUrl=$baseUrl)"
}
