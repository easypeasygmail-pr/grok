package de.odpisz.core.designsystem

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview

private val LightColors: ColorScheme =
    lightColorScheme(
        primary = Ink,
        onPrimary = Color.White,
        primaryContainer = Mist,
        onPrimaryContainer = InkDark,
        background = Paper,
        onBackground = InkText,
        surface = Card,
        onSurface = InkText,
        surfaceVariant = Paper,
        onSurfaceVariant = Ink,
        error = ErrorLight,
        onError = Color.White,
    )

private val DarkColors: ColorScheme =
    darkColorScheme(
        primary = Mist,
        onPrimary = InkDark,
        primaryContainer = Ink,
        onPrimaryContainer = Mist,
        background = PaperDark,
        onBackground = PaperText,
        surface = CardDark,
        onSurface = PaperText,
        surfaceVariant = InkDark,
        onSurfaceVariant = Mist,
        error = ErrorDark,
        onError = OnErrorDark,
    )

@Immutable
data class UrgencyColors(
    val green: Color,
    val onGreen: Color,
    val yellow: Color,
    val onYellow: Color,
    val red: Color,
    val onRed: Color,
)

private val LightUrgency =
    UrgencyColors(
        green = UrgencyGreen,
        onGreen = OnUrgencyGreen,
        yellow = UrgencyYellow,
        onYellow = OnUrgencyYellow,
        red = UrgencyRed,
        onRed = OnUrgencyRed,
    )

private val DarkUrgency =
    UrgencyColors(
        green = UrgencyGreenDark,
        onGreen = OnUrgencyGreenDark,
        yellow = UrgencyYellowDark,
        onYellow = OnUrgencyYellowDark,
        red = UrgencyRedDark,
        onRed = OnUrgencyRedDark,
    )

private val LocalUrgencyColors = staticCompositionLocalOf { LightUrgency }

/**
 * Single Material 3 theme for Odpisz.de.
 *
 * Add a color in [Color.kt] and wire it into both [LightColors] and [DarkColors]
 * (or both urgency sets). Screens use [MaterialTheme] or [OdpiszTheme], never a raw hex.
 */
@Composable
fun OdpiszTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val urgency = if (darkTheme) DarkUrgency else LightUrgency
    CompositionLocalProvider(
        LocalSpacing provides AppSpacing(),
        LocalUrgencyColors provides urgency,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = OdpiszTypography,
            shapes = OdpiszShapes,
            content = content,
        )
    }
}

object OdpiszTheme {
    val spacing: AppSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalSpacing.current

    val urgency: UrgencyColors
        @Composable
        @ReadOnlyComposable
        get() = LocalUrgencyColors.current
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OdpiszScaffold(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.design_back),
                            )
                        }
                    }
                },
                actions = actions,
            )
        },
        bottomBar = bottomBar,
        content = content,
    )
}

@Composable
fun OdpiszButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = OdpiszTheme.spacing.touch),
        colors = ButtonDefaults.buttonColors(),
    ) {
        Text(text)
    }
}

@Composable
fun OdpiszOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = OdpiszTheme.spacing.touch),
    ) {
        Text(text)
    }
}

@Preview(name = "Jasny")
@Preview(name = "Ciemny", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ThemePreview() {
    OdpiszTheme {
        Surface {
            OdpiszButton(text = "Dalej", onClick = {})
        }
    }
}
