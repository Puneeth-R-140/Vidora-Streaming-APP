package com.vidora.app.data.repository

import com.vidora.app.data.remote.MediaItem
import com.vidora.app.data.remote.TmdbService
import com.vidora.app.data.local.MediaDao
import com.vidora.app.util.NetworkResult
import com.vidora.app.util.safeApiCall
import com.vidora.app.util.retryApiCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepository @Inject constructor(
    private val tmdbService: TmdbService,
    private val mediaDao: MediaDao
) {
    fun getTrendingMovies(): Flow<NetworkResult<List<MediaItem>>> = flow {
        emit(NetworkResult.Loading)
        val result = retryApiCall {
            safeApiCall {
                val response = tmdbService.getTrending("movie")
                response.results
            }
        }
        emit(result)
    }

    fun getTrendingTVShows(): Flow<NetworkResult<List<MediaItem>>> = flow {
        emit(NetworkResult.Loading)
        val result = retryApiCall {
            safeApiCall {
                val response = tmdbService.getTrending("tv")
                response.results
            }
        }
        emit(result)
    }

    fun search(query: String): Flow<NetworkResult<List<MediaItem>>> = flow {
        emit(NetworkResult.Loading)
        val result = safeApiCall {
            val response = tmdbService.searchMulti(query)
            response.results.filter { it.mediaType == "movie" || it.mediaType == "tv" }
        }
        emit(result)
    }

    fun getDetails(mediaType: String, id: String): Flow<NetworkResult<MediaItem>> = flow {
        emit(NetworkResult.Loading)
        val result = retryApiCall {
            safeApiCall {
                tmdbService.getDetails(mediaType, id)
            }
        }
        emit(result)
    }

    fun getEpisodes(tvId: String, season: Int): Flow<NetworkResult<List<com.vidora.app.data.remote.Episode>>> = flow {
        emit(NetworkResult.Loading)
        val result = safeApiCall {
            tmdbService.getSeasonEpisodes(tvId, season).episodes
        }
        emit(result)
    }

    // Local Favorites
    fun getFavorites() = mediaDao.getAllFavorites()

    suspend fun toggleFavorite(media: MediaItem) {
        if (mediaDao.isFavorite(media.id)) {
            mediaDao.deleteFavorite(com.vidora.app.data.local.FavoriteEntity(media.id, media.displayTitle, media.posterPath, media.realMediaType))
        } else {
            mediaDao.insertFavorite(com.vidora.app.data.local.FavoriteEntity(media.id, media.displayTitle, media.posterPath, media.realMediaType))
        }
    }

    suspend fun isFavorite(id: String) = mediaDao.isFavorite(id)

    // History
    fun getWatchHistory() = mediaDao.getWatchHistory()

    suspend fun updateHistory(media: MediaItem, season: Int? = null, episode: Int? = null) {
        mediaDao.updateHistory(
            com.vidora.app.data.local.HistoryEntity(
                id = media.id,
                title = media.displayTitle,
                posterPath = media.posterPath,
                mediaType = media.realMediaType,
                season = season,
                episode = episode
            )
        )
    }
}
