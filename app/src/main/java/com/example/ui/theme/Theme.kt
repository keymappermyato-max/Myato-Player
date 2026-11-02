package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.model.AppSettings
import com.example.model.ThemePreset

val LocalAppSettings = compositionLocalOf { AppSettings() }

fun buildColorScheme(settings: AppSettings): ColorScheme {
    val preset = settings.themePreset
    val primaryColor = if (preset == ThemePreset.CUSTOM) {
        Color(settings.customPrimaryColor)
    } else {
        Color(preset.primaryHex)
    }
    val secondaryColor = if (preset == ThemePreset.CUSTOM) {
        Color(settings.customSecondaryColor)
    } else {
        Color(preset.secondaryHex)
    }
    val bgColor = Color(preset.backgroundHex)
    val surfaceColor = Color(preset.surfaceHex)

    return if (preset == ThemePreset.MINIMAL_WHITE) {
        lightColorScheme(
            primary = primaryColor,
            onPrimary = Color.White,
            primaryContainer = primaryColor.copy(alpha = 0.15f),
            onPrimaryContainer = primaryColor,
            secondary = secondaryColor,
            onSecondary = Color.White,
            background = bgColor,
            onBackground = LightTextPrimary,
            surface = surfaceColor,
            onSurface = LightTextPrimary,
            surfaceVariant = LightSurfaceElevated,
            onSurfaceVariant = LightTextSecondary,
            outline = Color(0xFFCBD5E1)
        )
    } else {
        darkColorScheme(
            primary = primaryColor,
            onPrimary = Color.White,
            primaryContainer = primaryColor.copy(alpha = 0.22f),
            onPrimaryContainer = primaryColor,
            secondary = secondaryColor,
            onSecondary = Color.Black,
            background = bgColor,
            onBackground = DarkTextPrimary,
            surface = surfaceColor,
            onSurface = DarkTextPrimary,
            surfaceVariant = DarkSurfaceElevated,
            onSurfaceVariant = DarkTextSecondary,
            outline = DarkBorder
        )
    }
}

@Composable
fun MyatoPlayerTheme(
    settings: AppSettings = AppSettings(),
    content: @Composable () -> Unit
) {
    val colorScheme = buildColorScheme(settings)

    CompositionLocalProvider(
        LocalAppSettings provides settings
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
