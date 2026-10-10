# Plan — 03-create-conversation

Inputs: `grill-notes.md` (D1–D18), `domain-model.md` (C1–C6). Verify on Android plus the iOS compile only; `iosMain` untouched.
Package root `com.example.dovashiapp` in `:shared`; paths below are under `shared/src/commonMain/kotlin/com/example/dovashiapp/` unless noted.

## Step 0 — Preconditions
- `phase_1` contains tickets 01–02: `LanguageCatalog`, `CreateConversationUseCase`, `CreateConversationRoute`, `ChatRoute`, `AppNavHost`, Koin `sharedModule`.
- Baseline: `./gradlew :shared:testAndroidHostTest` is green.

## Step 1 — UI state (`presentation/createconversation/CreateConversationUiState.kt`)
- `data class CreateConversationUiState(languages, language1: Language?, language2: Language?, isCreating = false, errorMessage: String? = null, createdConversationId: Long? = null)`.
- Computed `isSameLanguage` and `canStart` exactly as in domain-model §1 (C2).
- Pure function `defaultSelection(languages): Pair<Language?, Language?>` gives L1 = first entry and L2 = first entry with a different code (D6).

## Step 2 — ViewModel (`presentation/createconversation/CreateConversationViewModel.kt`)
- Constructor `(createConversation: CreateConversationUseCase, languages: List<Language> = LanguageCatalog.all)`. Exposes `uiState: StateFlow<CreateConversationUiState>` backed by a `MutableStateFlow`.
- `selectLanguage1(language)` / `selectLanguage2(language)` update the field and clear `errorMessage`.
- `start()`: return if `!canStart` (this covers double tap, C3). Set `isCreating = true` and launch in `viewModelScope`. On success set `createdConversationId = id` and keep `isCreating = true`, so the button stays disabled until navigation. On exception (not `CancellationException`) set `isCreating = false` and `errorMessage = "Couldn't create the conversation. Please try again."` (D13, C6).
- `onNavigationHandled()` sets `createdConversationId = null` (D12).

## Step 3 — Screen (`presentation/createconversation/CreateConversationScreen.kt`)
- Stateless `CreateConversationScreen(state, onLanguage1Selected, onLanguage2Selected, onStartClick, onBackClick)`.
- `Scaffold` + `TopAppBar(title = "Create Conversation", navigationIcon = Back icon, contentDescription "Back")` (D14, D17).
- A private `LanguageDropdown(label, selected, options, onSelected)` uses `ExposedDropdownMenuBox` with a read-only `OutlinedTextField` showing `selected?.name.orEmpty()`, with `label = { Text(label) }`, and a `DropdownMenuItem` per option (`name`).
- When `state.isSameLanguage`, show the inline error text "Choose two different languages" (error colour) below the pickers.
- When `state.errorMessage != null`, show it in error colour above the button.
- Full-width `Button("Start Conversation", enabled = state.canStart)`.
- Back icon: `composeResources/drawable/ic_arrow_back.xml` (Material "arrow_back" 24dp vector), loaded with `painterResource(Res.drawable.ic_arrow_back)` and drawn with `Icon(...)` (D15). Mirror it for RTL with `Modifier.graphicsLayer(scaleX = -1f)` when `LocalLayoutDirection` is Rtl.

## Step 4 — Navigation and DI
- `navigation/AppNavHost.kt`: `composable<CreateConversationRoute>` gets `koinViewModel<CreateConversationViewModel>()`, collects state with lifecycle and renders the screen. A `LaunchedEffect(state.createdConversationId)` navigates when the id is non-null: `navController.navigate(ChatRoute(id)) { popUpTo<CreateConversationRoute> { inclusive = true }; launchSingleTop = true }`, then calls `viewModel.onNavigationHandled()` (C5). `onBackClick = { navController.navigateUp() }`.
- `navigation/StubScreens.kt`: delete `CreateConversationStubScreen` (the Chat stub stays for ticket 04).
- `di/SharedModule.kt`: add `viewModel { CreateConversationViewModel(get()) }`.
- Verify: `./gradlew :androidApp:assembleDebug` succeeds.

## Step 5 — Tests
- `commonTest/.../presentation/createconversation/CreateConversationViewModelTest.kt` (Main dispatcher = `UnconfinedTestDispatcher`, reusing `FakeConversationRepository`, extended so it can be told to throw):
  - defaults are catalog[0] and catalog[1]; with the same-code-first catalog `[A, A', B]`, L2 = B
  - L1 = L2 sets `isSameLanguage = true` and `canStart = false`; `start()` then inserts nothing
  - start with (L1, L2) persists codes and title in that order and sets `createdConversationId`; the reversed selection persists reversed codes
  - two `start()` calls while the first is suspended insert exactly one conversation (gated fake)
  - a failure sets `errorMessage` and `isCreating = false`; a second start succeeds; selecting a Language clears the error
  - a catalog with one Language: L2 = null and `canStart = false`; an empty catalog: both null
  - `onNavigationHandled()` clears the id
- `androidHostTest/.../data/repository/SqlRepositoriesTest.kt` (extend): `CreateConversationUseCase` with real repositories stores `language1Code`/`language2Code` in the given order and `"<L1> ↔ <L2>"` as the title (AC2).
- Verify: `./gradlew :shared:testAndroidHostTest :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64` is green.

## Acceptance criteria
1. Tapping **+** on Home opens "Create Conversation" with Language 1 and Language 2 dropdowns, by default the first two catalog Languages (English / Mandarin Chinese in Phase 1), and a Start Conversation button.
2. Picker options come from the Language Catalog; the screen's logic has no `en`/`zh`/English/Chinese literals (C1).
3. Selecting the same Language for both fields shows "Choose two different languages" and disables Start; the use case also rejects it (C2, C4).
4. Start persists a new Conversation with `language1Code`/`language2Code` in selected order (en/zh or zh/en), title `"<L1> ↔ <L2>"`, `createdAt = updatedAt`, and opens the Chat stub for that id.
5. Back from the Chat stub returns to Home (the form is popped). Back on the form returns to Home without creating anything.
6. Double tap creates one conversation; a failure shows an error and lets the user retry.
7. All Step 5 tests pass, and `assembleDebug` and the iOS compile succeed.
