# Grill Notes — 14-platform-specific-components

Source: `Docs/Requirement/Phase1/14-platform-specific-components.md` · 2026-10-11. **No interview was run** (user instruction); decisions are Claude's, marked **(self)**.

Repo state: `AudioRecorder`, `AudioPlayer`, `FileStorage` (+ `MicrophonePermission`) are common interfaces with Android implementations. `iosMain` held only a template `MainViewController`: no Koin, no database driver, no HTTP engine, no recorder/player/storage/permission, so the iOS app could not open a screen (open since ticket 02). Open from ticket 04: Android audio focus.

| # | Question | Resolved answer |
|---|----------|-----------------|
| D1 | Scope? | iOS implementations of every interface plus the wiring that lets the iOS app run (database driver, Ktor Darwin engine, Koin, startup sweep); Android audio focus; Info.plist/xcconfig. (self) |
| D2 | Recording on iOS? | `AVAudioRecorder`, AAC `.m4a`, mono 16 kHz 64 kbps (same as Android), duration cap via `recordForDuration`; the delegate reports the cap or an encoder error as "stopped automatically"; our own stop detaches the delegate first so it never double-fires. (self) |
| D3 | Playback? | `AVAudioPlayer` with the same state machine as Android (resolve before stopping, resume if paused on the same path). (self) |
| D4 | Audio focus? | Android: transient focus requested on play, abandoned on pause/stop; loss pauses. iOS: the audio session is activated for recording (`PlayAndRecord`, speaker) or playback and deactivated with notify-others afterwards. (self) |
| D5 | Microphone permission? | `AVAudioSession.recordPermission` / `requestRecordPermission`, answered on the main queue; provided to the UI through the existing `LocalMicrophonePermission`. Usage text in Info.plist. (self) |
| D6 | File storage? | Foundation APIs under `Library/Application Support/Bilangual`, excluded from backup (like Android, ticket 06). Same traversal rules as Android: blank, absolute, `..` and symlinks out of the root resolve to null. Platform implementations stay separate (the ticket asks for them) and are tested to the same contract. (self) |
| D7 | Database on iOS? | SQLDelight native driver in the same directory, foreign keys on. `Dispatchers.IO` is not public on Native, so `Dispatchers.Default`. (self) |
| D8 | OpenAI key on iOS? | `Secrets.xcconfig` (git-ignored; `.example` committed) → `Config.xcconfig` includes it → Info.plist `OpenAIApiKey` → read at startup; an unexpanded `$(…)` or non-ASCII is dropped. Ticket 15 trade-off applies (key in the app bundle). (self) |
| D9 | Lifecycle? | Koin starts once in `MainViewController`; interrupted Messages are swept at startup; entering the background stops a Recording and processes it (Android `onStop` equivalent). Phone-call interruptions are not handled beyond that. (self) |
| D10 | Link flags? | The static `Shared` framework needs `-lsqlite3` in the app link (`OTHER_LDFLAGS` in `Config.xcconfig`); found by building with `xcodebuild`, invisible to unit tests. (self) |
