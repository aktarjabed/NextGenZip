package com.aktarjabed.nextgenzip.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aktarjabed.nextgenzip.data.models.ArchiveHistoryEntry
import com.aktarjabed.nextgenzip.viewmodel.HistoryViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun HistoryScreen(viewModel: HistoryViewModel = viewModel()) {
    val history = viewModel.archiveHistory.collectAsState(initial = emptyList())

    LazyColumn(
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(history.value) { entry ->
            ArchiveHistoryItem(entry)
        }
    }
}

@Composable
fun ArchiveHistoryItem(entry: ArchiveHistoryEntry) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = entry.archivePath, style = MaterialTheme.typography.titleMedium)
            Text(text = "Operation: ${entry.operation}")
            Text(text = "Timestamp: ${entry.timestamp}")
            Text(text = if (entry.success) "Status: Success" else "Status: Failed")
            if (entry.notes.isNotEmpty()) {
                Text(text = "Notes: ${entry.notes}")
            }
        }
    }
}
