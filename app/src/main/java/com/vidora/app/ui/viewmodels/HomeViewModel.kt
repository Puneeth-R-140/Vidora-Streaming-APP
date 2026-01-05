package com.vidora.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vidora.app.data.local.FavoriteEntity
import com.vidora.app.data.local.HistoryEntity
import com.vidora.app.data.remote.MediaItem
import com.vidora.app.data.repository.MediaRepository
import com.vidora.app.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val trendingMovies: List<MediaItem> = emptyList(),
    val popularShows: List<MediaItem> = emptyList(),
    val favorites: List<com.vidora.app.data.local.FavoriteEntity> = emptyList(),
    val history: List<com.vidora.app.data.local.HistoryEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val canRetry: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        loadHomeContent()
        observeLocalData()
    }

    private fun observeLocalData() {
        viewModelScope.launch {
            repository.getFavorites().collect { favs ->
                _uiState.update { it.copy(favorites = favs) }
            }
        }
        viewModelScope.launch {
            repository.getWatchHistory().collect { history ->
                _uiState.update { it.copy(history = history) }
            }
        }
    }

    fun loadHomeContent() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, canRetry = false) }
            
            // Collect movies
            repository.getTrendingMovies().collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is NetworkResult.Success -> {
                        _uiState.update { it.copy(trendingMovies = result.data) }
                        
                        // Now fetch TV shows
                        repository.getTrendingTVShows().collect { tvResult ->
                            when (tvResult) {
                                is NetworkResult.Success -> {
                                    _uiState.update { 
                                        it.copy(
                                            popularShows = tvResult.data,
                                            isLoading = false,
                                            error = null
                                        )
                                    }
                                }
                                is NetworkResult.Error -> {
                                    _uiState.update { 
                                        it.copy(
                                            isLoading = false,
                                            error = tvResult.message,
                                            canRetry = true
                                        )
                                    }
                                }
                                is NetworkResult.Loading -> {}
                            }
                        }
                    }
                    is NetworkResult.Error -> {
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                error = result.message,
                                canRetry = true
                            )
                        }
                    }
                }
            }
        }
    }
    
    fun retry() {
        loadHomeContent()
    }
}

fun FavoriteEntity.toMediaItem(): MediaItem {
    return MediaItem(
        id = id,
        title = title,
        name = null,
        overview = null,
        posterPath = posterPath,
        backdropPath = null,
        voteAverage = 0.0,
        releaseDate = null,
        firstAirDate = null,
        mediaType = mediaType,
        popularity = null,
        genres = null,
        credits = null,
        similar = null,
        numberOfSeasons = null,
        seasons = null
    )
}

fun HistoryEntity.toMediaItem(): MediaItem {
    return MediaItem(
        id = id,
        title = title,
        name = null,
        overview = null,
        posterPath = posterPath,
        backdropPath = null,
        voteAverage = 0.0,
        releaseDate = null,
        firstAirDate = null,
        mediaType = mediaType,
        popularity = null,
        genres = null,
        credits = null,
        similar = null,
        numberOfSeasons = null,
        seasons = null
    )
}
