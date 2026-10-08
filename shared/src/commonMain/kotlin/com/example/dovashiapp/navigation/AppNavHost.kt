package com.example.dovashiapp.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.dovashiapp.presentation.home.HomeScreen
import com.example.dovashiapp.presentation.home.HomeViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = HomeRoute) {
        composable<HomeRoute> {
            val viewModel = koinViewModel<HomeViewModel>()
            val state = viewModel.uiState.collectAsStateWithLifecycle().value
            HomeScreen(
                state = state,
                onCreateClick = { navController.navigate(CreateConversationRoute) { launchSingleTop = true } },
                onConversationClick = { navController.navigate(ChatRoute(it)) { launchSingleTop = true } },
            )
        }
        composable<CreateConversationRoute> { CreateConversationStubScreen() }
        composable<ChatRoute> { entry ->
            ChatStubScreen(entry.toRoute<ChatRoute>().conversationId)
        }
    }
}
