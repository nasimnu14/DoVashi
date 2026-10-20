# Plan — 16-architecture-and-extensibility

Inputs: `grill-notes.md` (D1–D6), `domain-model.md` (X1–X7).

## Step 1 — Architecture guard
- `androidHostTest/.../architecture/ArchitectureTest.kt` with the six checks in D2.

## Verify
`./gradlew :shared:testAndroidHostTest :shared:iosSimulatorArm64Test :androidApp:assembleDebug :androidApp:assembleRelease`; mutations: violating files (domain→data import, presentation→data import, OpenAI adapter named in domain, `"zh"` literal) fail four checks.

## Acceptance criteria
1. The structure and layering rules of doc 16 are executable.
2. Hard-coding a language or naming an OpenAI adapter outside DI fails the build.
3. No product behaviour changes.
