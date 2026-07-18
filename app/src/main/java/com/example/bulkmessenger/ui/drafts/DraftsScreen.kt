package com.example.bulkmessenger.ui.drafts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bulkmessenger.data.Draft
import com.example.bulkmessenger.viewmodel.DraftsViewModel

@Composable
fun DraftsScreen(onBack: () -> Unit = {}, viewModel: DraftsViewModel = viewModel()) {
    val drafts by viewModel.drafts.collectAsState()
    var showEditor by remember { mutableStateOf(false) }
    var editingDraft by remember { mutableStateOf<Draft?>(null) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Drafts") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingDraft = null; title = ""; body = ""; showEditor = true
            }) { Icon(Icons.Default.Add, contentDescription = "New draft") }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
            items(drafts, key = { it.id }) { draft ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f).clickable {
                                editingDraft = draft; title = draft.title; body = draft.body; showEditor = true
                            }
                        ) {
                            Text(draft.title, style = MaterialTheme.typography.titleMedium)
                            Text(draft.body, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                        }
                        IconButton(onClick = { viewModel.delete(draft) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        AlertDialog(
            onDismissRequest = { showEditor = false },
            title = { Text(if (editingDraft == null) "New Draft" else "Edit Draft") },
            text = {
                Column {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text("Message") }, minLines = 4, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (title.isNotBlank() && body.isNotBlank()) {
                        viewModel.save(title, body, editingDraft?.id)
                        showEditor = false
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showEditor = false }) { Text("Cancel") } }
        )
    }
}
