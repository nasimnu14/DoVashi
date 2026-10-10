package com.example.dovashiapp.data.network.openai

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** How an OpenAI HTTP status should be treated; each adapter maps it to its own failure reason. */
internal enum class OpenAiStatus { OK, UNAUTHORIZED, RATE_LIMITED, REJECTED, SERVER }

internal fun classifyStatus(code: Int): OpenAiStatus = when (code) {
    in 200..299 -> OpenAiStatus.OK
    401, 403 -> OpenAiStatus.UNAUTHORIZED
    429 -> OpenAiStatus.RATE_LIMITED
    // The request itself is unacceptable (too large, too short, bad format, unknown model): retrying won't help.
    400, 404, 413, 415, 422 -> OpenAiStatus.REJECTED
    else -> OpenAiStatus.SERVER
}

/**
 * Far above any real response. Only a sanity check: it reads Content-Length after Ktor has buffered the body, and
 * compressed or chunked responses carry none. The real bound on OpenAI replies is `max_completion_tokens`.
 */
internal const val MAX_RESPONSE_BYTES = 1L * 1024 * 1024

/**
 * Runs [block], turning any failure into [onFailure]'s result (usually a typed throw) — except our own cancellation
 * (which Ktor may wrap), which propagates. A stray CancellationException while the caller is still active counts as a
 * failure.
 */
internal suspend inline fun <T> openAiCall(onFailure: (Exception) -> T, block: () -> T): T = try {
    block()
} catch (e: Exception) {
    currentCoroutineContext().ensureActive()
    onFailure(e)
}
