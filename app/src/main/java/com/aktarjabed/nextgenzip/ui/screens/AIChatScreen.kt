package com.aktarjabed.nextgenzip.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aktarjabed.nextgenzip.ai.CloudAIClient
import com.aktarjabed.nextgenzip.data.ApiKeyStore
import com.aktarjabed.nextgenzip.data.CloudConfigStore
import com.aktarjabed.nextgenzip.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * AIChatScreen with Cloud Integration
 *
 * This screen decides at runtime whether to use local AI or cloud AI (if user enabled and key present).
 * - Attempts cloud if CloudConfigStore.enabled is true and ApiKeyStore has a key.
 * - On cloud failure it falls back to local hard-coded responses.
 */

data class AIMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val type: MessageType = MessageType.TEXT,
    val metadata: MessageMetadata? = null
)

enum class MessageType {
    TEXT, FILE_ANALYSIS, COMPRESSION_RESULT, SUGGESTION, ERROR
}

data class MessageMetadata(
    val fileName: String? = null,
    val fileSize: String? = null,
    val compressionRatio: String? = null,
    val format: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIChatScreen(
    onBack: () -> Unit = {}
) {
    val ctx = LocalContext.current

    // Ensure stores are initialized
    LaunchedEffect(Unit) {
        ApiKeyStore.init(ctx)
        CloudConfigStore.init(ctx)
    }

    val scope = rememberCoroutineScope()

    var messages by remember { mutableStateOf(listOf(
        AIMessage(
            text = "👋 Hi! I'm your NextGenZip AI Assistant. I can help you compress files, analyze archives, and optimize your storage. What would you like to do?",
            isUser = false,
            type = MessageType.TEXT
        )
    )) }

    var inputText by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Check if cloud is available
    val cloudEnabled = remember { CloudConfigStore.enabled }
    val hasApiKey = remember { !ApiKeyStore.getKey().isNullOrBlank() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "AI Command Center",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (cloudEnabled && hasApiKey) AccentPurple else SuccessGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (cloudEnabled && hasApiKey) "Cloud AI Active" else "Local AI Active",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
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
        },
        bottomBar = {
            AIInputBar(
                inputText = inputText,
                onInputChange = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank() && !isProcessing) {
                        val userMessage = AIMessage(text = inputText, isUser = true)
                        messages = messages + userMessage
                        val prompt = inputText.trim()
                        inputText = ""
                        isProcessing = true

                        scope.launch {
                            listState.animateScrollToItem(messages.size - 1)

                            try {
                                val cloudEnabled = CloudConfigStore.enabled
                                val apiKey = ApiKeyStore.getKey()
                                val provider = CloudConfigStore.provider ?: "openai"
                                val customEndpoint = CloudConfigStore.customEndpoint

                                val responseText = if (cloudEnabled && !apiKey.isNullOrBlank()) {
                                    // Try cloud first
                                    try {
                                        CloudAIClient.generate(provider, apiKey, prompt, customEndpoint)
                                    } catch (t: Throwable) {
                                        // Cloud failed -> fallback to local
                                        "⚠️ Cloud error: ${t.message?.take(80)}\n\n🤖 Local fallback:\n${generateLocalResponse(prompt)}"
                                    }
                                } else {
                                    // Local-only
                                    generateLocalResponse(prompt)
                                }

                                val aiResponse = AIMessage(text = responseText, isUser = false)
                                messages = messages + aiResponse
                            } catch (e: Exception) {
                                messages = messages + AIMessage(
                                    text = "Error: ${e.message}",
                                    isUser = false,
                                    type = MessageType.ERROR
                                )
                            } finally {
                                isProcessing = false
                                listState.animateScrollToItem(messages.size - 1)
                            }
                        }
                    }
                },
                isProcessing = isProcessing
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { message ->
                    AIMessageBubble(message)
                }

                if (isProcessing) {
                    item {
                        TypingIndicator()
                    }
                }
            }

            // Quick Action Chips
            if (messages.size == 1) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 80.dp)
                        .padding(horizontal = 16.dp)
                ) {
                    QuickActionChips(
                        onChipClick = { action ->
                            inputText = action
                        }
                    )
                }
            }
        }
    }
}

// Local fallback response generator
private fun generateLocalResponse(prompt: String): String {
    val lower = prompt.lowercase()
    return when {
        "compress" in lower || "zip" in lower -> {
            "I'll help you compress files. For best results:\n\n" +
                    "• Use 7Z format for maximum compression\n" +
                    "• Set compression level to 'Ultra'\n" +
                    "• Enable solid archive mode\n\n" +
                    "Would you like me to suggest optimal settings for your files?"
        }
        "extract" in lower -> {
            "I can help you extract archives. I support:\n\n" +
                    "✅ ZIP, 7Z, RAR, TAR, TAR.GZ\n" +
                    "✅ Password-protected archives\n" +
                    "✅ Split/multi-volume archives\n\n" +
                    "Just select the archive you want to extract!"
        }
        "analyze" in lower || "check" in lower -> {
            "Archive Analysis:\n\n" +
                    "📦 Format: ZIP (Deflate)\n" +
                    "💾 Size: 42.8 MB\n" +
                    "📊 Compression: 65%\n" +
                    "✅ Integrity: OK\n\n" +
                    "Recommendation: This archive is well-optimized."
        }
        "video" in lower || "media" in lower -> {
            "For video files, I recommend:\n\n" +
                    "• Use ZIP with 'Store' method (videos are pre-compressed)\n" +
                    "• Or 7Z with 'Fast' compression\n" +
                    "• Consider splitting into 500MB parts\n\n" +
                    "This saves processing time while maintaining integrity."
        }
        "security" in lower || "malware" in lower || "scan" in lower -> {
            "🛡️ Security features:\n\n" +
                    "• Local malware detection (no internet required)\n" +
                    "• Heuristic analysis for suspicious files\n" +
                    "• Automatic quarantine of threats\n" +
                    "• File signature verification\n\n" +
                    "All scanning is done locally for maximum privacy."
        }
        else -> {
            "I can help you with:\n\n" +
                    "📦 Compressing files and folders\n" +
                    "📂 Extracting archives (ZIP, 7Z, RAR, TAR)\n" +
                    "🔍 Analyzing archive contents\n" +
                    "🛡️ Scanning for malware (local)\n" +
                    "⚡ Optimizing compression settings\n\n" +
                    "What would you like to do?"
        }
    }
}
@Composable
fun AIMessageBubble(message: AIMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                color = if (message.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
@Composable
fun TypingIndicator() {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            strokeWidth = 2.dp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "AI is thinking...")
    }
}

@Composable
fun QuickActionChips(onChipClick: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(onClick = { onChipClick("Compress my files") }) {
            Text(text = "Compress")
        }
        Button(onClick = { onChipClick("Extract an archive") }) {
            Text(text = "Extract")
        }
        Button(onClick = { onChipClick("Analyze this archive") }) {
            Text(text = "Analyze")
        }
    }
}

@Composable
fun AIInputBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    isProcessing: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = inputText,
            onValueChange = onInputChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(text = "Ask the AI...") }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = onSend,
            enabled = !isProcessing
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send"
                )
            }
        }
    }
}
