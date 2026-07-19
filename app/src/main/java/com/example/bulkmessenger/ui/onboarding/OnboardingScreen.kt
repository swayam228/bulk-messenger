package com.example.bulkmessenger.ui.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.bulkmessenger.R
import com.example.bulkmessenger.ui.settings.BackupRestoreSection
import com.example.bulkmessenger.util.BackupPrefs
import com.example.bulkmessenger.worker.AutoBackupWorker

private enum class OnboardingStep { PROFILE, BACKUP_LOCATION }

@Composable
fun OnboardingScreen(onCreateUser: (name: String, colorHex: String) -> Unit) {
    val context = LocalContext.current
    var showRestore by remember { mutableStateOf(false) }
    var step by remember { mutableStateOf(OnboardingStep.PROFILE) }
    var pendingName by remember { mutableStateOf("") }
    var pendingColorHex by remember { mutableStateOf("") }

    val backupLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        context.contentResolver.takePersistableUriPermission(
            uri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        BackupPrefs.setBackupUri(context, uri)
        AutoBackupWorker.scheduleIfConfigured(context)
        onCreateUser(pendingName, pendingColorHex)
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (step) {
                OnboardingStep.PROFILE -> {
                    Image(
                        painter = painterResource(R.drawable.ic_logo),
                        contentDescription = null,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Let's get you set up",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Create your profile to start sending.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(32.dp))
                    ProfileForm(
                        submitLabel = "Get Started",
                        onSubmit = { name, colorHex ->
                            pendingName = name
                            pendingColorHex = colorHex
                            step = OnboardingStep.BACKUP_LOCATION
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(20.dp))
                    TextButton(onClick = { showRestore = !showRestore }) {
                        Text(if (showRestore) "Hide restore option" else "Restoring on a new device? Restore from backup")
                    }
                    if (showRestore) {
                        BackupRestoreSection(
                            onRestored = {},
                            showBackupButton = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                OnboardingStep.BACKUP_LOCATION -> {
                    Icon(
                        Icons.Filled.Backup,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Set up automatic backups",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Choose a file location where Bulk Messenger will keep your data backed up " +
                            "automatically every 6 hours, so nothing is lost if the app is ever uninstalled.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(32.dp))
                    Button(
                        onClick = { backupLocationLauncher.launch("bulkmessenger-backup.json") },
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Icon(Icons.Filled.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Choose backup location")
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "This is required to continue — you can change it later in Settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
