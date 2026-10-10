# Plan — 04-chat-screen

Inputs: `grill-notes.md` (D1–D16), `domain-model.md` (M1–M8). Verify on Android plus the iOS compile; no iosMain changes.
Paths are under `shared/src/commonMain/kotlin/com/example/dovashiapp/` unless noted.

## Step 0 — Preconditions
- Base has tickets 01–03 (`ChatRoute`, `ChatStubScreen`, Message table, `ic_arrow_back`). Baseline `./gradlew :shared:testAndroidHostTest` is green.

## Step 1 — Schema queries and domain
- `Conversation.sq`: `selectById: SELECT * FROM Conversation WHERE id = ?;`
- `Message.sq`: `selectByConversation: SELECT * FROM Message WHERE conversationId = ? ORDER BY createdAt DESC, id DESC;` (uses the existing index).
- `domain/model/Message.kt` (fields per domain-model §1).
- `ConversationRepository.observeConversation(id): Flow<Conversation?>`; `MessageRepository.observeMessages(conversationId): Flow<List<Message>>`.
- Use cases `ObserveConversationUseCase`, `ObserveMessagesUseCase` (delegate).

## Step 2 — Data impl
- `SqlConversationRepository.observeConversation`: `selectById(...).asFlow().mapToOneOrNull(dispatcher)`.
- `SqlMessageRepository.observeMessages`: `selectByConversation(...).asFlow().mapToList(dispatcher)` mapped to `Message`. Move the tolerant `parseStatus` to a shared internal function in `data/repository` and use it in both repositories.

## Step 3 — Audio interfaces (`audio/`) and Android implementations
- `audio/AudioPlayer.kt` (`AudioPlayer`, sealed `PlaybackState`); `audio/FileStorage.kt` (`resolve(reference): String?`). KDoc states that references are relative to app-private storage.
- `androidMain/.../storage/AndroidFileStorage.kt(rootDir: File)`: reject absolute or blank references; canonical path must stay under the canonical root; file must exist.
- `androidMain/.../audio/AndroidAudioPlayer.kt(fileStorage)`: one `MediaPlayer`. `play` resumes on the same paused path, otherwise resets, `setDataSource(resolved)`, `prepare()`, `start()`. Any exception → release, Idle, return false. Completion/error listeners → Idle. `pause`, `stop`, `release` are idempotent.
- `androidPlatformModule`: `single<FileStorage> { AndroidFileStorage(context.filesDir) }`, `factory<AudioPlayer> { AndroidAudioPlayer(get()) }`.

## Step 4 — Presentation logic (`presentation/`)
- `presentation/LanguageLabel.kt`: `languageLabel(code, languageByCode) = languageByCode(code)?.name ?: code`. Home's `pairLabel` uses it (no behaviour change).
- `presentation/conversation/ChatUiState.kt`: types from domain-model §1.
- `presentation/conversation/ChatUiStateMapper.kt`: `buildChatUiState(conversation?, messages, playback, unavailableIds, languageByCode)`. Null conversation → `NotFound`. Each bubble follows D7/D8/M3–M6: status labels RECORDING "Recording…", TRANSCRIBING "Transcribing…", TRANSLATING "Translating…", FAILED "Failed" (error), COMPLETED none. Playback is NONE when `audioPath` is null, UNAVAILABLE when the id is in `unavailableIds`, PAUSE when the state is `Playing(audioPath)`, otherwise PLAY.
- `presentation/conversation/ChatViewModel.kt(conversationId, observeConversation, observeMessages, audioPlayer, languageByCode = LanguageCatalog::byCode)`: `uiState` = `combine(conversation, messages, audioPlayer.state, unavailableIds)` → mapper → `stateIn(WhileSubscribed(5_000), Loading)`. `onPlaybackClick(messageId)`: find the Message in the latest emission; if Playing that path → `pause()`; else if `!play(path)` → add the id to `unavailableIds`. `onCleared()` → `audioPlayer.release()`.

## Step 5 — Screen (`presentation/conversation/ChatScreen.kt`)
- `Scaffold` + `TopAppBar(title = conversation title (ellipsized), Back arrow)`. Loading = blank; NotFound = "Conversation not found"; empty Content = "No messages yet".
- `LazyColumn(reverseLayout = true, contentPadding)` with `items(key = id)`. Bubble: rounded `Surface`, max width 85%, aligned by `side` (start = `secondaryContainer`, end = `primaryContainer`). Content order per D8: "Voice · <source>" label, original text, target label + translation, reading (italic, smaller), status line (error colour for FAILED), then a `TextButton` with a Play/Pause vector icon and label, or "Recording unavailable" text.
- New drawables `ic_play.xml`, `ic_pause.xml` (Material 24dp). The icons are decorative; the button label carries the meaning.

## Step 6 — Navigation, DI and debug seed
- `AppNavHost`: `composable<ChatRoute>` uses `koinViewModel<ChatViewModel> { parametersOf(route.conversationId) }`. Back uses `dropUnlessResumed { navigateUp() }`. Delete `StubScreens.kt`.
- `SharedModule`: the two use cases; `viewModel { (id: Long) -> ChatViewModel(id, get(), get(), get()) }`.
- `DebugSeeder.seedIfEmpty(sampleAudioPath: String? = null)`: the "recent" conversation gets L1→L2 COMPLETED with translation + reading (holds `sampleAudioPath`) and L2→L1 COMPLETED with translation and no reading. A new conversation holds TRANSLATING (transcript only) and FAILED (transcript only). Sample text may be literal (debug only).
- `androidApp` `DoVashiApplication` (debug branch): write a 1.5 s 440 Hz 16-bit mono PCM WAV to `filesDir/audio/seed-tone.wav` if missing, then `seedIfEmpty("audio/seed-tone.wav")`.
- Verify: `./gradlew :androidApp:assembleDebug`.

## Step 7 — Tests
- commonTest `ChatUiStateMapperTest`: both directions' labels and sides; unknown code → raw code; null source → start, no source label; reading shown only when non-blank; translation hidden when blank; every status label; playback NONE/PLAY/PAUSE/UNAVAILABLE; null conversation → NotFound; empty → Content with no bubbles.
- commonTest `ChatViewModelTest` (`FakeAudioPlayer`, fake repositories): Loading → Content; NotFound; tapping plays, then pauses the same Message; tapping another Message plays that path; `play` false → UNAVAILABLE; `onCleared` releases (via `viewModelStore.clear()` or a direct call through a test helper).
- androidHostTest `SqlRepositoriesTest`: `observeMessages` newest first, `id` tie-break, scoped to the conversation, re-emits after `insertMessage`, unknown status → FAILED; `observeConversation` returns the row, or null for an unknown id.
- androidHostTest `AndroidFileStorageTest` (temp dir): existing relative file resolves; missing → null; absolute → null; `../` escape → null; blank → null.
- `DebugSeeder` test updated for the new seed shape.
- Verify: `./gradlew :shared:testAndroidHostTest :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64`.

## Acceptance criteria
1. Tapping a conversation opens its Chat screen with the stored title; its Messages are listed chronologically with the newest at the bottom, and the screen opens there.
2. Each bubble shows "Voice", the original text, the source and target Language names from the Message record, the translation, and the reading when present; none of this branches on `en`/`zh`.
3. Bubble side follows the Message's source Language versus the Conversation's Language 2.
4. In-progress and failed Messages show their status line.
5. Play plays the Message's Recording; Pause pauses it; Play again resumes; another Message's Play switches; a missing file shows "Recording unavailable"; leaving the screen stops audio.
6. Unknown conversation id → "Conversation not found"; no Messages → "No messages yet".
7. Messages inserted later appear without a refresh.
8. All Step 7 tests pass; `assembleDebug` and the iOS compile succeed.
