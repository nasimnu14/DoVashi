# Grill Notes — 01-language-model

Decisions reached during the grilling interview, grounded against the Phase 1
requirement and the existing `10-local-database.md` / `16-architecture-and-extensibility.md`
docs (no app code exists yet — this is a greenfield repo). Q6–Q7 came from a later
`domain-modeling` pass; the shared vocabulary it produced lives in the root
[GLOSSARY.md](../../../../GLOSSARY.md).

## Q1 — Catalog storage mechanism

**Question:** Should the Phase 1 catalog be an in-memory seeded object rather than a SQLDelight table?

**Decision:** Yes. `object LanguageCatalog` living in `commonMain/domain/model`, exposing a seeded `List<Language>`. No DB table.

**Rationale:** `10-local-database.md` already stores `language1Code`/`language2Code`/`sourceLanguage`/`targetLanguage` as plain string codes on `Conversation`/`Message` — there's no `Language` foreign key or table in that schema. An in-memory object is the simplest-complete fit, and Phase 2 "inserts new rows" by adding entries to the same list.

## Q2 — Language.code validation

**Question:** Should `code` stay a plain `String` with no format enforcement?

**Decision:** Yes. Plain `String`, no ISO-639-1 regex, no value-class wrapper.

**Rationale:** The requirement fixes the data class to `code`/`name`/`nativeName` and states no validation rule. Don't invent a constraint it doesn't ask for.

## Q3 — Accessor API surface

**Question:** Should this ticket define lookup helpers now, or leave them to the first consumer?

**Decision:** Include both now: `LanguageCatalog.all: List<Language>` and `LanguageCatalog.byCode(code: String): Language?`.

**Rationale:** Nearly every downstream feature (create-conversation, detection, chat screen) needs to resolve a code to a `Language`. Defining the lookup once here avoids each consumer inventing its own.

## Q4 — Ticket scope boundary

**Question:** Does this ticket stop at model + catalog + accessor, with no UI/DI/detection wiring?

**Decision:** Yes. Model-only. Integration into Home Screen, Create Conversation, Language Detection, etc. are separate tickets.

## Q5 — Test coverage expectation

**Question:** Should acceptance tests check only (a) catalog has exactly `en` and `zh` with correct fields, and (b) `byCode` resolves both and returns `null` for unknown codes?

**Decision:** Yes — exactly those two checks. Nothing about immutability, ordering, or thread-safety (not required by the spec).

## Q6 — Short display name

**Question:** The Home screen example labels a row "English ↔ Mandarin", but `Language` has no short-name field. Is that example illustrative, so rows show the full name?

**Decision:** Yes. Rows show `Language.name` ("English ↔ Mandarin Chinese"). The model stays `code`/`name`/`nativeName`.

**Rationale:** The requirement fixes the three fields, and `10-local-database.md` already titles the conversation "English ↔ Mandarin Chinese".

## Q7 — Script for `zh`

**Question:** Is `zh` Mandarin written in Simplified characters?

**Decision:** Yes. `nativeName` = "普通话" (Simplified; Traditional would be "普通話"). Translation (#08) and Pronunciation (#09) target Simplified characters with pinyin.

**Rationale:** No doc named a script; the planned `nativeName` already implied Simplified.

## Summary

The ticket delivers:
- `Language` data class (`code`, `name`, `nativeName`) in `commonMain/domain/model`.
- `LanguageCatalog` object seeded with exactly English (`en`) and Mandarin Chinese (`zh`), exposing `all` and `byCode`.
- No persistence layer, no UI, no wiring into other features.
- Tests covering catalog contents and `byCode` lookup (hit + miss).
