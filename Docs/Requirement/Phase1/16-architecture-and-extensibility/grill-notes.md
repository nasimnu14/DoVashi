# Grill Notes — 16-architecture-and-extensibility

Source: `Docs/Requirement/Phase1/16-architecture-and-extensibility.md` · 2026-10-11. **No interview was run** (user instruction); decisions are Claude's, marked **(self)**.

Audit: the module layout matches the spec (`data/{network,repository}` + the generated `data/database` schema package, `domain/{model,repository,usecase}`, `presentation/{home,conversation,createconversation}`, `audio`, `navigation`, `di`; `androidMain` and `iosMain` each with `audio`, `permissions`, `storage` after ticket 14). Extras: `domain/service` (the two ports ticket 15 requires), `permissions` (common interface), `data/debug`. The target rule is `targetLanguageFor(source, conversation)` (language1 ↔ language2, no `if English`). Language codes appear only in `LanguageCatalog` and Whisper's name→code table. Layering is clean: `domain` imports nothing from `data/presentation/di/navigation`; `commonMain` has no platform imports.

| # | Question | Resolved answer |
|---|----------|-----------------|
| D1 | Anything to build? | No product code. The "definition of done" (Phase 2 by metadata only) already holds; the risk is regression. (self) |
| D2 | How is it protected? | `ArchitectureTest` (host) over the source tree: required folders; domain free of outer layers; presentation never imports `data`; `commonMain` free of `android.`/`java.`/`platform.`/Room; OpenAI adapters named only in `data/network/openai` and `di`; no `"en"`/`"zh"` literals outside `LanguageCatalog.kt` and `WhisperLanguages.kt`. (self) |
| D3 | Comments? | Ignored by the scanner (they legitimately say "English", "zh"). (self) |
| D4 | Phase 2 proof beyond the scan? | Existing pipeline tests already run on synthetic languages `aa`/`bb` in both directions with a Reading system on one side (tickets 06–09), so nothing depends on the two shipped languages. (self) |
| D5 | Spec differences kept? | `domain/service` and top-level `permissions` are deliberate; the spec lists neither but tickets 15 and 14 require them. (self) |
| D6 | Template leftovers (`Greeting`, `Platform`)? | Left; unrelated to architecture. (self) |
