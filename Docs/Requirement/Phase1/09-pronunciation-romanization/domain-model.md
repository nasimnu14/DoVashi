# Domain Model — 09-pronunciation-romanization

| Concept | Layer | Change |
|---|---|---|
| `Language.readingSystem: String?` | domain/model | New, non-blank when set. `zh` = "Hanyu Pinyin with tone marks", `en` = null |
| `Language.requiresReading` | domain/model | Now computed: `readingSystem != null` (no longer a constructor parameter) |
| `readingFor(target, reading, languageByCode)` | domain/model | Keeps the Reading only if the target requires one, it has at least one letter, and every letter passes `Script.isLatinLetter` (Latin + IPA/phonetic + Latin extensions + full-width Latin). Emoji are not letters; other supplementary characters are non-Latin |
| `TranslationRequest.readingSystem: String?` | domain/service | Replaces the boolean; `readingRequired` is derived |
| `TranslateMessageUseCase` | domain/usecase | Passes `languageByCode(target)?.readingSystem` |
| `OpenAiTranslationService` | data/network/openai | Prompt fields add `pronunciationSystem`; the instruction names it |

## Invariants
| ID | Invariant | Enforced by |
|---|---|---|
| R1 | A Reading exists only for targets whose metadata names a Reading system | `readingFor` + metadata |
| R2 | A stored Reading is Latin script only | `readingFor` |
| R3 | No `if (target == "zh")` anywhere; Phase 2 adds `readingSystem` entries only | Data-driven flag |
| R4 | OpenAI is told which Reading system to use | Request + prompt |
