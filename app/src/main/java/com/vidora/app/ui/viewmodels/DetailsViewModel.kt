package com.vidora.app.ui.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vidora.app.data.remote.MediaItem
import com.vidora.app.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailsUiState(
    val media: MediaItem? = null,
    val episodes: List<com.vidora.app.data.remote.Episode> = emptyList(),
    val currentSeason: Int = 1,
    val isFavorite: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val repository: MediaRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailsUiState())
    val uiState: StateFlow<DetailsUiState> = _uiState

    init {
        val id: String = savedStateHandle["id"] ?: ""
        val type: String = savedStateHandle["type"] ?: "movie"
        if (id.isNotEmpty()) {
            loadDetails(type, id)
        }
    }

    private fun loadDetails(type: String, id: String) {
        viewModelScope.launch {
            _uiState.emit(DetailsUiState(isLoading = true))
            
            repository.getDetails(type, id)
                .catch { e -> _uiState.emit(DetailsUiState(error = e.message)) }
                .collect { details ->
                    val isFav = repository.isFavorite(id)
                    _uiState.emit(_uiState.value.copy(media = details, isFavorite = isFav, isLoading = false))
                    if (type == "tv") {
                        loadEpisodes(id, 1)
                    }
                }
        }
    }

    fun loadEpisodes(id: String, season: Int) {
        viewModelScope.launch {
            repository.getEpisodes(id, season)
                .catch { e -> 
                    _uiState.emit(_uiState.value.copy(error = "Failed to load episodes: ${e.message}"))
                }
                .collect { episodes ->
                    _uiState.emit(_uiState.value.copy(
                        episodes = episodes, 
                        currentSeason = season,
                        error = null
                    ))
                }
        }
    }

    fun markWatched(media: MediaItem, season: Int? = null, episode: Int? = null) {
        viewModelScope.launch {
            repository.updateHistory(media, season, episode)
        }
    }

    fun toggleFavorite() {
        val media = _uiState.value.media ?: return
        viewModelScope.launch {
            repository.toggleFavorite(media)
            _uiState.emit(_uiState.value.copy(isFavorite = !(_uiState.value.isFavorite)))
        }
    }
}
