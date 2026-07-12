package com.appriyo.deulama.data.util

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * Section B of UNIT_TEST_PLAN.md — backs `data/util/ImageCompressor.kt`.
 *
 * NOTE ON SCOPE: the original test plan marked this file "pure JVM
 * impossible" and deferred it to the instrumented plan. That's not
 * actually true. `BitmapFactory.decodeStream` and `Bitmap.createScaledBitmap`
 * are static entry points backed by native code, but `mockkStatic`
 * intercepts the call *before* it ever reaches that native implementation
 * — so none of this needs Robolectric or a device. The only genuinely
 * Android-runtime-only piece would be decoding *real* image bytes, which
 * we never need to do here since we're testing the orchestration logic
 * (sampling math, error paths, recycle bookkeeping), not actual pixel data.
 *
 * IMPORTANT — cross-check with the audit: `compress()` as written
 * already throws `IllegalStateException("BitmapFactory returned null...")`
 * when the pixel-decode pass returns null. That contradicts the audit's
 * description of Bug #13 ("silently proceeds with a 0-byte file"). The
 * test below pins the *current* (correct-looking) throwing behaviour as
 * PROTECTIVE. If Bug #13 is real, it likely lives in whatever calls
 * `compress()` and swallows this exception — flag that back to Puko for
 * re-audit rather than assuming this file itself is the culprit.
 *
 * `androidx.core.graphics.scale` (used by `scaleDownIfStillTooLarge`) is
 * an `inline` function that expands to a direct call to
 * `Bitmap.createScaledBitmap(...)` at the `compress()` call site, so
 * stubbing `Bitmap.createScaledBitmap` statically is sufficient to
 * intercept it — no separate mocking of the extension function is needed.
 */
class ImageCompressorTest {

    private val uri: Uri = mockk(relaxed = true)

    @After
    fun tearDown() {
        unmockkStatic(BitmapFactory::class)
        unmockkStatic(Bitmap::class)
    }

    private fun contentResolverStreaming(): ContentResolver {
        val resolver = mockk<ContentResolver>()
        every { resolver.openInputStream(uri) } answers {
            ByteArrayInputStream(ByteArray(16)) as InputStream
        }
        return resolver
    }

    // -----------------------------------------------------------------
    // computeSampleSize — pure function, zero Android mocking required.
    // This is the actual scaling math and the highest-value target in
    // this file; it's 100% JVM-testable exactly as written.
    // -----------------------------------------------------------------

    @Test
    fun `computeSampleSize returns 1 when the image is already within maxEdge`() {
        assertEquals(1, ImageCompressor.computeSampleSize(width = 800, height = 600, maxEdge = 1024))
    }

    @Test
    fun `computeSampleSize returns 1 when the image is under 2x maxEdge`() {
        // longest=2000, ceiling check is against maxEdge*2=2048 -> 2000 doesn't trigger a halving
        assertEquals(1, ImageCompressor.computeSampleSize(width = 2000, height = 1000, maxEdge = 1024))
    }

    @Test
    fun `computeSampleSize halves once when the image is just over 2x maxEdge`() {
        // longest=4096, maxEdge*2=2048: 4096>2048 -> sample=2; 4096/2=2048>2048 false -> stop at 2
        assertEquals(2, ImageCompressor.computeSampleSize(width = 4096, height = 2048, maxEdge = 1024))
    }

    @Test
    fun `computeSampleSize keeps halving until under the 2x-maxEdge ceiling`() {
        // longest=8000: 8000/1>2048 -> 2; 8000/2=4000>2048 -> 4; 8000/4=2000>2048 false -> stop at 4
        assertEquals(4, ImageCompressor.computeSampleSize(width = 8000, height = 6000, maxEdge = 1024))
    }

    @Test
    fun `computeSampleSize uses the longest of width or height, not width alone`() {
        assertEquals(
            ImageCompressor.computeSampleSize(width = 100, height = 8000, maxEdge = 1024),
            ImageCompressor.computeSampleSize(width = 8000, height = 100, maxEdge = 1024),
        )
    }

    @Test
    fun `computeSampleSize with a zero-area image does not loop forever and returns 1`() {
        assertEquals(1, ImageCompressor.computeSampleSize(width = 0, height = 0, maxEdge = 1024))
    }

    // -----------------------------------------------------------------
    // compress() end-to-end via mockkStatic — no real bitmap decoding.
    // -----------------------------------------------------------------

    @Test
    fun `compress throws when the source image reports zero dimensions`() {
        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeStream(any(), isNull(), any()) } answers {
            val opts = thirdArg<BitmapFactory.Options>()
            opts.outWidth = 0
            opts.outHeight = 0
            null
        }

        val resolver = contentResolverStreaming()
        val exception = try {
            ImageCompressor.compress(uri, resolver, "image/png")
            null
        } catch (e: IllegalArgumentException) {
            e
        }
        assertTrue(
            "Expected an IllegalArgumentException from the require() bounds check",
            exception != null && exception.message!!.contains("Couldn't read image dimensions"),
        )
    }

    @Test
    fun `compress throws a clear error when the pixel decode returns null (protective see class doc re Bug 13)`() {
        mockkStatic(BitmapFactory::class)
        val boundsSlot = slot<BitmapFactory.Options>()
        every {
            BitmapFactory.decodeStream(any(), isNull(), capture(boundsSlot))
        } answers {
            // First invocation is the bounds-only pass: report a valid,
            // large image so we reach the second (pixel) pass. Second
            // invocation returns null to simulate a provider that can't
            // actually hand back readable bytes.
            if (boundsSlot.captured.inJustDecodeBounds) {
                boundsSlot.captured.outWidth = 2000
                boundsSlot.captured.outHeight = 1000
                null
            } else {
                null
            }
        }

        val resolver = contentResolverStreaming()
        val exception = try {
            ImageCompressor.compress(uri, resolver, "image/png")
            null
        } catch (e: IllegalStateException) {
            e
        }
        assertTrue(
            "Expected an IllegalStateException surfacing the null-decode failure — " +
                    "this is the behaviour that should be preserved (or improved on with a " +
                    "friendlier caller-facing message) when Bug #13 is actually fixed",
            exception != null && exception.message!!.contains("BitmapFactory returned null"),
        )
    }

    @Test
    fun `compress re-encodes as JPEG at the configured quality and returns non-empty bytes`() {
        mockkStatic(BitmapFactory::class)
        mockkStatic(Bitmap::class)

        val decodedBitmap = mockk<Bitmap>(relaxed = true)
        every { decodedBitmap.width } returns 800
        every { decodedBitmap.height } returns 600
        every {
            decodedBitmap.compress(Bitmap.CompressFormat.JPEG, ImageCompressor.JPEG_QUALITY, any())
        } answers {
            thirdArg<OutputStream>().write(byteArrayOf(1, 2, 3))
            true
        }

        every { BitmapFactory.decodeStream(any(), isNull(), any()) } answers {
            val opts = thirdArg<BitmapFactory.Options>()
            if (opts.inJustDecodeBounds) {
                opts.outWidth = 800
                opts.outHeight = 600
                null
            } else {
                decodedBitmap
            }
        }

        val resolver = contentResolverStreaming()
        val bytes = ImageCompressor.compress(uri, resolver, "image/jpeg")

        assertTrue(bytes.isNotEmpty())
        verify(exactly = 1) {
            decodedBitmap.compress(Bitmap.CompressFormat.JPEG, ImageCompressor.JPEG_QUALITY, any())
        }
    }

    @Test
    fun `compress recycles the decoded bitmap exactly once when no further downscale was needed`() {
        mockkStatic(BitmapFactory::class)
        mockkStatic(Bitmap::class)

        val decodedBitmap = mockk<Bitmap>(relaxed = true)
        // Already within MAX_EDGE_PX, so scaleDownIfStillTooLarge returns
        // the SAME instance without ever calling Bitmap.scale/createScaledBitmap.
        every { decodedBitmap.width } returns 500
        every { decodedBitmap.height } returns 400
        every { decodedBitmap.isRecycled } returns false
        every { decodedBitmap.compress(any(), any(), any()) } answers {
            thirdArg<OutputStream>().write(byteArrayOf(9))
            true
        }

        every { BitmapFactory.decodeStream(any(), isNull(), any()) } answers {
            val opts = thirdArg<BitmapFactory.Options>()
            if (opts.inJustDecodeBounds) {
                opts.outWidth = 500
                opts.outHeight = 400
                null
            } else {
                decodedBitmap
            }
        }

        val resolver = contentResolverStreaming()
        ImageCompressor.compress(uri, resolver, "image/png")

        // resized === bitmap on this path (guarded by `if (resized !== bitmap)`),
        // so recycle() must be called exactly once, never twice.
        verify(exactly = 1) { decodedBitmap.recycle() }
        verify(exactly = 0) { Bitmap.createScaledBitmap(any(), any(), any(), any()) }
    }

    @Test
    fun `compress downscales via createScaledBitmap and recycles both bitmaps when still oversized`() {
        mockkStatic(BitmapFactory::class)
        mockkStatic(Bitmap::class)

        val decodedBitmap = mockk<Bitmap>(relaxed = true)
        val scaledBitmap = mockk<Bitmap>(relaxed = true)

        every { decodedBitmap.width } returns 1500
        every { decodedBitmap.height } returns 1500
        every { decodedBitmap.isRecycled } returns false
        every { scaledBitmap.isRecycled } returns false
        every {
            Bitmap.createScaledBitmap(decodedBitmap, any(), any(), true)
        } returns scaledBitmap
        every { scaledBitmap.compress(any(), any(), any()) } answers {
            thirdArg<OutputStream>().write(byteArrayOf(9))
            true
        }

        every { BitmapFactory.decodeStream(any(), isNull(), any()) } answers {
            val opts = thirdArg<BitmapFactory.Options>()
            if (opts.inJustDecodeBounds) {
                opts.outWidth = 1500
                opts.outHeight = 1500
                null
            } else {
                decodedBitmap
            }
        }

        val resolver = contentResolverStreaming()
        ImageCompressor.compress(uri, resolver, "image/png")

        verify(exactly = 1) { decodedBitmap.recycle() }
        verify(exactly = 1) { scaledBitmap.recycle() }
    }

    @Test
    fun `compress passes originalMime through only as a hint, always re-encoding as JPEG`() {
        mockkStatic(BitmapFactory::class)
        mockkStatic(Bitmap::class)

        val decodedBitmap = mockk<Bitmap>(relaxed = true)
        every { decodedBitmap.width } returns 400
        every { decodedBitmap.height } returns 300
        every { decodedBitmap.compress(any(), any(), any()) } answers {
            thirdArg<OutputStream>().write(byteArrayOf(1))
            true
        }
        every { BitmapFactory.decodeStream(any(), isNull(), any()) } answers {
            val opts = thirdArg<BitmapFactory.Options>()
            if (opts.inJustDecodeBounds) {
                opts.outWidth = 400
                opts.outHeight = 300
                null
            } else {
                decodedBitmap
            }
        }

        val resolver = contentResolverStreaming()
        ImageCompressor.compress(uri, resolver, "image/webp")

        verify(exactly = 1) {
            decodedBitmap.compress(Bitmap.CompressFormat.JPEG, ImageCompressor.JPEG_QUALITY, any())
        }
    }
}