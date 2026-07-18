package com.example.bulkmessenger.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.bulkmessenger.util.SimHelper

/** Renders nothing on single-SIM devices — only shown when 2+ active SIMs are detected. */
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
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            sims.forEach { sim ->
                FilterChip(
                    selected = selectedSubscriptionId == sim.subscriptionId,
                    onClick = { onSelect(sim.subscriptionId) },
                    label = { Text(sim.label) }
                )
            }
        }
    }
}
