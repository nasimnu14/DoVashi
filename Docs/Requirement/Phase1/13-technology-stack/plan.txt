# Plan — 13-technology-stack

Inputs: `grill-notes.md` (D1–D5), `domain-model.md` (T1–T3).

## Step 1 — Guard test
- `TechStackTest`: mandated plugins/libraries present; platform drivers/engines present; no Room.

## Step 2 — Honest re-runs
- `shared/build.gradle.kts`: declare `.gitignore`, the version catalog, the build file, `androidApp/src` and `iosApp` as inputs of `testAndroidHostTest`.

## Verify
`./gradlew :shared:testAndroidHostTest`; mutations: a `room` line in the catalog and a key-shaped string in `androidApp/src` each fail the suite.

## Acceptance criteria
1. The stack is documented as realised and pinned by a test.
2. Room cannot slip into shared code unnoticed.
3. Guard tests re-run when the files they read change.
