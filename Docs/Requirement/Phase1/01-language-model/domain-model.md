# Domain Model — 01-language-model

Terms (Language, Language Code, Language Catalog, Language Pair, Conversation,
Mandarin Chinese) are defined in the root [GLOSSARY.md](../../../../GLOSSARY.md).

## Entities & relationships

### `Language` (value/entity)

| Field | Type | Notes |
|---|---|---|
| `code` | `String` | Primary identifier (e.g. `en`, `zh`). Unvalidated plain string — [[grill-notes]] Q2. |
| `name` | `String` | English display name (e.g. "Mandarin Chinese"). |
| `nativeName` | `String` | Name in the language's own script (e.g. "普通话" — Simplified, [[grill-notes]] Q7). |

Immutable `data class`. No identity beyond `code` — two `Language` instances with the
same `code` are equal. `code` is the de facto key other entities (`Conversation`,
`Message`, future `LanguageDetection` results) reference by string, not by object
reference — confirmed by the existing `Conversation.language1Code`/`language2Code`
and `Message.sourceLanguage`/`targetLanguage` columns in `10-local-database.md`,
which predate this ticket and already treat language as a plain code.

### `LanguageCatalog` (domain service / static registry)

Not an entity — a seeded, queryable collection of `Language`. Phase 1 seeds exactly
two rows (`en`, `zh`). Owns:

- `all: List<Language>` — the full catalog, in seed order.
- `byCode(code: String): Language?` — lookup, `null` on miss.

**Relationship to `Language`**: one `LanguageCatalog` aggregates many `Language`
rows. Nothing else in this ticket holds a reference to `Language` — `Conversation`
and `Message` (owned by the Local Database ticket, #10) store raw `code` strings and
would resolve them through `LanguageCatalog.byCode` only when a consumer (not this
ticket) needs the full `Language` object.

## Bounded context / ownership

- **Owning layer**: `domain/model` in `commonMain` (per `16-architecture-and-extensibility.md`'s
  module structure: `commonMain/domain/model`). Pure Kotlin, no platform code, no
  DI, no I/O — consistent with Q1's decision to keep the catalog in-memory rather
  than SQLDelight-backed.
- **Downstream consumers (not touched by this ticket)**: Language Detection (#07),
  Create Conversation (#03), Home Screen (#02), OpenAI Translation (#08),
  Pronunciation/Romanization (#09) — each will call `LanguageCatalog.all` or
  `.byCode` once wired, per Q3/Q4. This ticket defines the contract; it does not
  call it from anywhere.
- **Invariant**: the catalog is append-only across phases — Phase 2 (#01-expanded-language-catalog)
  adds rows to the same `LanguageCatalog.all` seed list without changing its shape,
  `Language`'s fields, or the `byCode` contract. This is the mechanism that satisfies
  the non-negotiable "no pipeline rewrite" constraint in `00-overview.md` and
  `16-architecture-and-extensibility.md`.
- **No state transitions**: `Language` and `LanguageCatalog` are static/immutable
  for the lifetime of the app process — there is no create/update/delete flow for
  this ticket (that would only apply to a Phase-2-style catalog expansion, which is
  a code change, not a runtime mutation).
