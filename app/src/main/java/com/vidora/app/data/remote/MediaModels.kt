package com.vidora.app.data.remote

import com.google.gson.annotations.SerializedName

data class TmdbResponse<T>(
    @SerializedName("results") val results: List<T>,
    @SerializedName("total_results") val totalResults: Int,
    @SerializedName("total_pages") val totalPages: Int,
    @SerializedName("page") val page: Int
)

data class MediaItem(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("overview") val overview: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("vote_average") val voteAverage: Double,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("first_air_date") val firstAirDate: String?,
    @SerializedName("media_type") val mediaType: String?,
    @SerializedName("popularity") val popularity: Double?,
    @SerializedName("genres") val genres: List<Genre>?,
    @SerializedName("credits") val credits: Credits?,
    @SerializedName("similar") val similar: TmdbResponse<MediaItem>?,
    @SerializedName("number_of_seasons") val numberOfSeasons: Int?,
    @SerializedName("seasons") val seasons: List<Season>?
) {
    val displayTitle: String get() = title ?: name ?: "Unknown"
    val realMediaType: String get() = mediaType ?: if (title != null) "movie" else "tv"
    val totalSeasons: Int get() = numberOfSeasons ?: seasons?.size ?: 1
}

data class Season(
    @SerializedName("id") val id: Int,
    @SerializedName("season_number") val seasonNumber: Int,
    @SerializedName("episode_count") val episodeCount: Int?
)

data class Genre(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String
)

data class Credits(
    @SerializedName("cast") val cast: List<CastMember>
)

data class CastMember(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("character") val character: String,
    @SerializedName("profile_path") val profilePath: String?
)

data class Episode(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("overview") val overview: String?,
    @SerializedName("episode_number") val episodeNumber: Int,
    @SerializedName("season_number") val seasonNumber: Int,
    @SerializedName("still_path") val stillPath: String?
)

data class SeasonEpisodesResponse(
    @SerializedName("episodes") val episodes: List<Episode>
)
