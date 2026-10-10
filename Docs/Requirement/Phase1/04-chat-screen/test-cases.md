# Test Cases — 04-chat-screen

> Source: Docs/Requirement/Phase1/04-chat-screen/plan.md (with grill-notes.md D1–D16 and domain-model.md invariants M1–M8)
> Type: Feature · Generated: 2026-10-10

## Scope
The Chat screen opened from a Home row or after Create Conversation:
- title, Message bubbles in chronological order (newest at the bottom)
- per-bubble Language labels, translation and Reading
- bubble side by source Language
- status lines
- Play/Pause of a Recording
- not-found and empty states

Android debug build with the ticket-04 debug seed. Recording new Messages is ticket 06, so content comes from the seed.

Cases tagged **[Auto]** are verified by the plan Step 7 tests. All other cases are manual.

## Open Questions
- The seed only runs on an empty database. Testers with an older debug install must clear app data first so the new seed (translations, sample tone) is created.
- Sample seed content (expected on a fresh debug install):
  - "English ↔ Mandarin Chinese" (most recent):
    - (1) English "How are you today?" → "你今天好吗？", reading "Nǐ jīntiān hǎo ma?", with the sample tone
    - (2) Mandarin Chinese "火车站在哪里？" → "Where is the train station?", no reading
  - A second "English ↔ Mandarin Chinese" conversation: a TRANSLATING Message "Can you help me?" and a FAILED Message "我需要一杯水"
  - The empty "Mandarin Chinese ↔ English" conversation
  - The older conversation with one FAILED Message that has no text

## Test Cases

### Positive / Happy Path

| ID | Title | Preconditions | Steps | Expected Result | Priority |
|---|---|---|---|---|---|
| TC-01 | Open a conversation from Home (AC1) | Fresh debug install (app data cleared) | 1. Launch the app<br>2. Tap the top row ("English ↔ Mandarin Chinese", preview "火车站在哪里？") | The Chat screen opens with title "English ↔ Mandarin Chinese" and a Back arrow; two bubbles are shown | High |
| TC-02 | Chronological order, opens at newest (AC1, M1) | TC-01 open | 1. Look at the list without scrolling | "How are you today?" is above "火车站在哪里？"; the newest bubble ("火车站在哪里？") is at the bottom, right above the screen edge | High |
| TC-03 | English → Chinese bubble content (AC2) | TC-01 open | 1. Read the "How are you today?" bubble top to bottom | Lines in order: "Voice · English", "How are you today?", "Mandarin Chinese", "你今天好吗？", "Nǐ jīntiān hǎo ma?", then a Play button | High |
| TC-04 | Chinese → English bubble content (AC2, M5) | TC-01 open | 1. Read the "火车站在哪里？" bubble | Lines: "Voice · Mandarin Chinese", "火车站在哪里？", "English", "Where is the train station?"; no reading line; no Play button (this Message has no Recording) | High |
| TC-05 | Bubble sides follow the source Language (AC3, M3) | TC-01 open | 1. Compare the horizontal position and colour of both bubbles | The English-source bubble sits on the left and the Mandarin-Chinese-source bubble on the right, in different colours | High |
| TC-06 | Play a Recording (AC5) | TC-01 open; media volume up | 1. Tap Play on "How are you today?" | A tone of about 1.5 seconds plays; while it plays the button reads "Pause"; afterwards it returns to "Play" | High |
| TC-07 | Pause and resume (AC5) | TC-01 open | 1. Tap Play<br>2. Within 1 second tap Pause<br>3. Tap Play again | After 2 the sound stops and the button reads "Play"; after 3 playback continues from where it paused (the remaining part of the tone) and ends normally | High |
| TC-08 | Status lines for in-progress and failed Messages (AC4) | Fresh seed | 1. Press Back to Home<br>2. Open the second "English ↔ Mandarin Chinese" conversation | "Can you help me?" shows "Translating…"; "我需要一杯水" shows "Failed" in error colour; neither shows a translation block | High |
| TC-09 | Empty conversation (AC6) | Fresh seed | 1. Open the "Mandarin Chinese ↔ English" conversation (preview "No messages yet") | The title is "Mandarin Chinese ↔ English" and the body shows "No messages yet" | Medium |
| TC-10 | New conversation opens an empty Chat (AC6) | Home visible | 1. Tap **+**, then Start Conversation | The Chat screen shows the new title and "No messages yet" | Medium |
| TC-11 | Back returns to Home (AC1) | TC-01 open | 1. Tap the Back arrow | Home is shown; the list is unchanged | Medium |
| TC-12 | Newest-first query, scoped to the conversation [Auto] (M1, M2) | Automated: real SQLite | 1. Insert Messages in two conversations with mixed timestamps<br>2. Observe one conversation | Only that conversation's Messages arrive, newest first with an id tie-break | High |
| TC-13 | Live update when a Message is added [Auto] (AC7) | Automated: real SQLite | 1. Observe Messages<br>2. Insert a Message | A new emission contains it, without re-subscribing | High |

### Negative

| ID | Title | Preconditions | Steps | Expected Result | Priority |
|---|---|---|---|---|---|
| TC-14 | Missing Recording file (AC5, D13) | TC-01 open; while the app stays running, delete the sample tone (`adb shell run-as com.example.dovashiapp rm files/audio/seed-tone.wav`). A relaunch regenerates it | 1. Tap Play on "How are you today?" | The button is replaced by "Recording unavailable"; no crash | Medium |
| TC-15 | Unknown conversation id → not found [Auto] (AC6) | Automated | 1. Open the ViewModel for an id that doesn't exist | State is NotFound ("Conversation not found") | Medium |
| TC-16 | Reference outside app storage is rejected [Auto] (M7) | Automated: temp dir | 1. Resolve "../x", "/etc/hosts", "" and a missing file | Each returns no path | High |
| TC-17 | Unknown Language Code shows the raw code [Auto] (M4) | Automated: mapper | 1. Map a Message whose source code isn't in the catalog | Source label shows the raw code | Low |
| TC-18 | Unknown stored status reads as Failed [Auto] | Automated: SQLite row with status "BOGUS" | 1. Observe Messages | The Message has status FAILED and shows "Failed" | Low |

### Edge Cases

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
