package com.appriyo.deulama.presentation.util

import com.appriyo.deulama.BuildConfig

/**
 * Tiny helper for converting relative media paths returned by the API
 * into absolute URLs that Coil / image loaders can fetch.
 *
 * The backend stores images (avatars, posters) as **relative** paths
 * like `uploads/profile/default.png` (see api.md — login & profile
 * response shapes). We prefix them with [BuildConfig.API_BASE_URL] to
 * make them loadable. Already-absolute URLs (`http://`, `https://`)
 * pass through unchanged.
 *
 * Returns `null` for null or blank inputs so callers can use a single
 * null-check to decide whether to render the image at all.
 */
object ImageUrls {

    /**
     * Resolve any media path to a fully-qualified URL.
     *
     * @param path The value returned by the API (relative or already absolute).
     * @return The URL to feed to the image loader, or `null` if blank.
     */
    fun absolute(path: String?): String? {
        if (path.isNullOrBlank()) return null
        if (path.startsWith("http://") || path.startsWith("https://")) return path
        val base = BuildConfig.API_BASE_URL.trimEnd('/')
        val rel = path.trimStart('/')
        return "$base/$rel"
    }
}