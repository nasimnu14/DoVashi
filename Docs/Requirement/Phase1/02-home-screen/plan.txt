# Plan — 02-home-screen

Inputs: `grill-notes.md`, `domain-model.md` (invariants I1–I8). Android-only verification; `iosMain` untouched.

## Step 0 — Preconditions (block if unmet)
- The user's KMM project exists with Compose Multiplatform shared UI; ticket 01's `Language` catalog is implemented.
- Record the real module name, package root, version catalog, and the catalog's lookup API. Every path below is relative to the shared module's source sets and should be adapted to them.

## Step 1 — Dependencies
- Add to the version catalog plus Gradle: SQLDelight (plugin, coroutines-extensions, android-driver; sqlite-driver for `androidUnitTest`), JetBrains `navigation-compose`, JetBrains `lifecycle-viewmodel-compose`, Koin (core, compose, compose-viewmodel), `kotlinx-datetime`, kotlinx-serialization plugin (for routes), `kotlinx-coroutines-test` (tests). Keep any versions the template already pins.
- Configure the SQLDelight database `DoVashiDatabase` in package `<root>.data.database`.
- Verify: `./gradlew :<module>:assembleDebug` succeeds.

## Step 2 — Schema (`data/database/*.sq`)
- `Conversation`: id INTEGER PK AUTOINCREMENT, title TEXT NOT NULL, language1Code TEXT NOT NULL, language2Code TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL.
- `Message`: id INTEGER PK AUTOINCREMENT, conversationId INTEGER NOT NULL REFERENCES Conversation(id), sourceLanguage TEXT, targetLanguage TEXT, audioPath TEXT, transcribedText TEXT, translatedText TEXT, reading TEXT, status TEXT NOT NULL, createdAt INTEGER NOT NULL.
- `CREATE INDEX message_conversation_created ON Message(conversationId, createdAt)`.
- Queries: `insertConversation`, `insertMessage`, `touchConversation(updatedAt, id)`, `lastInsertRowId`, `countConversations`, `selectSummaries`. The summary query returns the conversation columns, the latest message's transcribedText and status (correlated subquery `ORDER BY createdAt DESC, id DESC LIMIT 1`) and `COUNT(*)` of its messages, ordered `updatedAt DESC, id DESC` (I3, I4).

## Step 3 — Domain (`domain/model`, `domain/repository`)
- `Conversation`, `Message`, `MessageStatus` (RECORDING, TRANSCRIBING, TRANSLATING, COMPLETED, FAILED), `ConversationSummary(conversation, lastMessageText?, lastMessageStatus?, messageCount)`.
- `conversationTitle(language1, language2) = "${language1.name} ↔ ${language2.name}"`.
- `ConversationRepository { observeSummaries(): Flow<List<ConversationSummary>>; createConversation(title, language1Code, language2Code): Long; hasConversations(): Boolean }`.
- `MessageRepository { insertMessage(conversationId, status, sourceLanguage?, targetLanguage?, audioPath?, transcribedText?, translatedText?, reading?): Long }`.

## Step 4 — Use cases (`domain/usecase`)
- `ObserveConversationSummariesUseCase`, `InsertMessageUseCase`: delegate to the repositories.
- `CreateConversationUseCase(language1: Language, language2: Language)`: `require(language1.code != language2.code)` (I7), builds the title with `conversationTitle`, then calls the repository.

## Step 5 — Data impl (`data/repository`, `androidMain`)
- Repository impls take `DoVashiDatabase`, `Clock`, and a `CoroutineDispatcher`.
- `createConversation`: `createdAt = updatedAt = clock.now()` (I1).
- `insertMessage`: inside one `transaction { insertMessage; touchConversation(now, conversationId) }` (I2). Store status as `MessageStatus.name` and parse it back on read.
- `observeSummaries`: `selectSummaries().asFlow().mapToList(dispatcher)`. The query reads both tables, so writes to either re-emit it (I8).
- `androidMain`: `AndroidSqliteDriver` with foreign keys enabled (`setForeignKeyConstraintsEnabled(true)` in `onConfigure`).

## Step 6 — Presentation (`presentation/home`)
- `formatRelativeTime(then: Instant, now: Instant, zone: TimeZone)`: "Just now" (<1 min) · "X min ago" (<60 min) · "X hours ago" (<24 h) · "Yesterday" (previous local calendar day) · otherwise "MMM d" (e.g. "Sep 28").
- `previewText(summary)` follows I6; `pairLabel(conversation, catalog)` follows I5 (raw code if not in the catalog; no language literals).
- `HomeUiState = Loading | Empty | Content(rows: List<ConversationRowUi>)`; `ConversationRowUi(id, title, pairLabel, preview, timeLabel, messageCount)`.
- `HomeViewModel(observeSummaries, catalog, clock, zone)`: maps each emission with a fresh `clock.now()`; `stateIn(viewModelScope, WhileSubscribed(5_000), Loading)`.

## Step 7 — UI (`presentation/home/HomeScreen.kt`)
- Material 3 `Scaffold`: `TopAppBar("Conversations")`; FAB "+" bottom-right with `contentDescription = "New conversation"`.
- `Loading` → blank/neutral; `Empty` → "No conversations yet — tap + to start one"; `Content` → `LazyColumn(items(key = { it.id }))`.
- Row: title (headline), pairLabel (secondary), preview (`maxLines = 1`, ellipsis), trailing timeLabel + messageCount. Callbacks `onCreateClick()`, `onConversationClick(id)`.

## Step 8 — Navigation (`navigation`)
- `@Serializable object HomeRoute`, `@Serializable object CreateConversationRoute`, `@Serializable data class ChatRoute(val conversationId: Long)`.
- `NavHost(startDestination = HomeRoute)`. The Create stub shows "Hello World". The Chat stub shows "Hello World" and reads `conversationId` via `toRoute<ChatRoute>()`.
- The app's root composable hosts the NavHost.

## Step 9 — DI (`di`)
- Koin modules: driver (`expect`/`actual`, Android `actual` only), database, repositories, use cases, `Clock.System`, `TimeZone.currentSystemDefault()`, the language catalog (from ticket 01), `HomeViewModel` via `koinViewModel()`.
- Start Koin from the Android entry point (Application or MainActivity, matching the template).

## Step 10 — Debug seed
- Shared `DebugSeeder(catalog, conversationRepository, createConversation, insertMessage)`: if `!hasConversations()`, create 2–3 conversations from the first two catalog languages (no `en`/`zh` literals in code) and insert sample messages across statuses (COMPLETED with text, FAILED without text, one conversation with zero messages).
- The Android trigger runs only when the app is debuggable (`ApplicationInfo.FLAG_DEBUGGABLE` or `BuildConfig.DEBUG`); never in release.

## Step 11 — Tests
- `androidUnitTest` (in-memory `JdbcSqliteDriver` with foreign keys on): sort `updatedAt DESC` plus `id` tie-break; last message by createdAt then id; count across all statuses; zero-message conversation gives null last message and count 0; `insertMessage` re-sorts its conversation to the top in one emission (I2); `createConversation` sets `updatedAt == createdAt`.
- `commonTest`: relative-time buckets at each boundary (fixed Clock and TimeZone); `previewText` for every status and for zero messages; `pairLabel` with a known code and with an unknown code; `CreateConversationUseCase` rejects identical codes and builds the title; ViewModel goes `Loading → Empty`, then `→ Content` with the expected rows (fake flow).
- Verify: `./gradlew :<module>:testDebugUnitTest :<module>:assembleDebug` is green.

## Acceptance criteria
1. Home lists every conversation ordered `updatedAt DESC`, reading only the local DB (works in airplane mode).
2. Adding a message to any conversation moves it to the top with no manual refresh.
3. Each row shows the title, pair label (from the catalog, or the raw code), the preview per I6, the relative time, and the message count.
4. The empty state shows with no conversations. There's no empty-state flash while loading.
5. "+" opens the Create stub ("Hello World"). Tapping a row opens the Chat stub with the correct `conversationId`.
6. No `en`/`zh`/English/Chinese literals in main-source logic (seed sample text excepted).
7. The debug seed fills a fresh debug install. Release builds never seed.
8. All Step 11 tests pass and `assembleDebug` succeeds.

## Recorded project facts (Step 0, 2026-10-08) — no decisions changed
- Modules: `:shared` (KMP library, plugin `com.android.kotlin.multiplatform.library`, AGP 9.1.1) and `:androidApp`. Package root `com.example.dovashiapp`. Version catalog: `gradle/libs.versions.toml`.
- Already present: Compose Multiplatform 1.12.1, `lifecycle-viewmodel-compose`, `lifecycle-runtime-compose`. To add: SQLDelight, navigation-compose, Koin, kotlinx-datetime, serialization plugin, coroutines-test.
- Ticket 01 is done: `LanguageCatalog.all` / `LanguageCatalog.byCode(code): Language?` (an `object`, in `domain/model`).
- Tests: the source set is `androidHostTest` (not `androidUnitTest`); run `./gradlew :shared:testAndroidHostTest` and `./gradlew :androidApp:assembleDebug`. Existing tests live in `commonTest`.
- iOS targets are configured, so Step 9 uses no `expect`/`actual`: the Android `SqlDriver` is built in `androidMain`/`androidApp` and provided to Koin; `iosMain` stays empty.
- The debug-seed trigger lives in `:androidApp` (`FLAG_DEBUGGABLE`), since `BuildConfig` is not available in `:shared`.
