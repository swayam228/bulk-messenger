package com.example.bulkmessenger.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bulkmessenger.data.AppDatabase
import com.example.bulkmessenger.util.BackupHelper
import com.example.bulkmessenger.util.BackupPrefs
import com.example.bulkmessenger.util.SimHelper
import com.example.bulkmessenger.viewmodel.SessionViewModel
import com.example.bulkmessenger.worker.AutoBackupWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(sessionViewModel: SessionViewModel, onBack: () -> Unit) {
    val activeUser by sessionViewModel.activeUser.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sims = remember { SimHelper.getActiveSims(context) }
    val powerManager = remember { context.getSystemService(PowerManager::class.java) }
    val isBatteryExempted = remember { powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true }
    var backupUri by remember { mutableStateOf(BackupPrefs.getBackupFolderUri(context)) }
    var autoBackupStatus by remember { mutableStateOf<String?>(null) }

    val backupLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        BackupPrefs.setBackupFolderUri(context, uri)
        AutoBackupWorker.scheduleIfConfigured(context)
        backupUri = uri
        autoBackupStatus = null
    }

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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
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

            Text("Automatic backups", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            val currentBackupUri = backupUri
            if (currentBackupUri == null) {
                Text(
                    "Set a backup folder so your data (and a debug log) is automatically backed up every 6 hours in the background.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = { backupLocationLauncher.launch(null) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.CloudDone, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Choose backup folder")
                }
            } else {
                Text(
                    "Backing up automatically every 6 hours to “${BackupPrefs.displayName(context, currentBackupUri)}”. " +
                        "A debug log is kept in the same folder.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { backupLocationLauncher.launch(null) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Change location") }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                autoBackupStatus = try {
                                    val db = AppDatabase.getInstance(context)
                                    val json = BackupHelper.exportToJson(db)
                                    withContext(Dispatchers.IO) {
                                        val file = BackupPrefs.getOrCreateChildFile(context, BackupPrefs.BACKUP_FILE_NAME, "application/json")
                                            ?: throw IllegalStateException("Couldn't create the backup file")
                                        context.contentResolver.openOutputStream(file.uri, "wt")?.use { out ->
                                            out.write(json.toString(2).toByteArray())
                                        }
                                    }
                                    "Backup saved."
                                } catch (e: Exception) {
                                    "Backup failed: ${e.message}"
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Back up now") }
                }
                autoBackupStatus?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            HorizontalDivider()

            Text("Backup & Restore", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                "This app stores everything locally on your device only. Export a one-off backup, or restore from a previous one.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            BackupRestoreSection(onRestored = onBack)
        }
    }
}
