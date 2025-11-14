package com.aktarjabed.nextgenzip.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aktarjabed.nextgenzip.ui.components.FileListItem
import com.aktarjabed.nextgenzip.ui.components.PasswordDialog
import com.aktarjabed.nextgenzip.ui.components.ProgressCard
import com.aktarjabed.nextgenzip.ui.components.SplitSettingsDialog
import com.aktarjabed.nextgenzip.viewmodel.ArchiveViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    onNavigateBack: () -> Unit,
    viewModel: ArchiveViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var showPasswordDialog by remember { mutableStateOf(false) }
    var showSplitDialog by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        viewModel.addFiles(uris)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Archive") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showPasswordDialog = true }) {
                        Icon(Icons.Default.Lock, "Password")
                    }
                    IconButton(onClick = { showSplitDialog = true }) {
                        Icon(Icons.Default.Splitscreen, "Split")
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.selectedFiles.isNotEmpty() && !uiState.isProcessing) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.createArchive(context) },
                    icon = { Icon(Icons.Default.Archive, "Create") },
                    text = { Text("Create ZIP") }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isProcessing) {
                ProgressCard(
                    progress = uiState.progress,
                    message = uiState.statusMessage
                )
            }

            // Settings chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.password != null,
                    onClick = { showPasswordDialog = true },
                    label = { Text(if (uiState.password != null) "Password: ●●●●" else "No password") },
                    leadingIcon = { Icon(Icons.Default.Lock, null) }
                )

                FilterChip(
                    selected = uiState.splitSizeBytes > 0,
                    onClick = { showSplitDialog = true },
                    label = {
                        Text(if (uiState.splitSizeBytes > 0) "Split: ${uiState.splitSizeBytes / 1024 / 1024}MB" else "No split")
                    },
                    leadingIcon = { Icon(Icons.Default.CallSplit, null) }
                )
            }

            if (uiState.selectedFiles.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FileUpload,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No files selected",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Tap the button below to add files",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { filePicker.launch(arrayOf("*/*")) }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add Files")
                    }
                }
            } else {
                // File list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.selectedFiles) { uri ->
                        FileListItem(
                            uri = uri,
                            onRemove = { viewModel.removeFile(uri) }
                        )
                    }
                }

                Button(
                    onClick = { filePicker.launch(arrayOf("*/*")) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add More Files")
                }
            }
        }
    }

    if (showPasswordDialog) {
        PasswordDialog(
            currentPassword = uiState.password,
            onDismiss = { showPasswordDialog = false },
            onConfirm = { password ->
                viewModel.setPassword(password)
                showPasswordDialog = false
            }
        )
    }

    if (showSplitDialog) {
        SplitSettingsDialog(
            currentSplitSizeMB = (uiState.splitSizeBytes / 1024 / 1024).toInt(),
            onDismiss = { showSplitDialog = false },
            onConfirm = { sizeMB ->
                viewModel.setSplitSize(sizeMB * 1024L * 1024L)
                showSplitDialog = false
            }
        )
    }

    // Show result
    uiState.resultMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { viewModel.clearResult() },
            title = { Text(if (uiState.isSuccess) "Success" else "Error") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearResult()
                    if (uiState.isSuccess) onNavigateBack()
                }) {
                    Text("OK")
                }
            }
        )
    }
}
