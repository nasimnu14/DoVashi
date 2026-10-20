package com.example.dovashiapp.domain.model

/** Longest transcript, translation or Reading stored; far beyond any spoken Message, far below SQLite row limits. */
const val MAX_MESSAGE_TEXT_LENGTH = 10_000

/**
 * Text with invisible characters (whitespace, zero-width/format such as U+200B and U+FEFF, control) trimmed from
 * both ends, or null when nothing visible is left.
 */
fun visibleTextOrNull(text: String?): String? = text?.trim(::isInvisible)?.takeIf { it.isNotEmpty() }

private fun isInvisible(c: Char): Boolean =
    c.isWhitespace() || c.category == CharCategory.FORMAT || c.category == CharCategory.CONTROL
