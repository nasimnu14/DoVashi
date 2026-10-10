# Progress — 02 Home Screen

> /execute-plan · Plan: Docs/Requirement/Phase1/02-home-screen/plan.md · Started: 2026-10-08 02:00
> Status: ✅ Complete · Last updated: 2026-10-08 05:20
> Baseline: git HEAD `95204ba`; working tree already holds untracked project scaffold + ticket 01 code.

## Plan Steps
| # | Step | Status | Files | Verification |
|---|------|--------|-------|--------------|
| 0 | Preconditions (project + ticket 01 + record facts) | ✅ Done | plan.md addendum | Confirmed: `:shared`, `:androidApp`, `LanguageCatalog` present |
| 1 | Dependencies (SQLDelight, nav, Koin, datetime, serialization, coroutines-test) | ✅ Done | `gradle/libs.versions.toml`, root `build.gradle.kts`, `shared/build.gradle.kts` | `:androidApp:assembleDebug` → BUILD SUCCESSFUL |
| 2 | Schema (.sq files) | ✅ Done | `shared/src/commonMain/sqldelight/com/example/dovashiapp/data/database/{Conversation,Message}.sq` | `:shared:generateCommonMainDoVashiDatabaseInterface` → BUILD SUCCESSFUL |
| 3 | Domain models + repository interfaces | ⚠️ Done with deviation | `domain/model/{MessageStatus,Conversation,ConversationSummary,ConversationTitle}.kt`, `domain/repository/*.kt` | `:shared:compileAndroidMain` ✅ |
| 4 | Use cases | ✅ Done | `domain/usecase/{Observe…,CreateConversation,InsertMessage}UseCase.kt` | `:shared:compileAndroidMain` ✅ |
| 5 | Data impl + Android driver | ✅ Done | `data/repository/Sql{Conversation,Message}Repository.kt`, androidMain `data/database/AndroidSqlDriver.kt` | `--rerun-tasks :shared:compileAndroidMain` ✅ |
| 6 | Presentation logic (formatter, mappers, ViewModel) | ⚠️ Done with deviation | `presentation/home/{RelativeTimeFormatter,HomeUiState,HomeUiStateMapper,HomeViewModel}.kt` | compile ✅ |
| 7 | HomeScreen UI | ✅ Done | `presentation/home/HomeScreen.kt` | `:androidApp:assembleDebug` ✅ |
| 8 | Navigation + stubs | ✅ Done | `navigation/{Routes,AppNavHost,StubScreens}.kt`, `App.kt` | `:androidApp:assembleDebug` ✅ |
| 9 | DI (Koin) + app wiring | ✅ Done | `di/SharedModule.kt`, androidMain `di/AndroidPlatformModule.kt`, `androidApp/.../DoVashiApplication.kt`, manifest, `androidApp/build.gradle.kts` | `:androidApp:assembleDebug :androidApp:assembleRelease` ✅ |
| 10 | Debug seed | ✅ Done | `data/debug/DebugSeeder.kt`; trigger in `DoVashiApplication` (FLAG_DEBUGGABLE) | builds ✅; behaviour covered by manual TC-01/14/24 |
| 11 | Tests | ✅ Done | commonTest: `RelativeTimeFormatterTest`, `HomeUiStateMapperTest`, `HomeViewModelTest`, `CreateConversationUseCaseTest`, `testing/TestSupport.kt`; androidHostTest: `SqlRepositoriesTest` | `:shared:testAndroidHostTest --rerun` → 40 tests, 0 failures (8+8+2+3+11 new, 8 pre-existing) |

Status legend: ⏳ Pending · 🔄 In progress · ✅ Done · ⚠️ Done with issues · ⛔ Blocked · ⏭️ Skipped (already exists)

## Assumptions
- Verification commands: `./gradlew :shared:testAndroidHostTest` and `./gradlew :androidApp:assembleDebug` (from ticket 01's progress + plan addendum).
- Latest compatible versions are looked up at implementation time; the existing version catalog pins stay untouched.

## Step Log
### Step 1 — Dependencies · ✅ Done
- Changes: version catalog gained SQLDelight 2.4.1, Koin 4.2.2, kotlinx-datetime 0.8.0, kotlinx-serialization-core 1.11.0, kotlinx-coroutines 1.11.0 (core + test), JetBrains navigation-compose 2.9.2; plugins `sqldelight` and `kotlinSerialization` added to the root and `:shared`. `androidHostTest` gets the JDBC sqlite driver; `androidMain` gets the Android driver. SQLDelight database `DoVashiDatabase` in package `com.example.dovashiapp.data.database`.
- Verification: `./gradlew :androidApp:assembleDebug` → BUILD SUCCESSFUL (59 tasks).
- Deviation: navigation-compose pinned to 2.9.2 (latest stable) because 2.10.0 is still RC; lifecycle stays at the template's 2.11.0. Versions came from Maven Central metadata.

### Step 2 — Schema · ✅ Done
- Changes: `Conversation.sq` (table, `insertConversation`, `touchConversation`, `lastInsertRowId`, `countConversations`, `selectSummaries`), `Message.sq` (table with FK, index `message_conversation_created`, `insertMessage`).
- `selectSummaries` joins the latest message per conversation (ordered `createdAt DESC, id DESC`, limit 1) and counts all messages; ordered `updatedAt DESC, id DESC`.
- Verification: `./gradlew :shared:generateCommonMainDoVashiDatabaseInterface` → BUILD SUCCESSFUL.
- Deviation: none.

### Steps 3–10 — Implementation · see table
- Step 3 deviation: the domain `Message` class from the plan was **not** created. Nothing in this ticket reads one (`insertMessage` takes parameters, the summary query returns `ConversationSummary`), so it would be dead code. Ticket 04 adds it when it needs it.
- Step 5: no `expect`/`actual` (per plan addendum). `createAndroidSqlDriver` enables `foreign_keys` in `onConfigure`. `insertMessage` inserts first, then bumps `updatedAt`, in one `transactionWithResult`, so a failing FK rolls back both.
- Step 6 deviation: exactly one hour reads "1 hour ago" (plan literal "X hours ago" would print "1 hours ago"). This resolves the Open Question in test-cases.md; TC-30/TC-31 text adjusted accordingly. Future timestamps fall into "Just now" (resolves the TC-33 question).
- Step 6: `HomeViewModel` takes the catalog lookup as a function parameter defaulting to `LanguageCatalog::byCode`, so no catalog registration in Koin is needed.
- Step 7: message count is shown as "N messages" / "1 message" (plan said "a count").
- Step 8: the Chat stub also shows "Conversation #<id>" so TC-11 is manually checkable; the Create stub shows only "Hello World".
- Step 9: `KoinContext` was dropped (deprecated in Koin 4.2; `startKoin` sets up the Compose context). Koin is started in `DoVashiApplication`; `initKoin` lives in shared. **iOS:** `MainViewController` calls `App()` but nothing starts Koin or supplies a `SqlDriver` on iOS, so the iOS app would fail at runtime. Out of scope (Q5), listed under open issues.
- Step 10: seed = 3 conversations built through the real use cases with a private `SeedClock`: "recent" (2 COMPLETED messages, last 5 min ago), "empty" (no messages, 3 h ago, languages reversed to prove stored order is shown), "failed" (1 FAILED message without text, 30 h ago). Runs only when `FLAG_DEBUGGABLE` is set and the Conversation table is empty.

### Step 11 — Tests · ✅ Done
- 31 new tests. Repository tests use an in-memory JDBC SQLite database with `foreign_keys=true`, a fake clock and `UnconfinedTestDispatcher`.
- TC-22 limitation: the typed API cannot make `insertMessage` fail after the message row is written, so atomicity is proven only for the FK-failure case (nothing written, `updatedAt` unchanged). Real mid-transaction rollback relies on SQLDelight's `transactionWithResult`.
- Not covered by automation (needs a device): that `createAndroidSqlDriver` really turns foreign keys on, the debug seed, Compose rendering, navigation.
- Extra: `:shared:compileKotlinIosSimulatorArm64` started in the background to confirm `commonMain` still compiles for iOS (result recorded in the Final Report).

## Review Cycles
### Cycle 1 — Plan conformance & correctness · 2026-10-08
Fresh read-only subagent against plan, grill notes, domain model, test cases and the diff.
| # | Severity | Finding | File:line | Action | Result |
|---|----------|---------|-----------|--------|--------|
| 1 | Medium | Double-tap on **+** or a row pushes duplicate destinations (fails TC-37) | `navigation/AppNavHost.kt:22-23` | Fixed — `launchSingleTop = true` on both navigations | ✅ builds; behaviour is a manual check (TC-37) |
| 2 | Low/Medium | Debug seed ran asynchronously, so the first emission could be empty and flash the empty state; rows could arrive piecemeal | `DoVashiApplication.kt:18` | Fixed — seed now runs with `runBlocking` before the first screen (debug only) | ✅ builds; manual check TC-01/TC-23. Partial seed after a process kill mid-seed remains possible (milliseconds window, debug only, accepted) |
| 3 | Low | Progress log said the "failed" conversation had reversed languages; it is the "empty" one | `progress.md` | Fixed — log corrected | ✅ |
| 4 | Low | TC-22 only covered for FK failure, not a mid-transaction failure | `SqlRepositoriesTest.kt` | Not fixed — typed API cannot fail between the two statements; atomicity relies on `transactionWithResult` (already disclosed) | open, Low |
- Also found by this cycle's iOS check: `Dispatchers.IO` is internal on Kotlin/Native and broke `:shared:compileKotlinIosSimulatorArm64`. Fixed — the dispatcher moved from `SharedModule` to the Android platform module. ✅ iOS compile now succeeds.
- Discarded: none. Not reported by reviewer as defects but noted: `AppAndroidPreview` in `MainActivity.kt` calls `App()`, which needs Koin, so the Android Studio preview will fail (template code, outside plan; listed under open issues); `TimeZone` is captured once per ViewModel.
- Checked correct by reviewer: schema/index, summary query ordering and count, transactional `insertMessage`, I1/I3/I4/I5/I6/I7, formatter buckets, no language literals (AC6), Koin wiring, FAB label, `LazyColumn` keys.
- Summary: 4 found (+1 own) · 3 fixed (+1) · 1 open (Low).
- Verification: `:shared:testAndroidHostTest :androidApp:assembleDebug :androidApp:assembleRelease :shared:compileKotlinIosSimulatorArm64` → BUILD SUCCESSFUL, 40 tests, 0 failures.

### Cycle 2 — Edge cases & regressions · 2026-10-08
Fresh read-only subagent (code reading only, no Gradle) with six targeted questions.
| # | Severity | Finding | File:line | Action | Result |
|---|----------|---------|-----------|--------|--------|
| 1 | Medium | `MessageStatus.valueOf` throws on an unknown stored status inside the flow; Home would crash on every open | `SqlConversationRepository.kt:29` | Fixed — unknown values read as `FAILED` at the data boundary | ✅ new test `unknownStoredStatusReadsAsFailedInsteadOfCrashing`; mutation-checked (fails without the fix) |
| 2 | Medium | Tests could pass for the wrong reason: the newest row always also had the highest id | `SqlRepositoriesTest.kt` | Fixed — added `sortFollowsUpdatedAtNotInsertionOrder`, `lastMessageFollowsCreatedAtNotInsertionOrder` | ✅ pass |
| 3 | Low | If the clock moves backwards, `touchConversation` lowers `updatedAt` (can go below `createdAt`, conversation sinks) | `Conversation.sq:15` | Fixed — `MAX(updatedAt, :updatedAt)`. Note: invariant I2 now reads "bump to now, never backwards" | ✅ new test `updatedAtNeverMovesBackwardsWhenTheClockDoes`; mutation-checked |
| 4 | Low | `HomeViewModelTest` clock assertion was tautological | `HomeViewModelTest.kt` | Fixed — removed the empty assertion, kept label before/after re-emission | ✅ |
| 5 | Low | No tests for `DebugSeeder` or Dec 31 → Jan 1 | tests | Fixed — added `debugSeederCreatesVariedConversationsOnceThroughTheRealPath`, `yesterdayAcrossNewYear` | ✅ pass |
| 6 | Low | `zone` captured once per ViewModel; labels use a stale zone after a device timezone change until restart | `HomeViewModel.kt:18` | Not fixed — accepted, Phase 1 | open, Low |
| 7 | Low | Returning to Home within the 5 s `WhileSubscribed` window keeps old labels; after 5 s they refresh | `HomeViewModel.kt` | Not fixed — consistent with the Q23 decision (labels refresh only when data changes) | accepted |
| 8 | Low | Date label has no year; `COUNT(*)` rescans messages on each emission; seeder is compiled into release (never called there); seed is not atomic | various | Not fixed — spec / fine at Phase 1 scale / debug only | accepted |
- Discarded: none. Reviewer checked and found correct: month/day lookup, year rollover, exactly-24 h → "Yesterday", DST cases, no duplicate or dropped conversation in the join, index use, process death, seed idempotence, `FLAG_DEBUGGABLE` parsing.
- Not covered by automation (reviewer, agreed): the Android driver's `foreign_keys` callback, the 5 s re-subscription.
- Summary: 8 found · 5 fixed · 3 open or accepted (all Low).
- Verification: `:shared:testAndroidHostTest :androidApp:assembleDebug :androidApp:assembleRelease :shared:compileKotlinIosSimulatorArm64` → BUILD SUCCESSFUL; 46 tests, 0 failures.

### Cycle 3 — Integration, security & quality · 2026-10-08
Fresh read-only subagent (code reading only; lint and Gradle not run).
| # | Severity | Finding | File:line | Action | Result |
|---|----------|---------|-----------|--------|--------|
| 1 | Medium | Conversation text (and later audio paths) goes into Auto Backup in plaintext: `allowBackup="true"`, DB in the default `databases/` dir | `AndroidManifest.xml:5`, `AndroidSqlDriver.kt` | **Not fixed — product decision for the user** (see Open Issues) | open, Medium |
| 2 | Medium | No schema-migration setup (`schemaOutputDirectory`, `.sqm`, `verifyMigrations`); a later schema edit without a migration breaks installed apps | `shared/build.gradle.kts` | Not fixed — belongs to ticket 10 / first schema change; steps recorded in Open Issues | open, Medium |
| 3 | Low | `DebugSeeder` is compiled into release (never called there; gate is `FLAG_DEBUGGABLE`) | `DoVashiApplication.kt:13` | Not fixed — accepted | open, Low |
| 4 | Low | iOS gap (no Koin / `SqlDriver`) only documented in the progress log | `MainViewController.kt:5` | Not fixed — iosMain left untouched per Q5; recorded in Open Issues | open, Low |
| 5 | Low | Dead template code (`Greeting*`, `Platform*`, template resources) | `shared/.../Greeting*.kt` | Not fixed — pre-existing, outside the plan | open, Low |
| 6 | Low | FAB: child `Text("+")` stays in the merged node, so TalkBack can read "+" as well as "New conversation" | `HomeScreen.kt:43` | Fixed — `clearAndSetSemantics {}` on the "+" text | ✅ builds, tests green; announcement is a manual check (TC-16) |
| 7 | Low | Hardcoded UI strings and hand-pluralized "1 message" block localization; Loading state has no semantics; `ui-tooling` ships in release (template) | various | Not fixed — Phase 1 plain English per Q9 | open, Low |
- Discarded: none. Reviewer checked and found correct: no logging or PII calls, trust-boundary fallbacks, exported components (launcher only), LazyColumn keys and lifecycle-aware collection, all dependencies used, architecture layout matches doc 16, every [Auto] case has a matching test.
- Summary: 7 found · 1 fixed · 6 open (2 Medium, 4 Low).
- Verification: `:shared:testAndroidHostTest :androidApp:assembleDebug :androidApp:assembleRelease :shared:compileKotlinIosSimulatorArm64` → BUILD SUCCESSFUL; 46 tests, 0 failures.

**Stopping rule applied:** cycle 3 found no Critical/High issues → stopped after 3 cycles (the minimum).

## Final Report

**Outcome:** ✅ Done with open issues — all 11 plan steps implemented and verified; 3 review cycles complete with no Critical/High findings. 2 Medium items (backup of conversation text, migration setup) are deliberately left for the user / later tickets. Not yet run on a device or emulator.

### Changes
- Step 1: `gradle/libs.versions.toml`, `build.gradle.kts`, `shared/build.gradle.kts` (modified), `androidApp/build.gradle.kts` (modified: Koin + coroutines)
- Step 2: `shared/src/commonMain/sqldelight/com/example/dovashiapp/data/database/Conversation.sq`, `Message.sq` (created)
- Step 3: `domain/model/{MessageStatus,Conversation,ConversationSummary,ConversationTitle}.kt`, `domain/repository/{ConversationRepository,MessageRepository}.kt` (created)
- Step 4: `domain/usecase/{ObserveConversationSummaries,CreateConversation,InsertMessage}UseCase.kt` (created)
- Step 5: `data/repository/{SqlConversationRepository,SqlMessageRepository}.kt` (created); androidMain `data/database/AndroidSqlDriver.kt` (created)
- Step 6: `presentation/home/{RelativeTimeFormatter,HomeUiState,HomeUiStateMapper,HomeViewModel}.kt` (created)
- Step 7: `presentation/home/HomeScreen.kt` (created)
- Step 8: `navigation/{Routes,AppNavHost,StubScreens}.kt` (created); `App.kt` (replaced template content)
- Step 9: `di/SharedModule.kt` (created); androidMain `di/AndroidPlatformModule.kt` (created); `androidApp/.../DoVashiApplication.kt` (created); `AndroidManifest.xml` (modified: `android:name`)
- Step 10: `data/debug/DebugSeeder.kt` (created)
- Step 11: commonTest `testing/TestSupport.kt`, `presentation/home/{RelativeTimeFormatterTest,HomeUiStateMapperTest,HomeViewModelTest}.kt`, `domain/usecase/CreateConversationUseCaseTest.kt`; androidHostTest `data/repository/SqlRepositoriesTest.kt` (all created)
- Also: `Docs/Requirement/Phase1/02-home-screen/{plan.md,plan.txt}` (Step 0 facts appended), `test-cases.md` (singular-hour wording).

### Verification Run
| Command | Result |
|---------|--------|
| `./gradlew :shared:testAndroidHostTest --rerun` (final state) | BUILD SUCCESSFUL, 46 tests, 0 failures, 0 errors |
| `./gradlew :androidApp:assembleDebug :androidApp:assembleRelease` | BUILD SUCCESSFUL |
| `./gradlew :shared:compileKotlinIosSimulatorArm64` | First run FAILED (`Dispatchers.IO` is internal on Kotlin/Native, `SharedModule.kt`). Fixed by moving the dispatcher into the Android platform module. Rerun: BUILD SUCCESSFUL |
| Mutation check (temporarily reverted `MAX(...)` and the unknown-status fallback) | 2 targeted tests failed as expected; fixes restored, suite green |
| iOS tests, Android lint, instrumented / Compose UI tests, running the app on a device or emulator | **Not run** (no simulator in this environment; UI tests out of scope per Q15; lint not requested) |

### Deviations & Assumptions
- No domain `Message` class (nothing reads one in this ticket); ticket 04 adds it.
- "1 hour ago" singular; minutes stay "N min ago". Future timestamps read "Just now". `test-cases.md` updated to match.
- navigation-compose 2.9.2 (2.10 is still RC); all versions taken from Maven Central at implementation time.
- No `KoinContext` (deprecated in Koin 4.2). The IO dispatcher is provided by the Android platform module because `Dispatchers.IO` is internal on iOS.
- `touchConversation` now sets `updatedAt = MAX(updatedAt, now)` so it never moves backwards (invariant I2 refined).
- Unknown stored message status reads as `FAILED` instead of crashing.
- Chat stub also prints "Conversation #id" so TC-11 is checkable; message count shown as "N messages".
- Debug seed runs with `runBlocking` in `Application.onCreate` (debug only) so the list is populated before the first screen.
- Seed contents: 3 conversations, built through the real use cases with a private clock: "recent" (2 messages, last 5 min ago), "empty" (no messages, 3 h ago, reversed languages), "failed" (1 FAILED message, 30 h ago).

### Open Issues & Risks
- **[Medium — needs your decision] Backup of conversation text.** `android:allowBackup="true"` (template default) plus a DB in the default `databases/` directory means transcripts and translations can be uploaded to Google Drive backup in plaintext and restored onto other devices. Options: (a) `allowBackup="false"` / exclude the DB via `dataExtractionRules` + `fullBackupContent` (more private, history is lost on a new phone); (b) keep backup (history follows the user, privacy trade-off). Decide before audio recording (ticket 06) adds audio paths.
- **[Medium — for ticket 10 / first schema change] No migration setup.** Schema is version 1. Before any change to `Conversation.sq`/`Message.sq`: set `schemaOutputDirectory` in the `sqldelight` block, generate `1.db`, add `1.sqm` with each change, and enable `verifyMigrations`. Otherwise installed apps keep version 1 and queries fail with "no such column"; tests do not catch it because they build fresh in-memory databases.
- iOS: `MainViewController` calls `App()`, but nothing starts Koin or supplies a `SqlDriver` on iOS, so the iOS app would fail at runtime. It compiles. Out of scope (Q5).
- `DebugSeeder` ships in release builds (never called there; gate is `FLAG_DEBUGGABLE`).
- `AppAndroidPreview` in `MainActivity.kt` calls `App()`, which needs Koin, so the Android Studio preview will fail.
- Template leftovers unrelated to this ticket: `Greeting*`, `Platform*`, template resources, `ui-tooling` in release.
- Low: time zone captured once per ViewModel; time labels refresh only when data changes (Q23); date label has no year; hardcoded English strings; seed is not atomic (debug only); TC-22 mid-transaction rollback is not directly testable.
- Nothing is committed or pushed; everything is in the working tree.

### ⚠️ Critical Manual Checks
| # | What to check | Steps | Expected result | Why critical |
|---|---------------|-------|-----------------|--------------|
| 1 | Real Android SQLite driver: persistence and foreign keys (TC-13, TC-36; untested in automation) | 1. Install the debug build on a device or emulator, open it, let the seed run. 2. Force-stop, then reopen. 3. Enable airplane mode, force-stop, reopen. | The same 3 conversations load each time, no errors or network messages. | Automated tests use a JDBC driver; `createAndroidSqlDriver` and the `foreign_keys` callback are only exercised here. A wrong DB setup loses or corrupts all data. |
| 2 | Debug seed on a fresh install, with no empty-state flash (TC-01, TC-14, TC-23) | 1. Clear app data. 2. Launch the debug build and watch the first frames. 3. Relaunch. | List shows 3 rows ("5 min ago", "3 hours ago", "Yesterday" or a date), order newest first; "No conversations yet" never appears; relaunch does not duplicate rows. | First thing a developer sees; also proves the `runBlocking` seed finishes before the first read. |
| 3 | Release build never seeds (TC-15, TC-24) | 1. Build and install `assembleRelease` (signed as needed). 2. Launch, force-stop, relaunch. | Empty state "No conversations yet — tap + to start one", **+** visible, list stays empty. | Fake data in a real user's database would be a data-integrity bug. |
| 4 | Navigation and double-tap (TC-10, TC-11, TC-37) | 1. Tap **+**: "Hello World" appears; press Back. 2. Tap each row: stub shows the matching "Conversation #id"; Back. 3. Double-tap **+** and a row quickly; press Back once. | Correct stub and id each time; one Back returns to Home. | Wrong id means the Chat ticket opens the wrong conversation; stacked screens are a visible bug (`launchSingleTop` is only verified by manual test). |
| 5 | Row layout and text rendering (TC-02, TC-03, TC-25, TC-26, TC-35, TC-38) | 1. Check the app bar and FAB position. 2. Add a row with a 120-char title and 300-char preview (temporary seed edit) and one with 你好吗？. 3. Rotate; background and return. 4. Optionally seed 500 conversations and fling. | Single-line ellipsized rows, time and count stay visible, Chinese renders, list survives rotation, scrolling is smooth. | Layout and fonts are not covered by any automated test. |
| 6 | TalkBack on the FAB (TC-16) | 1. Enable TalkBack. 2. Focus the **+** button. | Announces "New conversation" only, not "+". | The accessibility fix is unverified on a device. |
| 7 | Live re-sort after a message is added (TC-12 is automated only) | There is no in-app way to add a message in this ticket. Use a temporary debug hook or wait for ticket 04. | The conversation moves to the top with no refresh. | Core acceptance criterion; the repository logic is tested, but the UI collecting it live is not. |
| 8 | Backup behaviour decision (see Open Issues) | Decide on `allowBackup`; if excluded, verify with `adb shell bmgr` or a device-to-device restore. | DB is (or is not) included in backups as decided. | Privacy of conversation content. |

## Pull Request
https://github.com/nasimnu14/DoVashi/pull/2

`feature/02-home-screen` → `phase_1`. **Merged** into `phase_1` by the user (merge commit `b100ec9`).
