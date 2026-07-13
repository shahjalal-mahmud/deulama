package com.appriyo.deulama.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Wire shape of `Drama::publicItem()` as returned by
 * `GET /api/dramas` and `GET /api/dramas/{id}`. JSON keys are
 * snake_case to match api.md; convert via [toDomain] in
 * `data/mapper/DramaMappers.kt`.
 *
 * `poster_url` and `banner_url` are nullable — the admin may not have
 * uploaded art for every entry. `imdb_rating` is nullable for dramas
 * that haven't been rated yet.
 */
@Serializable
data class DramaDto(
    val drama_id: Int,
    val title: String,
    val poster_url: String? = null,
    val banner_url: String? = null,
    val release_year: String,
    val imdb_rating: Double? = null,
    val genre: String = "",
    val genres: List<String> = emptyList(),
    val storyline: String = "",
    val stars: String = "",
    val created_at: String,
)

/**
 * Wrapper around a single drama in the `GET /api/dramas/{id}` response.
 *
 * The list endpoint (`GET /api/dramas`) returns the page as
 * `{ dramas: [...], pagination: {...} }`, but the single-drama endpoint
 * returns `{ drama: {...} }` — the API uses different keys for the two
 * shapes even though the inner object is the same [DramaDto]. We model
 * that asymmetry here so [DramaApi.getDrama] can decode the body
 * without a custom deserializer.
 */
@Serializable
data class DramaSingleDto(
    val drama: DramaDto,
)
