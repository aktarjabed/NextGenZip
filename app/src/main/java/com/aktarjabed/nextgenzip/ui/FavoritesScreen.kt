package com.aktarjabed.nextgenzip.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aktarjabed.nextgenzip.viewmodel.FavoritesViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun FavoritesScreen(viewModel: FavoritesViewModel = viewModel()) {
    val favorites = viewModel.favoriteArchives.collectAsState(initial = emptyList())

    LazyColumn(
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(favorites.value) { archivePath ->
            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = archivePath,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
