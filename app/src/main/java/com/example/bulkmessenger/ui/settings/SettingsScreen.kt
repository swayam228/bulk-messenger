package com.example.bulkmessenger.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bulkmessenger.util.SimHelper
import com.example.bulkmessenger.viewmodel.SessionViewModel

@Composable
fun SettingsScreen(sessionViewModel: SessionViewModel, onBack: () -> Unit) {
    val activeUser by sessionViewModel.activeUser.collectAsState()
    val context = LocalContext.current
    val sims = remember { SimHelper.getActiveSims(context) }
    val powerManager = remember { context.getSystemService(PowerManager::class.java) }
    val isBatteryExempted = remember { powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("Reliability", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (isBatteryExempted) {
                Text(
                    "Battery optimization is already disabled for this app — sends should run reliably in the background.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    "Some phones (Vivo, Oppo, Realme and similar) aggressively kill background apps, which can interrupt a send in progress. Exempting this app from battery optimization makes that far less likely.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.BatteryChargingFull, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Disable battery optimization")
                }
            }

            HorizontalDivider()

            Text("Default SIM", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (sims.size < 2) {
                Text(
                    "Only one active SIM detected on this device — nothing to choose from.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    "Used as the pre-selected SIM on Broadcast and Personalized Batch — you can still switch per-send.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    sims.forEach { sim ->
                        FilterChip(
                            selected = activeUser?.defaultSimSubscriptionId == sim.subscriptionId,
                            onClick = { sessionViewModel.setDefaultSim(sim.subscriptionId) },
                            label = { Text(sim.label) }
                        )
                    }
                }
            }

            HorizontalDivider()

            Text("Backup & Restore", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                "This app stores everything locally on your device only — nothing survives an uninstall unless you back it up yourself.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            BackupRestoreSection(onRestored = onBack)
        }
    }
}
