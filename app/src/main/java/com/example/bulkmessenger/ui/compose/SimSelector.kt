package com.example.bulkmessenger.ui.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bulkmessenger.util.SimHelper
import com.example.bulkmessenger.util.SimOption

/**
 * Renders nothing on single-SIM devices — only shown when 2+ active SIMs are detected. Styled
 * as a vertical list of selectable rows (like a bank picker in a payment app) rather than chips,
 * since a row can show the slot + carrier clearly instead of two identically-labeled toggles.
 */
@Composable
fun SimSelector(
    selectedSubscriptionId: Int?,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sims = remember { SimHelper.getActiveSims(context) }
    if (sims.size < 2) return

    Column(modifier) {
        Text("Send from", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            sims.forEach { sim ->
                SimRow(
                    sim = sim,
                    selected = selectedSubscriptionId == sim.subscriptionId,
                    onClick = { onSelect(sim.subscriptionId) }
                )
            }
        }
    }
}

@Composable
private fun SimRow(sim: SimOption, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${sim.slotIndex + 1}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(sim.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }
            RadioButton(
                selected = selected,
                onClick = null,
                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
            )
        }
    }
}
