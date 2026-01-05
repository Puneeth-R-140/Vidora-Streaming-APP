package com.vidora.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vidora.app.data.local.FavoriteEntity
import com.vidora.app.data.local.HistoryEntity
import com.vidora.app.data.remote.MediaItem
import com.vidora.app.ui.viewmodels.HomeViewModel
import com.vidora.app.ui.components.ErrorStateView
import com.vidora.app.ui.components.ShimmerCard
import com.vidora.app.ui.components.EmptyStateView
import com.vidora.app.ui.components.MediaCard
import com.vidora.app.ui.components.shimmerEffect

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onMediaClick: (MediaItem) -> Unit,
    onSearchClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (uiState.isLoading && uiState.trendingMovies.isEmpty()) {
            ShimmerHomeScreen(onSearchClick)
        } else if (uiState.error != null && uiState.trendingMovies.isEmpty()) {
            ErrorStateView(
                message = uiState.error ?: "Unknown error",
                onRetry = { viewModel.retry() }
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item { 
                    HeaderSection(onSearchClick = onSearchClick) 
                }

                if (uiState.history.isNotEmpty()) {
                    item {
                        MediaSection(
                            title = "Continue Watching",
                            items = uiState.history.map { it.toMediaItem() },
                            onMediaClick = onMediaClick
                        )
                    }
                }

                if (uiState.favorites.isNotEmpty()) {
                    item {
                        MediaSection(
                            title = "My Favorites",
                            items = uiState.favorites.map { it.toMediaItem() },
                            onMediaClick = onMediaClick
                        )
                    }
                }
                
                if (uiState.trendingMovies.isNotEmpty()) {
                    item {
                        MediaSection(
                            title = "Trending Movies",
                            items = uiState.trendingMovies,
                            onMediaClick = onMediaClick
                        )
                    }
                }
                
                if (uiState.popularShows.isNotEmpty()) {
                    item {
                        MediaSection(
                            title = "Popular TV Shows",
                            items = uiState.popularShows,
                            onMediaClick = onMediaClick
                        )
                    }
                }
                
                if (uiState.trendingMovies.isEmpty() && uiState.popularShows.isEmpty() && !uiState.isLoading) {
                    item {
                        EmptyStateView(message = "No content available right now.")
                    }
                }

                if (uiState.error != null) {
                    item {
                        Surface(
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = uiState.error ?: "",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                TextButton(onClick = { viewModel.retry() }) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShimmerHomeScreen(onSearchClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        HeaderSection(onSearchClick = onSearchClick)
        
        repeat(3) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .width(150.dp)
                        .height(24.dp)
                        .shimmerEffect()
                )
                
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    repeat(3) {
                        ShimmerCard()
                    }
                }
            }
        }
    }
}

@Composable
fun HeaderSection(onSearchClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "PN",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Stream your favorites",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
        
        IconButton(onClick = onSearchClick) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = Color.White
            )
        }
    }
}

@Composable
fun MediaSection(
    title: String,
    items: List<MediaItem>,
    onMediaClick: (MediaItem) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items) { item ->
                MediaCard(item = item, onClick = { onMediaClick(item) })
            }
        }
    }
}


