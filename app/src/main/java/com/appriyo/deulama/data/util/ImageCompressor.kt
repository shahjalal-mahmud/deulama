package com.appriyo.deulama.data.util

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import androidx.core.graphics.scale

/**
 * Best-effort client-side image compression for the
 * `PUT /api/profile` image upload.
 *
 * Strategy:
 *   1. Read the original image bounds via `inJustDecodeBounds=true`
 *      (cheap — no pixel decode).
 *   2. Compute `inSampleSize` so the downsampled bitmap's longest edge
 *      is no larger than [MAX_EDGE_PX].
 *   3. Re-open the URI and decode the actual pixels.
 *   4. Re-encode as JPEG quality [JPEG_QUALITY].
 *
 * Why:
 *   - Cuts upload payload for big photos from phones (often 4-8 MB).
 *   - Server still validates with `finfo_file()` — this is purely a
 *     bandwidth/latency optimisation, never a security check.
 *   - Always re-encodes as JPEG so we don't carry through PNG/WebP
 *     metadata that the server might reject.
 *
 * NOTE: must be called on `Dispatchers.IO` (bitmap decode + encode is
 * CPU/IO-heavy).
 */
object ImageCompressor {

    /** Longest-edge ceiling for the uploaded avatar. */
    const val MAX_EDGE_PX: Int = 1024

    /** JPEG quality used when re-encoding the downsampled bitmap. */
    const val JPEG_QUALITY: Int = 85

    /**
     * Compress [uri] into JPEG bytes. Caller must already have run
     * [ImageValidator.validate] — this function does NOT re-check size
     * or MIME (assumes the caller did it).
     *
     * @param originalMime the MIME returned by the content resolver, used
     *   only as a hint (we still re-encode as JPEG).
     */
    fun compress(
        uri: Uri,
        contentResolver: ContentResolver,
        originalMime: String,
    ): ByteArray {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri).use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
        }
        val (origW, origH) = bounds.outWidth to bounds.outHeight
        require(origW > 0 && origH > 0) {
            "Couldn't read image dimensions for $uri (mime=$originalMime)."
        }

        val sampleSize = computeSampleSize(origW, origH, MAX_EDGE_PX)

        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bitmap = contentResolver.openInputStream(uri).use { input ->
            BitmapFactory.decodeStream(input, null, decodeOpts)
        } ?: error("BitmapFactory returned null for $uri after inSampleSize=$sampleSize")

        val resized = scaleDownIfStillTooLarge(bitmap)

        val out = ByteArrayOutputStream()
        try {
            resized.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            out.flush()
            return out.toByteArray()
        } finally {
            out.close()
            // Don't recycle `resized` if it IS `bitmap` — Bitmap.compress
            // doesn't keep a reference, but recycle-safety is fragile
            // across Android versions; GC handles it.
            if (resized !== bitmap) bitmap.recycle()
            if (!resized.isRecycled) resized.recycle()
        }
    }

    /**
     * Picks an inSampleSize that's a power-of-two divisor and keeps the
     * longest edge just above [maxEdge]. Using a power-of-two is what
     * Android's docs recommend for memory efficiency.
     */
    internal fun computeSampleSize(width: Int, height: Int, maxEdge: Int): Int {
        var sample = 1
        val longest = maxOf(width, height)
        while (longest / sample > maxEdge * 2) {
            sample *= 2
        }
        return sample
    }

    /**
     * Some images decode to slightly under 2x [MAX_EDGE_PX] because
     * inSampleSize rounds. A final explicit scale keeps us within the
     * ceiling without going under it.
     */
    private fun scaleDownIfStillTooLarge(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height
        val longest = maxOf(w, h)
        if (longest <= MAX_EDGE_PX) return src
        val scale = MAX_EDGE_PX.toFloat() / longest.toFloat()
        val newW = (w * scale).toInt().coerceAtLeast(1)
        val newH = (h * scale).toInt().coerceAtLeast(1)
        return src.scale(newW, newH)
    }
}