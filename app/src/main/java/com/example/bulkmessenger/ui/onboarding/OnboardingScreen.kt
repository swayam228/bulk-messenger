package com.example.bulkmessenger.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bulkmessenger.R
import com.example.bulkmessenger.ui.settings.BackupRestoreSection

@Composable
fun OnboardingScreen(onCreateUser: (name: String, colorHex: String) -> Unit) {
    var showRestore by remember { mutableStateOf(false) }

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
                onSubmit = onCreateUser,
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
    }
}
