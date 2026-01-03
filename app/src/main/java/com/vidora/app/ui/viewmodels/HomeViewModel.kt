package com.vidora.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vidora.app.data.local.FavoriteEntity
import com.vidora.app.data.local.HistoryEntity
import com.vidora.app.data.remote.MediaItem
import com.vidora.app.data.repository.MediaRepository
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
    val error: String? = null
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
            _uiState.emit(_uiState.value.copy(isLoading = true, error = null))
            
            try {
                // Fetch both trending and popular concurrently? For simplicity now, sequential
                repository.getTrendingMovies()
                    .catch { e -> _uiState.emit(_uiState.value.copy(error = e.message)) }
                    .collect { movies ->
                        _uiState.emit(_uiState.value.copy(trendingMovies = movies))
                        
                        repository.getTrendingTVShows()
                            .catch { e -> _uiState.emit(_uiState.value.copy(error = e.message)) }
                            .collect { shows ->
                                _uiState.emit(_uiState.value.copy(popularShows = shows, isLoading = false))
                            }
                    }
            } catch (e: Exception) {
                _uiState.emit(_uiState.value.copy(isLoading = false, error = e.message))
            }
        }
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
