# Progress — 04 Chat Screen

> /execute-plan · Plan: Docs/Requirement/Phase1/04-chat-screen/plan.md · Started: 2026-10-10 16:37
> Status: ✅ Complete · Last updated: 2026-10-10 18:00
> Baseline: `feature/04-chat-screen` at `feature/03-create-conversation` HEAD; 60 tests green at the end of ticket 03.

## Plan Steps
| # | Step | Status | Files | Verification |
|---|------|--------|-------|--------------|
| 0 | Preconditions | ✅ Done | — | Base has `ChatRoute`, `ChatStubScreen`, Message table, `ic_arrow_back`; ticket 03 left the suite green (60) |
| 1 | Schema queries + domain | ✅ Done | `Conversation.sq`, `Message.sq`, `domain/model/Message.kt`, `domain/repository/*`, `domain/usecase/Observe{Conversation,Messages}UseCase.kt` | tests ✅ |
| 2 | Data impl | ✅ Done | `data/repository/Sql{Conversation,Message}Repository.kt`, `data/repository/MessageStatusParsing.kt` | tests ✅ |
| 3 | Audio interfaces + Android impls | ✅ Done | `audio/{AudioPlayer,FileStorage}.kt`; androidMain `audio/AndroidAudioPlayer.kt`, `storage/AndroidFileStorage.kt`, `di/AndroidPlatformModule.kt` | `:shared:compileAndroidMain` ✅ |
| 4 | Presentation logic | ✅ Done | `presentation/LanguageLabel.kt`, `presentation/home/HomeUiStateMapper.kt`, `presentation/conversation/{ChatUiState,ChatUiStateMapper,ChatViewModel}.kt` | compile ✅ |
| 5 | Screen | ✅ Done | `presentation/conversation/ChatScreen.kt`, `composeResources/drawable/ic_{play,pause}.xml` | compile ✅ |
| 6 | Navigation, DI, debug seed | ✅ Done | `navigation/AppNavHost.kt`, `navigation/StubScreens.kt` (deleted), `di/SharedModule.kt`, `data/debug/DebugSeeder.kt`, androidApp `DoVashiApplication.kt` | `:androidApp:assembleDebug` ✅ |
| 7 | Tests | ✅ Done | commonTest `ChatUiStateMapperTest` (11), `ChatViewModelTest` (7), `TestSupport.kt`; androidHostTest `SqlRepositoriesTest` (+5, seeder updated), `storage/AndroidFileStorageTest` (6) | `:shared:testAndroidHostTest` → 89 tests, 0 failures; iOS compile ✅ |

Status legend: ⏳ Pending · 🔄 In progress · ✅ Done · ⚠️ Done with issues · ⛔ Blocked · ⏭️ Skipped (already exists)

## Assumptions
- Verification: `./gradlew :shared:testAndroidHostTest :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64`.
- User instruction (2026-10-10): proceed without waiting for permission; record decisions.

## Step Log
### Steps 1–6 — Implementation · ✅ Done
- Step 1/2: `selectById` and `selectByConversation` queries (newest first). The tolerant `parseStatus` moved to `data/repository/MessageStatusParsing.kt` (internal) and is shared by both repositories. `observeConversation` uses `mapToOneOrNull`.
- Step 3: `AndroidAudioPlayer` holds one `MediaPlayer`. `play` resumes a paused same-path Recording and otherwise stops and starts fresh (synchronous `prepare()` of a local file). Any exception → Idle and `false`. Completion and error callbacks → `stop()`. `AndroidFileStorage` canonicalises against the canonical root and rejects blank, absolute, escaping and non-file references.
- Step 4: shared `languageLabel` helper; Home's `pairLabel` uses it (same output). `ChatViewModel` combines conversation, Messages, player state and unavailable ids. It keeps the latest Message list for click lookups and releases the player in `onCleared`.
- Step 5: reversed `LazyColumn`, bubbles capped at 85% of the width via `BoxWithConstraints`, `secondaryContainer` (start) vs `primaryContainer` (end), decorative play/pause vectors with text labels.
- Step 6: `koinViewModel<ChatViewModel> { parametersOf(id) }`; Back via `dropUnlessResumed`. `StubScreens.kt` deleted. The seed has 4 conversations: recent (en→zh with translation, reading and the sample tone; zh→en with translation), in-progress (TRANSLATING, then FAILED with transcript), empty (reversed), failed (no text). `DoVashiApplication` writes `files/audio/seed-tone.wav` (1.5 s, 440 Hz, 16 kHz mono PCM) in debug only, then seeds.
- Deviation: none in approach. The mapper also hides the Reading when there is no translation (a Reading without a translation would be orphaned).

### Step 7 — Tests · ✅ Done
- 29 new tests: mapper 11, ViewModel 7 (`ViewModelStore.clear()` proves release), repository 5, file storage 6. The seeder test was updated to the new 4-conversation shape, including the sample audio reference and Reading.
- Verification: `./gradlew :shared:testAndroidHostTest` → 89 tests, 0 failures; `:androidApp:assembleDebug` and `:shared:compileKotlinIosSimulatorArm64` → BUILD SUCCESSFUL.
- Not automatable here: `AndroidAudioPlayer` (MediaPlayer needs a device), Compose rendering, the debug tone file.

## Review Cycles
Cycles 1 and 2 ran in parallel on the same diff, each a fresh read-only subagent with its own lens. Fixes were applied together, and cycle 3 reviews the fixed code.

### Cycle 1 — Plan conformance & correctness · 2026-10-10 17:20
| # | Severity | Finding | File:line | Action | Result |
|---|----------|---------|-----------|--------|--------|
| 1 | Medium | `reverseLayout` + `spacedBy(8.dp)` without alignment packs short lists at the **top** (breaks TC-02/D6/M1 for short conversations) | `ChatScreen.kt` | Fixed — `Arrangement.spacedBy(8.dp, Alignment.Bottom)` | ✅ builds; manual TC-02 |
| 2 | Medium | MediaPlayer leaked when `setDataSource`/`prepare`/`start` threw (never assigned, so never released) | `AndroidAudioPlayer.kt` | Fixed — local `mediaPlayer`, released in the catch | ✅ builds; manual check (corrupt file) |
| 3 | Low | Audio kept playing through the ~700 ms exit animation (`onCleared` is late) | `AppNavHost.kt`, VM | Fixed — `LifecycleEventEffect(ON_PAUSE)`: if the entry is no longer current (popped, not backgrounded) → `stopPlayback()`. Covers the arrow and system back; background playback (D14) is unchanged | ✅ new VM test `stopPlaybackStopsTheRecording`; manual TC-19 |
| 4 | Low | No test that the target label comes from `Message.targetLanguage` | mapper test | Fixed — `targetLabelComesFromTheMessageRecordNotThePair` | ✅ |
| 5 | Low | New Message at index 0 keeps the old anchor → appears off-screen | `ChatScreen.kt` | Fixed — hoisted `LazyListState`; when the newest id changes and the user is at index ≤ 1, `animateScrollToItem(0)` | ✅ builds; manual (ticket 06 makes it observable) |
| 6 | Low | The `id DESC` tie-break test can't isolate the clause (SQLite's reverse index scan gives the same order) | repo test | Clause kept; comment added in the test | noted |
| 7 | Low | Resume failure left the state `Paused`; `play` after `release` worked | `AndroidAudioPlayer.kt` | Fixed — resume failure → `stop()` (Idle); `released` guard | ✅ |
- Discarded: none.

### Cycle 2 — Edge cases & regressions · 2026-10-10 17:20
| # | Severity | Finding | File:line | Action | Result |
|---|----------|---------|-----------|--------|--------|
| 1 | Medium | MediaPlayer leak (same as Cycle 1 #2) | | Fixed | ✅ |
| 2 | Medium (latent) | "Recording unavailable" stuck by id forever; Play shown while RECORDING | VM, mapper | Fixed — failures keyed by `id → reference`, so a changed reference gets a fresh try; RECORDING and blank references show no playback | ✅ new tests `aChangedReferenceIsNoLongerUnavailable`, `recordingInProgressOrBlankReferenceHasNoPlayback`, `unavailableClearsWhenTheReferenceChanges` |
| 3 | Medium (latent) | New Message off-screen (same as Cycle 1 #5) | | Fixed | ✅ |
| 4 | Low | `canonicalFile` can throw IOException → crash on Play | `AndroidFileStorage.kt` | Fixed — returns null | ✅ |
| 5 | Low | `pause()`/resume `start()` can throw IllegalStateException before the error callback arrives | `AndroidAudioPlayer.kt` | Fixed — caught, then `stop()` | ✅ |
| 6 | Low | Tapping a missing B silenced A | `AndroidAudioPlayer.kt` | Fixed — resolve before stopping | ✅ |
| 7 | Low | Synchronous `prepare()` on main | | Not fixed — local app files only; revisit if Recordings get large | accepted |
| 8 | Low | `launchSingleTop` with another `ChatRoute` id would reuse the old ViewModel | `AppNavHost.kt` | Fixed — `koinViewModel(key = "chat-$id")` | ✅ |
| 9 | Low | Tone written in place (a kill leaves it truncated forever) | `DoVashiApplication.kt` | Fixed — written to `.part`, then renamed; regenerated unless the length matches | ✅ builds |
| 10 | Low | Blank `audioPath` showed Play | mapper | Fixed (with #2) | ✅ |
| 11 | Low | Bubble list rebuilt on main for every playback change | VM | Not fixed — moving it off main would race with `latestMessages`; ticket 11's incremental loading bounds the list | accepted |
| 12 | Nit | `plan.txt` duplicates `plan.md` | docs | Discarded — intentional (the user's hand-edit copy, ticket workflow Step 6) | — |
- Verification: `./gradlew :shared:testAndroidHostTest :androidApp:assembleDebug :androidApp:assembleRelease :shared:compileKotlinIosSimulatorArm64` → BUILD SUCCESSFUL; 94 tests, 0 failures.
- Note for TC-14: deleting the tone and relaunching regenerates it. Delete it while the app is running instead.

### Cycle 3 — Integration, security & quality · 2026-10-10 17:50
| # | Severity | Finding | File:line | Action | Result |
|---|----------|---------|-----------|--------|--------|
| 1 | Medium | The ON_PAUSE stop didn't cover **system back**: predictive back moves the entry to STARTED while it's still current, then pops with only ON_STOP | `AppNavHost.kt` | Fixed — the same "popped?" check runs on both ON_PAUSE (toolbar arrow) and ON_STOP (system back). Backgrounding still keeps playing | ✅ builds; manual TC-19 (both back paths) |
| 2 | Low | Auto-scroll effect ran on first composition → rotation could snap a restored position to the bottom | `ChatScreen.kt` | Fixed — last seen newest id kept in `rememberSaveable`; scroll only when it changes | ✅ builds; manual TC-21 |
| 3 | Low | `setDataSource(String)` parses the path as a URI (a `:` in the file name breaks it) | `AndroidAudioPlayer.kt` | Fixed — `FileInputStream(path).use { setDataSource(it.fd) }` | ✅ |
| 4 | Low | KDoc, fake and real player disagreed on the failure state; "A playing, tap missing B" untested | `AudioPlayer.kt`, `TestSupport.kt` | Fixed — KDoc states both cases; the fake keeps playing on a missing file; the real player goes Idle when an existing file won't play; new VM test `missingRecordingDoesNotInterruptThePlayingOne` | ✅ |
| 5 | Low | "Recording unavailable" replaced the focused button silently | `ChatScreen.kt` | Fixed — `liveRegion = Polite` (same pattern as the Create screen) | ✅; manual TalkBack |
| 6 | Low | No `AudioAttributes` / audio focus | `AndroidAudioPlayer.kt` | Partly fixed — `USAGE_MEDIA` / `CONTENT_TYPE_SPEECH` set. Audio focus left to ticket 14 (platform audio) | open, Low |
| 7 | Low (privacy) | Recordings in `filesDir` + `allowBackup="true"` → Auto Backup once ticket 06 records | manifest | Not fixed — the open ticket-02 backup decision (Medium). Will be decided with the local-storage ticket (10) before release; noted for 06 | open |
| 8 | Low (latent) | Chat→Chat single-top navigation would keep the old ViewModel/player in the same entry | `AppNavHost.kt` | Not fixed — unreachable (no Chat→Chat navigation exists) | open, Low |
| 9 | Low | Test gaps: player state machine (MediaPlayer, no seam), symlink escape, Koin graph | — | Partly fixed — symlink test added (`symlinkPointingOutsideTheRootIsNull`). Player behaviour and the Koin graph remain manual / need new test dependencies | open, Low |
- Discarded: none.
- Summary: 9 found · 6 fixed (1 partly) · 3 open (Low).
- Verification: `./gradlew :shared:testAndroidHostTest :androidApp:assembleDebug :androidApp:assembleRelease :shared:compileKotlinIosSimulatorArm64` → BUILD SUCCESSFUL; 96 tests, 0 failures.

**Stopping rule applied:** cycle 3 found no Critical/High issues → stopped after 3 cycles.

## Final Report

**Outcome:** ✅ Done with open issues (Low only, plus the carried-over backup decision). All 7 plan steps are implemented; 3 review cycles found no Critical/High issues. Not run on a device or emulator.

### Changes
- Step 1: `Conversation.sq` (`selectById`), `Message.sq` (`selectByConversation`); `domain/model/Message.kt` (created); `domain/repository/{Conversation,Message}Repository.kt` (modified); `domain/usecase/Observe{Conversation,Messages}UseCase.kt` (created)
- Step 2: `data/repository/Sql{Conversation,Message}Repository.kt` (modified); `data/repository/MessageStatusParsing.kt` (created; shared tolerant status parsing)
- Step 3: `audio/{AudioPlayer,FileStorage}.kt` (created); androidMain `audio/AndroidAudioPlayer.kt`, `storage/AndroidFileStorage.kt` (created); `di/AndroidPlatformModule.kt` (modified)
- Step 4: `presentation/LanguageLabel.kt` (created); `presentation/home/HomeUiStateMapper.kt` (modified: uses it); `presentation/conversation/{ChatUiState,ChatUiStateMapper,ChatViewModel}.kt` (created)
- Step 5: `presentation/conversation/ChatScreen.kt`, `composeResources/drawable/ic_{play,pause}.xml` (created)
- Step 6: `navigation/AppNavHost.kt` (modified), `navigation/StubScreens.kt` (deleted), `di/SharedModule.kt` (modified), `data/debug/DebugSeeder.kt` (modified), androidApp `DoVashiApplication.kt` (modified: debug tone)
- Step 7: commonTest `presentation/conversation/{ChatUiStateMapperTest,ChatViewModelTest}.kt` (created), `testing/TestSupport.kt` (modified); androidHostTest `data/repository/SqlRepositoriesTest.kt` (modified), `storage/AndroidFileStorageTest.kt` (created)
- Also: `GLOSSARY.md` (+ Message, Recording, Reading).

### Verification Run
| Command | Result |
|---------|--------|
| `./gradlew :shared:testAndroidHostTest` (final) | BUILD SUCCESSFUL — 96 tests, 0 failures (60 before this ticket) |
| `./gradlew :androidApp:assembleDebug :androidApp:assembleRelease` | BUILD SUCCESSFUL |
| `./gradlew :shared:compileKotlinIosSimulatorArm64` | BUILD SUCCESSFUL |
| Device/emulator run, MediaPlayer playback, Compose UI tests, iOS run | **Not run** (no device in this environment) |

### Deviations & Assumptions
- A fresh `MediaPlayer` per Recording instead of resetting one instance (simpler release semantics).
- Reading is hidden when there is no translation. A RECORDING status or blank reference shows no Play.
- Failed playback is remembered per `(Message id, reference)`, so a changed reference gets a fresh try.
- Playback stops as soon as the Chat entry is popped (arrow or system back), but carries on in the background (D14).
- Auto-scrolls to a new newest Message only if the user was already at the newest.
- `AudioAttributes` usage media / speech.
- The debug tone is written atomically (temp file + rename) and regenerated unless its length matches.

### Open Issues & Risks
- **[Medium, carried from ticket 02 — decide before ticket 06 ships Recordings]** Backup: `allowBackup="true"` and Recordings/DB in default app storage, so voice and text go to Google Drive backup. Planned for ticket 10 (local storage).
- No audio focus handling (ticket 14). Synchronous `prepare()` on main (fine for local files).
- Untested by automation: `AndroidAudioPlayer` (needs a device), Compose layout, the back/stop lifecycle hooks, the Koin graph for the parametrized ViewModel.
- iOS: `AudioPlayer`/`FileStorage` have no iOS implementations and Koin isn't started on iOS (ticket 14).
- The bubble list is rebuilt on every playback change; ticket 11 bounds the list size.

### ⚠️ Critical Manual Checks
| # | What to check | Steps | Expected result | Why critical |
|---|---------------|-------|-----------------|--------------|
| 1 | Playback on a real device (TC-06, TC-07, TC-14) | 1. Clear app data and launch debug. 2. Open the top conversation and tap Play on "How are you today?". 3. Pause mid-tone, then Play. 4. While the app runs, delete `files/audio/seed-tone.wav` via `adb shell run-as`, then tap Play. | Tone plays, pauses and resumes from the same spot; the button flips Play/Pause/Play. After deletion, "Recording unavailable" appears and the app doesn't crash. | The `MediaPlayer` path has no automated coverage; a crash here blocks the core feature. |
| 2 | Stop when leaving, by both back paths (TC-19) | 1. Tap Play, then the toolbar arrow. 2. Reopen, tap Play, then use system back (gesture and button). 3. Tap Play, then press Home and return. | Steps 1–2: sound stops immediately. Step 3: the tone finishes in the background (by design). | Lifecycle hooks are only verifiable on a device; audio leaking after leaving is a visible bug. |
| 3 | Short-list position and order (TC-02, TC-05) | Open the seeded top conversation (2 bubbles). | Bubbles sit at the **bottom** above the edge, oldest on top; the English-source bubble is on the left, the Mandarin-Chinese-source bubble on the right, in different colours. | The cycle-1 arrangement fix (`Alignment.Bottom`) is only visible on screen. |
| 4 | Bubble content and Chinese rendering (TC-03, TC-04, TC-08) | Read each seeded bubble; open the second conversation. | Exact line order per D8: "Voice · English", text, "Mandarin Chinese", "你今天好吗？", italic "Nǐ jīntiān hǎo ma?", Play; "Translating…" and red "Failed" lines show. | Core content of the ticket; fonts and tone marks only render on a device. |
| 5 | Rotation keeps playback and scroll (TC-21) | 1. Scroll up a little in a long conversation (or Play in the seed one). 2. Rotate. | Playback continues; the scroll position doesn't jump to the bottom. | The cycle-3 `rememberSaveable` fix is only checkable on a device. |
| 6 | TalkBack (TC-25) | With TalkBack on, swipe through a bubble and trigger Play on a missing file. | Lines read in order; the button says "Play"/"Pause"; "Recording unavailable" is announced. | Accessibility is unverified. |
| 7 | Release build has no seed or tone (TC-26) | Install release; check the app and `files/audio`. | No sample data, no tone file. | Debug-only code must never touch a user's install. |

## Pull Request
`feature/04-chat-screen` → `phase_1`, **stacked on `feature/03-create-conversation`**. Merge ticket 03's PR first. `gh` isn't installed, so open it from a compare page:
- While 03 is unmerged (diff shows only ticket 04): https://github.com/nasimnu14/DoVashi/compare/feature/03-create-conversation...feature/04-chat-screen?expand=1
- After 03 is merged: https://github.com/nasimnu14/DoVashi/compare/phase_1...feature/04-chat-screen?expand=1

**Title:** feat(chat): Chat screen with Message bubbles and Recording playback (ticket 04)

**Description:**
Replaces the Chat stub with the real Chat screen:
- the Conversation's title and its Messages as chronological bubbles, newest at the bottom
- original text, the source and target Language names read from the Message record, the translation and an optional Reading
- bubble side driven by the source Language
- status lines
- Play/Pause of the Recording through a new `AudioPlayer`/`FileStorage` (Android: MediaPlayer, app-private storage)

Adds the domain `Message`, reactive Message and Conversation queries, and a richer debug seed with a sample tone. Depends on ticket 03.

🤖 Generated with [Claude Code](https://claude.com/claude-code)
