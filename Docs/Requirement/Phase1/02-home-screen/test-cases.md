# Test Cases — 02-home-screen

> Source: Docs/Requirement/Phase1/02-home-screen/plan.md (with grill-notes.md and domain-model.md invariants I1–I8)
> Type: Feature · Generated: 2026-10-08

## Scope
Home screen of the DoVashi Android app: conversation list from the local DB (sorted `updatedAt DESC`), row content (title, language pair, last-message preview, relative time, message count), live re-sort when a message is added, empty/loading states, and navigation from **+** and from a row to "Hello World" stubs. Android debug and release builds only.

Cases tagged **[Auto]** cannot be driven from the Phase 1 UI (no way to add a message or a conversation in-app yet). They are verified by the automated tests in plan Step 11, or by a tester using a debug-only hook if one is added. All other cases are manual.

## Open Questions
- No code exists yet, so UI copy and seed contents below come from the plan only. Seed data is specified only as "2–3 conversations, with sample messages across statuses (COMPLETED with text, FAILED without text, one with zero messages)". Manual cases assume the seed contains one of each of those three conversation shapes; adjust the names in the steps to the real seed once implemented.
- Resolved during implementation: exactly one hour reads "1 hour ago" (singular); minutes stay "N min ago".
- Bucket overlap: a message 23 hours old that crossed midnight is "23 hours ago" under the plan's bucket order (<24 h wins), not "Yesterday". Confirm this is intended (TC-31).
- A `updatedAt` in the future (device clock moved backwards) has no defined label (TC-33).
- There is no in-app way to add a message in this ticket, so AC2 (live re-sort) is only verifiable via automated tests or a debug hook. Confirm which.

## Test Cases

### Positive / Happy Path

| ID | Title | Preconditions | Steps | Expected Result | Priority |
|---|---|---|---|---|---|
| TC-01 | Debug seed populates a fresh install (AC7) | Debug build; app data cleared (never launched) | 1. Install and launch the debug build | The Conversations list shows 2–3 sample conversations; the empty-state message is not shown | High |
| TC-02 | Screen chrome | Debug build with seed data | 1. Open the app | Top bar title reads "Conversations"; a **+** floating button is visible at the bottom right | High |
| TC-03 | Row shows all required fields (AC3) | Seed data loaded; a conversation with at least one COMPLETED message | 1. Find that conversation's row | The row shows a title (headline), a smaller language-pair label, a one-line last-message preview, a relative time, and a message count | High |
| TC-04 | Sorted most recently active first (AC1) | Seed data with 3 conversations of different `updatedAt` | 1. Read the list top to bottom | Rows are ordered newest activity first; the top row has the most recent time label and the bottom row the oldest | High |
| TC-05 | Pair label uses catalog names in stored order (I5) | Seed data loaded | 1. Read the language-pair label of each row | Each label is "<language 1 name> ↔ <language 2 name>" using full catalog names (e.g. "English ↔ Mandarin Chinese"), not abbreviations or codes | High |
| TC-06 | Preview shows the original transcription (I6) | A conversation whose last message is COMPLETED with transcribed text | 1. Read its row preview | The preview shows that message's transcribed (original-language) text, not the translation | High |
| TC-07 | Preview for a FAILED message with no text (I6) | A conversation whose last message is FAILED with no transcribed text | 1. Read its row preview | The preview reads "Failed" | Medium |
| TC-08 | Conversation with no messages | A seeded conversation with zero messages | 1. Read its row | Preview reads "No messages yet"; count shows 0 | High |
| TC-09 | **[Auto]** Count includes messages of every status | A conversation with 5 messages: one each of RECORDING, TRANSCRIBING, TRANSLATING, COMPLETED, FAILED | 1. Query the home summaries | `messageCount` is 5 | Medium |
| TC-10 | **+** opens the Create stub | Home screen shown | 1. Tap **+**<br>2. Press the system Back button | Step 1 shows a screen containing only "Hello World". Step 2 returns to Home with the list unchanged | High |
| TC-11 | Row tap opens Chat stub for that conversation (AC5) | Seed data with at least 2 conversations | 1. Tap the first row<br>2. Press Back<br>3. Tap the second row | Each tap opens a "Hello World" Chat screen carrying that row's `conversationId` (verify via test log or debug display); Back returns to Home | High |
| TC-12 | **[Auto]** Adding a message moves its conversation to the top (AC2) | Three conversations A (top), B, C (bottom) | 1. Insert a message into C | In a single emission the order becomes C, A, B with no manual refresh; C's preview and count reflect the new message | High |
| TC-13 | Works offline (AC1) | Seed data already loaded; device in airplane mode | 1. Force-stop the app<br>2. Launch it | The full list loads with all rows and no error or network message | High |
| TC-14 | Seed does not run twice | Debug build launched once with seed data | 1. Force-stop and relaunch the app | The list is identical, with no duplicate conversations | Medium |
| TC-15 | Empty state on a release build (AC4, AC7) | Release build; app data cleared | 1. Launch the release build | Screen shows "No conversations yet — tap + to start one" and no rows; the **+** button is still visible | High |
| TC-16 | **+** has an accessible label | Debug build; TalkBack enabled | 1. Move TalkBack focus to the **+** button | TalkBack announces "New conversation" | Medium |
| TC-17 | **[Auto]** Creating a conversation sets `updatedAt` equal to `createdAt` (I1) | Empty DB | 1. Create a conversation through the use case | The stored row has `updatedAt == createdAt` and a title built from the two catalog language names | Medium |
| TC-18 | **[Auto]** Relative-time buckets (Q9) | Fixed clock and time zone | 1. Format times 30 s, 5 min, 3 h, the previous calendar day, and 10 days old | "Just now", "5 min ago", "3 hours ago", "Yesterday", and a short date such as "Sep 28" | High |

### Negative

| ID | Title | Preconditions | Steps | Expected Result | Priority |
|---|---|---|---|---|---|
| TC-19 | **[Auto]** Identical languages rejected (I7) | Catalog with at least English | 1. Call create-conversation with English for both languages | The call fails with a validation error and no row is written | High |
| TC-20 | **[Auto]** Unknown language code falls back to the raw code (Q21) | A conversation stored with `language1Code = en`, `language2Code = xx` (not in the catalog) | 1. Build the Home row | The pair label reads "English ↔ xx"; no crash and the row is still shown | High |
| TC-21 | **[Auto]** Message for a missing conversation is rejected (FK on) | Empty DB with foreign keys enabled | 1. Insert a message with `conversationId = 999` | The insert fails and no message row exists | High |
| TC-22 | **[Auto]** A failed message insert does not bump `updatedAt` (I2) | Conversation A with a known `updatedAt` | 1. Make `insertMessage` fail partway (e.g. violate a NOT NULL column)<br>2. Read A's `updatedAt` | `updatedAt` is unchanged (the transaction rolled back) | High |
| TC-23 | No empty-state flash while loading | Debug build with seed data | 1. Force-stop the app<br>2. Launch it and watch the first frames | The screen goes from neutral/blank straight to the list; "No conversations yet" never appears | Medium |
| TC-24 | Release build never seeds (AC7) | Release build; app data cleared | 1. Launch the release build<br>2. Force-stop and relaunch | The list stays empty after both launches | High |

### Edge Cases

| ID | Title | Preconditions | Steps | Expected Result | Priority |
|---|---|---|---|---|---|
| TC-25 | Long title and long preview stay on one line | A conversation whose title is 120 characters and whose last message text is 300 characters (insert through a test hook or temporary seed edit) | 1. View its row | The title does not push the time or count off the row; the preview is a single line ending in an ellipsis; the row height matches the other rows | Medium |
| TC-26 | Non-Latin preview renders correctly | A conversation whose last message text is "你好吗？" | 1. View its row | The preview shows 你好吗？ with no missing-glyph boxes | Medium |
| TC-27 | **[Auto]** Equal `updatedAt` ties break by `id` DESC (I3) | Conversations 1 and 2 with identical `updatedAt` | 1. Read the summaries | Conversation 2 is listed before conversation 1, and the order is the same on repeated reads | Medium |
| TC-28 | **[Auto]** Last message tie-break (I4) | One conversation with messages 1 and 2 sharing the same `createdAt` | 1. Read the summary | The last message is message 2 (higher id) | Medium |
| TC-29 | **[Auto]** Preview rules for blank text and statuses (I6) | Last message variants: RECORDING with null text; TRANSCRIBING with null text; COMPLETED with "   " (whitespace only) | 1. Build the preview for each | "Recording…", "Transcribing…", and an empty string respectively | Medium |
| TC-30 | **[Auto]** Time boundaries (Q9) | Fixed clock | 1. Format 59 s, 60 s, 59 min, 60 min, 23 h 59 min old timestamps | "Just now", "1 min ago", "59 min ago", "1 hour ago", "23 hours ago" | Medium |
| TC-31 | **[Auto]** "Yesterday" versus hours-ago overlap | Clock at 01:00 today | 1. Format 23:30 yesterday (1.5 h old)<br>2. Format 22:00 two days ago (27 h old, previous-previous day)<br>3. Format 20:00 yesterday (5 h old) | "1 hour ago"; a short date; "5 hours ago" — "Yesterday" appears only for timestamps 24 h or older that fall on the previous calendar day | Low |
| TC-32 | **[Auto]** Day boundary uses device local time zone | Fixed instant; two zones | 1. Format the same instant under UTC and under a UTC+10 zone | "Yesterday" versus a different label is decided by each zone's calendar day, not UTC | Low |
| TC-33 | **[Auto]** `updatedAt` in the future | Fixed clock; conversation `updatedAt` one hour ahead | 1. Format the time label | The app does not crash; the label is "Just now" or another defined fallback (pending the Open Question) | Low |
| TC-34 | Time label stays until data changes (Q23) | Seed data loaded; a row showing "Just now" or "N min ago" | 1. Leave the app open on Home for 5 minutes without any data change | The label is not rewritten by a timer; it refreshes the next time the list re-emits | Low |
| TC-35 | Rotation and background keep the list | Seed data loaded | 1. Rotate the device<br>2. Press Home and return to the app | The same rows in the same order are shown; no empty-state flash | Medium |
| TC-36 | Process death restores from the DB | Seed data loaded; Developer options "Don't keep activities" off | 1. Run `adb shell am kill` for the app package while it is backgrounded<br>2. Reopen the app | The full list reloads from the local DB | Medium |
| TC-37 | Double tap opens a single stub | Home screen shown | 1. Double-tap **+** quickly<br>2. Press Back once | Only one Create stub opens; one Back returns to Home | Medium |
| TC-38 | Large list scrolls smoothly | 500 seeded conversations (temporary debug seed edit) | 1. Open Home<br>2. Fling from top to bottom and back | Scrolling stays smooth, no crash or out-of-memory error, and the order remains `updatedAt DESC` | Low |
| TC-39 | Language-neutral logic | Source code of main sources | 1. Search main sources for the literals "en", "zh", "English" and "Chinese" outside the seed's sample text | No matches in repository, use-case, ViewModel or UI logic (AC6) | Medium |
