# Domain Model — 16-architecture-and-extensibility

Dependency direction (commonMain): `presentation → domain ← data`; `di` wires all; `navigation` composes presentation; `audio`/`permissions` are platform-facing interfaces used by domain and implemented in `androidMain`/`iosMain`.

## Invariants
| ID | Invariant | Enforced by |
|---|---|---|
| X1 | Folder structure matches the spec | `ArchitectureTest.moduleStructureMatchesTheSpec` |
| X2 | `domain` imports no outer layer | `domainDoesNotDependOnOuterLayers` |
| X3 | `presentation` never imports `data` | `presentationDoesNotReachIntoData` |
| X4 | `commonMain` is platform-free | `commonCodeUsesNoPlatformApis` + compiler |
| X5 | OpenAI is behind `TranslationService` / `SpeechToTextService` | `openAiIsReachableOnlyThroughItsPorts` |
| X6 | No language is hard-coded; target = other of the pair | `noLanguageIsHardCodedOutsideTheCatalogMetadata`, `targetLanguageFor` |
| X7 | Phase 2 = Language Catalog rows + provider support; no schema, detection or contract change | X6 + tickets 06–10 tests on synthetic languages |
