package com.aktarjabed.nextgenzip.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun CloudHelpDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onOpenOpenAIDocs: () -> Unit = {},
    onOpenGeminiDocs: () -> Unit = {}
) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("How to get API keys") },
        text = {
            Text(
                "OpenAI: Visit https://platform.openai.com/account/api-keys to create an API key. Copy it and paste into the app.\n\n" +
                "Custom providers: the endpoint must accept POST {\"prompt\":\"...\"} and return JSON {\"text\":\"...\"} (or raw text).\n\n" +
                "Important: API keys are stored encrypted on your device. Do not share them."
            )
        },
        confirmButton = {
            Button(onClick = onOpenOpenAIDocs) { Text("Open OpenAI docs") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
