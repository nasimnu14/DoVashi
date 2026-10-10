# Test Cases — 03-create-conversation

> Source: Docs/Requirement/Phase1/03-create-conversation/plan.md (with grill-notes.md D1–D18 and domain-model.md invariants C1–C6)
> Type: Feature · Generated: 2026-10-10

## Scope
The Create Conversation screen opened from **+** on Home: two Language dropdowns read from the Language Catalog (English and Mandarin Chinese in Phase 1), the same-Language rule, Start Conversation persisting a Conversation in the selected order and opening the Chat stub, back-stack behaviour, double-tap and failure handling. Android debug build; the Chat screen itself is still the ticket-02 stub.

Cases tagged **[Auto]** can't be forced from the Phase 1 UI (a catalog that isn't exactly two entries, a database failure, a precise concurrent tap). The automated tests in plan Step 5 verify them. All other cases are manual.

## Open Questions
- The Chat destination is still the ticket-02 stub ("Hello World" + "Conversation #id"), so "opens the Chat screen" is checked by the id shown on the stub.
- Process death resets the form to defaults (D16). This is accepted, not a defect.
- Added after review: while a create is in flight, the Back icon and dropdowns are disabled. If the insert has already committed when Back is pressed, the conversation stays on Home and no Chat screen opens (TC-24).

## Test Cases

### Positive / Happy Path

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

### Negative

| ID | Title | Preconditions | Steps | Expected Result | Priority |
|---|---|---|---|---|---|
| TC-10 | Same Language in both fields is rejected (AC3, C2) | On Create Conversation with defaults | 1. Set Language 2 to "English" | The text "Choose two different languages" appears; Start Conversation is disabled | High |
| TC-11 | Disabled Start does nothing (AC3, C4) | TC-10 state (English / English) | 1. Tap Start Conversation several times<br>2. Tap Back | Nothing happens on the taps; Home's row count is unchanged | High |
| TC-12 | Both set to Mandarin Chinese is rejected (AC3) | On Create Conversation with defaults | 1. Set Language 1 to "Mandarin Chinese" | The error text appears and Start Conversation is disabled | Medium |
| TC-13 | Use case rejects identical codes [Auto] (C4) | Automated | 1. Call create with the same Language twice | It is rejected and no row is written | Medium |
| TC-14 | Database failure shows an error and allows retry [Auto] (AC6, D13) | Automated: repository fake throws on the first call | 1. Tap Start<br>2. Tap Start again | After 1: "Couldn't create the conversation. Please try again." is shown and the button is enabled again; after 2: one conversation is created and navigation is requested | Medium |

### Edge Cases

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
