package com.aktarjabed.nextgenzip.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.aktarjabed.nextgenzip.ai.AIManager
import java.io.File
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.aktarjabed.nextgenzip.workers.ArchiveWorker

@Composable
fun HomeScreen() {
val context = LocalContext.current
var status by remember { mutableStateOf("Ready") }
val picked = remember { mutableStateListOf<Uri>() }
var extracting by remember { mutableStateOf(false) }
var extractProgress by remember { mutableStateOf(0f) }

val openPicker = rememberLauncherForActivityResult(
ActivityResultContracts.OpenMultipleDocuments()
) { uris ->
picked.clear()
picked.addAll(uris)
status = "Picked ${uris.size} file(s)"
}

Column(
modifier = Modifier
.fillMaxSize()
.padding(16.dp),
verticalArrangement = Arrangement.spacedBy(12.dp)
) {
Text(text = "NextGenZip - Stage B Demo")

Button(
onClick = { openPicker.launch(arrayOf("*/*")) },
modifier = Modifier.fillMaxWidth()
) {
Text("Pick Files (SAF)")
}

Text(text = "Status: $status")

Button(
onClick = {
if (picked.isEmpty()) {
status = "No files selected"
return@Button
}
status = "Creating ZIP..."
val uriStrings = picked.map { it.toString() }.toTypedArray()
val inputData = workDataOf(
ArchiveWorker.KEY_OP_TYPE to ArchiveWorker.OP_COMPRESS,
ArchiveWorker.KEY_URIS to uriStrings,
ArchiveWorker.KEY_OUT_PATH to "${context.cacheDir.absolutePath}/archive-${System.currentTimeMillis()}.zip"
)
val request = OneTimeWorkRequestBuilder<ArchiveWorker>()
.setInputData(inputData)
.build()
WorkManager.getInstance(context).enqueue(request)
status = "Compression job enqueued."
},
modifier = Modifier.fillMaxWidth()
) {
Text("Create ZIP")
}

Button(
onClick = {
if (picked.size != 1) {
status = "Select exactly 1 archive"
return@Button
}
extracting = true
status = "Extracting..."
val uriStrings = picked.map { it.toString() }.toTypedArray()
val outDir = File(context.cacheDir, "extracted-${System.currentTimeMillis()}")
val inputData = workDataOf(
ArchiveWorker.KEY_OP_TYPE to ArchiveWorker.OP_EXTRACT,
ArchiveWorker.KEY_URIS to uriStrings,
ArchiveWorker.KEY_OUT_PATH to outDir.absolutePath
)
val request = OneTimeWorkRequestBuilder<ArchiveWorker>()
.setInputData(inputData)
.build()
WorkManager.getInstance(context).enqueue(request)
status = "Extraction job enqueued."
},
modifier = Modifier.fillMaxWidth()
) {
Text("Extract Archive")
}

Button(
onClick = {
if (picked.isEmpty()) {
status = "No files selected"
return@Button
}
status = "AI analyzing..."
// In a real app, you might pass file info to the AI
// For this demo, it remains a mock call.
status = "AI feature not implemented in this version."
},
modifier = Modifier.fillMaxWidth()
) {
Text("AI Analyze (Mock)")
}

if (extracting) {
Row(
verticalAlignment = Alignment.CenterVertically,
modifier = Modifier.fillMaxWidth()
) {
CircularProgressIndicator(
progress = extractProgress,
modifier = Modifier.size(48.dp)
)
Spacer(modifier = Modifier.width(12.dp))
Text("${(extractProgress * 100).toInt()}%")
}
}

LazyColumn(modifier = Modifier.fillMaxSize()) {
items(picked) { uri ->
Text(
text = uri.toString(),
modifier = Modifier.padding(vertical = 4.dp)
)
}
}
}
}
