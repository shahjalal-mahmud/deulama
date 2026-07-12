package com.appriyo.deulama.data.util

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Section C of UNIT_TEST_PLAN.md — backs `data/util/ImageValidator.kt`.
 *
 * NOTE ON SCOPE: same correction as ImageCompressorTest — this file
 * touches only `ContentResolver`, `Cursor`, and `Uri`, all of which are
 * mockable interfaces/abstract classes. Nothing here calls into real
 * native code, so plain MockK mocks are sufficient; no Robolectric or
 * instrumented test is required.
 *
 * `Uri` itself is never called on directly by `ImageValidator` (it's
 * only ever passed through as an opaque key to the mocked
 * `ContentResolver`), so a relaxed mock is enough — we never need
 * `Uri.parse(...)`.
 */
class ImageValidatorTest {

    private val uri: Uri = mockk(relaxed = true)

    private fun contentResolverReturning(
        mime: String?,
        cursor: Cursor?,
    ): ContentResolver {
        val resolver = mockk<ContentResolver>()
        every { resolver.getType(uri) } returns mime
        // Match the SIZE-projection query loosely (any() for the
        // projection array) since Kotlin arrays don't have structural
        // equals, and we only ever call `query` once with one shape.
        every { resolver.query(uri, any(), null, null, null) } returns cursor
        return resolver
    }

    private fun cursorWithSize(size: Long?, columnIndex: Int = 0, hasRow: Boolean = true): Cursor {
        val cursor = mockk<Cursor>(relaxed = true)
        every { cursor.moveToFirst() } returns hasRow
        every { cursor.getColumnIndex(OpenableColumns.SIZE) } returns columnIndex
        if (columnIndex >= 0) {
            every { cursor.isNull(columnIndex) } returns (size == null)
            if (size != null) {
                every { cursor.getLong(columnIndex) } returns size
            }
        }
        return cursor
    }

    // -----------------------------------------------------------------
    // MIME checks
    // -----------------------------------------------------------------

    @Test
    fun `validate rejects a null MIME type`() {
        val resolver = contentResolverReturning(mime = null, cursor = null)
        val result = ImageValidator.validate(uri, resolver) as ImageValidator.Result.Invalid
        assertEquals("Image must be JPG, JPEG, PNG, or WebP.", result.reason)
    }

    @Test
    fun `validate rejects an unsupported MIME type`() {
        val resolver = contentResolverReturning(mime = "image/gif", cursor = null)
        val result = ImageValidator.validate(uri, resolver)
        assertTrue(result is ImageValidator.Result.Invalid)
    }

    @Test
    fun `validate accepts image-jpeg`() {
        val cursor = cursorWithSize(1_000L)
        val resolver = contentResolverReturning(mime = "image/jpeg", cursor = cursor)
        val result = ImageValidator.validate(uri, resolver) as ImageValidator.Result.Ok
        assertEquals("image/jpeg", result.mime)
    }

    @Test
    fun `validate accepts image-png`() {
        val cursor = cursorWithSize(1_000L)
        val resolver = contentResolverReturning(mime = "image/png", cursor = cursor)
        assertTrue(ImageValidator.validate(uri, resolver) is ImageValidator.Result.Ok)
    }

    @Test
    fun `validate accepts image-webp`() {
        val cursor = cursorWithSize(1_000L)
        val resolver = contentResolverReturning(mime = "image/webp", cursor = cursor)
        assertTrue(ImageValidator.validate(uri, resolver) is ImageValidator.Result.Ok)
    }

    @Test
    fun `validate lowercases the MIME type before comparing (accepts IMAGE-JPEG)`() {
        val cursor = cursorWithSize(1_000L)
        val resolver = contentResolverReturning(mime = "IMAGE/JPEG", cursor = cursor)
        val result = ImageValidator.validate(uri, resolver) as ImageValidator.Result.Ok
        assertEquals("image/jpeg", result.mime)
    }

    // -----------------------------------------------------------------
    // Size checks
    // -----------------------------------------------------------------

    @Test
    fun `validate rejects a zero-byte image`() {
        val cursor = cursorWithSize(0L)
        val resolver = contentResolverReturning(mime = "image/png", cursor = cursor)
        val result = ImageValidator.validate(uri, resolver) as ImageValidator.Result.Invalid
        assertEquals("Image is empty.", result.reason)
    }

    @Test
    fun `validate rejects an image larger than MAX_BYTES`() {
        val cursor = cursorWithSize(ImageValidator.MAX_BYTES + 1)
        val resolver = contentResolverReturning(mime = "image/png", cursor = cursor)
        val result = ImageValidator.validate(uri, resolver) as ImageValidator.Result.Invalid
        assertEquals("Image must be 5 MB or smaller.", result.reason)
    }

    @Test
    fun `validate accepts an image exactly at MAX_BYTES (boundary is inclusive)`() {
        val cursor = cursorWithSize(ImageValidator.MAX_BYTES)
        val resolver = contentResolverReturning(mime = "image/png", cursor = cursor)
        assertTrue(ImageValidator.validate(uri, resolver) is ImageValidator.Result.Ok)
    }

    @Test
    fun `validate reports the real size on success`() {
        val cursor = cursorWithSize(2_048L)
        val resolver = contentResolverReturning(mime = "image/png", cursor = cursor)
        val result = ImageValidator.validate(uri, resolver) as ImageValidator.Result.Ok
        assertEquals(2_048L, result.sizeBytes)
    }

    // -----------------------------------------------------------------
    // Provider doesn't expose SIZE -> trust the server, don't block
    // -----------------------------------------------------------------

    @Test
    fun `validate lets the request through with sizeBytes -1 when the provider returns a null cursor`() {
        val resolver = contentResolverReturning(mime = "image/png", cursor = null)
        val result = ImageValidator.validate(uri, resolver) as ImageValidator.Result.Ok
        assertEquals(-1L, result.sizeBytes)
    }

    @Test
    fun `validate lets the request through with sizeBytes -1 when the cursor has no rows`() {
        val cursor = cursorWithSize(size = 123L, hasRow = false)
        val resolver = contentResolverReturning(mime = "image/png", cursor = cursor)
        val result = ImageValidator.validate(uri, resolver) as ImageValidator.Result.Ok
        assertEquals(-1L, result.sizeBytes)
    }

    @Test
    fun `validate lets the request through with sizeBytes -1 when the SIZE column is missing`() {
        val cursor = cursorWithSize(size = 123L, columnIndex = -1)
        val resolver = contentResolverReturning(mime = "image/png", cursor = cursor)
        val result = ImageValidator.validate(uri, resolver) as ImageValidator.Result.Ok
        assertEquals(-1L, result.sizeBytes)
    }

    @Test
    fun `validate lets the request through with sizeBytes -1 when the SIZE column value is null`() {
        val cursor = cursorWithSize(size = null)
        val resolver = contentResolverReturning(mime = "image/png", cursor = cursor)
        val result = ImageValidator.validate(uri, resolver) as ImageValidator.Result.Ok
        assertEquals(-1L, result.sizeBytes)
    }

    // -----------------------------------------------------------------
    // Cursor lifecycle — `cursor.use { ... }` must always close
    // -----------------------------------------------------------------

    @Test
    fun `validate closes the cursor via use{} on the happy path`() {
        val cursor = cursorWithSize(1_000L)
        val resolver = contentResolverReturning(mime = "image/png", cursor = cursor)
        ImageValidator.validate(uri, resolver)
        verify(exactly = 1) { cursor.close() }
    }

    @Test
    fun `validate closes the cursor via use{} even when it has no rows`() {
        val cursor = cursorWithSize(size = 1L, hasRow = false)
        val resolver = contentResolverReturning(mime = "image/png", cursor = cursor)
        ImageValidator.validate(uri, resolver)
        verify(exactly = 1) { cursor.close() }
    }

    @Test
    fun `validate closes the cursor via use{} even when the SIZE column is missing`() {
        val cursor = cursorWithSize(size = 1L, columnIndex = -1)
        val resolver = contentResolverReturning(mime = "image/png", cursor = cursor)
        ImageValidator.validate(uri, resolver)
        verify(exactly = 1) { cursor.close() }
    }
}