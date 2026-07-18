package com.example.bulkmessenger.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bulkmessenger.data.Draft

/**
 * A message field with a Write manually / Use template toggle. Picking a template fills the
 * field with that draft's body but leaves it fully editable afterward.
 */
@Composable
fun TemplateMessageField(
    message: String,
    onMessageChange: (String) -> Unit,
    drafts: List<Draft>,
    modifier: Modifier = Modifier,
    minLines: Int = 3,
    // Only valid when the caller's own layout has a bounded height to distribute (e.g. a
    // non-scrolling screen Column) — set true there so the message box fills the remaining space
    // instead of just wrapping its minLines. Never set this inside a scrollable container.
    fillRemainingHeight: Boolean = false
) {
    var useTemplate by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !useTemplate,
                onClick = { useTemplate = false },
                label = { Text("Write manually") }
            )
            FilterChip(
                selected = useTemplate,
                onClick = { useTemplate = true },
                enabled = drafts.isNotEmpty(),
                label = { Text("Use template") }
            )
        }
        Spacer(Modifier.height(8.dp))

        if (useTemplate) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = "Select a template",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Template") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    if (drafts.isEmpty()) {
                        DropdownMenuItem(text = { Text("No drafts saved yet") }, onClick = {}, enabled = false)
                    }
                    drafts.forEach { draft ->
                        DropdownMenuItem(
                            text = { Text(draft.title) },
                            onClick = {
                                onMessageChange(draft.body)
                                expanded = false
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        OutlinedTextField(
            value = message,
            onValueChange = onMessageChange,
            label = { Text("Message") },
            modifier = if (fillRemainingHeight) {
                Modifier.fillMaxWidth().weight(1f)
            } else {
                Modifier.fillMaxWidth()
            },
            minLines = minLines
        )
    }
}
