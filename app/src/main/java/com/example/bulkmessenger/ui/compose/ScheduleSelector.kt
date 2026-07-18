package com.example.bulkmessenger.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bulkmessenger.util.showDateTimePicker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** "Send now" vs "Schedule for later" selector, used by both Broadcast and Personalized screens. */
@Composable
fun ScheduleSelector(
    scheduledAtMillis: Long?,
    onScheduledAtChange: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = scheduledAtMillis == null,
                onClick = { onScheduledAtChange(null) },
                label = { Text("Send now") }
            )
            FilterChip(
                selected = scheduledAtMillis != null,
                onClick = {
                    showDateTimePicker(context) { picked -> onScheduledAtChange(picked) }
                },
                label = { Text("Schedule for later") },
                leadingIcon = { Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        if (scheduledAtMillis != null) {
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Scheduled for ${formatScheduled(scheduledAtMillis)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    IconButton(onClick = { onScheduledAtChange(null) }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Clear schedule",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

private fun formatScheduled(millis: Long): String =
    SimpleDateFormat("dd MMM, h:mm a", Locale.getDefault()).format(Date(millis))
