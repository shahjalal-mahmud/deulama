package com.appriyo.deulama.data.util

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns

/**
 * Client-side pre-flight check for the `PUT /api/profile` image upload.
 *
 * Per api.md the server **is** the final authority (`finfo_file()` reads
 * real bytes, not the Content-Type header), but we run this first so the
 * UI can reject obviously-bad inputs without round-tripping — keeps the
 * 422 envelope out of the user's face for the common cases (too big,
 * wrong format).
 *
 * - Allowed MIME: `image/jpeg`, `image/png`, `image/webp`
 *   (the PHP server maps `.jpg` / `.jpeg` / `.png` / `.webp`).
 * - Max size: 5 MB on the wire (the file the user picks may be larger
 *   because we still compress in [ImageCompressor] before upload).
 */
object ImageValidator {

    /** Server-side cap from api.md. */
    const val MAX_BYTES: Long = 5L * 1024 * 1024

    /** Acceptable MIME types for the profile-image upload. */
    val ALLOWED_MIME: Set<String> = setOf(
        "image/jpeg",
        "image/png",
        "image/webp",
    )

    /**
     * Result of validating a user-picked image. On success carries the
     * MIME and original size so the caller can decide whether to also
     * run [ImageCompressor.compress].
     */
    sealed interface Result {
        data class Ok(
            val mime: String,
            val sizeBytes: Long,
        ) : Result

        data class Invalid(val reason: String) : Result
    }

    /**
     * Inspect the URI's size + declared MIME. Pure read-only call —
     * safe to invoke from the main thread for typical photos (cursor
     * lookup is fast). Heavy IO should still happen on `Dispatchers.IO`
     * downstream.
     */
    fun validate(uri: Uri, contentResolver: ContentResolver): Result {
        val mime = contentResolver.getType(uri)?.lowercase()
        if (mime == null || mime !in ALLOWED_MIME) {
            return Result.Invalid(
                "Image must be JPG, JPEG, PNG, or WebP.",
            )
        }

        val sizeBytes = querySizeBytes(uri, contentResolver)
        if (sizeBytes == null) {
            // Some providers don't expose SIZE (rare, but happens). We
            // still let the request through and trust the server's
            // finfo_file() check — failing the upload here would be
            // a regression for users on those providers.
            return Result.Ok(mime = mime, sizeBytes = -1L)
        }
        if (sizeBytes <= 0L) {
            return Result.Invalid("Image is empty.")
        }
        if (sizeBytes > MAX_BYTES) {
            return Result.Invalid("Image must be 5 MB or smaller.")
        }
        return Result.Ok(mime = mime, sizeBytes = sizeBytes)
    }

    /**
     * Reads the byte size via [OpenableColumns.SIZE]. Returns null when
     * the provider doesn't expose the column.
     */
    private fun querySizeBytes(uri: Uri, contentResolver: ContentResolver): Long? {
        val cursor = contentResolver.query(
            uri,
            arrayOf(OpenableColumns.SIZE),
            null,
            null,
            null,
        ) ?: return null
        cursor.use { c ->
            if (!c.moveToFirst()) return null
            val idx = c.getColumnIndex(OpenableColumns.SIZE)
            if (idx < 0) return null
            return if (c.isNull(idx)) null else c.getLong(idx)
        }
    }
}