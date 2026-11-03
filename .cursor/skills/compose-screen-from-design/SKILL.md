---
name: compose-screen-from-design
description: >
  Build or update a Jetpack Compose screen from a Figma frame, a Mobbin
  reference, or a screenshot. Use when the user pastes a Figma URL, asks to
  match a mobile design, implement a screen from a mock, or look up an Android
  UI pattern. Maps the reference onto Material 3 tokens, MVVM state, and an
  emulator screenshot check.
metadata:
  author: android-ai-starter-kit
  version: "1.0"
---

> **Nota (PL):** Skill po angielsku, bo agenty czytają go pewniej. Użyj go, gdy ekran ma powstać z Figmy, Mobbin albo ze zrzutu. Najpierw tokeny i stany (ładowanie, pusty, błąd, treść), potem kod. Nie wklejaj wygenerowanego UI.

# Compose screen from a design reference

Build one screen that belongs in this codebase. The reference decides layout and hierarchy. `AGENTS.md` decides architecture. The existing theme decides color, type, and spacing.

## Inputs

You need at least one of:

- A Figma URL that points at a frame (link to selection).
- A Mobbin search: platform, product category, and the pattern (for example Android banking, transaction-list empty state).
- A local screenshot or a written description of the screen.

If the source is missing, say what you assumed in one line and build the closest useful screen. Do not invent a second, unrelated flow.

## Steps

1. Read `AGENTS.md`. Match Navigation Compose, `ViewModel`, and `StateFlow` unless this module already uses Navigation 3 — then read the `navigation-3` skill and follow it.
2. **Figma.** If the Figma MCP server is connected, read the frame (`get_design_context` / `get_screenshot` / variables). Treat the payload as a visual spec, not as code to paste. If a Figma design-to-code skill is installed, follow it, then adapt the result to this app's components. If Figma is not connected, say so and work from the URL description the user gave.
3. **Mobbin.** If the Mobbin MCP server is connected, search screens or flows (`search_screens`, `search_flows`). Use several products. For each reference you actually follow, keep the product name and the Mobbin URL. A Mobbin screen shows that a product shipped a pattern. It does not prove the pattern is right for this app. Do not copy trademarks, marketing copy, or image assets.
4. Inventory the reference before coding:
   - regions (bar, content, primary action)
   - repeated components that already exist in this project
   - states the mock forgot: loading, empty, error, disabled, long text, large font
5. Open `design-system-tokens` when the theme has no token for a color, text style, or space the screen needs. Add the token. Then build the screen. Do not hardcode the value "just for this screen".
6. Extend an existing component with a parameter or a small wrapper when it is close. Create a new composable only when nothing in the theme fits, and say why.
7. State shape:

   ```kotlin
   data class InvoiceUiState(
       val status: Status = Status.Loading,
       val title: String = "",
       val rows: List<Row> = emptyList(),
       val message: String? = null,
   ) {
       enum class Status { Loading, Empty, Error, Content }
   }
   ```

   The composable takes `state` and `onEvent`. It does not call repositories, and it does not hold the screen's data in `remember`.
8. Wire a type-safe route and a `hiltViewModel()`. Pass ids through navigation, not whole models.
9. Put user-visible strings in resources.
10. Add previews for light and dark, including the empty and error states when they differ visually.
11. Add a ViewModel unit test for content and for the error/retry path.
12. Hand off to `verify-on-emulator`. In the summary, list how the implementation differs from the reference and why (tokens, missing states, platform conventions).

## Layout habits that keep the screen native

- `Scaffold` + `innerPadding` for system bars. One inset strategy, not padding plus `safeDrawingPadding` on the same node.
- Primary actions stay reachable above the navigation bar. Text fields stay above the IME (`android:windowSoftInputMode="adjustResize"` on the activity).
- Icon buttons need a 48.dp target and a content description when they mean something.
- Lists use `LazyColumn` with stable keys. Don't nest a vertically scrolling column inside another vertical scroll.
- Respect a large font. No fixed-height text boxes that clip Polish strings.

## Done

- The screen compiles as part of the app's navigation.
- Loading, empty, error, and content are real branches, not TODOs.
- Colors and spacing come from the theme.
- A unit test covers success and failure.
- A screenshot was taken, or you stated why no device was available.
