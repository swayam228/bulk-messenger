package com.example.bulkmessenger.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.bulkmessenger.data.AppDatabase
import com.example.bulkmessenger.util.BackupHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Backup-to-file / restore-from-file controls, shared between Settings (normal use) and
 * Onboarding (so a fresh install can restore instead of being forced to create a throwaway
 * profile first). Restore always replaces all local data — see BackupHelper.
 */
@Composable
fun BackupRestoreSection(onRestored: () -> Unit, modifier: Modifier = Modifier, showBackupButton: Boolean = true) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var pendingRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            statusMessage = try {
                val db = AppDatabase.getInstance(context)
                val json = BackupHelper.exportToJson(db)
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toString(2).toByteArray())
                    }
                }
                "Backup saved."
            } catch (e: Exception) {
                "Backup failed: ${e.message}"
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) pendingRestoreUri = uri }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (showBackupButton) {
                OutlinedButton(
                    onClick = { exportLauncher.launch("bulkmessenger-backup-${System.currentTimeMillis()}.json") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Backup to file")
                }
            }
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Restore from file")
            }
        }
        statusMessage?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    val uri = pendingRestoreUri
    if (uri != null) {
        AlertDialog(
            onDismissRequest = { pendingRestoreUri = null },
            title = { Text("Replace all local data?") },
            text = { Text("Restoring will delete every profile, draft, and job history entry currently on this device and replace them with the contents of the backup file. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingRestoreUri = null
                    scope.launch {
                        statusMessage = try {
                            val text = withContext(Dispatchers.IO) {
                                context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
                            } ?: throw IllegalStateException("Couldn't read the selected file")
                            val db = AppDatabase.getInstance(context)
                            BackupHelper.importFromJson(JSONObject(text), db)
                            onRestored()
                            "Restore complete."
                        } catch (e: Exception) {
                            "Restore failed: ${e.message}"
                        }
                    }
                }) { Text("Replace data") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreUri = null }) { Text("Cancel") }
            }
        )
    }
}
