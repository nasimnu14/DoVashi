# Plan — 10-local-database

Inputs: `grill-notes.md` (D1–D7), `domain-model.md` (L1–L4).

## Step 1 — Migration setup
- `shared/build.gradle.kts`: `schemaOutputDirectory`, `verifyMigrations`.
- Generate `databases/1.db` with `generateCommonMainDoVashiDatabaseSchema`.
- README: how to change the schema.

## Step 2 — Schema conformance test
- `SchemaConformanceTest` (host): columns of both tables, the FK, the five status names.

## Verify
`./gradlew :shared:testAndroidHostTest :shared:iosSimulatorArm64Test :shared:verifySqlDelightMigration :androidApp:assembleDebug`; mutation: an extra column without migration fails verification.

## Acceptance criteria
1. Tables and statuses match doc 10, pinned by a test.
2. A schema change without a migration fails the build check.
3. Existing behaviour unchanged.
