# Test Cases — 01-language-model

> Source: Docs/Requirement/Phase1/01-language-model/plan.md
> Type: Feature (domain model) · Generated: 2026-10-08

## Scope

This covers the `Language` data model and the seeded, in-memory
`LanguageCatalog` added under `shared/src/commonMain/kotlin/com/example/dovashiapp/domain/model/`.
There is no UI, database table, or wiring into other features in this ticket
(confirmed in `plan.md`'s "Explicitly out of scope"), so there is no screen to
click through. Verification here is done by running the module's test suite
and, where noted, inspecting values directly (IDE debugger / "Evaluate
Expression", or a scratch `main()`), since that is the only way a human can
observe this layer before any consumer wires it up.

## Open Questions

- None — plan, grill-notes, and domain-model for this ticket left no
  ambiguity to ground against the codebase (it's a new, isolated package with
  no existing conflicting code).

## Test Cases

### Positive / Happy Path

| ID | Title | Preconditions | Steps | Expected Result | Priority |
|---|---|---|---|---|---|
| TC-01 | Catalog seeds exactly two languages (Acceptance: catalog contains only English and Mandarin Chinese) | `shared` module builds successfully | 1. Run `./gradlew :shared:testAndroidHostTest` (or open `LanguageCatalogTest` in the IDE and run it)<br>2. Observe the test report | All tests in `LanguageCatalogTest` pass, including the assertion that `LanguageCatalog.all.size == 2` | High |
| TC-02 | English entry has correct fields | `shared` module builds successfully | 1. In the IDE, open `LanguageCatalog.kt` or set a breakpoint/debugger "Evaluate Expression" on `LanguageCatalog.all`<br>2. Inspect the entry where `code == "en"` | `code = "en"`, `name = "English"`, `nativeName = "English"` | High |
| TC-03 | Mandarin Chinese entry has correct fields | `shared` module builds successfully | 1. Using the IDE debugger/Evaluate Expression on `LanguageCatalog.all`<br>2. Inspect the entry where `code == "zh"` | `code = "zh"`, `name = "Mandarin Chinese"`, `nativeName = "普通话"` | High |
| TC-04 | `byCode` resolves the English entry | `shared` module builds successfully | 1. Using Evaluate Expression, call `LanguageCatalog.byCode("en")` | Returns a non-null `Language` equal to `Language("en", "English", "English")` | High |
| TC-05 | `byCode` resolves the Mandarin Chinese entry | `shared` module builds successfully | 1. Using Evaluate Expression, call `LanguageCatalog.byCode("zh")` | Returns a non-null `Language` equal to `Language("zh", "Mandarin Chinese", "普通话")` | High |
| TC-06 | New language can be added without touching other files (Acceptance: catalog extensible) | `shared` module builds successfully | 1. Temporarily add a third `Language` entry (e.g. `code = "fr"`) to `LanguageCatalog.all` only<br>2. Re-run `LanguageCatalogTest`<br>3. Revert the temporary addition | The build and all existing tests still pass with no changes needed to `Language.kt`, test files, or any other package | Medium |

### Negative

| ID | Title | Preconditions | Steps | Expected Result | Priority |
|---|---|---|---|---|---|
| TC-07 | `byCode` returns null for an unseeded code | `shared` module builds successfully | 1. Using Evaluate Expression, call `LanguageCatalog.byCode("fr")` (or any code not in the seed list) | Returns `null` — no exception thrown | High |
| TC-08 | `byCode` returns null for an empty string | `shared` module builds successfully | 1. Using Evaluate Expression, call `LanguageCatalog.byCode("")` | Returns `null` — no exception thrown | Low |

### Edge Cases

| ID | Title | Preconditions | Steps | Expected Result | Priority |
|---|---|---|---|---|---|
| TC-09 | `byCode` is case-sensitive | `shared` module builds successfully | 1. Using Evaluate Expression, call `LanguageCatalog.byCode("EN")` (uppercase) | Returns `null` — the seeded code is lowercase `"en"` and no case-folding is implemented (per plan: no validation/normalization of `code`) | Low |
| TC-10 | `all` is stable across repeated access | `shared` module builds successfully | 1. Using Evaluate Expression, call `LanguageCatalog.all` twice in the same session | Both calls return the same 2 entries in the same order (`en`, `zh`) | Low |

## Critical Manual Checks

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
