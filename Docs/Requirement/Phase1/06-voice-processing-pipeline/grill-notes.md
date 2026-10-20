# Grill Notes — 06-voice-processing-pipeline

Source: `Docs/Requirement/Phase1/06-voice-processing-pipeline.md` (with `05`, `07`, `08`, `09`, `10`, `12`, `14`, `16`) · 2026-10-10.

**No interview was run** (user instruction 2026-10-10). Decisions are Claude's, marked **(self)**.

This ticket composes tickets 05 (Message steps), 07 (STT + detection), 08 (translation) and 09 (Reading). It also honours the hand-offs they recorded:
- one attempt per Message (S8) and a startup sweep (S9), from ticket 05
- a Recording duration cap and a visible-text check, from ticket 07.

## Recording
| # | Question | Resolved answer |
|---|----------|-----------------|
| D1 | Interaction? | Tap the microphone to start, tap again to stop (doc 06 "Tap microphone → Start … Stop recording"). One Recording at a time, app-wide. (self) |
| D2 | When is the Message row created? | At start, as RECORDING with its Recording reference, so the bubble shows "Recording…" live and the audio path is persisted first (doc 06 "every step persists its result as soon as it succeeds"). (self) |
| D3 | Limits? | Max 2 minutes: the recorder auto-stops and the Recording is processed. This keeps uploads small and transcripts under the 10,000-character limit. Under 0.5 s counts as an accidental tap: the Message row and file are deleted. (self) |
| D4 | Format? | AAC in MPEG-4 (`.m4a`), mono, 16 kHz, 64 kbps: small, and supported by Whisper and MediaPlayer. File at `files/audio/<uuid>.m4a`, stored as the relative reference `audio/<uuid>.m4a` (ticket 04 D12). (self) |
| D5 | Interfaces (doc 14)? | `AudioRecorder` (start into a path with a max duration and a callback, stop → duration or null, cancel) and `FileStorage` additions (new Recording reference, writable path, delete). Android implementations here; iOS in ticket 14. (self) |
| D6 | Microphone permission? | `MicrophonePermission` interface in `commonMain/permissions` (`isGranted`, `request(onResult)`), provided to the UI through a CompositionLocal. Android implementation in `androidMain/permissions` uses `rememberLauncherForActivityResult`; `MainActivity` provides it. Default is "unavailable" (iOS until ticket 14). Denied → inline "Microphone access is needed to record. You can allow it in Settings." Granted from the prompt → recording starts. (self) |
| D7 | Playback while recording? | Starting a Recording stops any playback, so the speaker isn't captured. (self) |
| D8 | Leaving the Chat screen while recording? | The Recording stops and is processed; speech is never thrown away. **Revised in review:** it also stops when the app leaves the foreground (Activity `onStop`, not on rotation), because Android silences the microphone in the background, and when the recorder reports an error. A long Recording that turns out unusable shows "The recording didn't work. Please try again." whoever stopped it; an accidental tap (under 0.5 s) leaves nothing and says nothing. Rotation does not stop it (the ViewModel survives). (self) |

## Processing
| # | Question | Resolved answer |
|---|----------|-----------------|
| D9 | Order? | Doc 06's pipeline, each step persisted by ticket 05's guarded writes: mark Transcribing → STT (07) → visible-text check → resolve source/target from the Conversation's pair (07) → save transcription → translate and save, with the Reading only when needed (08/09) → COMPLETED. (self) |
| D10 | Failures? | Any failure marks the Message FAILED and keeps everything saved so far: silence (empty transcript), undetermined language, STT or translation errors, a missing Conversation, or a step refused. The Retry UI is ticket 12. (self) |
| D11 | Where does processing run? | In an app-scoped coroutine scope (`MessageProcessor`), so it finishes even if the user leaves the screen. One job per Message: starting another cancels and joins the previous one first (ticket 05 S8). (self) |
| D12 | Interrupted processing (process death)? | At app start, before any UI or seed: Messages left TRANSCRIBING or TRANSLATING are marked FAILED (ticket 05 S9) so Retry (12) can apply. **Revised in review:** Messages left RECORDING are deleted with their file instead, because a Recording cut off by process death was never finalised and can't be played or transcribed. The sweep is best effort: a failure doesn't block the launch. (self) |
| D13 | Debug seed? | Its TRANSLATING sample would be swept to FAILED, so the seed instead shows a FAILED Message with a transcript (the "retry from transcript" shape for ticket 12). (self) |

## UI
| # | Question | Resolved answer |
|---|----------|-----------------|
| D14 | Mic control? | A bottom bar on the Chat screen: a round button with a mic icon and "Tap to speak". While recording it shows a stop icon and "Recording 0:07 · tap to stop", plus the elapsed time. Disabled while another Conversation is recording. Content descriptions "Start recording" and "Stop recording". (self) |
| D15 | Live feedback? | The bubble flows through the statuses as they persist (Recording… → Transcribing… → Translating… → content) thanks to the reactive queries from ticket 04. (self) |

## Verification
| # | Question | Resolved answer |
|---|----------|-----------------|
| D16 | Tests? | Pipeline use case with fakes (happy path and every failure); processor (S8 cancel-and-join); recording controller with a fake recorder (start, too short, stop, max duration, cancel, recorder failure, already recording); repository delete and sweep; file-storage additions; ChatViewModel mic state. The recorder, permission dialog and real end-to-end flow need a device and a key: manual. (self) |

Glossary: no new terms. ADRs: none.
