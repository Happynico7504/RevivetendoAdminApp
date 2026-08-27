package net.nicochristmann.revivetendo.admin.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = SlatePrimary,
    background = SlateBackground,
    surface = SlateSurface,
    onSurface = SlateOnSurface,
    error = SlateError,
)

private val LightColors = lightColorScheme(
    primary = SlatePrimary,
    error = SlateError,
)

@Composable
fun RevivetendoAdminTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content,
    )
}
