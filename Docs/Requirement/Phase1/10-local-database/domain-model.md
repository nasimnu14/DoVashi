# Domain Model — 10-local-database

| Concept | Layer | Note |
|---|---|---|
| `Conversation` table | data/database | id, title, language1Code, language2Code, createdAt, updatedAt (as doc 10) |
| `Message` table | data/database | doc 10 columns; FK to Conversation; index (conversationId, createdAt) |
| `MessageStatus` | domain/model | RECORDING, TRANSCRIBING, TRANSLATING, COMPLETED, FAILED |
| `databases/1.db` | build | Schema snapshot for migration verification |

## Invariants
| ID | Invariant | Enforced by |
|---|---|---|
| L1 | Tables match doc 10 | `SchemaConformanceTest` |
| L2 | Schema changes ship a migration | `verifyMigrations` + `verifySqlDelightMigration` |
| L3 | Language columns hold plain codes, no per-language schema | Unchanged columns |
| L4 | Reads need no network | Architecture (OpenAI only in adapters) |
