# Plan — 09-pronunciation-romanization

> **Revised during review (see progress.md):**
> - The Latin rule is an allow-list (`Script.isLatinLetter`): Latin with diacritics, IPA/phonetic letters, Latin Ext-C/D/E and full-width Latin.
> - Letters of any other script, including supplementary Han, drop the Reading; emoji are not letters.
> - At least one letter is required.
> - A blank `readingSystem` is rejected.

Inputs: `grill-notes.md` (D1–D7), `domain-model.md` (R1–R4).

## Step 1 — Metadata
- `Language`: replace the `requiresReading` constructor parameter with `readingSystem: String? = null`; add `val requiresReading: Boolean get() = readingSystem != null`.
- `LanguageCatalog`: `zh` gets `readingSystem = "Hanyu Pinyin with tone marks"`; `en` none.
- `GLOSSARY.md`: refine **Reading**.

## Step 2 — Reading rule
- `domain/model/Reading.kt`: after the existing checks, return null if any letter in the Reading has a non-Latin `Script` (`Script.of(c) != null && != LATIN`).

## Step 3 — Request and prompt
- `TranslationRequest(sourceLanguage, targetLanguage, transcribedText, readingSystem: String?)` + `val readingRequired get() = readingSystem != null`.
- `TranslateMessageUseCase`: `readingSystem = languageByCode(target)?.readingSystem`.
- `OpenAiTranslationService`: user fields add `"pronunciationSystem": readingSystem` (or null). The instruction: "Also provide an English-readable pronunciation of the translation in Latin script using <system>, in englishReading."

## Step 4 — Tests
- Update the existing tests for the new constructor shape (`readingSystem = "…"` instead of `requiresReading = true`).
- `LanguageCatalogTest`: `zh` system set; only `zh` requires a Reading.
- `ReadingTest`: "Nǐ hǎo ma?" kept; "你好" dropped; "Ni hao 你" dropped; "Genki desu ka?" kept; digits/punctuation fine.
- `OpenAiTranslationServiceTest`: the user fields carry `pronunciationSystem`; the instruction names it; not required → null field.
- `TranslateMessageUseCaseTest`: the request carries the target's system.
- Verify: `./gradlew :shared:testAndroidHostTest :shared:iosSimulatorArm64Test :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64`.

## Acceptance criteria
1. A Reading is stored only when the target Language's metadata names a Reading system (Phase 1: Mandarin Chinese only), and only if it is Latin script.
2. The flag is metadata (`readingSystem`), with no code comparisons; Phase 2 adds entries only.
3. OpenAI is asked for the target's specific Reading system.
4. Tests pass on host and iOS simulator; builds succeed; Chat still shows the italic Reading under Mandarin Chinese translations.
