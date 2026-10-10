# Grill Notes — 04-chat-screen

Source: `Docs/Requirement/Phase1/04-chat-screen.md` (with `05-message-structure.md`, `10-local-database.md`, `11-infinite-chat-scrolling.md`, `14-platform-specific-components.md`, `16-architecture-and-extensibility.md`) · 2026-10-10.

**No interview was run** (user instruction 2026-10-10: skip grill, don't wait for permission). Claude made every decision below from the docs, `GLOSSARY.md`, earlier tickets and the code. Each is marked **(self)**.

Repo state: tickets 01–03 exist on this branch's base (`feature/03-create-conversation`). The Chat destination is the ticket-02 stub. The `Message` table exists (ticket 02); no domain `Message` class exists yet (deferred by ticket 02 to this ticket).

## Scope

| # | Question | Resolved answer |
|---|----------|-----------------|
| D1 | What does this ticket build? | The real Chat screen in place of the stub: the Conversation's title, its Messages as bubbles in chronological order, and Play/Pause of each Message's Recording. (self) |
| D2 | What is explicitly left to later tickets? | Microphone and recording (06); generating translations and readings (07–09); Retry (12); incremental loading (11). This ticket observes all Messages of the Conversation reactively, and ticket 11 swaps in incremental loading behind the same screen. (self) |
| D3 | Domain `Message`? | Created here in `domain/model`, mirroring the `Message` table (all nullable pipeline fields, `status`, `createdAt`). (self) |

## Data

| # | Question | Resolved answer |
|---|----------|-----------------|
| D4 | Queries? | `Message.selectByConversation` newest first (`createdAt DESC, id DESC`) and `Conversation.selectById`. Both are reactive flows, so Messages added by later tickets appear without a refresh. (self) |
| D5 | Repository surface? | `MessageRepository.observeMessages(conversationId): Flow<List<Message>>` (newest first); `ConversationRepository.observeConversation(id): Flow<Conversation?>`. Unknown stored status reads as `FAILED`, same as Home. (self) |

## Layout

| # | Question | Resolved answer |
|---|----------|-----------------|
| D6 | Order and initial position? | `LazyColumn(reverseLayout = true)` over the newest-first list. The newest Message sits at the bottom, the screen opens there, and older Messages are above, as in WhatsApp. This also matches ticket 11's "older loaded when scrolling upward". (self) |
| D7 | Which side does a bubble sit on? | Data-driven: `sourceLanguage == conversation.language2Code` → end (right); anything else (Language 1, unknown, or not yet detected) → start. No language literals. (self) |
| D8 | Bubble content, top to bottom? | (1) "Voice" header, plus `· <source language name>` when known; (2) the transcribed text (original); (3) the target language name label and the translated text, when a translation exists; (4) the reading under the translation, when non-blank; (5) a status line for non-COMPLETED Messages: "Recording…", "Transcribing…", "Translating…", "Failed"; (6) Play/Pause when the Message has a Recording. This matches the mockups' "🎵 Voice / original / target name / translation / ▶ Play". (self) |
| D9 | Language names? | Catalog `name`, falling back to the raw code (same rule as Home's pair label, I5). Extracted to one shared helper used by both screens. (self) |
| D10 | Top bar? | The stored Conversation title, with the Back arrow from ticket 03. Unknown id → "Conversation not found". No Messages → "No messages yet". (self) |

## Playback

| # | Question | Resolved answer |
|---|----------|-----------------|
| D11 | Interface? | `AudioPlayer` (named in doc 14) in commonMain `audio/`: `state: StateFlow<PlaybackState>` (Idle / Playing(path) / Paused(path)), `play(audioPath): Boolean`, `pause()`, `stop()`, `release()`. One Recording plays at a time; Play on another Message stops the current one. Pressing Play on a paused Message resumes it. (self) |
| D12 | Where is `audioPath` resolved? | The stored `audioPath` is a reference relative to app-private storage, so paths survive iOS container moves. `FileStorage` (doc 14) resolves it: `resolve(reference): String?` returns null for absolute or escaping (`..`) references and missing files. `AndroidAudioPlayer(fileStorage)` uses `MediaPlayer`. The recorder (06) will write files under the same root. (self) |
| D13 | Playback failure? | If `play` returns false (missing or corrupt file), the bubble shows "Recording unavailable" instead of Play. An asynchronous error or completion returns to Idle. (self) |
| D14 | Lifecycle? | One player per Chat ViewModel (Koin `factory`), released in `onCleared`, so leaving the screen stops audio. Playback is not paused on background. (self) |
| D15 | iOS? | Interfaces in commonMain; Android implementations in `androidMain/audio` and `androidMain/storage`. iOS implementations are ticket 14. iOS must still compile. (self) |

## Verification

| # | Question | Resolved answer |
|---|----------|-----------------|
| D16 | Manual data and tests? | Debug seed enriched: translated Messages in both directions (with a reading on one), a TRANSLATING and a FAILED Message, and a debug-only generated tone `audio/seed-tone.wav` (written by the Android debug trigger) on one Message, so Play/Pause can be checked by hand before recording exists. Automated: mapper, ViewModel (fake player) and SQLite repository tests, plus `AndroidFileStorage` path-guard tests on a temp dir. No Compose UI tests (ticket 02 Q15). (self) |

Glossary: added **Message**, **Recording** and **Reading** (domain terms used by tickets 04–12). ADRs: none.
