package com.aktarjabed.nextgenzip.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aktarjabed.nextgenzip.ai.CloudAIClient
import com.aktarjabed.nextgenzip.data.ApiKeyStore
import com.aktarjabed.nextgenzip.data.CloudConfigStore
import com.aktarjabed.nextgenzip.data.model.ArchiveFormat
import com.aktarjabed.nextgenzip.ui.components.CloudHelpDialog
import com.aktarjabed.nextgenzip.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSettingsScreen(
    onBack: () -> Unit = {},
    onOpenUsage: () -> Unit = {},
    onOpenTroubleshoot: () -> Unit = {}
) {
    val ctx = LocalContext.current

    // Initialize secure stores
    LaunchedEffect(Unit) {
        ApiKeyStore.init(ctx)
        CloudConfigStore.init(ctx)
        CloudUsageStore.init(ctx)
    }

    val scope = rememberCoroutineScope()

    // Local state driven from CloudConfigStore
    var enableCloud by remember { mutableStateOf(CloudConfigStore.enabled) }
    var provider by remember { mutableStateOf(CloudConfigStore.provider ?: "openai") }
    var customEndpoint by remember { mutableStateOf(CloudConfigStore.customEndpoint ?: "") }
    var apiKeyText by remember { mutableStateOf(ApiKeyStore.getKey() ?: "") }
    var testing by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }

    // Other settings
    var compressionLevel by remember { mutableStateOf(5) }
    var enableMalwareScan by remember { mutableStateOf(true) }
    var autoQuarantine by remember { mutableStateOf(true) }
    var enableEncryption by remember { mutableStateOf(false) }
    var splitArchiveSize by remember { mutableStateOf(500) }
    var showHiddenFiles by remember { mutableStateOf(false) }
    var biometricLock by remember { mutableStateOf(false) }
    var autoCleanTemp by remember { mutableStateOf(true) }
    var defaultFormat by remember { mutableStateOf(ArchiveFormat.ZIP) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Advanced Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // CLOUD AI INTEGRATION SECTION (NEW)
            item {
                SettingsSection(
                    title = "🌐 Cloud AI Integration",
                    icon = Icons.Default.Star
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (enableCloud)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Enable/Disable Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Enable Cloud AI",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "Optional: Use cloud models with your API key (securely stored)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                                Switch(
                                    checked = enableCloud,
                                    onCheckedChange = {
                                        enableCloud = it
                                        CloudConfigStore.enabled = it
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = ElectricBlue,
                                        checkedTrackColor = ElectricBlue.copy(alpha = 0.5f)
                                    )
                                )
                            }

                            if (enableCloud) {
                                Divider()

                                // Provider Selection
                                Text(
                                    "Provider",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ProviderChip(
                                        "OpenAI",
                                        provider == "openai"
                                    ) {
                                        provider = "openai"
                                        CloudConfigStore.provider = "openai"
                                    }
                                    ProviderChip(
                                        "Custom",
                                        provider == "custom"
                                    ) {
                                        provider = "custom"
                                        CloudConfigStore.provider = "custom"
                                    }
                                }

                                // API Key Input
                                OutlinedTextField(
                                    value = apiKeyText,
                                    onValueChange = { apiKeyText = it },
                                    label = { Text("API Key") },
                                    placeholder = { Text("Enter API key (will be encrypted)") },
                                    singleLine = true,
                                    trailingIcon = {
                                        if (apiKeyText.isNotBlank()) {
                                            IconButton(onClick = { apiKeyText = "" }) {
                                                Icon(Icons.Default.Close, "Clear")
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ElectricBlue,
                                        focusedLabelColor = ElectricBlue
                                    )
                                )

                                // Custom Endpoint (if provider is custom)
                                if (provider == "custom") {
                                    OutlinedTextField(
                                        value = customEndpoint,
                                        onValueChange = { customEndpoint = it },
                                        label = { Text("Custom Endpoint (POST)") },
                                        placeholder = { Text("https://your-host/v1/generate") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                // Action Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            ApiKeyStore.saveKey(apiKeyText.trim())
                                            CloudConfigStore.provider = provider
                                            CloudConfigStore.customEndpoint = customEndpoint.ifBlank { null }
                                            CloudConfigStore.enabled = true
                                            enableCloud = true
                                            Toast.makeText(ctx, "✅ Cloud settings saved securely", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ElectricBlue
                                        )
                                    ) {
                                        Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Save Key")
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            ApiKeyStore.clearKey()
                                            apiKeyText = ""
                                            Toast.makeText(ctx, "API key cleared", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Clear")
                                    }
                                }

                                // Test Connection
                                Button(
                                    onClick = {
                                        scope.launch {
                                            testing = true
                                            try {
                                                val key = ApiKeyStore.getKey()
                                                val prov = CloudConfigStore.provider ?: provider
                                                val endpoint = CloudConfigStore.customEndpoint ?: customEndpoint

                                                if (key.isNullOrBlank()) {
                                                    Toast.makeText(ctx, "⚠️ Set API key before testing", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    val result = CloudAIClient.generate(
                                                        prov,
                                                        key,
                                                        "Say: Hello from NextGenZip test",
                                                        endpoint
                                                    )
                                                    Toast.makeText(
                                                        ctx,
                                                        "✅ Test OK: ${result.take(100)}...",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            } catch (t: Throwable) {
                                                Toast.makeText(
                                                    ctx,
                                                    "❌ Test failed: ${t.message?.take(100)}",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            } finally {
                                                testing = false
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !testing,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AccentPurple
                                    )
                                ) {
                                    if (testing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(Icons.Default.Send, null)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (testing) "Testing..." else "Test Connection")
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        "💡 Local AI is always available without any API key. Enable cloud mode only if you want enhanced AI capabilities.",
                                        modifier = Modifier.padding(12.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // AI Settings Section
            item {
                SettingsSection(
                    title = "🤖 AI Settings",
                    icon = Icons.Default.Star
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SettingCard(
                            title = "Local AI Model",
                            description = "Manage offline AI models"
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                ModelInfoRow("Status", "Ready", SuccessGreen)
                                ModelInfoRow("Size", "248 MB", ElectricBlue)
                                ModelInfoRow("Version", "v2.1", AccentPurple)
                            }
                        }
                    }
                }
            }

            // Compression Settings Section
            item {
                SettingsSection(
                    title = "📦 Compression Settings",
                    icon = Icons.Default.Settings
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SettingCard(
                            title = "Default Format",
                            description = "Choose default archive format"
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FormatChip(
                                    format = ArchiveFormat.ZIP,
                                    selected = defaultFormat == ArchiveFormat.ZIP,
                                    onSelect = { defaultFormat = ArchiveFormat.ZIP }
                                )
                                FormatChip(
                                    format = ArchiveFormat.SEVEN_ZIP,
                                    selected = defaultFormat == ArchiveFormat.SEVEN_ZIP,
                                    onSelect = { defaultFormat = ArchiveFormat.SEVEN_ZIP }
                                )
                                FormatChip(
                                    format = ArchiveFormat.TAR,
                                    selected = defaultFormat == ArchiveFormat.TAR,
                                    onSelect = { defaultFormat = ArchiveFormat.TAR }
                                )
                            }
                        }

                        SettingCard(
                            title = "Compression Level",
                            description = "Balance between speed and size"
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Fast", style = MaterialTheme.typography.labelSmall)
                                    Text(
                                        when (compressionLevel) {
                                            in 0..2 -> "Fast"
                                            in 3..5 -> "Normal"
                                            in 6..7 -> "High"
                                            else -> "Ultra"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = ElectricBlue
                                    )
                                    Text("Ultra", style = MaterialTheme.typography.labelSmall)
                                }

                                Slider(
                                    value = compressionLevel.toFloat(),
                                    onValueChange = { compressionLevel = it.toInt() },
                                    valueRange = 0f..9f,
                                    steps = 8,
                                    colors = SliderDefaults.colors(
                                        thumbColor = ElectricBlue,
                                        activeTrackColor = ElectricBlue
                                    )
                                )
                            }
                        }

                        SettingSwitchCard(
                            title = "Default Encryption",
                            description = "Always encrypt new archives",
                            icon = Icons.Default.Lock,
                            checked = enableEncryption,
                            onCheckedChange = { enableEncryption = it }
                        )
                    }
                }
            }

            // Security Settings
            item {
                SettingsSection(
                    title = "🛡️ Security Settings",
                    icon = Icons.Default.Lock
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SettingSwitchCard(
                            title = "Malware Scanning",
                            description = "Scan files before extraction (Local AI)",
                            icon = Icons.Default.Warning,
                            checked = enableMalwareScan,
                            onCheckedChange = { enableMalwareScan = it }
                        )

                        SettingSwitchCard(
                            title = "Auto Quarantine",
                            description = "Automatically isolate threats",
                            icon = Icons.Default.Lock,
                            checked = autoQuarantine,
                            onCheckedChange = { autoQuarantine = it },
                            enabled = enableMalwareScan
                        )

                        SettingSwitchCard(
                            title = "Auto-Clean Temp Files",
                            description = "Delete temporary files after extraction",
                            icon = Icons.Default.Delete,
                            checked = autoCleanTemp,
                            onCheckedChange = { autoCleanTemp = it }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderChip(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onSelect,
        label = {
            Text(
                label,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = ElectricBlue,
            selectedLabelColor = Color.White
        )
    )
}
@Composable
fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        content()
    }
}

@Composable
fun SettingCard(
    title: String,
    description: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            content()
        }
    }
}

@Composable
fun SettingSwitchCard(
    title: String,
    description: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ElectricBlue,
                    checkedTrackColor = ElectricBlue.copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
fun FormatChip(
    format: ArchiveFormat,
    selected: Boolean,
    onSelect: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onSelect,
        label = {
            Text(
                text = format.extension.uppercase(),
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = ElectricBlue,
            selectedLabelColor = Color.White
        )
    )
}

@Composable
fun ModelInfoRow(
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
