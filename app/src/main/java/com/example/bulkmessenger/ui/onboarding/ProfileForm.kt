package com.example.bulkmessenger.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.bulkmessenger.ui.compose.AvatarCircle
import com.example.bulkmessenger.ui.compose.AvatarColorPalette
import com.example.bulkmessenger.ui.compose.parseAvatarColor

/** Name + avatar-color picker, shared by the first-launch onboarding screen and Home's "Add profile" sheet. */
@Composable
fun ProfileForm(
    submitLabel: String,
    onSubmit: (name: String, colorHex: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(AvatarColorPalette.first()) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AvatarCircle(name = name, colorHex = selectedColor, size = 64.dp)

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Your name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Pick a color", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AvatarColorPalette.forEach { hex ->
                val isSelected = hex == selectedColor
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(parseAvatarColor(hex), CircleShape)
                        .border(
                            width = if (isSelected) 3.dp else 0.dp,
                            color = MaterialTheme.colorScheme.onSurface,
                            shape = CircleShape
                        )
                        .clickable { selectedColor = hex },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White)
                    }
                }
            }
        }

        Button(
            onClick = { onSubmit(name.trim(), selectedColor) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = name.isNotBlank()
        ) {
            Text(submitLabel)
        }
    }
}
