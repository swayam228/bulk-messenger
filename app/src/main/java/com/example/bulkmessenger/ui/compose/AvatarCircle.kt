package com.example.bulkmessenger.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Preset swatches offered when creating/editing a profile. */
val AvatarColorPalette = listOf(
    "#3D5AFE", // indigo
    "#E64A8C", // rose
    "#00897B", // teal
    "#FB8C00", // amber
    "#7B3FE4", // purple
    "#2E7D32", // green
)

fun parseAvatarColor(hex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color.Gray)

@Composable
fun AvatarCircle(name: String, colorHex: String, size: Dp = 36.dp) {
    val color = remember(colorHex) { parseAvatarColor(colorHex) }
    Box(
        modifier = Modifier.size(size).background(color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            (name.trim().firstOrNull() ?: '?').uppercaseChar().toString(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )
    }
}
