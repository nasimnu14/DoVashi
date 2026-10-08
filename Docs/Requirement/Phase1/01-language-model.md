# Feature: Language Model

## Phase 1 scope

Seed exactly two `Language` entries:

- English — code `en`
- Mandarin Chinese — code `zh`

## Requirements

- Represent languages with a reusable data model, not hard-coded conditions:

  ```kotlin
  data class Language(
      val code: String,
      val name: String,
      val nativeName: String
  )
  ```

- The language list/catalog must be data (a seeded list, table, or similar),
  not `if`/`else` branches naming specific languages.
- Nothing downstream (UI, detection, translation, pronunciation) may assume
  there are only two possible languages in the *catalog* — only that a given
  *conversation* has exactly two. Even in Phase 1, model the catalog as
  extensible so Phase 2 only inserts new rows.

## Acceptance criteria

- A new `Language` can be added to the catalog without touching detection,
  translation, or UI logic.
- The catalog exposed to the app in Phase 1 contains only English and
  Mandarin Chinese.
