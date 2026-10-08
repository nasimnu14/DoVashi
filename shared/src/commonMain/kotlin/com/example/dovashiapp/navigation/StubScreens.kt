package com.example.dovashiapp.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun CreateConversationStubScreen() {
    Box(Modifier.fillMaxSize().safeContentPadding(), contentAlignment = Alignment.Center) {
        Text("Hello World")
    }
}

@Composable
fun ChatStubScreen(conversationId: Long) {
    Box(Modifier.fillMaxSize().safeContentPadding(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Hello World")
            Text("Conversation #$conversationId")
        }
    }
}
