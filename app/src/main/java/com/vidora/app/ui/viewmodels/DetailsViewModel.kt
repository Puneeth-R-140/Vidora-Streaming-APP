package com.vidora.app.ui.viewmodels

import androidx.lifecycle.SavedStateHandle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vidora.app.data.remote.MediaItem
import com.vidora.app.data.repository.MediaRepository
import com.vidora.app.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailsUiState(
    val media: MediaItem? = null,
    val episodes: List<com.vidora.app.data.remote.Episode> = emptyList(),
    val currentSeason: Int = 1,
    val isFavorite: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val canRetry: Boolean = false
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
            repository.getDetails(type, id).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true, error = null, canRetry = false) }
                    }
                    is NetworkResult.Success -> {
                        val isFav = repository.isFavorite(id)
                        _uiState.update { 
                            it.copy(
                                media = result.data, 
                                isFavorite = isFav, 
                                isLoading = false,
                                error = null
                            ) 
                        }
                        if (type == "tv") {
                            loadEpisodes(id, 1)
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

    fun loadEpisodes(id: String, season: Int) {
        viewModelScope.launch {
            repository.getEpisodes(id, season).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is NetworkResult.Success -> {
                        _uiState.update { 
                            it.copy(
                                episodes = result.data, 
                                currentSeason = season,
                                isLoading = false,
                                error = null
                            ) 
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
        val mediaId = _uiState.value.media?.id
        val mediaType = _uiState.value.media?.realMediaType ?: "movie"
        if (mediaId != null) {
            loadDetails(mediaType, mediaId)
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
