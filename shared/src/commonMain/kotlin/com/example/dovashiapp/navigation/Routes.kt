package com.example.dovashiapp.navigation

import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object CreateConversationRoute

@Serializable
data class ChatRoute(val conversationId: Long)
