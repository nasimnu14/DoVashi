package com.example.dovashiapp.data.repository

import com.example.dovashiapp.domain.model.MessageStatus

// Persisted text may predate or postdate this build's enum; one unknown value must not crash a whole list.
internal fun parseStatus(value: String): MessageStatus =
    MessageStatus.entries.firstOrNull { it.name == value } ?: MessageStatus.FAILED
