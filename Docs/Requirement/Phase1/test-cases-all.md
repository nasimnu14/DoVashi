# Phase 1 — Test Cases

Consolidated from each ticket's `test-cases.md` (source of truth stays in the ticket folders). English ↔ Mandarin Chinese MVP. `[Auto]` = covered by an automated test; the rest are manual or on-device checks that have not been run on a device.

## Summary

| Ticket | Cases | Tagged [Auto] |
|---|---|---|
| 01-language-model | 10 | 0 |
| 02-home-screen | 39 | 15 |
| 03-create-conversation | 25 | 6 |
| 04-chat-screen | 26 | 7 |
| 05-message-structure | 19 | 17 |
| 06-voice-processing-pipeline | 26 | 8 |
| 07-language-detection | 26 | 24 |
| 08-openai-translation | 20 | 19 |
| 09-pronunciation-romanization | 12 | 10 |
| 10-local-database | 6 | 4 |
| 11-infinite-chat-scrolling | 11 | 7 |
| 12-retry | 11 | 8 |
| 13-technology-stack | 5 | 4 |
| 14-platform-specific-components | 11 | 3 |
| 15-openai-api-security | 8 | 5 |
| 16-architecture-and-extensibility | 7 | 6 |
| **Total** | **262** | **143** |

---

## 01-language-model

> Source: Docs/Requirement/Phase1/01-language-model/plan.md
> Type: Feature (domain model) · Generated: 2026-10-08

### Scope

This covers the `Language` data model and the seeded, in-memory
`LanguageCatalog` added under `shared/src/commonMain/kotlin/com/example/dovashiapp/domain/model/`.
There is no UI, database table, or wiring into other features in this ticket
(confirmed in `plan.md`'s "Explicitly out of scope"), so there is no screen to
click through. Verification here is done by running the module's test suite
and, where noted, inspecting values directly (IDE debugger / "Evaluate
Expression", or a scratch `main()`), since that is the only way a human can
observe this layer before any consumer wires it up.

### Open Questions

- None — plan, grill-notes, and domain-model for this ticket left no
  ambiguity to ground against the codebase (it's a new, isolated package with
  no existing conflicting code).

### Test Cases

#### Positive / Happy Path

| ID | Title | Preconditions | Steps | Expected Result | Priority |
|---|---|---|---|---|---|
| TC-01 | Catalog seeds exactly two languages (Acceptance: catalog contains only English and Mandarin Chinese) | `shared` module builds successfully | 1. Run `./gradlew :shared:testAndroidHostTest` (or open `LanguageCatalogTest` in the IDE and run it)<br>2. Observe the test report | All tests in `LanguageCatalogTest` pass, including the assertion that `LanguageCatalog.all.size == 2` | High |
| TC-02 | English entry has correct fields | `shared` module builds successfully | 1. In the IDE, open `LanguageCatalog.kt` or set a breakpoint/debugger "Evaluate Expression" on `LanguageCatalog.all`<br>2. Inspect the entry where `code == "en"` | `code = "en"`, `name = "English"`, `nativeName = "English"` | High |
| TC-03 | Mandarin Chinese entry has correct fields | `shared` module builds successfully | 1. Using the IDE debugger/Evaluate Expression on `LanguageCatalog.all`<br>2. Inspect the entry where `code == "zh"` | `code = "zh"`, `name = "Mandarin Chinese"`, `nativeName = "普通话"` | High |
| TC-04 | `byCode` resolves the English entry | `shared` module builds successfully | 1. Using Evaluate Expression, call `LanguageCatalog.byCode("en")` | Returns a non-null `Language` equal to `Language("en", "English", "English")` | High |
| TC-05 | `byCode` resolves the Mandarin Chinese entry | `shared` module builds successfully | 1. Using Evaluate Expression, call `LanguageCatalog.byCode("zh")` | Returns a non-null `Language` equal to `Language("zh", "Mandarin Chinese", "普通话")` | High |
| TC-06 | New language can be added without touching other files (Acceptance: catalog extensible) | `shared` module builds successfully | 1. Temporarily add a third `Language` entry (e.g. `code = "fr"`) to `LanguageCatalog.all` only<br>2. Re-run `LanguageCatalogTest`<br>3. Revert the temporary addition | The build and all existing tests still pass with no changes needed to `Language.kt`, test files, or any other package | Medium |

#### Negative

| ID | Title | Preconditions | Steps | Expected Result | Priority |
|---|---|---|---|---|---|
| TC-07 | `byCode` returns null for an unseeded code | `shared` module builds successfully | 1. Using Evaluate Expression, call `LanguageCatalog.byCode("fr")` (or any code not in the seed list) | Returns `null` — no exception thrown | High |
| TC-08 | `byCode` returns null for an empty string | `shared` module builds successfully | 1. Using Evaluate Expression, call `LanguageCatalog.byCode("")` | Returns `null` — no exception thrown | Low |

#### Edge Cases

| ID | Title | Preconditions | Steps | Expected Result | Priority |
|---|---|---|---|---|---|
| TC-09 | `byCode` is case-sensitive | `shared` module builds successfully | 1. Using Evaluate Expression, call `LanguageCatalog.byCode("EN")` (uppercase) | Returns `null` — the seeded code is lowercase `"en"` and no case-folding is implemented (per plan: no validation/normalization of `code`) | Low |
| TC-10 | `all` is stable across repeated access | `shared` module builds successfully | 1. Using Evaluate Expression, call `LanguageCatalog.all` twice in the same session | Both calls return the same 2 entries in the same order (`en`, `zh`) | Low |

### Critical Manual Checks

- **TC-06** cannot be fully automated in a unit test without a temporary code
  change — a human must add and then revert the extra `Language` row to
  confirm the "no pipeline rewrite" acceptance criterion holds in practice,
  not just in test assertions.
- **TC-02 / TC-03 / TC-04 / TC-05 / TC-07 / TC-08 / TC-09 / TC-10** are all
  exercised by the automated `LanguageCatalogTest` suite described in
  `plan.md` step 4, so they are automatable — flagged here only because this
  ticket ships no UI for a tester to click through; running the test suite
  (TC-01) is the practical substitute for manual verification until a
  downstream ticket (Create Conversation, Language Detection, etc.) wires
  `LanguageCatalog` into something visible.

---

## 02-home-screen

> Source: Docs/Requirement/Phase1/02-home-screen/plan.md (with grill-notes.md and domain-model.md invariants I1–I8)
> Type: Feature · Generated: 2026-10-08

### Scope
Home screen of the DoVashi Android app: conversation list from the local DB (sorted `updatedAt DESC`), row content (title, language pair, last-message preview, relative time, message count), live re-sort when a message is added, empty/loading states, and navigation from **+** and from a row to "Hello World" stubs. Android debug and release builds only.

Cases tagged **[Auto]** cannot be driven from the Phase 1 UI (no way to add a message or a conversation in-app yet). They are verified by the automated tests in plan Step 11, or by a tester using a debug-only hook if one is added. All other cases are manual.

### Open Questions
- No code exists yet, so UI copy and seed contents below come from the plan only. Seed data is specified only as "2–3 conversations, with sample messages across statuses (COMPLETED with text, FAILED without text, one with zero messages)". Manual cases assume the seed contains one of each of those three conversation shapes; adjust the names in the steps to the real seed once implemented.
- Resolved during implementation: exactly one hour reads "1 hour ago" (singular); minutes stay "N min ago".
- Bucket overlap: a message 23 hours old that crossed midnight is "23 hours ago" under the plan's bucket order (<24 h wins), not "Yesterday". Confirm this is intended (TC-31).
- A `updatedAt` in the future (device clock moved backwards) has no defined label (TC-33).
- There is no in-app way to add a message in this ticket, so AC2 (live re-sort) is only verifiable via automated tests or a debug hook. Confirm which.

### Test Cases

#### Positive / Happy Path

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

#### Negative

| ID | Title | Preconditions | Steps | Expected Result | Priority |
|---|---|---|---|---|---|
| TC-19 | **[Auto]** Identical languages rejected (I7) | Catalog with at least English | 1. Call create-conversation with English for both languages | The call fails with a validation error and no row is written | High |
| TC-20 | **[Auto]** Unknown language code falls back to the raw code (Q21) | A conversation stored with `language1Code = en`, `language2Code = xx` (not in the catalog) | 1. Build the Home row | The pair label reads "English ↔ xx"; no crash and the row is still shown | High |
| TC-21 | **[Auto]** Message for a missing conversation is rejected (FK on) | Empty DB with foreign keys enabled | 1. Insert a message with `conversationId = 999` | The insert fails and no message row exists | High |
| TC-22 | **[Auto]** A failed message insert does not bump `updatedAt` (I2) | Conversation A with a known `updatedAt` | 1. Make `insertMessage` fail partway (e.g. violate a NOT NULL column)<br>2. Read A's `updatedAt` | `updatedAt` is unchanged (the transaction rolled back) | High |
| TC-23 | No empty-state flash while loading | Debug build with seed data | 1. Force-stop the app<br>2. Launch it and watch the first frames | The screen goes from neutral/blank straight to the list; "No conversations yet" never appears | Medium |
| TC-24 | Release build never seeds (AC7) | Release build; app data cleared | 1. Launch the release build<br>2. Force-stop and relaunch | The list stays empty after both launches | High |

#### Edge Cases

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

---

## 03-create-conversation

> Source: Docs/Requirement/Phase1/03-create-conversation/plan.md (with grill-notes.md D1–D18 and domain-model.md invariants C1–C6)
> Type: Feature · Generated: 2026-10-10

### Scope
The Create Conversation screen opened from **+** on Home: two Language dropdowns read from the Language Catalog (English and Mandarin Chinese in Phase 1), the same-Language rule, Start Conversation persisting a Conversation in the selected order and opening the Chat stub, back-stack behaviour, double-tap and failure handling. Android debug build; the Chat screen itself is still the ticket-02 stub.

Cases tagged **[Auto]** can't be forced from the Phase 1 UI (a catalog that isn't exactly two entries, a database failure, a precise concurrent tap). The automated tests in plan Step 5 verify them. All other cases are manual.

### Open Questions
- The Chat destination is still the ticket-02 stub ("Hello World" + "Conversation #id"), so "opens the Chat screen" is checked by the id shown on the stub.
- Process death resets the form to defaults (D16). This is accepted, not a defect.
- Added after review: while a create is in flight, the Back icon and dropdowns are disabled. If the insert has already committed when Back is pressed, the conversation stays on Home and no Chat screen opens (TC-24).

### Test Cases

#### Positive / Happy Path

| ID | Title | Preconditions | Steps | Expected Result | Priority |
|---|---|---|---|---|---|
| TC-01 | **+** opens Create Conversation (AC1) | Debug build installed; Home visible | 1. Tap **+** | A screen titled "Create Conversation" shows a "Language 1" field, a "Language 2" field and a "Start Conversation" button | High |
| TC-02 | Default selection (AC1, D6) | On Create Conversation, freshly opened | 1. Read both fields | Language 1 shows "English"; Language 2 shows "Mandarin Chinese"; Start Conversation is enabled; no error text is shown | High |
| TC-03 | Options come from the catalog (AC2, C1) | On Create Conversation | 1. Tap the Language 1 field<br>2. Read the menu<br>3. Close it, then repeat for Language 2 | Each menu lists exactly "English" and "Mandarin Chinese", in that order | High |
| TC-04 | Create English ↔ Mandarin Chinese (AC4) | Defaults shown (English / Mandarin Chinese); note the number of rows on Home | 1. Tap Start Conversation | The Chat stub opens showing "Conversation #<n>" with a new id | High |
| TC-05 | New conversation appears on Home with correct title (AC4) | TC-04 just completed | 1. Press Back from the Chat stub | Home is shown (not the form); a new row at the top reads "English ↔ Mandarin Chinese" with pair label "English ↔ Mandarin Chinese", preview "No messages yet", time "Just now", "0 messages" | High |
| TC-06 | Create in reverse order (AC4, D9) | On Create Conversation with defaults | 1. Set Language 1 to "Mandarin Chinese" (error appears)<br>2. Set Language 2 to "English"<br>3. Tap Start Conversation<br>4. Press Back | The error disappears after step 2; the Chat stub opens; on Home the new top row's title is "Mandarin Chinese ↔ English" | High |
| TC-07 | Stored order matches selection [Auto] (AC4) | Automated: real SQLite repositories | 1. Create with (English, Mandarin Chinese)<br>2. Create with (Mandarin Chinese, English) | Rows store `language1Code`/`language2Code` as en/zh and zh/en respectively; titles follow the same order; `createdAt` equals `updatedAt` | High |
| TC-08 | Back on the form creates nothing (AC5) | On Create Conversation; note the Home row count | 1. Tap the Back arrow in the top bar | Home is shown; the row count is unchanged | High |
| TC-09 | System Back on the form (AC5) | On Create Conversation | 1. Use the system Back gesture/button | Home is shown; no conversation is created | Medium |

#### Negative

| ID | Title | Preconditions | Steps | Expected Result | Priority |
|---|---|---|---|---|---|
| TC-10 | Same Language in both fields is rejected (AC3, C2) | On Create Conversation with defaults | 1. Set Language 2 to "English" | The text "Choose two different languages" appears; Start Conversation is disabled | High |
| TC-11 | Disabled Start does nothing (AC3, C4) | TC-10 state (English / English) | 1. Tap Start Conversation several times<br>2. Tap Back | Nothing happens on the taps; Home's row count is unchanged | High |
| TC-12 | Both set to Mandarin Chinese is rejected (AC3) | On Create Conversation with defaults | 1. Set Language 1 to "Mandarin Chinese" | The error text appears and Start Conversation is disabled | Medium |
| TC-13 | Use case rejects identical codes [Auto] (C4) | Automated | 1. Call create with the same Language twice | It is rejected and no row is written | Medium |
| TC-14 | Database failure shows an error and allows retry [Auto] (AC6, D13) | Automated: repository fake throws on the first call | 1. Tap Start<br>2. Tap Start again | After 1: "Couldn't create the conversation. Please try again." is shown and the button is enabled again; after 2: one conversation is created and navigation is requested | Medium |

#### Edge Cases

| ID | Title | Preconditions | Steps | Expected Result | Priority |
|---|---|---|---|---|---|
| TC-15 | Double tap creates one conversation (AC6, C3) | On Create Conversation with defaults; note the Home row count | 1. Double-tap Start Conversation quickly<br>2. Press Back once | One Chat stub opens; one Back returns to Home; the row count went up by exactly 1 | High |
| TC-16 | Concurrent starts insert once [Auto] (C3) | Automated: create call suspended | 1. Call start twice before the first finishes | Exactly one conversation is created | High |
| TC-17 | Selection survives rotation (D16) | On Create Conversation | 1. Set Language 1 to "Mandarin Chinese" and Language 2 to "English"<br>2. Rotate the device | After rotation the fields still show Mandarin Chinese / English and no error | Medium |
| TC-18 | Error state survives rotation | TC-10 state (English / English) | 1. Rotate the device | The error text is still shown and Start Conversation is still disabled | Low |
| TC-19 | Re-selecting the same value | On Create Conversation with defaults | 1. Open Language 1 and pick "English" again | Nothing changes; no error; Start stays enabled | Low |
| TC-20 | Catalog with a single Language [Auto] (D6) | Automated: catalog of one Language | 1. Open the screen | Language 2 is empty and Start is disabled | Low |
| TC-21 | Catalog where the first two entries share a code [Auto] (D6) | Automated: catalog [A, A', B] | 1. Open the screen | Language 1 = A, Language 2 = B | Low |
| TC-22 | Back arrow accessibility (D17) | TalkBack on; on Create Conversation | 1. Focus the top-bar arrow<br>2. Focus each dropdown | The arrow is announced as "Back"; each field is announced with its label "Language 1" / "Language 2" and its current value | Medium |
| TC-23 | Back from Chat after creating twice (C5) | Home visible | 1. Create a conversation (TC-04), press Back<br>2. Create another, press Back | Each time Back goes straight to Home; the form never reappears; Home shows both new rows, newest on top | Medium |
| TC-24 | Back right after Start (accepted behaviour) | On Create Conversation with defaults; note the Home row count | 1. Tap Start Conversation and immediately use system Back | Either the Chat stub opens and one Back returns to Home, or Home is shown directly; in both cases the row count went up by exactly 1 and the Chat stub never opens on top of Home after Back | Medium |
| TC-25 | Process death during create (accepted, D16) | Developer options "Don't keep activities" or `adb shell am kill` | 1. Tap Start and kill the process immediately<br>2. Reopen the app | At most one new conversation exists; no crash | Low |

---

## 04-chat-screen

> Source: Docs/Requirement/Phase1/04-chat-screen/plan.md (with grill-notes.md D1–D16 and domain-model.md invariants M1–M8)
> Type: Feature · Generated: 2026-10-10

### Scope
The Chat screen opened from a Home row or after Create Conversation:
- title, Message bubbles in chronological order (newest at the bottom)
- per-bubble Language labels, translation and Reading
- bubble side by source Language
- status lines
- Play/Pause of a Recording
- not-found and empty states

Android debug build with the ticket-04 debug seed. Recording new Messages is ticket 06, so content comes from the seed.

Cases tagged **[Auto]** are verified by the plan Step 7 tests. All other cases are manual.

### Open Questions
- The seed only runs on an empty database. Testers with an older debug install must clear app data first so the new seed (translations, sample tone) is created.
- Sample seed content (expected on a fresh debug install):
  - "English ↔ Mandarin Chinese" (most recent):
    - (1) English "How are you today?" → "你今天好吗？", reading "Nǐ jīntiān hǎo ma?", with the sample tone
    - (2) Mandarin Chinese "火车站在哪里？" → "Where is the train station?", no reading
  - A second "English ↔ Mandarin Chinese" conversation: two FAILED Messages with transcripts, "Can you help me?" and "我需要一杯水" (TRANSLATING until ticket 06)
  - The empty "Mandarin Chinese ↔ English" conversation
  - The older conversation with one FAILED Message that has no text

### Test Cases

#### Positive / Happy Path

| ID | Title | Preconditions | Steps | Expected Result | Priority |
|---|---|---|---|---|---|
| TC-01 | Open a conversation from Home (AC1) | Fresh debug install (app data cleared) | 1. Launch the app<br>2. Tap the top row ("English ↔ Mandarin Chinese", preview "火车站在哪里？") | The Chat screen opens with title "English ↔ Mandarin Chinese" and a Back arrow; two bubbles are shown | High |
| TC-02 | Chronological order, opens at newest (AC1, M1) | TC-01 open | 1. Look at the list without scrolling | "How are you today?" is above "火车站在哪里？"; the newest bubble ("火车站在哪里？") is at the bottom, right above the screen edge | High |
| TC-03 | English → Chinese bubble content (AC2) | TC-01 open | 1. Read the "How are you today?" bubble top to bottom | Lines in order: "Voice · English", "How are you today?", "Mandarin Chinese", "你今天好吗？", "Nǐ jīntiān hǎo ma?", then a Play button | High |
| TC-04 | Chinese → English bubble content (AC2, M5) | TC-01 open | 1. Read the "火车站在哪里？" bubble | Lines: "Voice · Mandarin Chinese", "火车站在哪里？", "English", "Where is the train station?"; no reading line; no Play button (this Message has no Recording) | High |
| TC-05 | Bubble sides follow the source Language (AC3, M3) | TC-01 open | 1. Compare the horizontal position and colour of both bubbles | The English-source bubble sits on the left and the Mandarin-Chinese-source bubble on the right, in different colours | High |
| TC-06 | Play a Recording (AC5) | TC-01 open; media volume up | 1. Tap Play on "How are you today?" | A tone of about 1.5 seconds plays; while it plays the button reads "Pause"; afterwards it returns to "Play" | High |
| TC-07 | Pause and resume (AC5) | TC-01 open | 1. Tap Play<br>2. Within 1 second tap Pause<br>3. Tap Play again | After 2 the sound stops and the button reads "Play"; after 3 playback continues from where it paused (the remaining part of the tone) and ends normally | High |
| TC-08 | Status lines for in-progress and failed Messages (AC4) | Fresh seed | 1. Press Back to Home<br>2. Open the second "English ↔ Mandarin Chinese" conversation | "Can you help me?" and "我需要一杯水" both show "Failed" in error colour (since ticket 06 the seed no longer leaves a Message mid-pipeline); neither shows a translation block | High |
| TC-09 | Empty conversation (AC6) | Fresh seed | 1. Open the "Mandarin Chinese ↔ English" conversation (preview "No messages yet") | The title is "Mandarin Chinese ↔ English" and the body shows "No messages yet" | Medium |
| TC-10 | New conversation opens an empty Chat (AC6) | Home visible | 1. Tap **+**, then Start Conversation | The Chat screen shows the new title and "No messages yet" | Medium |
| TC-11 | Back returns to Home (AC1) | TC-01 open | 1. Tap the Back arrow | Home is shown; the list is unchanged | Medium |
| TC-12 | Newest-first query, scoped to the conversation [Auto] (M1, M2) | Automated: real SQLite | 1. Insert Messages in two conversations with mixed timestamps<br>2. Observe one conversation | Only that conversation's Messages arrive, newest first with an id tie-break | High |
| TC-13 | Live update when a Message is added [Auto] (AC7) | Automated: real SQLite | 1. Observe Messages<br>2. Insert a Message | A new emission contains it, without re-subscribing | High |

#### Negative

| ID | Title | Preconditions | Steps | Expected Result | Priority |
|---|---|---|---|---|---|
| TC-14 | Missing Recording file (AC5, D13) | TC-01 open; while the app stays running, delete the sample tone (`adb shell run-as com.example.dovashiapp rm files/audio/seed-tone.wav`). A relaunch regenerates it | 1. Tap Play on "How are you today?" | The button is replaced by "Recording unavailable"; no crash | Medium |
| TC-15 | Unknown conversation id → not found [Auto] (AC6) | Automated | 1. Open the ViewModel for an id that doesn't exist | State is NotFound ("Conversation not found") | Medium |
| TC-16 | Reference outside app storage is rejected [Auto] (M7) | Automated: temp dir | 1. Resolve "../x", "/etc/hosts", "" and a missing file | Each returns no path | High |
| TC-17 | Unknown Language Code shows the raw code [Auto] (M4) | Automated: mapper | 1. Map a Message whose source code isn't in the catalog | Source label shows the raw code | Low |
| TC-18 | Unknown stored status reads as Failed [Auto] | Automated: SQLite row with status "BOGUS" | 1. Observe Messages | The Message has status FAILED and shows "Failed" | Low |

#### Edge Cases

| ID | Title | Preconditions | Steps | Expected Result | Priority |
|---|---|---|---|---|---|
| TC-19 | Leaving the screen stops audio (AC5, M8) | TC-01 open | 1. Tap Play<br>2. Immediately tap Back | Sound stops at once; Home is shown | High |
| TC-20 | Only one Recording at a time [Auto] (M6) | Automated: fake player | 1. Play Message A<br>2. Play Message B | B's path is played; A's bubble shows Play, B's shows Pause | Medium |
| TC-21 | Rotation keeps playback and position | TC-01 open | 1. Tap Play<br>2. Rotate the device | The tone keeps playing and the button still reads "Pause" (or "Play" if it ended); list position unchanged | Medium |
| TC-22 | Long text wraps | Temporarily insert a Message with 400 characters of text (debug hook or DB edit) | 1. Open its conversation | The bubble wraps to multiple lines within ~85% of the width; nothing is cut off | Low |
| TC-23 | Long title ellipsizes | A conversation whose title is longer than the top bar | 1. Open it | Title is one line with an ellipsis; the Back arrow stays visible | Low |
| TC-24 | Background while playing | TC-01 open | 1. Tap Play<br>2. Press Home on the device<br>3. Return to the app | The tone finishes (it isn't paused on background); the button reads "Play" afterwards | Low |
| TC-25 | TalkBack on a bubble and Play | TalkBack on; TC-01 open | 1. Swipe through the first bubble | Each text line is read in order; the button is announced as "Play" (then "Pause" while playing) | Medium |
| TC-26 | Seed tone written only in debug | Release build installed | 1. Open the app | No sample conversations and no `files/audio/seed-tone.wav` | Medium |

---

## 05-message-structure

> Source: Docs/Requirement/Phase1/05-message-structure/plan.md (with grill-notes.md D1–D12 and domain-model.md invariants S1–S7)
> Type: Feature · Generated: 2026-10-10

### Scope
This suite covers four structural rules of a Message:
- whether a Reading is needed (from target-Language metadata)
- the Message Status transition order
- step-by-step persistence that never loses earlier parts
- the Conversation `updatedAt` bump on every step

This ticket has no new UI and no in-app way to drive the steps (the pipeline is ticket 06). Most cases are therefore **[Auto]**; the manual cases are regression smoke checks of Home and Chat.

### Open Questions
- Phase 1 never shows a Message with a missing Reading for a Mandarin Chinese target. Per D5 the Message still completes, and the bubble simply has no Reading line. Confirm when the translation ticket (08/09) lands.

### Test Cases

#### Positive / Happy Path

| ID | Title | Preconditions | Steps | Expected Result | Priority |
|---|---|---|---|---|---|
| TC-01 | Catalog says which Languages need a Reading [Auto] (AC1) | Automated | 1. Read the catalog entries | Mandarin Chinese needs a Reading; English does not | High |
| TC-02 | Reading kept for a target that needs one [Auto] (AC2, S1) | Automated: test catalog where Language B needs a Reading | 1. Save a translation with Reading " bee " for a Message whose target is B | Stored Reading is "bee" | High |
| TC-03 | Reading dropped for a target that doesn't need one [Auto] (AC2, S1) | Automated: target Language A doesn't need a Reading | 1. Save a translation with Reading "x" | Stored Reading is empty (null) | High |
| TC-04 | Full lifecycle stores each part [Auto] (AC4) | Automated: real SQLite; a Message inserted as RECORDING with a Recording reference | 1. Mark Transcribing<br>2. Save transcription (text, source, target)<br>3. Save translation | After each step the Message has the next status; at the end it is COMPLETED with Recording, transcript, both codes, translation and Reading all present | High |
| TC-05 | Every step moves the Conversation to the top [Auto] (AC5, S5) | Automated: two conversations; the clock advances between steps | 1. Run each step on the older conversation's Message | After each step that conversation's `updatedAt` equals the step time and it sorts first | High |
| TC-06 | Retry steps are allowed; late results are not [Auto] (AC3) | Automated: FAILED Messages, one with a Recording, one with a saved transcript | 1. Mark Transcribing on the first<br>2. Retry translation on the second<br>3. Save a (late) transcription on a FAILED Message | 1–2 are accepted; 3 is refused (only retry steps leave FAILED) | Medium |
| TC-07 | Chat still renders a completed Message (regression) | Fresh debug install | 1. Open the top conversation | Bubbles look exactly as in ticket 04 (original, target name, translation, italic Reading on the Mandarin Chinese translation, Play) | High |
| TC-08 | Home still lists and sorts (regression) | Fresh debug install | 1. Look at Home | Four seeded conversations, newest activity first, previews as in ticket 04 | Medium |

#### Negative

| ID | Title | Preconditions | Steps | Expected Result | Priority |
|---|---|---|---|---|---|
| TC-09 | Completed is final [Auto] (AC3, S3) | Automated: a COMPLETED Message | 1. Try each of mark Transcribing, save transcription, save translation, mark Failed | Each is refused; the Message is unchanged | High |
| TC-10 | Skipping a stage is refused [Auto] (S3) | Automated | 1. RECORDING → save transcription<br>2. TRANSCRIBING → save translation | Both refused | High |
| TC-11 | A refused step changes nothing [Auto] (S4) | Automated: the clock advanced before the refused call | 1. Attempt an invalid step | All fields and the Conversation `updatedAt` are unchanged | High |
| TC-12 | Blank or invalid transcription is rejected [Auto] (S7) | Automated | 1. Save a transcription with blank or zero-width-only text<br>2. With a blank source code<br>3. With source = target | 1 is not applied (no error); 2–3 are rejected as caller bugs; nothing is written | Medium |
| TC-13 | Blank or over-long translation is not applied [Auto] (S7) | Automated | 1. Save translation "  "<br>2. Save a 10,001-character translation | Not applied; nothing written | Medium |
| TC-14 | Unknown Message id [Auto] | Automated | 1. Run any step for an id that doesn't exist | Returns "not applied"; no Conversation is touched | Medium |

#### Edge Cases

| ID | Title | Preconditions | Steps | Expected Result | Priority |
|---|---|---|---|---|---|
| TC-15 | Failure keeps earlier parts [Auto] (AC4, S6) | Automated: Message at TRANSLATING with Recording and transcript | 1. Mark Failed | Status FAILED; Recording, transcript and codes unchanged | High |
| TC-16 | Blank Reading for a requiring target [Auto] (D5) | Automated | 1. Save translation with Reading "   " for a target that needs one | Message is COMPLETED with no Reading | Medium |
| TC-17 | Unknown target code [Auto] (D4) | Automated | 1. Save translation with Reading for a Message whose target code isn't in the catalog | Reading is not stored | Low |
| TC-18 | Observers update after each step [Auto] (AC5) | Automated: observing the conversation's Messages | 1. Run a step | A new emission shows the new status | Medium |
| TC-19 | Full transition matrix [Auto] (S3) | Automated | 1. Check every from → to pair | Exactly the pairs in the domain-model table are allowed | Medium |

---

## 06-voice-processing-pipeline

> Source: Docs/Requirement/Phase1/06-voice-processing-pipeline/plan.md (grill-notes D1–D16, domain-model P1–P6)
> Type: Feature · Generated: 2026-10-10 (create-manual-test-cases format)

### Scope
The voice flow end to end on Android: microphone permission, recording, the persisted processing steps, failures, limits, the startup sweep and the mic UI. Live cases need `OPENAI_API_KEY` in `local.properties` and a device.

### Test Cases

#### Positive / Happy Path
| ID | Title | Preconditions | Steps | Expected Result | Priority |
|---|---|---|---|---|---|
| TC-01 | First use asks for the microphone (AC4) | Fresh install; open a Conversation | 1. Tap the mic button | The system microphone permission dialog appears | High |
| TC-02 | Grant and record (AC1, AC4) | TC-01 dialog showing | 1. Allow<br>2. Say "Good morning"<br>3. Tap the button again | Recording starts right after allowing ("Recording 0:0x · tap to stop"); after stopping, a bubble appears and goes Recording… → Transcribing… → Translating… → final | High |
| TC-03 | English → Mandarin Chinese result (AC1, AC3) | English ↔ Mandarin Chinese Conversation; key configured | 1. Record "How are you?" | Bubble: "Voice · English", "How are you?", "Mandarin Chinese", a Chinese translation, pinyin, Play; on the left | High |
| TC-04 | Mandarin Chinese → English result (AC1, AC3) | Same | 1. Record "你好吗？" | Bubble: "Voice · Mandarin Chinese", the Chinese text, "English", an English translation, no Reading; on the right | High |
| TC-05 | Play the new Recording | TC-03 done | 1. Tap Play | Your recorded voice plays | High |
| TC-06 | Processing finishes after leaving (D11) | Key configured | 1. Record, stop<br>2. Immediately press Back | On Home the conversation's preview updates to the transcript within seconds; reopening shows the finished bubble | Medium |
| TC-07 | Pipeline success path [Auto] | Fakes | 1. Process a Recording | Message COMPLETED with transcript, codes, translation | High |

#### Negative
| ID | Title | Preconditions | Steps | Expected Result | Priority |
|---|---|---|---|---|---|
| TC-08 | Permission denied (AC4, P6) | Fresh install | 1. Tap mic<br>2. Deny | "Microphone access is needed to record. You can allow it in Settings." shows; nothing is recorded | High |
| TC-09 | No API key (AC2) | `OPENAI_API_KEY` empty | 1. Record and stop | Bubble ends "Failed" and keeps the Recording (Play works) | High |
| TC-10 | Airplane mode (AC2) | Key configured; airplane mode on | 1. Record and stop | Bubble ends "Failed"; Play works | High |
| TC-11 | Silence (AC2) | Key configured | 1. Record 3 s of silence | Bubble ends "Failed"; no invented text | Medium |
| TC-12 | Every failure keeps saved parts [Auto] (P1) | Fakes: STT error / translation error / undetermined / missing Conversation | 1. Process | FAILED; the Recording is kept, and the transcript too when it was saved | High |

#### Edge Cases
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

---

## 07-language-detection

> Source: Docs/Requirement/Phase1/07-language-detection/plan.md (with grill-notes.md D1–D15 and domain-model.md L1–L7)
> Type: Feature · Generated: 2026-10-10 (create-manual-test-cases format)

### Scope
- Speech-to-text through `SpeechToTextService` (OpenAI `whisper-1`), returning the transcript and detected language.
- Generic resolution of a Message's Source and Target Language from the Conversation's Language Pair: STT detection first, script fallback second, "the other Language" for the target.
- API key handling.

This ticket has no UI and the recording flow is ticket 06, so nearly every case is **[Auto]** (MockEngine, no real network). The live-call cases are manual and need a real key; they become fully exercisable in ticket 06.

### Open Questions
- Whisper sometimes reports Mandarin speech as another language (e.g. "cantonese"/"yue", "japanese"). The script fallback resolves Han text to the Han-script Language of the pair. In a Phase 2 Mandarin Chinese ↔ Japanese pair, kanji-only Japanese text stays undetermined (D14). Accepted.

### Test Cases

#### Positive / Happy Path

| ID | Title | Preconditions | Steps | Expected Result | Priority |
|---|---|---|---|---|---|
| TC-01 | English speech in an English ↔ Mandarin Chinese Conversation (AC2) [Auto] | Conversation (en, zh); STT returns "Good morning", "english" | 1. Resolve | Source en, target zh | High |
| TC-02 | Chinese speech (AC2) [Auto] | Same Conversation; STT returns "你好", "chinese" | 1. Resolve | Source zh, target en | High |
| TC-03 | Language order doesn't matter (AC2) [Auto] | Conversation (zh, en) | 1. Resolve "Good morning"/english and "你好"/chinese | (en → zh) and (zh → en) respectively | High |
| TC-04 | Target is always "the other Language" (AC3) [Auto] | Any test pair | 1. Compute the target for Language 1 and for Language 2 | Language 2 and Language 1 respectively | High |
| TC-05 | Region and case variants of the detected code [Auto] | STT code "ZH-cn" | 1. Resolve | Source zh | Medium |
| TC-06 | Script fallback when STT names a third language (D10) [Auto] | STT returns "你好" with "yue" | 1. Resolve | Source zh (Han text, only zh is Han in the pair) | High |
| TC-07 | Mixed text resolves by dominant script (D13) [Auto] | STT returns "我用iPhone", null code | 1. Resolve | Source zh | Medium |
| TC-08 | Transcription request shape (AC5) [Auto] | MockEngine; key "k"; Recording "audio/a.m4a" | 1. Transcribe | One POST to `…/v1/audio/transcriptions` with bearer "k", multipart `model=whisper-1`, `response_format=verbose_json`, file "a.m4a" (audio/mp4); returns the text and code | High |
| TC-09 | Whisper language names map to codes [Auto] | — | 1. Map english, chinese, Japanese, zh-CN | en, zh, ja, zh | Medium |
| TC-10 | Live transcription with a real key (manual) | `OPENAI_API_KEY` set in `local.properties`; a debug build from ticket 06 or a scratch harness | 1. Record "Good morning" in an English ↔ Mandarin Chinese Conversation<br>2. Record "你好" | Transcripts match the speech; detected sources English and Mandarin Chinese | High |

#### Negative

| ID | Title | Preconditions | Steps | Expected Result | Priority |
|---|---|---|---|---|---|
| TC-11 | Missing API key fails fast (AC5, L6) [Auto] | Blank key | 1. Transcribe | MISSING_API_KEY; zero HTTP requests | High |
| TC-12 | Missing Recording (L6) [Auto] | Reference doesn't resolve | 1. Transcribe | RECORDING_UNAVAILABLE; zero requests | Medium |
| TC-13 | Rejected key (L7) [Auto] | Server returns 401 | 1. Transcribe | UNAUTHORIZED; message doesn't contain the key | High |
| TC-14 | Rate limit and server errors [Auto] | Server returns 429, then 500 | 1. Transcribe twice | RATE_LIMITED, then SERVER | Medium |
| TC-15 | Network failure [Auto] | Engine throws an I/O error | 1. Transcribe | NETWORK | Medium |
| TC-16 | Malformed response [Auto] | 200 with invalid JSON or without `text` | 1. Transcribe | INVALID_RESPONSE | Medium |
| TC-17 | Same-script pair with a third-language code is undetermined (D14, L4) [Auto] | Pair A {LATIN} ↔ C {LATIN}; STT code unknown | 1. Resolve "hola" | null (no guess) | Medium |
| TC-18 | Text without letters is undetermined (L4) [Auto] | STT "123 !!!", null code | 1. Resolve | null | Low |

#### Edge Cases

| ID | Title | Preconditions | Steps | Expected Result | Priority |
|---|---|---|---|---|---|
| TC-19 | Language without script metadata [Auto] | Pair where one Language has no scripts; unknown code | 1. Resolve | Only the Language with matching scripts can be chosen; none → null | Low |
| TC-20 | Script tie is undetermined (D13) [Auto] | Text "hello 你" (1 Latin word, 1 Han character), no detected code | 1. Resolve | null | Low |
| TC-21 | Key never committed (L5, manual) | Repository checkout | 1. `git grep -n "sk-"` and inspect `local.properties` tracking | No key in tracked files; `local.properties` is ignored | High |
| TC-22 | File storage read (FileStorage.read) [Auto] | Temp dir | 1. Read an existing, a missing and an escaping reference | Bytes, null, null | Medium |
| TC-23 | Oversized or rejected Recording [Auto] | Recording reported as 26 MB; or server 400/413/415 | 1. Transcribe | RECORDING_REJECTED; no request for the oversized file | Medium |
| TC-24 | Silence becomes an empty transcript [Auto] | Response with every segment `no_speech_prob > 0.6`, `avg_logprob < -1` | 1. Transcribe | Transcript is empty (ticket 06 then doesn't save it) | High |
| TC-25 | Leaving the screen mid-request cancels cleanly [Auto] | Request in flight | 1. Cancel the caller | Cancellation propagates; no NETWORK failure is reported | Medium |
| TC-26 | Over-long transcript [Auto] | Response text of 10,001 characters | 1. Transcribe | RECORDING_REJECTED | Low |

---

## 08-openai-translation

> Source: Docs/Requirement/Phase1/08-openai-translation/plan.md (grill-notes D1–D11, domain-model T1–T6)
> Type: Feature · Generated: 2026-10-10 (create-manual-test-cases format)

### Scope
`TranslationService` and its OpenAI Chat Completions implementation (strict structured output in doc 08's JSON contract), plus `TranslateMessageUseCase`. No UI. The recording flow is ticket 06, so cases are **[Auto]** (MockEngine) except the live calls.

### Test Cases

#### Positive / Happy Path
| ID | Title | Preconditions | Steps | Expected Result | Priority |
|---|---|---|---|---|---|
| TC-01 | English → Mandarin Chinese request (AC1) [Auto] | Request en → zh, "How are you?", Reading required | 1. Translate | Request body has the model, the strict schema with `sourceLanguage` enum ["en"] and `targetLanguage` enum ["zh"], and a user message whose fields carry both codes and names, the text and `pronunciationRequired: true`; `englishReading` typed `string` | High |
| TC-02 | Parse the contract (AC2) [Auto] | Response content `{"sourceLanguage":"en","targetLanguage":"zh","translatedText":"你好吗？","englishReading":"Nǐ hǎo ma?"}` | 1. Translate | Result carries all five contract fields (transcript filled from the request) | High |
| TC-03 | Chinese → English, no Reading (AC2) [Auto] | zh → en, Reading not required; `englishReading` null | 1. Translate | Result Reading null | High |
| TC-04 | Use case completes a Translating Message (AC5) [Auto] | Message TRANSLATING with transcript, en → zh | 1. Translate the Message | Status COMPLETED with translation and Reading | High |
| TC-05 | Reading flag from target metadata (AC3) [Auto] | Test catalog: target requires a Reading / doesn't | 1. Translate each | Request flag true / false respectively | High |
| TC-06 | Live translation (manual) | `OPENAI_API_KEY` configured; ticket 06 build | 1. Speak "How are you?" in English ↔ Mandarin Chinese<br>2. Speak "你好吗？" | Chinese translation with pinyin; English translation without a Reading | High |

#### Negative
| ID | Title | Preconditions | Steps | Expected Result | Priority |
|---|---|---|---|---|---|
| TC-07 | Missing key [Auto] | Blank key | 1. Translate | MISSING_API_KEY; no request | High |
| TC-08 | HTTP failures [Auto] | 401, 429, 400, 500 | 1. Translate each | UNAUTHORIZED, RATE_LIMITED, REJECTED, SERVER | Medium |
| TC-09 | Transport failure / cancellation [Auto] | I/O error; caller cancelled | 1. Translate | NETWORK; cancellation propagates | Medium |
| TC-10 | Refusal [Auto] | `message.refusal` set | 1. Translate | REFUSED | Medium |
| TC-11 | Truncated or empty output [Auto] | `finish_reason` "length"; empty choices; non-JSON content; an `error` object | 1. Translate | REJECTED for "length"; INVALID_RESPONSE otherwise | Medium |
| TC-12 | Echo mismatch [Auto] | Response `sourceLanguage` "zh" for an en request | 1. Translate | INVALID_RESPONSE | Medium |
| TC-13 | Use case preconditions [Auto] | Message not TRANSLATING / no transcript / unknown id | 1. Translate | false, no service call | Medium |

#### Edge Cases
| ID | Title | Preconditions | Steps | Expected Result | Priority |
|---|---|---|---|---|---|
| TC-14 | Unneeded Reading is dropped [Auto] | Target doesn't require a Reading but the model returns one | 1. Translate the Message | Stored Reading null | Medium |
| TC-15 | Service failure leaves the Message for the caller [Auto] | Service throws | 1. Translate the Message | Exception propagates; Message still TRANSLATING (06 marks it failed) | Medium |
| TC-16 | Oversized response [Auto] | Content-Length over 1 MB | 1. Translate | INVALID_RESPONSE | Low |
| TC-17 | STT behaviour unchanged after refactor [Auto] | Ticket 07 tests | 1. Run them | All pass | High |
| TC-18 | Over-long translation [Auto] | Translation of 10,001 characters | 1. Translate | REJECTED (retry won't help) | Medium |
| TC-19 | Inputs changed during the call [Auto] | The Message is failed, retried and re-transcribed while the request is in flight | 1. Translate the Message | Result discarded; returns false; the new attempt's Message is untouched | High |
| TC-20 | Content filter [Auto] | `finish_reason` "content_filter" | 1. Translate | REFUSED | Low |

---

## 09-pronunciation-romanization

> Source: Docs/Requirement/Phase1/09-pronunciation-romanization/plan.md (grill-notes D1–D7, domain-model R1–R4)
> Type: Feature · Generated: 2026-10-10 (create-manual-test-cases format)

### Scope
Reading system as Language metadata, the Latin-script rule for stored Readings, and the Reading system passed to OpenAI. No UI change.

### Test Cases

#### Positive / Happy Path
| ID | Title | Preconditions | Steps | Expected Result | Priority |
|---|---|---|---|---|---|
| TC-01 | Mandarin Chinese has a Reading system (AC1, AC2) [Auto] | Catalog | 1. Read `zh` and `en` | `zh`: "Hanyu Pinyin with tone marks", needs a Reading; `en`: none | High |
| TC-02 | Pinyin with tone marks is stored (AC1) [Auto] | Target `zh`; Reading "Nǐ hǎo ma?" | 1. Save the translation | Reading stored as "Nǐ hǎo ma?" | High |
| TC-03 | OpenAI is asked for the system (AC3) [Auto] | Request to `zh` | 1. Translate | User fields include `"pronunciationSystem":"Hanyu Pinyin with tone marks"`; the instruction names it | High |
| TC-04 | Chat shows the Reading (regression) | Fresh debug install | 1. Open the top conversation | "Nǐ jīntiān hǎo ma?" in italics under "你今天好吗？"; none under the English translation | Medium |
| TC-05 | Live pinyin (manual) | Key configured; ticket 06 build | 1. Speak English in an English ↔ Mandarin Chinese Conversation | Translation with pinyin tone marks underneath | High |

#### Negative
| ID | Title | Preconditions | Steps | Expected Result | Priority |
|---|---|---|---|---|---|
| TC-06 | Non-Latin Reading dropped (R2) [Auto] | Target `zh`; Reading "你好" or "Ni hao 你" | 1. Save | Reading null; Message still completes | High |
| TC-07 | No Reading for English (AC1) [Auto] | Target `en`; model sends "x" | 1. Save | Reading null | High |

#### Edge Cases
| ID | Title | Preconditions | Steps | Expected Result | Priority |
|---|---|---|---|---|---|
| TC-08 | Phase 2 shape needs only metadata (AC2) [Auto] | Test Language with `readingSystem = "Hepburn romaji"` | 1. Save Reading "Genki desu ka?" | Stored; the request carries "Hepburn romaji" | Medium |
| TC-09 | Digits and punctuation in a Reading [Auto] | Reading "Nǐ 3 suì le!" | 1. Save | Stored | Low |
| TC-10 | Reading without letters [Auto] | Reading "—" | 1. Save | Reading null | Low |
| TC-11 | Other phonetic scripts dropped [Auto] | Reading in Zhuyin, half-width kana, Greek or supplementary Han | 1. Save | Reading null | Medium |
| TC-12 | IPA, full-width Latin and emoji kept [Auto] | Reading "ɑ", "Ｎｉ", "Xièxie 👍" | 1. Save | Stored as given | Low |

---

## 10-local-database

| ID | Title | Steps | Expected | Priority |
|---|---|---|---|---|
| TC-01 | Conversation columns [Auto] | `SchemaConformanceTest` | Exactly the six doc-10 columns | High |
| TC-02 | Message columns + FK [Auto] | same | Ten columns; FK to Conversation | High |
| TC-03 | Status values [Auto] | same | The five names | Medium |
| TC-04 | Unmigrated change is caught [Auto] | Add a column to `Message.sq`, run `verifySqlDelightMigration` | Fails: column ADDED | High |
| TC-05 | Offline browsing (manual) | Airplane mode; open Home, a Conversation, play a Recording | All work; no errors | High |
| TC-06 | Upgrade keeps data (manual, when a v2 exists) | Install old build, add data, install new | Data intact | High |

---

## 11-infinite-chat-scrolling

| ID | Title | Expected | Priority |
|---|---|---|---|
| TC-01 | SQL limit [Auto] | Only the newest N rows; more with a bigger limit | High |
| TC-02 | Window stays live and slides [Auto] | New Message appears, oldest of the window drops | High |
| TC-03 | VM opens with one page of 100 [Auto] | 30 bubbles, newest first | High |
| TC-04 | VM loads older page by page [Auto] | 60, then all 70; extra calls no-op | High |
| TC-05 | Small Conversation [Auto] | `onLoadOlder` ignored | Medium |
| TC-06 | Arrival keeps older loaded [Auto] | Oldest loaded stays; size +1 | High |
| TC-07 | Reopen resets to newest page [Auto] | 30 bubbles | Medium |
| TC-08 | Scroll trigger (manual) | Seed 200 Messages; scroll up steadily: older pages appear without a visible jump; top reached stops loading | High |
| TC-09 | Opens at newest (manual) | Back out and reopen a long Conversation: newest at bottom | High |
| TC-10 | Rotation (manual) | Scroll position kept | Low |
| TC-11 | New Message while scrolled up (manual) | Record while reading old Messages: view does not jump | Medium |

---

## 12-retry

| ID | Title | Expected | Priority |
|---|---|---|---|
| TC-01 | Translation failure retried from transcript [Auto] | COMPLETED; STT called once in total | High |
| TC-02 | Transcription failure retried from Recording [Auto] | COMPLETED; STT called twice | High |
| TC-03 | Retry fails again [Auto] | FAILED, transcript + audio kept, retryable again | High |
| TC-04 | Silence retry [Auto] | FAILED again | Low |
| TC-05 | Non-Failed / unknown / nothing-to-resume [Auto] | No-op | Medium |
| TC-06 | Double tap [Auto] | Same job; attempt not cancelled; COMPLETED | High |
| TC-07 | Mapper `canRetry` [Auto] | Failed + (transcript or audio) only | High |
| TC-08 | VM retry click [Auto] | Bubble completes, button gone | High |
| TC-09 | Live: airplane mode, record, see Failed + Retry; network on, tap Retry (manual) | Translation appears; no new recording | High |
| TC-10 | Retry then leave screen (manual) | Message completes in the background | Medium |
| TC-11 | Kill app mid-retry (manual) | Reopens as Failed with Retry | Medium |

---

## 13-technology-stack

| ID | Title | Expected | Priority |
|---|---|---|---|
| TC-01 | Mandated stack present [Auto] | `TechStackTest.sharedModuleUsesTheMandatedStack` passes | High |
| TC-02 | Both platforms' driver and engine [Auto] | passes | Medium |
| TC-03 | No Room [Auto] | Adding `room` to the catalog fails the test | High |
| TC-04 | Guards re-run on change [Auto] | Catalog/`androidApp` edits re-run the host tests (mutation checked) | High |
| TC-05 | Both apps build (regression) | `assembleDebug`, Xcode simulator build succeed | Medium |

---

## 14-platform-specific-components

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

---

## 15-openai-api-security

> Source: plan.md · Generated: 2026-10-11

| ID | Title | Steps | Expected | Priority |
|---|---|---|---|---|
| TC-01 | No hard-coded key [Auto] | Run `NoHardcodedSecretsTest` | Passes; adding `sk-` + 30 chars to a source file makes it fail | High |
| TC-02 | Key files ignored [Auto] | Same test | `.gitignore` lists `local.properties`, `*.key`, `Secrets.xcconfig` | High |
| TC-03 | Authorization masked in logs [Auto] | `HttpClientFactoryTest` | Bearer token absent from the log | High |
| TC-04 | Audio upload summarised [Auto] | `HttpClientFactoryTest` | "binary N-byte body omitted", no bytes dumped | Medium |
| TC-05 | No logger, no logs [Auto] | `HttpClientFactoryTest` | Nothing logged | High |
| TC-06 | Debug build logs to logcat (manual) | Run debug build, record a message, `adb logcat -s DoVashi-HTTP` | Requests/responses shown; `Authorization: ██` | Medium |
| TC-07 | Release build is silent (manual) | Install release build, repeat | No `DoVashi-HTTP` lines | High |
| TC-08 | Missing key fails fast (regression) | Build without a key, record | Message fails with MISSING_API_KEY, no network call | Medium |

---

## 16-architecture-and-extensibility

| ID | Title | Expected | Priority |
|---|---|---|---|
| TC-01 | Structure [Auto] | Required folders exist in all three source sets | Medium |
| TC-02 | Domain layering [Auto] | A domain file importing `data.*` fails | High |
| TC-03 | Presentation layering [Auto] | A presentation file importing `data.*` fails | High |
| TC-04 | Platform-free common [Auto] | No `android.`/`java.`/`platform.` imports in `commonMain` | High |
| TC-05 | OpenAI behind ports [Auto] | Naming an adapter outside `data/network/openai` and `di` fails | High |
| TC-06 | No hard-coded language [Auto] | A `"zh"`/`"en"` literal outside the catalog metadata fails | High |
| TC-07 | Phase 2 dry run (manual) | Add a third Language to the catalog (e.g. Japanese with its scripts and Reading system): it appears in the pair pickers, a Conversation with it completes the pipeline with no other code change | High |
