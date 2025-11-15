package com.aktarjabed.nextgenzip.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aktarjabed.nextgenzip.data.ApiKeyStore
import com.aktarjabed.nextgenzip.data.CloudConfigStore
import com.aktarjabed.nextgenzip.data.CloudUsageStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TroubleshootingScreen(onBack: () -> Unit = {}) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var running by remember { mutableStateOf(false) }
    var lastResult by remember { mutableStateOf<String?>(null) }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Troubleshooter") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) }
        })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Run the automatic checks to diagnose common cloud issues.", style = MaterialTheme.typography.bodyMedium)
            Button(onClick = {
                if (running) return@Button
                running = true
                lastResult = null
                scope.launch {
                    val messages = mutableListOf<String>()
                    try {
                        // check network by trying to resolve a URL quickly (no heavy network)
                        withContext(Dispatchers.IO) {
                            val ok = try {
                                java.net.URL("https://api.openai.com").openConnection().connectTimeout = 3000
                                true
                            } catch (e: Exception) { false }
                            messages.add("Network check: ${if (ok) "OK" else "FAILED"}")
                        }

                        // check key present
                        val key = ApiKeyStore.getKey()
                        messages.add("API key: ${if (!key.isNullOrBlank()) "Present" else "Missing"}")

                        // check cloud config
                        messages.add("Cloud enabled: ${CloudConfigStore.enabled}, provider: ${CloudConfigStore.provider}")

                        // check last usage error
                        val lastErr = CloudUsageStore.lastError
                        messages.add("Last cloud error: ${lastErr ?: "None"}")

                        lastResult = messages.joinToString("\n")
                    } catch (t: Throwable) {
                        lastResult = "Troubleshooter failed: ${t.message}"
                    } finally {
                        running = false
                    }
                }
            }, enabled = !running) {
                if (running) CircularProgressIndicator(modifier = Modifier.size(18.dp))
                else Text("Run Checks")
            }

            lastResult?.let {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Result:", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(onClick = {
                ApiKeyStore.clearKey()
                CloudConfigStore.enabled = false
                CloudUsageStore.reset()
                Toast.makeText(ctx, "Cleared cloud settings", Toast.LENGTH_SHORT).show()
            }) {
                Text("Reset Cloud Config & Usage")
            }
        }
    }
}
