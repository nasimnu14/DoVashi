# Domain Model — 04-chat-screen

Sources: `04-chat-screen.md`, `05-message-structure.md`, `10-local-database.md`, `14-platform-specific-components.md`, `16-architecture-and-extensibility.md`, `GLOSSARY.md`, `grill-notes.md`, code on `feature/03-create-conversation`.

## 1. Entities & relationships

### Message (domain/model, new class; table from ticket 02)
`Message(id, conversationId, sourceLanguage: String?, targetLanguage: String?, audioPath: String?, transcribedText: String?, translatedText: String?, reading: String?, status: MessageStatus, createdAt: Long)`. Language fields are Language Codes. `audioPath` is a Recording reference relative to app-private storage.

### Conversation (consumed, ticket 02)
Read by id for the title and the Language Pair (`language1Code`, `language2Code`), which decides bubble side (D7).

### AudioPlayer (audio/, new interface; Android impl)
| Member | Meaning |
|---|---|
| `state: StateFlow<PlaybackState>` | `Idle`, `Playing(audioPath)` or `Paused(audioPath)` |
| `play(audioPath): Boolean` | Resume if paused on the same path; otherwise stop the current one and start this one from the beginning. Returns false if it can't start |
| `pause()` / `stop()` / `release()` | Pause the current Recording; stop and go Idle; free native resources |

### FileStorage (audio/, new interface; Android impl)
`resolve(reference): String?` returns the absolute path of an existing file under the app-private root, or null (absolute reference, `..` escape, or missing file). Ticket 06 extends it to create Recording files.

### Presentation state (presentation/conversation, new)
- `ChatUiState = Loading | NotFound | Content(title, bubbles: List<MessageBubbleUi>)`
- `MessageBubbleUi(id, side: BubbleSide(START|END), sourceLabel: String?, originalText: String?, targetLabel: String?, translatedText: String?, reading: String?, statusLabel: String?, isStatusError: Boolean, playback: BubblePlayback(NONE|PLAY|PAUSE|UNAVAILABLE))`

```
Conversation 1 ─────* Message             (observed newest first)
Message.sourceLanguage/targetLanguage ──► LanguageCatalog name (raw code fallback)
Message.audioPath ──► FileStorage.resolve ──► AudioPlayer
Conversation + [Message] + PlaybackState + unavailable ids ──► ChatUiState
```

## 2. Bounded context / ownership

| Layer | Owns |
|---|---|
| `domain/model` | `Message` |
| `domain/repository` | `MessageRepository.observeMessages`, `ConversationRepository.observeConversation` |
| `domain/usecase` | `ObserveConversationUseCase`, `ObserveMessagesUseCase` (one per action, as in ticket 02) |
| `data/database` | `selectById` (Conversation.sq), `selectByConversation` (Message.sq) |
| `data/repository` | Row → domain mapping, including tolerant status parsing shared with Home |
| `audio` (commonMain) | `AudioPlayer`, `PlaybackState`, `FileStorage` interfaces |
| `androidMain/audio`, `androidMain/storage` | `AndroidAudioPlayer` (MediaPlayer), `AndroidFileStorage(rootDir)` |
| `presentation` | Shared `languageLabel(code, languageByCode)` helper, used by Home and Chat |
| `presentation/conversation` | `ChatUiState`, `buildChatUiState` mapper, `ChatViewModel`, `ChatScreen` |
| `navigation` | `ChatRoute` → `ChatScreen`; Chat stub removed |
| `di` | Use cases, `ChatViewModel` (parameter: conversation id); the Android module binds `FileStorage` (single) and `AudioPlayer` (factory) |
| `data/debug` + `androidApp` | Richer seed; debug-only tone file |

## 3. Invariants

| ID | Invariant | Enforced by |
|---|---|---|
| M1 | Messages are shown chronologically, the newest at the bottom; the screen opens at the newest | `selectByConversation` order + `reverseLayout` |
| M2 | Only Messages of this Conversation are shown | Query `WHERE conversationId = ?` |
| M3 | Bubble side comes from data: source == Language 2 → end, else start | Mapper; no language literals |
| M4 | Language labels come from the Message record via the catalog (raw code fallback), never assumed | Mapper + `languageLabel` |
| M5 | Reading line only when `reading` is non-blank; translation block only when `translatedText` is non-blank | Mapper |
| M6 | At most one Recording plays at a time; the bubble shows Pause only for the Recording that is playing | `AudioPlayer` contract + mapper |
| M7 | A Recording reference never resolves outside app-private storage | `AndroidFileStorage.resolve` |
| M8 | Leaving the Chat screen stops playback and frees the player | `ChatViewModel.onCleared` → `release()` |
