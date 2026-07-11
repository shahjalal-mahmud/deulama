package com.appriyo.deulama.data.mapper

import com.appriyo.deulama.data.remote.dto.DramaDto
import com.appriyo.deulama.domain.model.Drama

/**
 * DramaDto -> Drama. Pure conversion — no validation, no defaulting
 * of nullable fields. Anything the server returns null stays null so
 * UI layers can render placeholders.
 */
internal fun DramaDto.toDomain(): Drama = Drama(
    dramaId = drama_id,
    title = title,
    posterUrl = poster_url,
    bannerUrl = banner_url,
    releaseYear = release_year,
    imdbRating = imdb_rating,
    genre = genre,
    genres = genres,
    storyline = storyline,
    stars = stars,
    createdAt = created_at,
)
