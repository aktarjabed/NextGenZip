package com.aktarjabed.nextgenzip.ui.screens

import android.app.AlertDialog
import android.widget.EditText
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aktarjabed.nextgenzip.security.SecurityConfig
import com.aktarjabed.nextgenzip.viewmodel.MalwareDetectionViewModel
import kotlinx.coroutines.launch

@Composable
fun SecuritySettingsScreen(
    onClose: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel: MalwareDetectionViewModel = viewModel()

    var apiKey by remember { mutableStateOf("") }
    var enableMalwareScan by remember { mutableStateOf(true) }
    var autoQuarantine by remember { mutableStateOf(true) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Load settings on first compose
    LaunchedEffect(Unit) {
        scope.launch {
            apiKey = SecurityConfig.getVirusTotalApiKey(context) ?: ""
            enableMalwareScan = SecurityConfig.isMalwareScanEnabled(context)
            autoQuarantine = SecurityConfig.isAutoQuarantineEnabled(context)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(0.dp)
    ) {
        // Top Bar
        TopAppBar(
            title = { Text("🔐 Security Settings", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, "Close")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        )

        // Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Section 1: Malware Scanning
            item {
                Text(
                    "🛡️ Malware Detection",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Enable Malware Scanning",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Scan files before extraction for threats",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }

                        Switch(
                            checked = enableMalwareScan,
                            onCheckedChange = { checked ->
                                enableMalwareScan = checked
                                scope.launch {
                                    SecurityConfig.setMalwareScanEnabled(context, checked)
                                }
                            },
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }

            // Section 2: Auto Quarantine
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Auto Quarantine Threats",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Automatically isolate detected malware",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }

                        Switch(
                            checked = autoQuarantine,
                            onCheckedChange = { checked ->
                                autoQuarantine = checked
                                scope.launch {
                                    SecurityConfig.setAutoQuarantine(context, checked)
                                }
                            },
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }

            // Section 3: VirusTotal Integration
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "☁️ Cloud Threat Detection",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE3F2FD)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "VirusTotal API Integration",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            "Real-time scanning using 70+ antivirus engines",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (apiKey.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                                    .background(
                                        color = Color(0xFFC8E6C9),
                                        shape = MaterialTheme.shapes.small
                                    )
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Configured",
                                    tint = Color.Green,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "✅ API Key Configured",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF1B5E20),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                                    .background(
                                        color = Color(0xFFFFCDD2),
                                        shape = MaterialTheme.shapes.small
                                    )
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Not Configured",
                                    tint = Color.Red,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "❌ API Key Not Set",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFC62828),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { showApiKeyDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Configure API Key")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            "Get your free API key from: https://www.virustotal.com/gui/sign-in",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // Section 4: Detection Levels
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "📊 Detection Levels",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        RiskLevelItem("🔴 Critical", "81-100%", "Immediate threat - Auto quarantine")
                        Spacer(modifier = Modifier.height(12.dp))
                        RiskLevelItem("🟠 High", "61-80%", "Significant threat - Recommend quarantine")
                        Spacer(modifier = Modifier.height(12.dp))
                        RiskLevelItem("🟡 Medium", "41-60%", "Requires review - Show confirmation")
                        Spacer(modifier = Modifier.height(12.dp))
                        RiskLevelItem("🔵 Low", "21-40%", "Minor concern - Show warning")
                        Spacer(modifier = Modifier.height(12.dp))
                        RiskLevelItem("✅ Safe", "0-20%", "No threat detected - Extract normally")
                    }
                }
            }

            // Section 5: Information
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF9C4)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "💡 Tips for Best Protection",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        TipItem("Enable malware scanning to protect against known threats")
                        TipItem("Enable auto-quarantine to isolate suspicious files automatically")
                        TipItem("Set up VirusTotal API for comprehensive cloud-based detection")
                        TipItem("Review quarantined files regularly and delete unknown ones")
                        TipItem("Don't restore files unless you're certain they're safe")
                    }
                }
            }

            // Section 6: About
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "ℹ️ About Security",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            "NextGenZip uses AI-powered malware detection to protect your files:\n\n" +
                            "• Local heuristic scanning (instant, no internet)\n" +
                            "• VirusTotal cloud scanning (70+ engines)\n" +
                            "• Automatic quarantine system\n" +
                            "• Archive sanitization\n" +
                            "• Threat history tracking",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // API Key Dialog
    if (showApiKeyDialog) {
        ApiKeyDialog(
            currentKey = apiKey,
            onConfirm = { newKey ->
                apiKey = newKey
                scope.launch {
                    SecurityConfig.setVirusTotalApiKey(context, newKey)
                }
                showApiKeyDialog = false
            },
            onDismiss = { showApiKeyDialog = false }
        )
    }
}

@Composable
private fun RiskLevelItem(level: String, range: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                level,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                range,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
        Text(
            description,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        )
    }
}

@Composable
private fun TipItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text("✓ ", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApiKeyDialog(
    currentKey: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var inputKey by remember { mutableStateOf(currentKey) }
    var showPassword by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("VirusTotal API Key", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    "Get your free API key from:\nhttps://www.virustotal.com/gui/sign-in",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = inputKey,
                    onValueChange = { inputKey = it },
                    label = { Text("API Key") },
                    placeholder = { Text("Paste your API key") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    singleLine = true,
                    visualTransformation = if (showPassword) {
                        androidx.compose.ui.text.input.VisualTransformation.None
                    } else {
                        androidx.compose.ui.text.input.PasswordVisualTransformation()
                    }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = showPassword,
                        onCheckedChange = { showPassword = it }
                    )
                    Text(
                        "Show API Key",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(inputKey) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
