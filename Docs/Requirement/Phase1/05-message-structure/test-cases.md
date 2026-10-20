# Test Cases — 05-message-structure

> Source: Docs/Requirement/Phase1/05-message-structure/plan.md (with grill-notes.md D1–D12 and domain-model.md invariants S1–S7)
> Type: Feature · Generated: 2026-10-10

## Scope
This suite covers four structural rules of a Message:
- whether a Reading is needed (from target-Language metadata)
- the Message Status transition order
- step-by-step persistence that never loses earlier parts
- the Conversation `updatedAt` bump on every step

This ticket has no new UI and no in-app way to drive the steps (the pipeline is ticket 06). Most cases are therefore **[Auto]**; the manual cases are regression smoke checks of Home and Chat.

## Open Questions
- Phase 1 never shows a Message with a missing Reading for a Mandarin Chinese target. Per D5 the Message still completes, and the bubble simply has no Reading line. Confirm when the translation ticket (08/09) lands.

## Test Cases

### Positive / Happy Path

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

### Negative

| ID | Title | Preconditions | Steps | Expected Result | Priority |
|---|---|---|---|---|---|
| TC-09 | Completed is final [Auto] (AC3, S3) | Automated: a COMPLETED Message | 1. Try each of mark Transcribing, save transcription, save translation, mark Failed | Each is refused; the Message is unchanged | High |
| TC-10 | Skipping a stage is refused [Auto] (S3) | Automated | 1. RECORDING → save transcription<br>2. TRANSCRIBING → save translation | Both refused | High |
| TC-11 | A refused step changes nothing [Auto] (S4) | Automated: the clock advanced before the refused call | 1. Attempt an invalid step | All fields and the Conversation `updatedAt` are unchanged | High |
| TC-12 | Blank or invalid transcription is rejected [Auto] (S7) | Automated | 1. Save a transcription with blank or zero-width-only text<br>2. With a blank source code<br>3. With source = target | 1 is not applied (no error); 2–3 are rejected as caller bugs; nothing is written | Medium |
| TC-13 | Blank or over-long translation is not applied [Auto] (S7) | Automated | 1. Save translation "  "<br>2. Save a 10,001-character translation | Not applied; nothing written | Medium |
| TC-14 | Unknown Message id [Auto] | Automated | 1. Run any step for an id that doesn't exist | Returns "not applied"; no Conversation is touched | Medium |

### Edge Cases

| ID | Title | Preconditions | Steps | Expected Result | Priority |
|---|---|---|---|---|---|
| TC-15 | Failure keeps earlier parts [Auto] (AC4, S6) | Automated: Message at TRANSLATING with Recording and transcript | 1. Mark Failed | Status FAILED; Recording, transcript and codes unchanged | High |
| TC-16 | Blank Reading for a requiring target [Auto] (D5) | Automated | 1. Save translation with Reading "   " for a target that needs one | Message is COMPLETED with no Reading | Medium |
| TC-17 | Unknown target code [Auto] (D4) | Automated | 1. Save translation with Reading for a Message whose target code isn't in the catalog | Reading is not stored | Low |
| TC-18 | Observers update after each step [Auto] (AC5) | Automated: observing the conversation's Messages | 1. Run a step | A new emission shows the new status | Medium |
| TC-19 | Full transition matrix [Auto] (S3) | Automated | 1. Check every from → to pair | Exactly the pairs in the domain-model table are allowed | Medium |
