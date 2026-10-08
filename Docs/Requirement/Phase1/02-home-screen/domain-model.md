# Domain Model — 02-home-screen

Sources: `02-home-screen.md`, `10-local-database.md`, `01-language-model.md`, `16-architecture-and-extensibility.md`, `grill-notes.md`.
No code exists yet; everything below is the target shape. Items marked **(S5)** were resolved in Step 5 design questions.

## 1. Entities & relationships

### Language — *consumed, owned by ticket 01*
`Language(code, name, nativeName)`, from the seeded catalog (`en`, `zh` in Phase 1).
Home only needs **code → `name` lookup**; an unknown code yields no match (caller falls back to the raw code, Q21).

### Conversation — *persisted, created by this ticket*
| Field | Type | Notes |
|---|---|---|
| `id` | `Long` | `INTEGER PRIMARY KEY AUTOINCREMENT` (Q18) |
| `title` | `String` | Auto-derived from the pair at creation; displayed as stored (Q6) |
| `language1Code` | `String` | Plain code, soft reference to the catalog (not a FK — the catalog is data owned by ticket 01) |
| `language2Code` | `String` | Same as above |
| `createdAt` | `Long` | Epoch millis |
| `updatedAt` | `Long` | Epoch millis; the Home sort key |

### Message — *persisted, created by this ticket (columns per `10-local-database.md`)*
| Field | Type | Notes |
|---|---|---|
| `id` | `Long` | Autoincrement |
| `conversationId` | `Long` | FK → `Conversation.id` (Q18) |
| `sourceLanguage`, `targetLanguage` | `String?` | Codes; null until detection **(S5)** |
| `audioPath` | `String?` | File path/reference only, never bytes; nullable in case the path is only known after recording stops **(S5)** |
| `transcribedText`, `translatedText`, `reading` | `String?` | Filled progressively by the pipeline **(S5)** |

Required (NOT NULL): `id`, `conversationId`, `status`, `createdAt` **(S5)**.
| `status` | `MessageStatus` | Stored as `TEXT` enum name (Q18) |
| `createdAt` | `Long` | Epoch millis |

Index: `Message(conversationId, createdAt)` (Q18).

### MessageStatus — *enum*
`RECORDING, TRANSCRIBING, TRANSLATING, COMPLETED, FAILED`. The order of status changes belongs to the pipeline and retry tickets (06/12). Home only **reads** status to choose the preview placeholder.

### ConversationSummary — *read model, new in this ticket*
The Home query's output: one per conversation.
`conversation: Conversation`, `lastMessageText: String?`, `lastMessageStatus: MessageStatus?` (both null ⇔ zero messages), `messageCount: Long`.

### HomeUiState / ConversationRowUi — *presentation state*
- `HomeUiState = Loading | Empty | Content(rows)` (A2, Q8).
- `ConversationRowUi(id, title, pairLabel, preview, timeLabel, messageCount)`: all strings are already formatted for display.

### Routes — *navigation*
`HomeRoute`, `CreateConversationRoute` (stub, "Hello World"), `ChatRoute(conversationId: Long)` (stub, "Hello World"); type-safe `@Serializable` (Q3, Q12, Q17).

### Relationships
```
Language (catalog) ◄─code── Conversation.language1Code / language2Code   (2 per conversation)
Conversation 1 ──────────* Message                                        (FK conversationId)
Language (catalog) ◄─code── Message.sourceLanguage / targetLanguage       (nullable until detection)
Conversation + latest Message + count ──► ConversationSummary ──► ConversationRowUi
```

## 2. Bounded contexts / ownership

| Layer (per `16-architecture`) | Owns |
|---|---|
| `domain/model` | `Conversation`, `Message`, `MessageStatus`, `ConversationSummary`; `conversationTitle(language1, language2): String` = `"${l1.name} ↔ ${l2.name}"` **(S5)** |
| `domain/usecase` | One per action **(S5)**: `ObserveConversationSummariesUseCase`, `CreateConversationUseCase(language1: Language, language2: Language)` (enforces I7, builds the title with `conversationTitle`, calls the repository), `InsertMessageUseCase` |
| `domain/repository` | `ConversationRepository` (`observeSummaries(): Flow<List<ConversationSummary>>`, `createConversation(...)`), `MessageRepository` (`insertMessage(...)`). Split so later chat paging lands in `MessageRepository` |
| `data/database` | SQLDelight schema + queries: the summary query (latest message plus count per conversation), inserts, `touchUpdatedAt` |
| `data/repository` | Repository impls; injected `Clock`; transaction boundaries; mapping between enums and their `TEXT` values |
| `presentation/home` | `HomeViewModel` (depends on `ObserveConversationSummariesUseCase` + the language catalog), `HomeUiState`, `HomeScreen`, relative-time formatter, preview/pair-label mapping |
| `navigation` | Routes, `NavHost`, the Create and Chat stub screens |
| `di` | Koin modules (driver, database, repos, clock, catalog, ViewModel) |
| `androidMain` | `SqlDriver` factory (with `foreign_keys=ON`), deciding whether this is a debug build so the seed may run |
| `iosMain` | Empty placeholder (Q5) |

Debug seeding: the seed content and logic live in shared code and go through `CreateConversationUseCase` / `InsertMessageUseCase`, so seeded rows satisfy every invariant. It takes its languages from the catalog rather than from `en`/`zh` literals. Only the **trigger** is Android and debug-only (Q14, A4).

## 3. Invariants

| ID | Invariant | Enforced by |
|---|---|---|
| I1 | On create, `updatedAt = createdAt` | `createConversation` |
| I2 | Every message insert **and every future status/text update** sets parent `updatedAt = now` in the **same transaction** | `MessageRepository` impl; future pipeline tickets must keep it (Q7, Q19) |
| I3 | Home order = `updatedAt DESC`, tie-break `id DESC` (deterministic) | Summary SQL query |
| I4 | Last message = max by (`createdAt`, `id`); count = all messages, any status | Summary SQL query (Q20) |
| I5 | Pair label = `name(language1Code) ↔ name(language2Code)` in stored order; raw code if not in the catalog; no language literals | Presentation mapper (Q21, Phase 1 overview constraint) |
| I6 | Preview = `transcribedText` if non-blank; else placeholder by status: RECORDING "Recording…", TRANSCRIBING "Transcribing…", FAILED "Failed"; any other status with no text → empty string; zero messages → "No messages yet" | Presentation mapper (Q7, Q20) |
| I7 | `language1Code ≠ language2Code` | `CreateConversationUseCase` rejects identical codes (`require`), and ticket 03's picker also prevents it **(S5)** |
| I8 | The time label is recalculated only when the list re-emits (any data change) | ViewModel maps on each emission using the injected `Clock` (Q23) |
