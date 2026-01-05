package com.vidora.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.vidora.app.data.remote.MediaItem
import com.vidora.app.ui.viewmodels.SearchViewModel
import com.vidora.app.ui.components.MediaCard
import com.vidora.app.ui.components.shimmerEffect

import com.vidora.app.ui.components.ErrorStateView
import com.vidora.app.ui.components.ShimmerCard
import com.vidora.app.ui.components.EmptyStateView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onMediaClick: (MediaItem) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TextField(
            value = uiState.query,
            onValueChange = { viewModel.onQueryChange(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text("Search Movies & TV Shows...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            colors = TextFieldDefaults.textFieldColors(
                containerColor = MaterialTheme.colorScheme.surface,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        if (uiState.isLoading && uiState.results.isEmpty()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(9) {
                    ShimmerCard(width = 100.dp, height = 150.dp)
                }
            }
        } else if (uiState.error != null && uiState.results.isEmpty()) {
            ErrorStateView(
                message = uiState.error ?: "Search failed",
                onRetry = { viewModel.retry() }
            )
        } else if (uiState.results.isEmpty() && uiState.query.length >= 2 && !uiState.isLoading) {
            EmptyStateView(message = "No results found for \"${uiState.query}\"")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.results) { item ->
                    MediaCard(item = item, onClick = { onMediaClick(item) })
                }
            }
        }
    }
}

