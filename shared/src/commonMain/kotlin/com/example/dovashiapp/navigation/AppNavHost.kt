package com.example.dovashiapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.dovashiapp.presentation.conversation.ChatScreen
import com.example.dovashiapp.presentation.conversation.ChatViewModel
import com.example.dovashiapp.presentation.createconversation.CreateConversationScreen
import com.example.dovashiapp.presentation.createconversation.CreateConversationViewModel
import com.example.dovashiapp.presentation.home.HomeScreen
import com.example.dovashiapp.presentation.home.HomeViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

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
        composable<CreateConversationRoute> { entry ->
            val viewModel = koinViewModel<CreateConversationViewModel>()
            val state = viewModel.uiState.collectAsStateWithLifecycle().value
            LaunchedEffect(state.createdConversationId) {
                val id = state.createdConversationId ?: return@LaunchedEffect
                // The user may have pressed Back while the insert finished; never navigate from a popped form.
                if (navController.currentBackStackEntry?.id != entry.id) return@LaunchedEffect
                navController.navigate(ChatRoute(id)) {
                    // Back from Chat returns to Home, not to this form.
                    popUpTo<CreateConversationRoute> { inclusive = true }
                    launchSingleTop = true
                }
                viewModel.onNavigationHandled()
            }
            CreateConversationScreen(
                state = state,
                onLanguage1Selected = viewModel::selectLanguage1,
                onLanguage2Selected = viewModel::selectLanguage2,
                // A form that is already leaving (Back pressed, exit animation running) ignores taps.
                onStartClick = dropUnlessResumed { viewModel.start() },
                onBackClick = dropUnlessResumed { navController.navigateUp() },
            )
        }
        composable<ChatRoute> { entry ->
            val conversationId = entry.toRoute<ChatRoute>().conversationId
            val viewModel = koinViewModel<ChatViewModel>(key = "chat-$conversationId") { parametersOf(conversationId) }
            val state = viewModel.uiState.collectAsStateWithLifecycle().value
            // Popped (not merely backgrounded): stop now instead of after the exit animation. The toolbar arrow
            // pops before ON_PAUSE; system back (predictive) pauses first and pops before ON_STOP.
            val stopIfPopped = { if (navController.currentBackStackEntry?.id != entry.id) viewModel.stopPlayback() }
            LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { stopIfPopped() }
            LifecycleEventEffect(Lifecycle.Event.ON_STOP) { stopIfPopped() }
            ChatScreen(
                state = state,
                onPlaybackClick = viewModel::onPlaybackClick,
                onBackClick = dropUnlessResumed { navController.navigateUp() },
            )
        }
    }
}
