package com.example.dovashiapp.domain.model

data class Conversation(
    val id: Long,
    val title: String,
    val language1Code: String,
    val language2Code: String,
    val createdAt: Long,
    val updatedAt: Long,
)
