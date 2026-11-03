---
name: design-system-tokens
description: >
  Create or extend a Material 3 theme in a Kotlin Jetpack Compose app: color
  schemes, typography, shapes, spacing, and semantic component defaults,
  including dark mode. Use before building screens, when colors or dp values
  are hardcoded, or when the user asks for a theme, design tokens, or a
  design system.
metadata:
  author: android-ai-starter-kit
  version: "1.0"
---

> **Nota (PL):** Skill po angielsku, bo agenty czytają go pewniej. Użyj go, zanim narysujesz nowy ekran albo gdy w Compose są wpisane kolory i „magiczne” dp. Rozszerzaj istniejący motyw. Nie stawiaj obok drugiego systemu.

# Design system tokens

Tokens are Kotlin, living in the app theme. Screens consume semantic values. They do not mention hex or a raw palette.

## When the project already has a theme

Extend it. Do not add a parallel `DesignSystem`, a second `MaterialTheme`, or a copied palette with slightly different names. Search for `MaterialTheme(`, `Color.kt`, `Theme.kt`, `Type.kt` before creating files.

## When the project has no theme

Add these under `ui/theme/` and wrap the activity content in `AppTheme`.

### Color

- A light `lightColorScheme` and a dark `darkColorScheme`. Both must set `background`, `surface`, `primary`, `onPrimary`, `error`, and the matching `on*` roles you actually use.
- `dynamicDarkColorScheme` / `dynamicLightColorScheme` (Android 12+) are optional and always have the static schemes as fallback.
- Extra meanings that Material roles cannot carry (`success`, `warning`) go in a small `CompositionLocal`, with values for both schemes. Do not leak a raw palette object to feature code.
- Take seeds from a Figma variable collection when the user provided one (Figma MCP `get_variable_defs`). Name the Kotlin roles after meaning (`colorScheme.primary`), not after the Figma paint name (`blue-500`), at the call site.

### Type and shape

- Start from `Typography()` and override only the styles the product uses (`titleLarge`, `bodyLarge`, `labelLarge`, …).
- One font family for the product, declared once. No ad-hoc `fontSize` in screens.
- `Shapes` with the Material corners the components already expect. Change a corner in the theme, not with a one-off `RoundedCornerShape` on each card — unless that component is genuinely a special shape.

### Spacing

A single scale. Screens use these names.

```kotlin
object AppSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
}
```

Expose it as `AppTheme.spacing` via a `CompositionLocal` if you need it inside composables without an import of the object. Do not add `10.dp`, `12.dp`, `18.dp` unless the scale grows on purpose and the new step is named.

## Component defaults

Add a wrapper only when the same component is already copied with the same overrides (a primary button, a text field, a top app bar). The wrapper applies tokens and accessibility defaults (`Modifier` height, content description parameter). It does not hide Material 3 so thoroughly that a caller cannot pass `Modifier`.

Prefer Material 3 components (`Button`, `OutlinedTextField`, `TopAppBar`, `NavigationBar`) over redrawing them.

## Rules

- No `Color(0xFF…)` and no `.sp` in feature code. The theme files are the exception.
- Dark scheme is required even if the mock is light-only. Check that `onSurface` text is readable on `surface`.
- Don't bump `targetSdk` or add unrelated libraries while touching the theme.
- Leave a short KDoc on `AppTheme` that says how to add a token: which file, both schemes, and that screens must use the semantic name.

## Done

- Light and dark themes both build.
- New or edited screens referenced by the task use the tokens.
- You can point at the file where the next color should be added.
- A `@Preview` of a representative component shows light and dark.
