package com.example.bulkmessenger.ui.compose

import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bulkmessenger.util.ContactPickerHelper
import com.example.bulkmessenger.viewmodel.PersonalizedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PersonalizedRowEditorScreen(
    rowId: String?,
    viewModel: PersonalizedViewModel,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val existing = remember(rowId) { rowId?.let { viewModel.getRow(it) } }
    var number by remember { mutableStateOf(existing?.phoneNumber ?: "") }
    var message by remember { mutableStateOf(existing?.message ?: "") }
    val isEditing = existing != null

    val contactPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data
        if (uri != null) {
            scope.launch {
                val contact = withContext(Dispatchers.IO) { ContactPickerHelper.resolve(context, uri) }
                if (contact != null) {
                    number = contact.phoneNumber
                    if (message.isBlank() && contact.name != null) {
                        message = "Hi ${contact.name}, "
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Recipient" else "Add Recipient") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(onClick = {
                            viewModel.removeRow(rowId!!)
                            onDone()
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete row")
                        }
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
            OutlinedTextField(
                value = number,
                onValueChange = { number = it },
                label = { Text("Phone number") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

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

            val drafts by viewModel.drafts.collectAsState()
            TemplateMessageField(
                message = message,
                onMessageChange = { message = it },
                drafts = drafts,
                modifier = Modifier.fillMaxWidth().weight(1f),
                minLines = 6,
                fillRemainingHeight = true
            )

            Button(
                onClick = {
                    viewModel.saveRow(rowId, number.trim(), message)
                    onDone()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = number.isNotBlank() && message.isNotBlank()
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (isEditing) "Save Changes" else "Add to Batch")
            }
        }
    }
}
