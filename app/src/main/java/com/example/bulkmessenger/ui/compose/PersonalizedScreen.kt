package com.example.bulkmessenger.ui.compose

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.bulkmessenger.viewmodel.PersonalizedRow
import com.example.bulkmessenger.viewmodel.PersonalizedViewModel
import com.example.bulkmessenger.viewmodel.SessionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val CSV_EXAMPLE = "+919876543210,Hi Raj, your order #482 has shipped!\n" +
    "+919876543211,Hi Neha, don't forget your appointment tomorrow at 5pm.\n" +
    "+919876543212,Hi Amit, thanks for signing up — here's your 10% code: WELCOME10"

@Composable
fun PersonalizedScreen(
    viewModel: PersonalizedViewModel,
    sessionViewModel: SessionViewModel,
    onDone: () -> Unit,
    onBack: () -> Unit = {},
    onAddRow: () -> Unit,
    onEditRow: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val activeUser by sessionViewModel.activeUser.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var csvText by remember { mutableStateOf("") }
    var showCsvDialog by remember { mutableStateOf(false) }
    var pasteListMode by remember { mutableStateOf(false) }
    var pasteText by remember { mutableStateOf("") }
    var addNumbersFeedback by remember { mutableStateOf<String?>(null) }
    var scheduledAtMillis by remember { mutableStateOf<Long?>(null) }
    var selectedSimId by remember(activeUser) { mutableStateOf(activeUser?.defaultSimSubscriptionId) }

    val csvFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val text = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { stream -> stream.bufferedReader().readText() }
                }
                if (text != null) csvText = text
                showCsvDialog = true
            }
        }
    }

    LaunchedEffect(state.lastCreatedJobId) {
        if (state.lastCreatedJobId != null) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Personalized Batch") },
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
            Text("Add rows", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !pasteListMode,
                    onClick = { pasteListMode = false; addNumbersFeedback = null },
                    label = { Text("Single entry") }
                )
                FilterChip(
                    selected = pasteListMode,
                    onClick = { pasteListMode = true; addNumbersFeedback = null },
                    label = { Text("Paste list") }
                )
            }

            if (!pasteListMode) {
                OutlinedButton(
                    onClick = onAddRow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Add Recipient")
                }
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
                        val result = viewModel.addNumbersAsRows(pasteText)
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
                Text(
                    "Numbers are added with a blank message — tap a row below to fill it in, or use a template.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showCsvDialog = true }, modifier = Modifier.weight(1f)) {
                    Text("Paste CSV")
                }
                OutlinedButton(onClick = { csvFileLauncher.launch("*/*") }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Import .csv")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recipients", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("${state.rows.size}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }

            if (state.rows.isEmpty()) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "No recipients yet.\nAdd one, or import a CSV above.",
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
                    items(state.rows, key = { it.rowId }) { row ->
                        PersonalizedRowCard(
                            row = row,
                            onClick = { onEditRow(row.rowId) },
                            onRemove = { viewModel.removeRow(row.rowId) }
                        )
                    }
                }
            }

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
                onClick = { viewModel.sendAll(scheduledAtMillis, selectedSimId) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = state.rows.any { it.phoneNumber.isNotBlank() && it.message.isNotBlank() }
            ) {
                val count = state.rows.count { it.phoneNumber.isNotBlank() && it.message.isNotBlank() }
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (scheduledAtMillis != null) "Schedule All ($count)" else "Send All ($count)")
            }
        }
    }

    if (showCsvDialog) {
        AlertDialog(
            onDismissRequest = { showCsvDialog = false },
            title = { Text("Import CSV") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        "Format: one row per line, phone number first, then a comma, then the message.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Only the first comma splits number from message — commas inside the message itself are fine.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            CSV_EXAMPLE,
                            modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Paste your rows below, or use \"Import .csv\" to load a file instead:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = csvText,
                        onValueChange = { csvText = it },
                        minLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.importCsv(csvText)
                    csvText = ""
                    showCsvDialog = false
                }) { Text("Import") }
            },
            dismissButton = {
                TextButton(onClick = { csvText = ""; showCsvDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun PersonalizedRowCard(row: PersonalizedRow, onClick: () -> Unit, onRemove: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    row.phoneNumber.ifBlank { "No number" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    row.message.ifBlank { "No message yet" },
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = if (row.message.isBlank()) FontStyle.Italic else FontStyle.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
