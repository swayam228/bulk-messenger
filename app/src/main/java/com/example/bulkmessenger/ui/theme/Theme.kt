package com.example.bulkmessenger.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val Indigo80 = Color(0xFFC2CAFF)
private val Indigo40 = Color(0xFF3D5AFE)
private val Indigo20 = Color(0xFF152085)

private val Teal80 = Color(0xFF94F1E3)
private val Teal40 = Color(0xFF00897B)

private val Amber80 = Color(0xFFFFCC80)
private val Amber40 = Color(0xFFFB8C00)

private val Rose40 = Color(0xFFE64A8C)
private val Rose80 = Color(0xFFFFB3D1)

private val LightColors = lightColorScheme(
    primary = Indigo40,
    onPrimary = Color.White,
    primaryContainer = Indigo80,
    onPrimaryContainer = Indigo20,
    secondary = Teal40,
    onSecondary = Color.White,
    secondaryContainer = Teal80,
    onSecondaryContainer = Color(0xFF00352C),
    tertiary = Rose40,
    onTertiary = Color.White,
    tertiaryContainer = Rose80,
    onTertiaryContainer = Color(0xFF5C0F35),
    background = Color(0xFFFAFAFE),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE9E8F5),
    error = Color(0xFFD32F2F),
)

private val DarkColors = darkColorScheme(
    primary = Indigo80,
    onPrimary = Indigo20,
    primaryContainer = Indigo40,
    onPrimaryContainer = Color.White,
    secondary = Teal80,
    onSecondary = Color(0xFF00382F),
    secondaryContainer = Teal40,
    onSecondaryContainer = Color.White,
    tertiary = Rose80,
    onTertiary = Color(0xFF5C0F35),
    tertiaryContainer = Rose40,
    onTertiaryContainer = Color.White,
    background = Color(0xFF121218),
    surface = Color(0xFF1A1B23),
    surfaceVariant = Color(0xFF2E2F3D),
    error = Color(0xFFEF9A9A),
)

/** Distinct accent colors for Home screen action cards — keeps the landing screen from looking monochrome. */
object AccentColors {
    val Broadcast = Indigo40
    val Personalized = Rose40
    val Drafts = Teal40
    val JobHistory = Amber40
}

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun BulkMessengerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = AppShapes,
        content = content
    )
}
