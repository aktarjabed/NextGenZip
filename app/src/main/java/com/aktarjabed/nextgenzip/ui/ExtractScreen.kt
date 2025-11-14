package com.aktarjabed.nextgenzip.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aktarjabed.nextgenzip.ui.components.PasswordDialog
import com.aktarjabed.nextgenzip.ui.components.ProgressCard
import com.aktarjabed.nextgenzip.viewmodel.ArchiveViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtractScreen(
    onNavigateBack: () -> Unit,
    viewModel: ArchiveViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var showPasswordDialog by remember { mutableStateOf(false) }
    var selectedArchive by remember { mutableStateOf<Uri?>(null) }

    val archivePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        selectedArchive = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Extract Archive") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.isProcessing) {
                ProgressCard(
                    progress = uiState.progress,
                    message = uiState.statusMessage
                )
            }

            ElevatedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Select Archive",
                        style = MaterialTheme.typography.titleMedium
                    )

                    if (selectedArchive != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedArchive?.lastPathSegment ?: "Unknown",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Ready to extract",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = { selectedArchive = null }) {
                                Icon(Icons.Default.Close, "Remove")
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                archivePicker.launch(arrayOf(
                                    "application/zip",
                                    "application/x-7z-compressed",
                                    "application/x-rar-compressed",
                                    "application/gzip",
                                    "application/x-tar"
                                ))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Browse Files")
                        }
                    }
                }
            }

            if (selectedArchive != null) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Options",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Password Protection",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = if (uiState.password != null) "Password set" else "No password",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            TextButton(onClick = { showPasswordDialog = true }) {
                                Text(if (uiState.password != null) "Change" else "Set")
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        selectedArchive?.let { uri ->
                            viewModel.extractArchive(context, uri)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isProcessing
                ) {
                    Icon(Icons.Default.UnfoldMore, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Extract Archive")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Info card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Column {
                        Text(
                            text = "Supported Formats",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "ZIP, 7Z, RAR, TAR.GZ, TAR.XZ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
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

    // Show result
    uiState.resultMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { viewModel.clearResult() },
            title = { Text(if (uiState.isSuccess) "Success" else "Error") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearResult()
                    if (uiState.isSuccess) {
                        selectedArchive = null
                    }
                }) {
                    Text("OK")
                }
            }
        )
    }
}
