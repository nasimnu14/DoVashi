# Grill Notes — 13-technology-stack

Source: `Docs/Requirement/Phase1/13-technology-stack.md` · 2026-10-11. **No interview was run** (user instruction); decisions are Claude's, marked **(self)**.

Audit of what exists: KMP (`kotlinMultiplatform`, Android + iosArm64/iosSimulatorArm64); Compose Multiplatform shared UI; SQLDelight for the shared database (Android driver, native driver); Ktor Client (OkHttp / Darwin engines, ContentNegotiation, Logging) for OpenAI; coroutines + Flow throughout; kotlinx.serialization for the OpenAI JSON; Koin for DI. Business logic, data, use cases, repositories, OpenAI networking, state management and nearly all UI are in `commonMain`. No Room anywhere.

| # | Question | Resolved answer |
|---|----------|-----------------|
| D1 | Anything to build? | No. Every mandated technology is already in use and the iOS side is complete after ticket 14. This ticket records the audit and keeps it true. (self) |
| D2 | How is it kept true? | `TechStackTest` (host): the shared build uses each mandated plugin/library (including both platform drivers and engines) and neither the catalog nor the build mentions Room. (self) |
| D3 | A guard that doesn't re-run is no guard | Mutation check showed the test task stayed up to date when only the version catalog changed (undeclared inputs). `shared/build.gradle.kts` now declares the guarded files as inputs of `testAndroidHostTest`; this also fixes the same gap for ticket 15's secrets scan (`androidApp/src`, `iosApp`, `.gitignore`). (self) |
| D4 | Koin "if DI is required"? | Required (view models, use cases, platform bindings); lightweight, KMP-compatible. (self) |
| D5 | Template leftovers (`Greeting`, `Platform`)? | Left alone: used by the template tests, unrelated to the stack. (self) |
