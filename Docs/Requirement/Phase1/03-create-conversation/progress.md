# Progress — 03 Create Conversation

> /execute-plan · Plan: Docs/Requirement/Phase1/03-create-conversation/plan.md · Started: 2026-10-10 16:00
> Status: ✅ Complete · Last updated: 2026-10-10 17:25
> Baseline: git HEAD `b100ec9` on `feature/03-create-conversation` (from `phase_1`).

## Plan Steps
| # | Step | Status | Files | Verification |
|---|------|--------|-------|--------------|
| 0 | Preconditions | ✅ Done | — | Tickets 01–02 present; baseline `:shared:testAndroidHostTest` green (46 tests) |
| 1 | UI state | ✅ Done | `presentation/createconversation/CreateConversationUiState.kt` | compile ✅ |
| 2 | ViewModel | ✅ Done | `presentation/createconversation/CreateConversationViewModel.kt` | compile ✅ |
| 3 | Screen | ✅ Done | `presentation/createconversation/CreateConversationScreen.kt`, `composeResources/drawable/ic_arrow_back.xml` | `:androidApp:assembleDebug` ✅ |
| 4 | Navigation + DI | ✅ Done | `navigation/AppNavHost.kt`, `navigation/StubScreens.kt`, `di/SharedModule.kt` | `:androidApp:assembleDebug` ✅ |
| 5 | Tests | ✅ Done | commonTest `CreateConversationViewModelTest.kt` (11), `testing/TestSupport.kt`; androidHostTest `SqlRepositoriesTest.kt` (+1) | `:shared:testAndroidHostTest` → 58 tests, 0 failures; `:shared:compileKotlinIosSimulatorArm64` ✅ |

Status legend: ⏳ Pending · 🔄 In progress · ✅ Done · ⚠️ Done with issues · ⛔ Blocked · ⏭️ Skipped (already exists)

## Assumptions
- Verification commands: `./gradlew :shared:testAndroidHostTest`, `./gradlew :androidApp:assembleDebug`, `./gradlew :shared:compileKotlinIosSimulatorArm64` (from ticket 02).
- User instruction (2026-10-10): don't wait for permission; record decisions instead of asking.

## Step Log
### Steps 1–4 — Implementation · ✅ Done
- Step 1: `CreateConversationUiState` with computed `isSameLanguage`/`canStart` and `defaultSelection()`.
- Step 2: `CreateConversationViewModel(createConversation, languages = LanguageCatalog.all)`. `start()` is single-flight via `canStart` (`isCreating`). It stays `isCreating = true` after success so the button remains disabled until navigation. It rethrows `CancellationException` and shows the error for any other `Exception`.
- Step 3: stateless screen; `ExposedDropdownMenuBox` + read-only `OutlinedTextField` (`menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)`); the Back icon is a vector drawable (generated accessor `dovashiapp.shared.generated.resources.Res`), mirrored in RTL via `graphicsLayer`. The content column scrolls vertically so small screens and landscape still reach the button.
- Step 4: `AppNavHost` navigates in a `LaunchedEffect(createdConversationId)` with `popUpTo<CreateConversationRoute> { inclusive = true }` and `launchSingleTop`, then calls `onNavigationHandled()`. `CreateConversationStubScreen` was removed. Koin: `viewModel { CreateConversationViewModel(get()) }`.
- Verification: `./gradlew :androidApp:assembleDebug` → BUILD SUCCESSFUL.
- Deviation: added `verticalScroll` to the form (not in the plan) so it stays usable in landscape and with large fonts.

### Step 5 — Tests · ✅ Done
- `FakeConversationRepository` gained a default empty `summaries` flow, `failNextCreate` and a `gate` (`CompletableDeferred`) to suspend creation.
- 11 ViewModel tests (TC-07 order, TC-10/11 duplicate, TC-14 failure + retry, TC-16 concurrent start, TC-20/21 catalog shapes, plus defaults and navigation-handled) and 1 SQLite repository test (TC-07: stored order both ways, titles, `createdAt == updatedAt`).
- Verification: `./gradlew :shared:testAndroidHostTest` → 58 tests, 0 failures, 0 errors; `./gradlew :shared:compileKotlinIosSimulatorArm64` → BUILD SUCCESSFUL.

## Review Cycles
Cycles 1 and 2 ran **in parallel** on the same diff, with different lenses and separate fresh read-only subagents. Their fixes were applied together. Cycle 3 then reviewed the whole diff including those fixes.

### Cycle 1 — Plan conformance & correctness · 2026-10-10 16:30
| # | Severity | Finding | File:line | Action | Result |
|---|----------|---------|-----------|--------|--------|
| 1 | Medium | Back pressed while the insert finishes: the effect could still `navigate(ChatRoute)` from the popped form. `popUpTo` then silently does nothing, so Chat opens on top of Home after the user pressed Back | `navigation/AppNavHost.kt` (Create route) | Fixed — navigate only when `navController.currentBackStackEntry?.id == entry.id`. Back icon and dropdowns are disabled while `isCreating`. A Start → Back insert that has already committed still leaves the conversation on Home (accepted, now TC-24) | ✅ builds; behaviour is a manual check (TC-24) |
| 2 | Low | The duplicate-language test could pass for the wrong reason (the use case's `require` failure caught as an error) | `CreateConversationViewModelTest.kt` | Fixed — also asserts `errorMessage == null` and `!isCreating` | ✅ |
| 3 | Low | Missing precondition asserts; `selectLanguage2` clearing the error not tested | same | Fixed — precondition asserts added; `selectingEitherLanguageClearsTheError` | ✅ |
| 4 | Low | `start()` check-then-update not atomic | `CreateConversationViewModel.kt` | Fixed — `getAndUpdate` atomic check-and-set | ✅ |
| 5 | Low | `status.md` said 7 [Auto] (actually 6); progress path | `status.md` | Fixed — counts corrected; progress file is moved into the ticket folder at the end of Step 8, as the workflow says | ✅ |
| 6 | Low | Ticket-02 bookkeeping (`status.md`/`progress.md` marking PR #2 merged) is in the diff | `02-home-screen/*` | Not a defect — committed as a separate commit on this branch, like `95204ba` did for ticket 01 | noted |
- Discarded: none.
- Summary: 6 found · 5 fixed · 1 noted.

### Cycle 2 — Edge cases & regressions · 2026-10-10 16:30
| # | Severity | Finding | File:line | Action | Result |
|---|----------|---------|-----------|--------|--------|
| 1 | Low | Start then Back: the row is still committed | `AppNavHost.kt`, VM | Accepted and documented (TC-24): the insert takes milliseconds and the conversation appears on Home | accepted |
| 2 | Low | Effect can navigate from a popped entry | `AppNavHost.kt` | Fixed (same fix as Cycle 1 #1) | ✅ |
| 3 | Low | A `CancellationException` that isn't scope cancellation (e.g. a future `withTimeout`) was rethrown, leaving the button disabled for good | VM `start()` | Fixed — `catch (e: Exception) { ensureActive(); …error… }` | ✅ new test `leavingTheScreenDuringCreateShowsNoErrorAndCreatesNothing`; mutation-checked (removing `ensureActive()` fails it) |
| 4 | Low | Exception swallowed without logging | VM | Not fixed — the project has no logger; ticket 02 review recorded "no logging" as intended. Suggest adding one when the network layer lands | open, Low |
| 5 | Low | Process death mid-create: row committed, form resets, duplicate possible | VM | Accepted (D16); added as TC-25 | accepted |
| 6 | Low | Cancellation path untested | tests | Fixed — see #3 | ✅ |
- Discarded: none. Reviewer confirmed no regression on the Home route or Chat stub, existing tests are unaffected by the fake's new defaults, and rotation mid-create still navigates once.
- Summary: 6 found · 3 fixed · 3 accepted/open (Low).
- Incident: the mutation-check cleanup ran `git checkout -- .` and reverted the uncommitted edits to the 5 tracked code files and the 2 ticket-02 docs. All edits were reapplied from the session record (including the Cycle 1 fix) and the full suite was re-run.
- Verification: `./gradlew :shared:testAndroidHostTest :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64` → BUILD SUCCESSFUL; 59 tests, 0 failures.

### Cycle 3 — Integration, security & quality · 2026-10-10 17:10
Fresh read-only subagent; it also re-checked the cycle 1–2 fixes.
| # | Severity | Finding | File:line | Action | Result |
|---|----------|---------|-----------|--------|--------|
| 1 | Medium | The popped form still takes taps during NavHost's default 700 ms exit fade: Back, then a quick Start, inserts a conversation (breaks AC5) | `AppNavHost.kt` | Fixed — `onStartClick`/`onBackClick` wrapped in `dropUnlessResumed { }` (lifecycle-runtime-compose) | ✅ builds; manual check (TC-08/TC-24) |
| 2 | Medium | Error texts not announced by screen readers; fields not marked as errors | `CreateConversationScreen.kt` | Fixed — `ErrorText` with `liveRegion = Polite`; both fields `isError = isSameLanguage` (refines D17) | ✅ builds; manual TalkBack check |
| 3 | Low | ViewModel accepted selections while creating | VM | Fixed — ignored while `isCreating` | ✅ new test `selectionIsIgnoredWhileCreating` |
| 4 | Low | Greyed-out dropdowns/Back flicker during the exit fade | Screen | Fixed — the visual disabling added in cycle 1 was removed; #1 and #3 now enforce it | ✅ |
| 5 | Low | Hand-rolled RTL mirroring; the resources parser supports `autoMirrored` | Screen, `ic_arrow_back.xml` | Fixed — `android:autoMirrored="true"`, mirroring code removed | ✅ builds; RTL is a manual check |
| 6 | Low | Stale `expanded` after disable | Screen | Moot after #4 | — |
| 7 | Low | Test name overclaimed "CreatesNothing" | test | Fixed — renamed `leavingTheScreenDuringCreateShowsNoError`, with a comment pointing to TC-24 | ✅ |
| 8 | Low | `?: return` after `isCreating = true` could leave the screen stuck if `canStart` changed later | VM | Fixed — `checkNotNull` | ✅ |
| 9 | Low | No automated test for the navigation effect or guard; no Koin graph check | `AppNavHost.kt` | Not fixed — would need the Compose UI-test dependency (out of scope, same as ticket 02 Q15); covered by manual checks | open, Low |
| 10 | Low | First runtime Compose resource; iOS never run | Screen | Not fixable here — Critical Manual Check #5 | open |
- Discarded: none. Reviewer verified `getAndUpdate`, `ensureActive()`, the guard across configuration change, `menuAnchor` semantics in material3 1.12.0-alpha03, no secrets/PII, and consistency with Home's patterns.
- Summary: 10 found · 7 fixed · 1 moot · 2 open (Low).
- Verification: `./gradlew :shared:testAndroidHostTest :androidApp:assembleDebug :androidApp:assembleRelease :shared:compileKotlinIosSimulatorArm64` → BUILD SUCCESSFUL; 60 tests, 0 failures.

**Stopping rule applied:** cycle 3 found no Critical/High issues → stopped after 3 cycles.

## Final Report

**Outcome:** ✅ Done with open issues (Low only). All 5 plan steps are implemented and verified, and 3 review cycles found no Critical/High issues. Not yet run on a device, emulator or simulator.

### Changes
- Step 1: `shared/src/commonMain/kotlin/com/example/dovashiapp/presentation/createconversation/CreateConversationUiState.kt` (created)
- Step 2: `.../presentation/createconversation/CreateConversationViewModel.kt` (created)
- Step 3: `.../presentation/createconversation/CreateConversationScreen.kt` (created); `shared/src/commonMain/composeResources/drawable/ic_arrow_back.xml` (created)
- Step 4: `.../navigation/AppNavHost.kt` (modified: real Create route, guarded one-shot navigation, `dropUnlessResumed`); `.../navigation/StubScreens.kt` (modified: Create stub removed); `.../di/SharedModule.kt` (modified: ViewModel binding)
- Step 5: `shared/src/commonTest/.../presentation/createconversation/CreateConversationViewModelTest.kt` (created, 13 tests); `shared/src/commonTest/.../testing/TestSupport.kt` (modified: fake gains `failNextCreate`, `gate`, default flow); `shared/src/androidHostTest/.../data/repository/SqlRepositoriesTest.kt` (modified, +1 test)
- Also (separate commit): `Docs/Requirement/Phase1/02-home-screen/{status.md,progress.md}` mark PR #2 as merged.

### Verification Run
| Command | Result |
|---------|--------|
| `./gradlew :shared:testAndroidHostTest` (final) | BUILD SUCCESSFUL — 60 tests, 0 failures, 0 errors (46 before this ticket) |
| `./gradlew :androidApp:assembleDebug :androidApp:assembleRelease` | BUILD SUCCESSFUL |
| `./gradlew :shared:compileKotlinIosSimulatorArm64` | BUILD SUCCESSFUL |
| Mutation check: `ensureActive()` removed | `leavingTheScreenDuringCreate…` failed as expected; restored |
| iOS tests, lint, Compose UI tests, running on a device/emulator/simulator | **Not run** (no device here; UI tests out of scope) |

### Deviations & Assumptions
- Form scrolls vertically (landscape, large fonts).
- `start()` uses an atomic `getAndUpdate`. Selections are ignored while creating. Taps on a leaving form are dropped (`dropUnlessResumed`).
- Error texts are live regions, and the fields show `isError` when the Languages match (refines D17).
- Back icon mirrors in RTL via `autoMirrored` in the vector resource.
- Accepted: Start then immediate Back can leave the new conversation on Home without opening Chat (TC-24). Process death mid-create can let a duplicate be created (TC-25, D16).

### Open Issues & Risks
- No automated test for the navigation effect, its back-stack guard, or the Koin graph (needs the Compose UI-test dependency).
- No logger: a failed create shows a generic message and the exception isn't recorded anywhere.
- iOS: `MainViewController` still never starts Koin (pre-existing from ticket 02), so the iOS app can't open any screen yet. Ticket 14 owns iOS wiring.
- Carried over from ticket 02: backup of conversation data (Medium, user decision), migration setup (ticket 10).

### ⚠️ Critical Manual Checks
| # | What to check | Steps | Expected result | Why critical |
|---|---------------|-------|-----------------|--------------|
| 1 | Create → Chat → Back lands on Home (TC-04, TC-05, TC-23) | 1. Debug build: tap **+**. 2. Tap Start Conversation. 3. Note the id on the Chat stub. 4. Press Back. | The Chat stub shows a new id. Back goes straight to Home, and the top row reads "English ↔ Mandarin Chinese", "No messages yet", "Just now", "0 messages". | Core flow of the ticket; the `popUpTo` back-stack behaviour isn't covered by automation. |
| 2 | Reverse order and the same-language rule (TC-06, TC-10–12) | 1. Set Language 1 to Mandarin Chinese. 2. Check the error and disabled Start. 3. Set Language 2 to English. 4. Start, then Back. | The error shows with both fields red and Start greyed. After step 3 the error clears. Home's new row reads "Mandarin Chinese ↔ English". | AC3/AC4. The stored order drives every later language decision. |
| 3 | Back and taps during a create or exit (TC-08, TC-09, TC-15, TC-24) | 1. Tap the Back arrow and confirm nothing was created. 2. Press system Back, then quickly tap where Start was. 3. Double-tap Start. 4. Tap Start and immediately press Back. | 1–2: no new row. 3: exactly one new row and one Back to Home. 4: at most one new row, and Chat never opens on top of Home after Back. | These guards (`dropUnlessResumed`, the back-stack guard, single-flight) are only proven manually. |
| 4 | TalkBack (TC-22) | 1. Enable TalkBack. 2. Focus the arrow, then each field. 3. Set both fields to English. | The arrow is announced as "Back" and the fields as "Language 1"/"Language 2" with their values. "Choose two different languages" is announced when it appears. | Accessibility fixes from cycle 3 are unverified on a device. |
| 5 | iOS resource bundling (smoke) | Once ticket 14 wires iOS Koin, open Create Conversation on the simulator. | The Back arrow renders with no resource crash. | `ic_arrow_back` is the app's first runtime Compose resource; on iOS it has only been compiled. |
| 6 | Rotation and RTL (TC-17, TC-18) | 1. Change the selection, then rotate. 2. Switch the device language to an RTL locale (e.g. Arabic) and reopen the form. | The selection and error survive rotation. In RTL the arrow points right and sits on the right. | Lifecycle and layout aren't covered by automated tests. |

## Pull Request
`feature/03-create-conversation` → `phase_1` (pushed to `origin`). The `gh` CLI isn't installed, so open the PR from the compare page:
https://github.com/nasimnu14/DoVashi/compare/phase_1...feature/03-create-conversation?expand=1

**Title:** feat(create-conversation): catalog-driven Create Conversation screen (ticket 03)

**Description:**
Replaces the Create Conversation stub with the real screen. Language 1 and Language 2 dropdowns read from the Language Catalog. Choosing the same Language for both is rejected (inline error, Start disabled). Start Conversation persists the pair in the selected order and opens the Chat stub, with the form popped off the back stack. Also marks PR #2 (ticket 02) as merged in its ticket docs.

🤖 Generated with [Claude Code](https://claude.com/claude-code)
