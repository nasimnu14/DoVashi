package com.example.dovashiapp.di

import app.cash.sqldelight.db.SqlDriver
import com.example.dovashiapp.data.database.DoVashiDatabase
import com.example.dovashiapp.data.debug.DebugSeeder
import com.example.dovashiapp.data.repository.SqlConversationRepository
import com.example.dovashiapp.data.repository.SqlMessageRepository
import com.example.dovashiapp.domain.repository.ConversationRepository
import com.example.dovashiapp.domain.repository.MessageRepository
import com.example.dovashiapp.domain.usecase.CreateConversationUseCase
import com.example.dovashiapp.domain.usecase.InsertMessageUseCase
import com.example.dovashiapp.domain.usecase.ObserveConversationUseCase
import com.example.dovashiapp.domain.usecase.ObserveConversationSummariesUseCase
import com.example.dovashiapp.domain.usecase.ObserveMessagesUseCase
import com.example.dovashiapp.presentation.conversation.ChatViewModel
import com.example.dovashiapp.presentation.createconversation.CreateConversationViewModel
import com.example.dovashiapp.presentation.home.HomeViewModel
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.datetime.TimeZone
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Platform modules must provide a [SqlDriver], the [CoroutineDispatcher] used for database work,
 * a [com.example.dovashiapp.audio.FileStorage] and an [com.example.dovashiapp.audio.AudioPlayer] factory.
 */
val sharedModule = module {
    single { DoVashiDatabase(get<SqlDriver>()) }
    single<Clock> { Clock.System }
    factory { TimeZone.currentSystemDefault() }
    single<ConversationRepository> { SqlConversationRepository(get(), get(), get()) }
    single<MessageRepository> { SqlMessageRepository(get(), get(), get()) }
    factory { ObserveConversationSummariesUseCase(get()) }
    factory { CreateConversationUseCase(get()) }
    factory { InsertMessageUseCase(get()) }
    factory { ObserveConversationUseCase(get()) }
    factory { ObserveMessagesUseCase(get()) }
    factory { DebugSeeder(get(), get(), get()) }
    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { CreateConversationViewModel(get()) }
    viewModel { (conversationId: Long) -> ChatViewModel(conversationId, get(), get(), get()) }
}

fun initKoin(platformModule: Module): Koin =
    startKoin { modules(sharedModule, platformModule) }.koin
