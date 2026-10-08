# Grill Notes — 02-home-screen

Source: `Docs/Requirement/Phase1/02-home-screen.md` · Grilled 2026-10-08 · 3 rounds, 23 questions + 5 confirmed assumptions.

Repo state at grill time: no Kotlin/Gradle project exists (only `Docs/`).

## Scope & dependencies

| # | Question | Resolved answer |
|---|----------|-----------------|
| Q1 | Does this ticket scaffold the KMP project? | No — the user creates the KMM project themselves. |
| Q2 | Implement the Language Model inline? | No — ticket `01-language-model` is implemented before this ticket starts; Home consumes its catalog for code → name. |
| Q3 | What does the **+** button do? | Real navigation to a stub Create Conversation destination; full Create UI stays in ticket 03. |
| Q4 | (Answered as stub content) | The Create stub screen shows only "Hello World". |
| Q5 | Platform target? | Android only for build/verify; `iosMain` stays an empty placeholder source set. |
| Q10 | Shared UI tech? | Compose Multiplatform, Home UI in `commonMain`. |
| Q16 | Workflow timing? | Run Steps 3–7 now; Step 8 is ⛔ Blocked until the KMM project and ticket 01 exist. |

## Data layer (this ticket creates the schema)

| # | Question | Resolved answer |
|---|----------|-----------------|
| Q11 | Who creates the DB schema? | This ticket creates the SQLDelight `Conversation` and `Message` tables with exactly the columns in `10-local-database.md`; ticket 10 extends later. |
| Q18 | Column types / constraints? | `INTEGER` autoincrement ids; `INTEGER` epoch-millis timestamps; `status` as `TEXT` enum name; FK `Message.conversationId → Conversation.id`; index on `Message(conversationId, createdAt)`. |
| Q19 | Write surface? | Only `createConversation` (sets `updatedAt = createdAt`) and `insertMessage` (insert + bump parent `updatedAt` in one transaction). Status-update methods come with pipeline tickets, which must also bump `updatedAt` — documented invariant. |
| Q6 | Title source? | Auto-derived from the language pair at creation ("for now"); Home displays the stored `title` as-is. |
| Q7 | Re-sort mechanism? | Every message insert/update bumps the parent conversation's `updatedAt` via the repository; a reactive query re-sorts the list automatically. |
| A1 | Paging on Home? | (assumed, confirmed) No — one reactive query over all conversations, `updatedAt DESC`. |
| A5 | FK enforcement? | (assumed, confirmed) `PRAGMA foreign_keys=ON` via driver config. |

## Row content

| # | Question | Resolved answer |
|---|----------|-----------------|
| Q13 | Title vs pair redundancy? | Show both: title as headline, pair as secondary label; accept Phase 1 redundancy. Pair uses catalog `name` ("English ↔ Mandarin Chinese"). |
| Q21 | Unknown language code? | Pair label falls back to the raw code (e.g. `en ↔ ja`); always stored `language1 ↔ language2` order. |
| Q7/Q20 | Last-message preview? | Latest message by `createdAt DESC`, `id DESC` tie-break. Show `transcribedText` if non-empty, else status placeholder: "Recording…", "Transcribing…", "Failed". Zero messages → "No messages yet". |
| Q8/Q20 | Message count? | Shown; counts all messages regardless of status. |
| Q9 | Time format? | Buckets: "Just now" (<1 min), "X min ago" / "X hours ago" (<24h), "Yesterday" (previous calendar day), else short date ("Sep 28"). Device local time; plain English strings. Shows conversation `updatedAt`. |
| Q23 | When are relative times recomputed? | Only when data changes (message added/updated, conversation created). No foreground refresh, no ticking timer. Trade-off accepted: idle screen can show stale "Just now". |

## Screen & navigation

| # | Question | Resolved answer |
|---|----------|-----------------|
| Q22 | Layout? | Material 3; top app bar "Conversations"; **+** FAB bottom-right; `LazyColumn` keyed by id; row = title headline, pair secondary, single-line ellipsized preview, trailing time + count. |
| Q8 | Empty state? | "No conversations yet — tap + to start one", FAB still visible. |
| A2 | Initial load? | (assumed, confirmed) Neutral loading state before first emission — no empty-state flash. |
| Q12 | Row tap? | Real navigation to a "Hello World" Chat stub receiving `conversationId`; ticket 04 fills it in. |
| Q17 | Libraries? | JetBrains `navigation-compose` (type-safe `@Serializable` routes), JetBrains `lifecycle-viewmodel-compose`, Koin, `kotlinx-datetime` with injectable `Clock`. Use template-pinned versions where present. |
| A3 | Accessibility? | (assumed, confirmed) FAB content description "New conversation". |

## Verification

| # | Question | Resolved answer |
|---|----------|-----------------|
| Q4/Q14 | Manual data for visual check? | Debug-only seed: 2–3 sample conversations + messages, inserted only when the `Conversation` table is empty (A4), using only catalog codes. Never runs in release. |
| Q15 | Automated test scope? | Repository query (sort, last message, count, re-sort after `insertMessage`), relative-time formatter, ViewModel state mapping. No Compose UI / instrumentation tests this ticket. |
