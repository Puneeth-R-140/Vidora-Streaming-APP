package com.vidora.app.ui.screens

import com.vidora.app.data.local.FavoriteEntity
import com.vidora.app.data.local.HistoryEntity
import com.vidora.app.data.remote.MediaItem

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
