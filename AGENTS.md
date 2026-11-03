> **Nota (PL):** Ten plik jest po angielsku celowo — agenty trzymają się go ściślej niż polskich reguł. Skrót: Jetpack Compose + Material 3, jedna aktywność, Navigation Compose, MVVM/UDF (`ViewModel` + `StateFlow`), Hilt, Coroutines/Flow, Room, jeden klient HTTP (Retrofit albo Ktor), Gradle Kotlin DSL i version catalog. Najpierw plan, potem tokeny, potem ekran, na końcu build, emulator i zrzut. Reszta poniżej jest obowiązująca.

# Android / Kotlin agent rules

Opinionated defaults for a modern native Android app. Follow them unless the existing module already chose a different, consistent pattern — then match the module and say so. Do not introduce a second way.

## Stack

- Kotlin. Jetpack Compose for all new UI. No new XML layouts, Fragments, or Activities for screens.
- Material 3. One `MaterialTheme` in the app. Light and dark `ColorScheme` from the start.
- Single `ComponentActivity`. Call `enableEdgeToEdge()` before `setContent`.
- Navigation: [Navigation Compose](https://developer.android.com/guide/navigation/design) (`androidx.navigation:navigation-compose`) with type-safe `@Serializable` routes. Use the vendored `navigation-3` skill only when the project already uses Navigation 3 or the user explicitly asks to migrate.
- MVVM + unidirectional data flow. A `ViewModel` owns a `StateFlow<UiState>`. The UI renders state and sends events. The ViewModel is the only writer of that state.
- Hilt. `@HiltAndroidApp`, `@AndroidEntryPoint` on the activity, `hiltViewModel()` in Compose.
- Coroutines and Flow. `viewModelScope`. No `GlobalScope`, no `runBlocking` on the main thread.
- Room for structured local data. Schema migrations are explicit. `fallbackToDestructiveMigration()` is not acceptable for user data.
- HTTP: Retrofit or Ktor Client. Pick the one already in the project. Do not add the other.
- Gradle Kotlin DSL (`build.gradle.kts`) and a version catalog (`gradle/libs.versions.toml`). No floating versions (`+`, `latest`) in the catalog.

## Layout

Feature-first packages. Keep a feature's state, screen, and components together.

```
feature/<name>/
  <Name>Screen.kt      # composable, previews
  <Name>ViewModel.kt
  <Name>UiState.kt     # state + events
data/
  repository/          # interfaces the ViewModel sees
  remote/              # DTO + API
  local/               # Room
ui/theme/              # color, type, shape, spacing
```

A repository returns domain models. DTOs and entities stay inside `data/`.

## UI

- Design tokens before pixels. Screens use `MaterialTheme` and the spacing scale. No raw `Color(0xFF…)`, font sizes, or one-off `dp` in a screen. If the theme has no token you need, add the token first.
- Every data screen has four states: loading, empty, error (with a retry event), content. Do not ship a happy path alone.
- Edge-to-edge: apply Scaffold `innerPadding` (or an explicit inset strategy — one, not both). Lists use `contentPadding`. Nothing tappable sits under the status bar, navigation bar, or IME. For a full pass, open the `edge-to-edge` skill.
- Accessibility: `contentDescription` on meaningful icons, `null` on decorative ones. Minimum 48.dp touch targets. Don't encode state by color alone. Support font scale; don't lock a text line to a fixed height that clips it.
- Dark mode is a real theme, not an afterthought. Check contrast on both schemes.
- User-facing copy lives in string resources, not inside composables.
- Previews for new screens: light and dark (`uiMode = Configuration.UI_MODE_NIGHT_YES`).

```kotlin
Scaffold { innerPadding ->
    LazyColumn(
        modifier = Modifier.fillMaxSize().consumeWindowInsets(innerPadding),
        contentPadding = innerPadding,
    ) { /* rows */ }
}
```

## State

```kotlin
data class FeatureUiState(
    val status: Status = Status.Loading,
    val items: List<Item> = emptyList(),
    val message: String? = null,
) {
    enum class Status { Loading, Empty, Error, Content }
}

sealed interface FeatureUiEvent {
    data object Retry : FeatureUiEvent
    data class Open(val id: String) : FeatureUiEvent
}
```

- Collect with `collectAsStateWithLifecycle()`.
- Expose `StateFlow` with `SharingStarted.WhileSubscribed(5_000)`. Keep `MutableStateFlow` private.
- Use a sealed state instead of a single data class when the states carry different fields.
- `remember { mutableStateOf(...) }` is for local control state (scroll, animation), not for data that survives rotation or comes from a repository.
- Navigation arguments are ids and primitives. Load the object in the ViewModel. Scope the ViewModel to the back stack entry that owns the screen.
- One-off effects (navigation, snackbar) are a `Channel` / `SharedFlow` of events, not flags stuck in `UiState`.

## Data and concurrency

- Inject repository interfaces into the ViewModel so tests can pass a fake.
- Map network and database errors into `Status.Error`. Do not let them crash the composition.
- Set connect and read timeouts on the HTTP client.
- `Dispatchers.IO` at the repository boundary. The ViewModel does not reach for a dispatcher to call a repository that is already main-safe.
- Room: `Flow` from the DAO. Map entities to domain models before they leave the repository.

## Gradle and style

- Add dependencies through the version catalog, in the module that uses them.
- `minSdk` stays whatever the app already ships. `compileSdk` / `targetSdk` track the current stable SDK. Don't bump them as a side effect of an unrelated task.
- ktlint formats, detekt analyzes. Fix new findings in code you touch. Don't mass-reformat unrelated files. Depend on current ktlint coordinates `io.github.ktlint` (the old `com.pinterest.ktlint` artifacts are retired).
- No `!!`. No unused suppressions. Trailing commas in multiline calls and parameter lists.

## Testing

- ViewModel tests: JUnit4, a `TestDispatcher`, Turbine (or equivalent) on the `StateFlow`. Cover the main success path and the error path.
- Compose UI tests for a flow that is awkward to lock with a unit test (empty vs content, a button actually sends the event).
- Fakes over mocks. Don't spin up Hilt, Room, or an emulator for a pure state transition.
- When the task is "set up testing from scratch", open the vendored `testing-setup` skill and follow it.
- Run the tests you add. A test that was not executed is not done.

## Workflow

1. **Plan first.** Before editing, write a short plan: route, `UiState`, events, repository, new dependencies, tests. If the user asked to confirm the plan, stop there. Otherwise proceed after the plan is in the reply.
2. **Tokens before screens.** If color, type, or spacing is hardcoded or missing, fix the theme before building the screen. Open `design-system-tokens`.
3. **Vertical slice.** Domain and repository, then ViewModel, then composable, then navigation, then the unit test. Don't boil the ocean.
4. **Build, run, screenshot.** Assemble the debug variant, install it, open the screen, capture a screenshot, and judge it (insets, clipping, contrast, the four states). Open `verify-on-emulator`. Fix and repeat. After three visual passes, report what is still wrong instead of looping.
5. **Report what you did not verify.** If no device was available, say that. A green compile is not a visual check.

## Which skill to open

| Situation | Skill |
| --- | --- |
| Screen from Figma, Mobbin, or a screenshot | `compose-screen-from-design` |
| Theme, colors, type, spacing | `design-system-tokens` |
| Run on a device and look at the result | `verify-on-emulator` |
| System bar overlap, IME, edge-to-edge | `edge-to-edge` (Google) |
| Navigation 3 migration or scenes | `navigation-3` (Google) |
| XML view to Compose | `migrate-xml-views-to-jetpack-compose` (Google) |
| New test harness, screenshot tests | `testing-setup` (Google) |
| Tablets, foldables, large windows | `adaptive` (Google) |
| Jank, recomposition | `jetpack-compose-performance` (Google) |

Google skills are vendored under `third_party/android-skills` (Apache 2.0) and linked from `.cursor/skills/` and `.claude/skills/`. Read the skill before improvising a migration.

## Do not

- Do not paste generated Compose from Figma or copy Mobbin branding, marketing text, or image assets. Adapt the structure to this design system.
- Do not add a library for a problem the project already solves.
- Do not refactor adjacent code in a bugfix. Do not change behavior in a refactor.
- Do not put API keys, keystores, `local.properties`, or `google-services.json` in the repo. MCP here is OAuth or a local process, never a committed token.
- Do not claim the UI is verified without a screenshot or an explicit reason the device was unreachable.
