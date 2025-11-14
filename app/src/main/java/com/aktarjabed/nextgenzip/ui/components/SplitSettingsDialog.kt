package com.aktarjabed.nextgenzip.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SplitSettingsDialog(
    currentSplitSizeMB: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var splitSize by remember { mutableStateOf(currentSplitSizeMB.toString()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Split Archive") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Split the archive into multiple volumes. Minimum size: 1 MB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                OutlinedTextField(
                    value = splitSize,
                    onValueChange = {
                        splitSize = it.filter { char -> char.isDigit() }
                        error = null
                    },
                    label = { Text("Split Size (MB)") },
                    suffix = { Text("MB") },
                    isError = error != null,
                    supportingText = error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick size buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(100, 500, 1024, 2048).forEach { size ->
                        FilterChip(
                            selected = splitSize == size.toString(),
                            onClick = { splitSize = size.toString() },
                            label = { Text("${size}MB") }
                        )
                    }
                }

                TextButton(
                    onClick = { splitSize = "0" },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Disable Split")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val size = splitSize.toIntOrNull() ?: 0
                    when {
                        size == 0 -> onConfirm(0) // Disable split
                        size < 1 -> error = "Minimum split size is 1 MB"
                        else -> onConfirm(size)
                    }
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
