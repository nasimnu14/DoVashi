# Test Cases — 14-platform-specific-components

| ID | Title | Expected | Priority |
|---|---|---|---|
| TC-01 | iOS file storage contract [Auto] | 8 tests: resolve, escapes, symlink, read/size, refs, writable, delete, directory | High |
| TC-02 | Android storage contract (regression) [Auto] | unchanged | Medium |
| TC-03 | iOS build [Auto-ish] | `xcodebuild` succeeds | High |
| TC-04 | iOS launch (done on simulator) | Home screen "Conversations" shown | High |
| TC-05 | iOS create Conversation + record on a device (manual) | Mic prompt, record, stop, transcript + translation appear | High |
| TC-06 | iOS playback (manual) | Tap play on a Message: audio plays; completes back to Play | High |
| TC-07 | Mic denied (manual) | Message "Microphone access is needed…" | Medium |
| TC-08 | Background during recording (manual) | Recording stops and is processed | Medium |
| TC-09 | Android audio focus (manual) | Music pauses during playback, resumes after; a call pauses playback | Medium |
| TC-10 | iOS key (manual) | With `Secrets.xcconfig`, translation works; without it, fails fast with a missing key | High |
| TC-11 | Data survives relaunch / excluded from backup (manual) | Conversations persist; not in iCloud backup | Medium |
