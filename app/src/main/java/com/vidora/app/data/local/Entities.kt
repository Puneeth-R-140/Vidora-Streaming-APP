package com.vidora.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val posterPath: String?,
    val mediaType: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "watch_history")
data class HistoryEntity(
    @PrimaryKey val id: String,
    val title: String,
    val posterPath: String?,
    val mediaType: String,
    val lastWatchedAt: Long = System.currentTimeMillis(),
    val progress: Float = 0f,
    val season: Int? = null,
    val episode: Int? = null
)

fun FavoriteEntity.toMediaItem() = com.vidora.app.data.remote.MediaItem(
    id = id,
    title = if (mediaType == "movie") title else null,
    name = if (mediaType == "tv") title else null,
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

fun HistoryEntity.toMediaItem() = com.vidora.app.data.remote.MediaItem(
    id = id,
    title = if (mediaType == "movie") title else null,
    name = if (mediaType == "tv") title else null,
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
