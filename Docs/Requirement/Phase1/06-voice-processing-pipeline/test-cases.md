# Test Cases — 06-voice-processing-pipeline

> Source: Docs/Requirement/Phase1/06-voice-processing-pipeline/plan.md (grill-notes D1–D16, domain-model P1–P6)
> Type: Feature · Generated: 2026-10-10 (create-manual-test-cases format)

## Scope
The voice flow end to end on Android: microphone permission, recording, the persisted processing steps, failures, limits, the startup sweep and the mic UI. Live cases need `OPENAI_API_KEY` in `local.properties` and a device.

## Test Cases

### Positive / Happy Path
| ID | Title | Preconditions | Steps | Expected Result | Priority |
|---|---|---|---|---|---|
| TC-01 | First use asks for the microphone (AC4) | Fresh install; open a Conversation | 1. Tap the mic button | The system microphone permission dialog appears | High |
| TC-02 | Grant and record (AC1, AC4) | TC-01 dialog showing | 1. Allow<br>2. Say "Good morning"<br>3. Tap the button again | Recording starts right after allowing ("Recording 0:0x · tap to stop"); after stopping, a bubble appears and goes Recording… → Transcribing… → Translating… → final | High |
| TC-03 | English → Mandarin Chinese result (AC1, AC3) | English ↔ Mandarin Chinese Conversation; key configured | 1. Record "How are you?" | Bubble: "Voice · English", "How are you?", "Mandarin Chinese", a Chinese translation, pinyin, Play; on the left | High |
| TC-04 | Mandarin Chinese → English result (AC1, AC3) | Same | 1. Record "你好吗？" | Bubble: "Voice · Mandarin Chinese", the Chinese text, "English", an English translation, no Reading; on the right | High |
| TC-05 | Play the new Recording | TC-03 done | 1. Tap Play | Your recorded voice plays | High |
| TC-06 | Processing finishes after leaving (D11) | Key configured | 1. Record, stop<br>2. Immediately press Back | On Home the conversation's preview updates to the transcript within seconds; reopening shows the finished bubble | Medium |
| TC-07 | Pipeline success path [Auto] | Fakes | 1. Process a Recording | Message COMPLETED with transcript, codes, translation | High |

### Negative
| ID | Title | Preconditions | Steps | Expected Result | Priority |
|---|---|---|---|---|---|
| TC-08 | Permission denied (AC4, P6) | Fresh install | 1. Tap mic<br>2. Deny | "Microphone access is needed to record. You can allow it in Settings." shows; nothing is recorded | High |
| TC-09 | No API key (AC2) | `OPENAI_API_KEY` empty | 1. Record and stop | Bubble ends "Failed" and keeps the Recording (Play works) | High |
| TC-10 | Airplane mode (AC2) | Key configured; airplane mode on | 1. Record and stop | Bubble ends "Failed"; Play works | High |
| TC-11 | Silence (AC2) | Key configured | 1. Record 3 s of silence | Bubble ends "Failed"; no invented text | Medium |
| TC-12 | Every failure keeps saved parts [Auto] (P1) | Fakes: STT error / translation error / undetermined / missing Conversation | 1. Process | FAILED; the Recording is kept, and the transcript too when it was saved | High |

### Edge Cases
| ID | Title | Preconditions | Steps | Expected Result | Priority |
|---|---|---|---|---|---|
| TC-13 | Accidental tap (AC5) | Permission granted | 1. Tap mic and immediately tap again | No bubble remains; no file left in `files/audio` | Medium |
| TC-14 | 2-minute cap (AC5) | Permission granted | 1. Start recording and wait 2 minutes | Recording stops by itself at 2:00 and is processed | Medium |
| TC-15 | Only one Recording [Auto] (AC5) | Fake recorder | 1. Start twice | Second start reports already recording | Medium |
| TC-16 | Playback stops when recording starts (D7) | A bubble with audio | 1. Tap Play<br>2. Tap the mic | Playback stops; recording starts | Low |
| TC-17 | Leaving while recording (D8) | Recording in progress | 1. Press Back | Recording stops and is processed (bubble appears in that conversation) | Medium |
| TC-18 | Rotation while recording | Recording | 1. Rotate | Recording continues; the timer keeps counting | Medium |
| TC-19 | Interrupted processing swept (AC6) [Auto + manual] | A Message left TRANSCRIBING, and one left RECORDING (automated); kill the app mid-processing or mid-recording (manual) | 1. Restart | The TRANSCRIBING one shows "Failed"; the cut-off RECORDING one is gone, with its file | High |
| TC-20 | One job per Message [Auto] (P3) | Processor with a gated job | 1. Process the same Message twice | The first job is cancelled before the second starts | High |
| TC-21 | Too-short and failed recordings leave nothing [Auto] | Fake recorder: 300 ms / null / start fails | 1. Stop or start | Row and file deleted | Medium |
| TC-22 | Mic disabled while another conversation records [Auto] | Recording in conversation A | 1. Open conversation B | Mic shows unavailable | Low |
| TC-23 | App sent to the background while recording (D8) | Recording in progress, about 5 s in | 1. Press the device Home button<br>2. Return to the app | The Recording stopped when the app left the screen; its bubble is processed normally; rotation alone never stops it | High |
| TC-24 | Lost Recording is explained (D8) [Auto + manual] | Automated: recorder returns nothing after 30 s / manual: another app grabs the microphone mid-recording | 1. Stop (or let it stop) | "The recording didn't work. Please try again." appears above the mic; no bubble remains | Medium |
| TC-25 | Double tap on first use (permission) | Fresh install | 1. Double-tap the mic quickly | One permission dialog; allowing it starts recording; no denial message appears | Medium |
| TC-26 | Voice data excluded from backup (user decision 2026-10-11) | A recorded Message; local backup transport enabled | 1. `adb shell bmgr backupnow com.example.dovashiapp`<br>2. Clear app data, then `adb shell bmgr restore com.example.dovashiapp`<br>3. Open the app | No conversations or Recordings come back | High |
