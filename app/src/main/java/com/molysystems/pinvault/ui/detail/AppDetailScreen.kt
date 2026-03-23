package com.molysystems.pinvault.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.molysystems.pinvault.data.model.CredentialField
import com.molysystems.pinvault.data.model.RecoveryCode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    onNavigateUp: () -> Unit,
    viewModel: AppDetailViewModel = hiltViewModel()
) {
    val app by viewModel.app.collectAsState()
    val fields by viewModel.credentialFields.collectAsState()
    val codes by viewModel.recoveryCodes.collectAsState()
    val remaining by viewModel.remainingCodeCount.collectAsState()

    var showAddFieldDialog by rememberSaveable { mutableStateOf(false) }
    var showEditFieldDialog by rememberSaveable { mutableStateOf(false) }
    var editingField by remember { mutableStateOf<CredentialField?>(null) }
    var showAddCodesDialog by rememberSaveable { mutableStateOf(false) }
    var recoveryExpanded by rememberSaveable { mutableStateOf(false) }

    val clipboard = LocalClipboardManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(app?.displayName ?: "App Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // ── Credential Fields section ──────────────────────────────────
            item {
                SectionHeader(
                    title = "Credentials",
                    action = {
                        IconButton(onClick = { showAddFieldDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add field")
                        }
                    }
                )
            }

            if (fields.isEmpty()) {
                item {
                    Text(
                        "No credentials yet. Tap + to add.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            items(fields, key = { it.id }) { field ->
                CredentialFieldItem(
                    field = field,
                    onCopy = {
                        val value = viewModel.decryptField(field)
                        clipboard.setText(AnnotatedString(value))
                    },
                    onEdit = {
                        editingField = field
                        showEditFieldDialog = true
                    },
                    onDelete = { viewModel.deleteCredentialField(field) }
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // ── Recovery Codes section ─────────────────────────────────────
            item {
                SectionHeader(
                    title = "Recovery Codes",
                    subtitle = if (codes.isNotEmpty()) "$remaining of ${codes.size} remaining" else null,
                    action = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { showAddCodesDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Add codes")
                            }
                            if (codes.isNotEmpty()) {
                                IconButton(onClick = { recoveryExpanded = !recoveryExpanded }) {
                                    Icon(
                                        if (recoveryExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = if (recoveryExpanded) "Collapse" else "Expand"
                                    )
                                }
                            }
                        }
                    }
                )
            }

            if (codes.isEmpty()) {
                item {
                    Text(
                        "No recovery codes yet. Tap + to add.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else if (recoveryExpanded) {
                items(codes, key = { it.id }) { code ->
                    RecoveryCodeItem(
                        code = code,
                        decryptedValue = viewModel.decryptRecoveryCode(code),
                        onToggleUsed = { isUsed ->
                            if (isUsed) viewModel.markCodeUsed(code.id)
                            else viewModel.markCodeUnused(code.id)
                        },
                        onDelete = { viewModel.deleteRecoveryCode(code) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    // Add field dialog
    if (showAddFieldDialog) {
        AddFieldDialog(
            onConfirm = { label, value ->
                viewModel.addCredentialField(label, value)
                showAddFieldDialog = false
            },
            onDismiss = { showAddFieldDialog = false }
        )
    }

    // Edit field dialog
    if (showEditFieldDialog && editingField != null) {
        EditFieldDialog(
            field = editingField!!,
            currentLabel = editingField!!.label,
            onConfirm = { newLabel, newValue ->
                viewModel.updateCredentialField(editingField!!, newLabel, newValue)
                showEditFieldDialog = false
                editingField = null
            },
            onDismiss = {
                showEditFieldDialog = false
                editingField = null
            }
        )
    }

    // Add recovery codes dialog
    if (showAddCodesDialog) {
        AddRecoveryCodesDialog(
            onConfirm = { label, rawCodes ->
                viewModel.addRecoveryCodes(label, rawCodes)
                showAddCodesDialog = false
            },
            onDismiss = { showAddCodesDialog = false }
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String? = null,
    action: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        action()
    }
    HorizontalDivider()
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
private fun CredentialFieldItem(
    field: CredentialField,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Key,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(field.label, style = MaterialTheme.typography.labelMedium)
                Text(
                    "••••••••",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onCopy) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy ${field.label}")
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit ${field.label}")
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete ${field.label}",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun RecoveryCodeItem(
    code: RecoveryCode,
    decryptedValue: String,
    onToggleUsed: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = code.isUsed,
                onCheckedChange = onToggleUsed
            )
            Text(
                decryptedValue,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f),
                color = if (code.isUsed)
                    MaterialTheme.colorScheme.onSurfaceVariant
                else
                    MaterialTheme.colorScheme.onSurface
            )
            Text(
                code.serviceLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 4.dp)
            )
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete code",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun AddFieldDialog(
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var label by rememberSaveable { mutableStateOf("") }
    var value by rememberSaveable { mutableStateOf("") }
    var showValue by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Credential") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label (e.g. Login PIN)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Value") },
                    singleLine = true,
                    visualTransformation = if (showValue) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        TextButton(onClick = { showValue = !showValue }) {
                            Text(if (showValue) "Hide" else "Show")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(label.trim(), value) },
                enabled = label.isNotBlank() && value.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun EditFieldDialog(
    field: CredentialField,
    currentLabel: String,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var label by rememberSaveable { mutableStateOf(currentLabel) }
    var value by rememberSaveable { mutableStateOf("") }
    var showValue by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Credential") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("New Value") },
                    placeholder = { Text("Leave blank to keep current") },
                    singleLine = true,
                    visualTransformation = if (showValue) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        TextButton(onClick = { showValue = !showValue }) {
                            Text(if (showValue) "Hide" else "Show")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(label.trim(), value) },
                enabled = label.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AddRecoveryCodesDialog(
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var serviceLabel by rememberSaveable { mutableStateOf("") }
    var rawCodes by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Recovery Codes") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = serviceLabel,
                    onValueChange = { serviceLabel = it },
                    label = { Text("Service label (e.g. Google 2FA)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = rawCodes,
                    onValueChange = { rawCodes = it },
                    label = { Text("Recovery codes (one per line)") },
                    minLines = 5,
                    maxLines = 12,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                Text(
                    "Paste all codes at once. Each line is saved as one code.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(serviceLabel.trim(), rawCodes) },
                enabled = serviceLabel.isNotBlank() && rawCodes.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
