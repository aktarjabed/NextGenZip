package com.aktarjabed.nextgenzip.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aktarjabed.nextgenzip.data.CloudUsageStore
import java.text.DateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudUsageDashboard(onBack: () -> Unit = {}) {
    val store = CloudUsageStore // already initialized by App
    Scaffold(topBar = {
        TopAppBar(title = { Text("Cloud Usage") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) }
        })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Requests", style = MaterialTheme.typography.titleMedium)
                    Text("Total calls: ${store.callCount}", style = MaterialTheme.typography.bodyLarge)
                    Text("Last latency: ${store.lastLatencyMs} ms", style = MaterialTheme.typography.bodyMedium)
                    Text("Last error: ${store.lastError ?: "None"}", style = MaterialTheme.typography.bodyMedium)
                    if (store.lastTimestamp > 0) {
                        Text("Last call: ${DateFormat.getDateTimeInstance().format(Date(store.lastTimestamp))}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Notes", style = MaterialTheme.typography.titleMedium)
                    Text("- Usage count is local only and does not include token accounting.", style = MaterialTheme.typography.bodySmall)
                    Text("- To track actual token or cost, implement provider-side token reporting (optional).", style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { CloudUsageStore.reset() }) { Text("Reset Usage Data") }
        }
    }
}
