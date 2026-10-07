# Plan — 01-language-model

## Context

Fresh KMP scaffold exists (`shared/` module, package root `com.example.dovashiapp`,
default Android Studio template — `Greeting.kt`/`Platform.kt` boilerplate, no
domain layer yet). No SQLDelight/Ktor/Koin wiring exists and none is needed for
this ticket. Grounded in [grill-notes.md](./grill-notes.md) and
[domain-model.md](./domain-model.md).

## Scope

Add the `Language` data model and a seeded, in-memory `LanguageCatalog` to
`commonMain`. No UI, no DB table, no DI, no wiring into other features.

## Steps

1. **Create package** `shared/src/commonMain/kotlin/com/example/dovashiapp/domain/model/`
   (matches the `commonMain/domain/model` layer from
   `16-architecture-and-extensibility.md`).

2. **`Language.kt`** — add:
   ```kotlin
   data class Language(
       val code: String,
       val name: String,
       val nativeName: String
   )
   ```

3. **`LanguageCatalog.kt`** — add:
   ```kotlin
   object LanguageCatalog {
       val all: List<Language> = listOf(
           Language(code = "en", name = "English", nativeName = "English"),
           Language(code = "zh", name = "Mandarin Chinese", nativeName = "普通话")
       )

       fun byCode(code: String): Language? = all.find { it.code == code }
   }
   ```

4. **Test package** `shared/src/commonTest/kotlin/com/example/dovashiapp/domain/model/`
   — `LanguageCatalogTest.kt` using `kotlin.test` (already a `commonTest`
   dependency per `shared/build.gradle.kts`):
   - `all` has exactly 2 entries.
   - `all` contains `Language("en", "English", "English")`.
   - `all` contains `Language("zh", "Mandarin Chinese", "普通话")`.
   - `byCode("en")` and `byCode("zh")` resolve to the matching entries.
   - `byCode("fr")` (or any unseeded code) returns `null`.

5. **Build check**: `./gradlew :shared:compileCommonMainKotlinMetadata` and
   `./gradlew :shared:testAndroidHostTest` (or the equivalent common/JVM test
   task) pass.

## Explicitly out of scope

- No SQLDelight table (catalog is in-memory per grill Q1).
- No UI screens, no Repository/UseCase wrapper beyond `LanguageCatalog` itself
  (nothing downstream consumes it yet — per grill Q4).
- No validation of `code` format (per grill Q2).

## Acceptance criteria

- `LanguageCatalog.all` contains exactly English (`en`) and Mandarin Chinese
  (`zh`), each with correct `name`/`nativeName`.
- `LanguageCatalog.byCode` resolves both codes and returns `null` for any
  other code.
- A new `Language` can be added to `LanguageCatalog.all` without touching any
  other file (satisfies the Phase 1 acceptance criteria in
  `01-language-model.md`).
- All new tests pass; no existing file is modified except adding the two new
  Kotlin files + one test file.
