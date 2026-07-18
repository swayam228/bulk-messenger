package com.example.bulkmessenger.ui.compose

import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bulkmessenger.util.ContactPickerHelper
import com.example.bulkmessenger.util.PickedContact
import com.example.bulkmessenger.viewmodel.BroadcastViewModel
import com.example.bulkmessenger.viewmodel.SessionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BroadcastScreen(
    sessionViewModel: SessionViewModel,
    onDone: () -> Unit,
    onBack: () -> Unit = {},
    viewModel: BroadcastViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val sentTodayNumbers by viewModel.sentTodayNumbers.collectAsState()
    val activeUser by sessionViewModel.activeUser.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var manualNumber by remember { mutableStateOf("") }
    var pasteListMode by remember { mutableStateOf(false) }
    var pasteText by remember { mutableStateOf("") }
    var addNumbersFeedback by remember { mutableStateOf<String?>(null) }
    var scheduledAtMillis by remember { mutableStateOf<Long?>(null) }
    var selectedSimId by remember(activeUser) { mutableStateOf(activeUser?.defaultSimSubscriptionId) }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data
        if (uri != null) {
            scope.launch {
                val contact = withContext(Dispatchers.IO) { ContactPickerHelper.resolve(context, uri) }
                contact?.let { viewModel.addRecipient(it) }
            }
        }
    }

    LaunchedEffect(state.lastCreatedJobId) {
        if (state.lastCreatedJobId != null) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Broadcast") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Add recipients", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !pasteListMode,
                    onClick = { pasteListMode = false; addNumbersFeedback = null },
                    label = { Text("One by one") }
                )
                FilterChip(
                    selected = pasteListMode,
                    onClick = { pasteListMode = true; addNumbersFeedback = null },
                    label = { Text("Paste list") }
                )
            }

            if (!pasteListMode) {
                OutlinedTextField(
                    value = manualNumber,
                    onValueChange = { manualNumber = it },
                    label = { Text("Type a phone number") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        TextButton(
                            onClick = { viewModel.addManualNumber(manualNumber); manualNumber = "" },
                            enabled = manualNumber.isNotBlank()
                        ) { Text("Add") }
                    }
                )
            } else {
                OutlinedTextField(
                    value = pasteText,
                    onValueChange = { pasteText = it },
                    label = { Text("Paste numbers, one per line") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4
                )
                Button(
                    onClick = {
                        val result = viewModel.addNumbers(pasteText)
                        pasteText = ""
                        addNumbersFeedback = buildString {
                            append("${result.added} number${if (result.added == 1) "" else "s"} added")
                            if (result.skipped > 0) append(", ${result.skipped} already in the list")
                        }
                    },
                    enabled = pasteText.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Add Numbers") }
                addNumbersFeedback?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }

            OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                    contactPickerLauncher.launch(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Pick from Contacts")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recipients", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("${state.recipients.size}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }

            if (state.recipients.isEmpty()) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "No recipients yet.\nPick a contact or type a number above.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(state.recipients) { contact ->
                        RecipientRow(
                            contact = contact,
                            sentToday = contact.phoneNumber in sentTodayNumbers,
                            onRemove = { viewModel.removeRecipient(contact) }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = state.message,
                onValueChange = { viewModel.updateMessage(it) },
                label = { Text("Message") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            SimSelector(
                selectedSubscriptionId = selectedSimId,
                onSelect = {
                    selectedSimId = it
                    sessionViewModel.setDefaultSim(it)
                }
            )

            ScheduleSelector(
                scheduledAtMillis = scheduledAtMillis,
                onScheduledAtChange = { scheduledAtMillis = it }
            )

            Button(
                onClick = { viewModel.sendToAll(scheduledAtMillis, selectedSimId) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = state.recipients.isNotEmpty() && state.message.isNotBlank()
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (scheduledAtMillis != null) "Schedule for ${state.recipients.size} recipient(s)"
                    else "Send to ${state.recipients.size} recipient(s)"
                )
            }
        }
    }
}

@Composable
private fun RecipientRow(contact: PickedContact, sentToday: Boolean = false, onRemove: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box {
                    Box(
                        modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            (contact.name?.firstOrNull() ?: '#').uppercaseChar().toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    if (sentToday) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(10.dp)
                                .background(Color(0xFFFFA726), CircleShape)
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(contact.name ?: contact.phoneNumber, style = MaterialTheme.typography.bodyMedium)
                        if (sentToday) {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Sent today",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFFA726)
                            )
                        }
                    }
                    if (contact.name != null) {
                        Text(contact.phoneNumber, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
