package com.example.dovashiapp.di

import app.cash.sqldelight.db.SqlDriver
import com.example.dovashiapp.data.database.DoVashiDatabase
import com.example.dovashiapp.data.debug.DebugSeeder
import com.example.dovashiapp.data.network.createHttpClient
import com.example.dovashiapp.data.network.openai.OpenAiSpeechToTextService
import com.example.dovashiapp.data.network.openai.OpenAiTranslationService
import com.example.dovashiapp.data.repository.SqlConversationRepository
import com.example.dovashiapp.data.repository.SqlMessageRepository
import com.example.dovashiapp.domain.repository.ConversationRepository
import com.example.dovashiapp.domain.repository.MessageRepository
import com.example.dovashiapp.domain.service.SpeechToTextService
import com.example.dovashiapp.domain.service.TranslationService
import com.example.dovashiapp.domain.usecase.CreateConversationUseCase
import com.example.dovashiapp.domain.usecase.InsertMessageUseCase
import com.example.dovashiapp.domain.usecase.MarkMessageFailedUseCase
import com.example.dovashiapp.domain.usecase.MarkMessageTranscribingUseCase
import com.example.dovashiapp.domain.usecase.ObserveConversationUseCase
import com.example.dovashiapp.domain.usecase.ObserveConversationSummariesUseCase
import com.example.dovashiapp.domain.usecase.ObserveMessagesUseCase
import com.example.dovashiapp.domain.usecase.ResolveMessageLanguagesUseCase
import com.example.dovashiapp.domain.usecase.RetryTranslationUseCase
import com.example.dovashiapp.domain.usecase.SaveTranscriptionUseCase
import com.example.dovashiapp.domain.usecase.SaveTranslationUseCase
import com.example.dovashiapp.domain.usecase.TranslateMessageUseCase
import com.example.dovashiapp.domain.usecase.FailInterruptedMessagesUseCase
import com.example.dovashiapp.domain.usecase.MessageProcessor
import com.example.dovashiapp.domain.usecase.ProcessRecordingUseCase
import com.example.dovashiapp.domain.usecase.VoiceRecordingController
import com.example.dovashiapp.presentation.conversation.ChatViewModel
import com.example.dovashiapp.presentation.createconversation.CreateConversationViewModel
import com.example.dovashiapp.presentation.home.HomeViewModel
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.qualifier.named
import kotlinx.datetime.TimeZone
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** Lives as long as the app: Message processing must outlive any one screen. */
private val APP_SCOPE = named("appScope")

/**
 * Platform modules must provide a [SqlDriver], the [CoroutineDispatcher] used for database work,
 * a [com.example.dovashiapp.audio.FileStorage], an [com.example.dovashiapp.audio.AudioPlayer] factory,
 * an [com.example.dovashiapp.audio.AudioRecorder], an [io.ktor.client.engine.HttpClientEngine] and the
 * [com.example.dovashiapp.data.network.openai.OpenAiConfig]. The recorder is driven on `Dispatchers.Main`.
 */
val sharedModule = module {
    single { DoVashiDatabase(get<SqlDriver>()) }
    single<Clock> { Clock.System }
    factory { TimeZone.currentSystemDefault() }
    single<ConversationRepository> { SqlConversationRepository(get(), get(), get()) }
    single<MessageRepository> { SqlMessageRepository(get(), get(), get()) }
    single { createHttpClient(get()) }
    single<SpeechToTextService> { OpenAiSpeechToTextService(get(), get(), get(), get()) }
    single<TranslationService> { OpenAiTranslationService(get(), get()) }
    factory { ObserveConversationSummariesUseCase(get()) }
    factory { CreateConversationUseCase(get()) }
    factory { InsertMessageUseCase(get()) }
    factory { ObserveConversationUseCase(get()) }
    factory { ObserveMessagesUseCase(get()) }
    factory { MarkMessageTranscribingUseCase(get()) }
    factory { SaveTranscriptionUseCase(get()) }
    factory { SaveTranslationUseCase(get()) }
    factory { RetryTranslationUseCase(get()) }
    factory { ResolveMessageLanguagesUseCase() }
    factory { TranslateMessageUseCase(get(), get(), get()) }
    factory { ProcessRecordingUseCase(get(), get(), get(), get(), get(), get(), get()) }
    factory { FailInterruptedMessagesUseCase(get(), get()) }
    // Exceptions are handled where they happen; an Error (e.g. out of memory) should still crash and be reported.
    single(APP_SCOPE) { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single { MessageProcessor(get(APP_SCOPE), get()) }
    single { VoiceRecordingController(get(), get(), get(), get(), get(), get(APP_SCOPE), Dispatchers.Main) }
    factory { MarkMessageFailedUseCase(get()) }
    factory { DebugSeeder(get(), get(), get()) }
    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { CreateConversationViewModel(get()) }
    viewModel { (conversationId: Long) -> ChatViewModel(conversationId, get(), get(), get(), get()) }
}

fun initKoin(platformModule: Module): Koin =
    startKoin { modules(sharedModule, platformModule) }.koin
