package com.example.bulkmessenger.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bulkmessenger.data.UserProfile
import com.example.bulkmessenger.ui.compose.AvatarCircle
import com.example.bulkmessenger.ui.onboarding.ProfileForm
import com.example.bulkmessenger.ui.theme.AccentColors
import com.example.bulkmessenger.viewmodel.SessionViewModel
import com.example.bulkmessenger.viewmodel.ThemeMode

@Composable
fun HomeScreen(
    onNewBroadcast: () -> Unit,
    onNewPersonalized: () -> Unit,
    onDrafts: () -> Unit,
    onJobHistory: () -> Unit,
    sessionViewModel: SessionViewModel,
    onSettings: () -> Unit = {},
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onToggleTheme: () -> Unit = {}
) {
    val users by sessionViewModel.users.collectAsState()
    val activeUser by sessionViewModel.activeUser.collectAsState()
    var showProfileSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bulk Messenger") },
                actions = {
                    IconButton(onClick = { showProfileSheet = true }) {
                        val user = activeUser
                        if (user != null) {
                            AvatarCircle(name = user.name, colorHex = user.avatarColorHex, size = 28.dp)
                        } else {
                            AvatarCircle(name = "?", colorHex = "#9E9E9E", size = 28.dp)
                        }
                    }
                    IconButton(onClick = onToggleTheme) {
                        val (icon, description) = when (themeMode) {
                            ThemeMode.SYSTEM -> Icons.Filled.BrightnessAuto to "Theme: System (tap for Light)"
                            ThemeMode.LIGHT -> Icons.Filled.LightMode to "Theme: Light (tap for Dark)"
                            ThemeMode.DARK -> Icons.Filled.DarkMode to "Theme: Dark (tap for System)"
                        }
                        Icon(icon, contentDescription = description)
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        "Send one message to many people, or a different message to each.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Automated sending, one tap.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionCard(
                    icon = Icons.Filled.Campaign,
                    accent = AccentColors.Broadcast,
                    title = "New Broadcast",
                    subtitle = "Same message → many numbers",
                    onClick = onNewBroadcast
                )
                ActionCard(
                    icon = Icons.Filled.EditNote,
                    accent = AccentColors.Personalized,
                    title = "New Personalized Batch",
                    subtitle = "Different message per number",
                    onClick = onNewPersonalized
                )
                ActionCard(
                    icon = Icons.Filled.Description,
                    accent = AccentColors.Drafts,
                    title = "Message Drafts",
                    subtitle = "Reusable saved templates",
                    onClick = onDrafts
                )
                ActionCard(
                    icon = Icons.Filled.History,
                    accent = AccentColors.JobHistory,
                    title = "Job History",
                    subtitle = "Track sent, failed, pending",
                    onClick = onJobHistory
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }

    if (showProfileSheet) {
        ProfileSwitcherSheet(
            users = users,
            activeUserId = activeUser?.id,
            onSelectUser = { id ->
                sessionViewModel.switchUser(id)
                showProfileSheet = false
            },
            onCreateUser = { name, colorHex ->
                sessionViewModel.createUser(name, colorHex) { showProfileSheet = false }
            },
            onDismiss = { showProfileSheet = false }
        )
    }
}

@Composable
private fun ProfileSwitcherSheet(
    users: List<UserProfile>,
    activeUserId: Long?,
    onSelectUser: (Long) -> Unit,
    onCreateUser: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var showAddForm by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            if (showAddForm) {
                Text("New profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(16.dp))
                ProfileForm(submitLabel = "Create", onSubmit = onCreateUser)
            } else {
                Text("Profiles", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                users.forEach { user ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectUser(user.id) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AvatarCircle(name = user.name, colorHex = user.avatarColorHex)
                        Spacer(Modifier.width(12.dp))
                        Text(user.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        if (user.id == activeUserId) {
                            Icon(Icons.Filled.Check, contentDescription = "Active", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                HorizontalDivider()
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAddForm = true }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Add profile", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun ActionCard(
    icon: ImageVector,
    accent: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(accent.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
