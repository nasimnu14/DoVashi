# Progress — 16 Architecture & Extensibility

> /execute-plan · Plan: Docs/Requirement/Phase1/16-architecture-and-extensibility/plan.md · Started: 2026-10-11 06:20
> Status: ✅ Complete · Baseline: `02d698e` (ticket 13), green.

## Plan Steps
| # | Step | Status | Files | Verification |
|---|------|--------|-------|--------------|
| 1 | Architecture guard | ✅ Done | `androidHostTest/.../architecture/ArchitectureTest.kt` | 6 tests ✅; 4 mutations fail as intended |

## Step Log
- The audit found no violations: the six checks passed on the first run.
- Mutation: first attempt used non-compiling violators (build failed before the test ran, so it proved nothing); redone with compiling files `domain/model/ZzMut.kt` (imports `data.*` and an OpenAI adapter, contains `"zh"`) and `presentation/home/ZzMut2.kt` → 4 of 6 checks FAILED (domain layering, presentation layering, OpenAI ports, hard-coded language). Files removed. The platform-import check is backed by the compiler (commonMain can't resolve `android.*`) and was not mutated separately.
- Verification: `./gradlew :shared:testAndroidHostTest :shared:iosSimulatorArm64Test :androidApp:assembleDebug :androidApp:assembleRelease` → BUILD SUCCESSFUL: 282 host tests, 229 iOS-simulator tests, 0 failures.

## Review Cycles (self-review)
| Cycle | Lens | Findings | Result |
|---|---|---|---|
| 1 | Conformance | All doc-16 rules covered; spec folders `database` is the generated SQLDelight package (checked via the schema folder) | clean |
| 2 | Edge cases | Scanner ignores comment/KDoc lines (they mention "English"/"zh"); `Whisper` name table is allowed to hold language names since it is provider metadata; string-literal rule matches only exact `"en"`/`"zh"` | clean |
| 3 | Quality | Test runs on the host JVM only (reads sources); inputs already declared via `shared/src` being the module's source set | clean |

## Final Report
**Outcome:** ✅ Done. No product code changed; the architecture rules are now executable.

### ⚠️ Critical Manual Checks
| # | What to check | Steps | Expected | Why critical |
|---|---|---|---|---|
| 1 | Phase 2 dry run | Add one more `Language` to `LanguageCatalog` (with scripts / Reading system) and a Conversation with it | Works end to end with no other edit | The point of doc 16; only a live run with real STT/translation proves provider support |
