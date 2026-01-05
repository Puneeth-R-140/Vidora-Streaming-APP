package com.vidora.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FavoriteBorder
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
import coil.compose.AsyncImage
import com.vidora.app.data.remote.MediaItem
import com.vidora.app.ui.viewmodels.DetailsViewModel

import com.vidora.app.ui.components.ErrorStateView
import com.vidora.app.ui.components.ShimmerCard
import com.vidora.app.ui.components.shimmerEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    viewModel: DetailsViewModel,
    onWatchClick: (String, String, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val media = uiState.media

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (uiState.isLoading && media == null) {
            ShimmerDetailsScreen()
        } else if (uiState.error != null && media == null) {
            ErrorStateView(
                message = uiState.error ?: "Unknown error",
                onRetry = { viewModel.retry() }
            )
        } else if (media != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(250.dp)) {
                    AsyncImage(
                        model = "https://image.tmdb.org/t/p/original${media.backdropPath}",
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                }

                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = media.displayTitle,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.toggleFavorite() }) {
                            Icon(
                                imageVector = if (uiState.isFavorite) 
                                    Icons.Filled.Favorite 
                                else 
                                    Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (uiState.isFavorite) Color.Red else Color.White
                            )
                        }
                    }
                    
                    Text(
                        text = "${media.realMediaType.uppercase()} • ${media.voteAverage}/10",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = { 
                            if (media.realMediaType == "movie") {
                                viewModel.markWatched(media)
                            }
                            val finalUrl = when {
                                media.realMediaType == "tv" -> "https://watch.vidora.su/watch/tv/${media.id}/1/1"
                                else -> "https://watch.vidora.su/watch/movie/${media.id}"
                            }
                            onWatchClick(media.id, media.realMediaType, finalUrl)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Watch Now", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Overview",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = media.overview ?: "No description available.",
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = Color.LightGray
                    )

                    if (media.realMediaType == "tv") {
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Episodes",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            val totalSeasons = media.totalSeasons
                            if (totalSeasons > 1) {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items((1..totalSeasons).toList()) { seasonNum ->
                                        FilterChip(
                                            selected = uiState.currentSeason == seasonNum,
                                            onClick = { viewModel.loadEpisodes(media.id, seasonNum) },
                                            label = { Text("S$seasonNum") }
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        if (uiState.episodes.isEmpty() && uiState.isLoading) {
                            Column {
                                repeat(3) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(80.dp)
                                            .padding(vertical = 4.dp)
                                            .shimmerEffect()
                                    )
                                }
                            }
                        } else if (uiState.episodes.isEmpty() && uiState.error != null) {
                            TextButton(onClick = { 
                                media?.let { viewModel.loadEpisodes(it.id, uiState.currentSeason) }
                            }) {
                                Text("Click to retry loading episodes", color = MaterialTheme.colorScheme.error)
                            }
                        } else {
                            uiState.episodes.forEach { episode ->
                                EpisodeItem(episode = episode) {
                                    viewModel.markWatched(media, episode.seasonNumber, episode.episodeNumber)
                                    val episodeUrl = "https://watch.vidora.su/watch/tv/${media.id}/${episode.seasonNumber}/${episode.episodeNumber}"
                                    onWatchClick(media.id, "tv", episodeUrl)
                                }
                            }
                        }
                    }
                }
            }
        } else if (uiState.error != null && media == null) {
            ErrorStateView(
                message = uiState.error ?: "Unknown error",
                onRetry = { viewModel.retry() }
            )
        }
    }
}

@Composable
fun ShimmerDetailsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .shimmerEffect()
        )

        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(32.dp)
                    .shimmerEffect()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(20.dp)
                    .shimmerEffect()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .shimmerEffect()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            repeat(3) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.3f)
                        .height(24.dp)
                        .shimmerEffect()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .shimmerEffect()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeItem(
    episode: com.vidora.app.data.remote.Episode,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = "https://image.tmdb.org/t/p/w500${episode.stillPath}",
                contentDescription = null,
                modifier = Modifier.width(100.dp).height(60.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "${episode.episodeNumber}. ${episode.name}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = episode.overview ?: "",
                    fontSize = 12.sp,
                    maxLines = 2,
                    color = Color.Gray
                )
            }
        }
    }
}
