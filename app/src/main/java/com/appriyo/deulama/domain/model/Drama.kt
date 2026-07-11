package com.appriyo.deulama.domain.model

/**
 * Domain representation of a drama as returned by the public catalog
 * endpoints (`GET /api/dramas` and `GET /api/dramas/{id}`).
 *
 * Fields mirror the server's `Drama::publicItem()` shape 1:1 so the
 * only conversion the mapper needs is snake_case -> camelCase.
 *
 * - `releaseYear` stays as the raw `String` ("2019") the API returns
 *   — same convention as `User.createdAt`; parse it lazily only when
 *   a screen actually needs numeric sorting or formatting.
 * - `createdAt` likewise stays as the raw API string for the same
 *   reason.
 * - `posterUrl` / `bannerUrl` are nullable because the admin may not
 *   have uploaded art for every entry yet — UIs MUST render a
 *   placeholder when either is null.
 * - `imdbRating` is nullable so a drama with no rating yet doesn't
 *   crash trending sort.
 */
data class Drama(
    val dramaId: Int,
    val title: String,
    val posterUrl: String?,
    val bannerUrl: String?,
    val releaseYear: String,
    val imdbRating: Double?,
    val genre: String,
    val genres: List<String>,
    val storyline: String,
    val stars: String,
    val createdAt: String,
)
