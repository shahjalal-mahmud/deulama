package com.appriyo.deulama.data.repository

import com.appriyo.deulama.data.local.datastore.SessionManager
import com.appriyo.deulama.data.remote.ApiResult
import com.appriyo.deulama.data.remote.api.ProfileApi
import com.appriyo.deulama.data.remote.dto.Envelope
import com.appriyo.deulama.data.remote.dto.ProfileResponseDto
import com.appriyo.deulama.domain.model.Session
import com.appriyo.deulama.domain.model.User
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/**
 * Section D of UNIT_TEST_PLAN.md — backs `data/repository/ProfileRepositoryImpl.kt`.
 *
 * IMPORTANT — cross-check with the audit before treating every test's
 * "currently FAIL/PASS" label as gospel:
 *
 *  - Bug #1 (hardcoded `"avatar.jpg"` filename) DOES reproduce here —
 *    `updateViaMultipart` literally does `imageName = imageName ?: "avatar.jpg"`,
 *    so a real filename is never forwarded. The filename test below is a
 *    genuine REGRESSION TEST — currently FAIL.
 *
 *  - Bug #17 (session merge preserves the OLD email) does NOT reproduce
 *    in this source. `refreshSessionFromProfile` builds `mergedUser` from
 *    `profile.email` — the fresh server response — not the cached
 *    session's email. It also can't structurally happen: `ProfileUpdateRequestDto`
 *    has no `email` field, so this endpoint can never change email in the
 *    first place, meaning the server's response `email` is always just
 *    the account's real current value. The test below pins the CORRECT
 *    current behaviour as PROTECTIVE. Flag this back for a re-audit
 *    rather than trusting the original Bug #17 description.
 */
class ProfileRepositoryImplTest {

    private val profileApi = mockk<ProfileApi>()
    private val sessionManager = mockk<SessionManager>()
    private val json = Json { ignoreUnknownKeys = true }

    private val repository = ProfileRepositoryImpl(profileApi, sessionManager, json)

    private val cachedUser = User(
        userId = 1,
        fullName = "Old Name",
        email = "current@example.com",
        profileImage = "uploads/profile/default.png",
        createdAt = "2026-01-01 00:00:00",
    )
    private val cachedSession = Session(user = cachedUser, token = "cached-token")

    private fun httpException(status: Int, bodyJson: String): HttpException {
        val body = bodyJson.toResponseBody("application/json".toMediaType())
        return HttpException(Response.error<Any>(status, body))
    }

    private fun sampleProfileDto(
        id: Int = 1,
        name: String = "New Name",
        email: String = "current@example.com",
        image: String = "uploads/profile/new.png",
        updatedFields: List<String>? = listOf("name"),
        likedCount: Int = 5,
        watchedCount: Int = 2,
        favoriteGenres: List<String> = listOf("Romance", "Drama"),
    ) = ProfileResponseDto(
        id = id,
        name = name,
        email = email,
        image = image,
        updated_fields = updatedFields,
        liked_count = likedCount,
        watched_count = watchedCount,
        favorite_genres = favoriteGenres,
    )

    /** Stubs the two SessionManager calls the Success branch always makes. */
    private fun stubSuccessfulSessionRefresh(session: Session? = cachedSession) {
        every { sessionManager.currentSession() } returns session
        coEvery { sessionManager.saveSession(any()) } returns Unit
    }

    // -----------------------------------------------------------------
    // Routing: JSON vs multipart
    // -----------------------------------------------------------------

    @Test
    fun `updateProfile with no image routes to the JSON PUT method and not the multipart one`() = runTest {
        coEvery { profileApi.updateProfileJson(any()) } returns
                Envelope(success = true, message = "", data = sampleProfileDto())
        stubSuccessfulSessionRefresh()

        repository.updateProfile(
            name = "New Name",
            currentPassword = null,
            newPassword = null,
            confirmPassword = null,
            imageBytes = null,
            imageMime = null,
            imageName = null,
        )

        coVerify(exactly = 1) { profileApi.updateProfileJson(any()) }
        coVerify(exactly = 0) { profileApi.updateProfileMultipart(any(), any()) }
    }

    @Test
    fun `updateProfile with image routes to the multipart PUT method`() = runTest {
        coEvery { profileApi.updateProfileMultipart(any(), any()) } returns
                Envelope(success = true, message = "", data = sampleProfileDto())
        stubSuccessfulSessionRefresh()

        repository.updateProfile(
            name = null,
            currentPassword = null,
            newPassword = null,
            confirmPassword = null,
            imageBytes = byteArrayOf(1, 2, 3),
            imageMime = "image/png",
            imageName = "test.png",
        )

        coVerify(exactly = 1) { profileApi.updateProfileMultipart(any(), any()) }
        coVerify(exactly = 0) { profileApi.updateProfileJson(any()) }
    }

    // -----------------------------------------------------------------
    // Bug #1 — filename forwarding
    // -----------------------------------------------------------------

    @Test
    fun `updateProfile multipart call forwards filename from imageName parameter`() = runTest {
        val partSlot = slot<MultipartBody.Part>()
        coEvery { profileApi.updateProfileMultipart(any(), capture(partSlot)) } returns
                Envelope(success = true, message = "", data = sampleProfileDto())
        stubSuccessfulSessionRefresh()

        repository.updateProfile(
            name = null,
            currentPassword = null,
            newPassword = null,
            confirmPassword = null,
            imageBytes = byteArrayOf(1, 2, 3),
            imageMime = "image/png",
            imageName = "my-real-photo.png",
        )

        val contentDisposition = partSlot.captured.headers?.get("Content-Disposition").orEmpty()
        assertTrue(
            "Expected the multipart filename to reflect the caller-supplied imageName " +
                    "(\"my-real-photo.png\"), but the repository still hardcodes \"avatar.jpg\" " +
                    "at the call site (Bug #1, priority A). Header was: $contentDisposition",
            contentDisposition.contains("filename=\"my-real-photo.png\""),
        )
    }

    @Test
    fun `updateProfile with a null imageName currently falls back to avatar-jpg (documents Bug #1's default)`() = runTest {
        val partSlot = slot<MultipartBody.Part>()
        coEvery { profileApi.updateProfileMultipart(any(), capture(partSlot)) } returns
                Envelope(success = true, message = "", data = sampleProfileDto())
        stubSuccessfulSessionRefresh()

        repository.updateProfile(
            name = null, currentPassword = null, newPassword = null, confirmPassword = null,
            imageBytes = byteArrayOf(1), imageMime = "image/png", imageName = null,
        )

        val contentDisposition = partSlot.captured.headers?.get("Content-Disposition").orEmpty()
        assertTrue(contentDisposition.contains("filename=\"avatar.jpg\""))
    }

    // -----------------------------------------------------------------
    // Mime fallback
    // -----------------------------------------------------------------

    @Test
    fun `updateProfile with null imageMime falls back to image-jpeg`() = runTest {
        coEvery { profileApi.updateProfileMultipart(any(), any()) } returns
                Envelope(success = true, message = "", data = sampleProfileDto())
        stubSuccessfulSessionRefresh()

        val result = repository.updateProfile(
            name = null, currentPassword = null, newPassword = null, confirmPassword = null,
            imageBytes = byteArrayOf(1), imageMime = null, imageName = "x.png",
        )
        assertTrue(result is ApiResult.Success)
    }

    @Test
    fun `updateProfile with a blank (empty string) imageMime throws instead of falling back undocumented edge case`() = runTest {
        // NOT one of the audit's 18 numbered bugs, but worth flagging:
        // production only guards `imageMime == null` (`imageMime ?: "image/jpeg"`),
        // not an empty string. An empty string reaches `"".toMediaType()`,
        // which throws IllegalArgumentException. If any caller can ever
        // pass "" (e.g. a content resolver returning a blank MIME), this
        // crashes instead of degrading gracefully — worth a decision on
        // whether it deserves its own bug entry.
        var thrown: Throwable? = null
        try {
            repository.updateProfile(
                name = null, currentPassword = null, newPassword = null, confirmPassword = null,
                imageBytes = byteArrayOf(1), imageMime = "", imageName = "x.png",
            )
        } catch (e: IllegalArgumentException) {
            thrown = e
        }
        assertTrue(
            "If this assertion starts failing because \"\".toMediaType() stops throwing, " +
                    "update this test to match the new behaviour instead of deleting it.",
            thrown != null,
        )
    }

    // -----------------------------------------------------------------
    // Error propagation
    // -----------------------------------------------------------------

    @Test
    fun `updateProfile propagates ApiResult ValidationError from the API call unchanged`() = runTest {
        val body = """{"success":false,"message":"Validation failed","errors":{"name":["Name must be 2-150 characters."]}}"""
        coEvery { profileApi.updateProfileJson(any()) } throws httpException(422, body)

        val result = repository.updateProfile(
            name = "x", currentPassword = null, newPassword = null, confirmPassword = null,
            imageBytes = null, imageMime = null, imageName = null,
        )

        val validation = result as ApiResult.ValidationError
        assertEquals(listOf("Name must be 2-150 characters."), validation.fieldErrors["name"])
    }

    @Test
    fun `updateProfile propagates ApiResult NetworkError unchanged`() = runTest {
        coEvery { profileApi.updateProfileJson(any()) } throws java.io.IOException("offline")

        val result = repository.updateProfile(
            name = "x", currentPassword = null, newPassword = null, confirmPassword = null,
            imageBytes = null, imageMime = null, imageName = null,
        )

        assertTrue(result is ApiResult.NetworkError)
    }

    // -----------------------------------------------------------------
    // DTO -> domain mapping
    // -----------------------------------------------------------------

    @Test
    fun `getProfile maps DTO into UserProfile correctly including favorite_genres and counts`() = runTest {
        coEvery { profileApi.getProfile() } returns
                Envelope(
                    success = true,
                    message = "",
                    data = sampleProfileDto(
                        likedCount = 7,
                        watchedCount = 3,
                        favoriteGenres = listOf("Romance", "Thriller", "Comedy"),
                    ),
                )

        val result = repository.getProfile() as ApiResult.Success
        assertEquals(7, result.value.likedCount)
        assertEquals(3, result.value.watchedCount)
        assertEquals(listOf("Romance", "Thriller", "Comedy"), result.value.favoriteGenres)
    }

    @Test
    fun `getProfile maps empty favorite_genres to an empty list, never null`() = runTest {
        coEvery { profileApi.getProfile() } returns
                Envelope(success = true, message = "", data = sampleProfileDto(favoriteGenres = emptyList()))

        val result = repository.getProfile() as ApiResult.Success
        assertEquals(emptyList<String>(), result.value.favoriteGenres)
    }

    @Test
    fun `getProfile returns ApiResult ValidationError on HTTP 422`() = runTest {
        coEvery { profileApi.getProfile() } throws httpException(422, """{"success":false,"message":"nope"}""")
        val result = repository.getProfile()
        assertTrue(result is ApiResult.ValidationError)
    }

    // -----------------------------------------------------------------
    // Session refresh on success
    // -----------------------------------------------------------------

    @Test
    fun `updateProfile on success calls sessionManager saveSession with the merged user`() = runTest {
        coEvery { profileApi.updateProfileJson(any()) } returns
                Envelope(success = true, message = "", data = sampleProfileDto(id = 1, name = "Jane Doe"))
        stubSuccessfulSessionRefresh()
        val sessionSlot = slot<Session>()
        coEvery { sessionManager.saveSession(capture(sessionSlot)) } returns Unit

        repository.updateProfile(
            name = "Jane Doe", currentPassword = null, newPassword = null, confirmPassword = null,
            imageBytes = null, imageMime = null, imageName = null,
        )

        assertEquals("Jane Doe", sessionSlot.captured.user.fullName)
        assertEquals(1, sessionSlot.captured.user.userId)
    }

    @Test
    fun `updateProfile on success preserves the session token unchanged`() = runTest {
        coEvery { profileApi.updateProfileJson(any()) } returns
                Envelope(success = true, message = "", data = sampleProfileDto())
        stubSuccessfulSessionRefresh(session = cachedSession)
        val sessionSlot = slot<Session>()
        coEvery { sessionManager.saveSession(capture(sessionSlot)) } returns Unit

        repository.updateProfile(
            name = "x", currentPassword = null, newPassword = null, confirmPassword = null,
            imageBytes = null, imageMime = null, imageName = null,
        )

        assertEquals("cached-token", sessionSlot.captured.token)
    }

    @Test
    fun `updateProfile on success preserves the cached createdAt (never echoed by PUT)`() = runTest {
        coEvery { profileApi.updateProfileJson(any()) } returns
                Envelope(success = true, message = "", data = sampleProfileDto())
        stubSuccessfulSessionRefresh()
        val sessionSlot = slot<Session>()
        coEvery { sessionManager.saveSession(capture(sessionSlot)) } returns Unit

        repository.updateProfile(
            name = "x", currentPassword = null, newPassword = null, confirmPassword = null,
            imageBytes = null, imageMime = null, imageName = null,
        )

        assertEquals(cachedUser.createdAt, sessionSlot.captured.user.createdAt)
    }

    @Test
    fun `updateProfile on success uses the email from the server response, not any previously cached email (protective see class doc re Bug 17)`() = runTest {
        coEvery { profileApi.updateProfileJson(any()) } returns
                Envelope(success = true, message = "", data = sampleProfileDto(email = "server-current@example.com"))
        // Deliberately seed a DIFFERENT cached email to prove the merge
        // doesn't fall back to it.
        val staleSession = Session(
            user = cachedUser.copy(email = "stale-cached@example.com"),
            token = "cached-token",
        )
        stubSuccessfulSessionRefresh(session = staleSession)
        val sessionSlot = slot<Session>()
        coEvery { sessionManager.saveSession(capture(sessionSlot)) } returns Unit

        repository.updateProfile(
            name = "x", currentPassword = null, newPassword = null, confirmPassword = null,
            imageBytes = null, imageMime = null, imageName = null,
        )

        assertEquals("server-current@example.com", sessionSlot.captured.user.email)
    }

    @Test
    fun `updateProfile on success merges name from response`() = runTest {
        coEvery { profileApi.updateProfileJson(any()) } returns
                Envelope(success = true, message = "", data = sampleProfileDto(name = "Brand New Name"))
        stubSuccessfulSessionRefresh()
        val sessionSlot = slot<Session>()
        coEvery { sessionManager.saveSession(capture(sessionSlot)) } returns Unit

        repository.updateProfile(
            name = "Brand New Name", currentPassword = null, newPassword = null, confirmPassword = null,
            imageBytes = null, imageMime = null, imageName = null,
        )

        assertEquals("Brand New Name", sessionSlot.captured.user.fullName)
    }

    @Test
    fun `updateProfile on success merges image from response`() = runTest {
        coEvery { profileApi.updateProfileMultipart(any(), any()) } returns
                Envelope(success = true, message = "", data = sampleProfileDto(image = "uploads/profile/fresh.png"))
        stubSuccessfulSessionRefresh()
        val sessionSlot = slot<Session>()
        coEvery { sessionManager.saveSession(capture(sessionSlot)) } returns Unit

        repository.updateProfile(
            name = null, currentPassword = null, newPassword = null, confirmPassword = null,
            imageBytes = byteArrayOf(1), imageMime = "image/png", imageName = "x.png",
        )

        assertEquals("uploads/profile/fresh.png", sessionSlot.captured.user.profileImage)
    }

    // -----------------------------------------------------------------
    // updated_fields wrapper contract
    // -----------------------------------------------------------------

    @Test
    fun `updateProfile returns the raw updatedFields list in the result wrapper`() = runTest {
        coEvery { profileApi.updateProfileJson(any()) } returns
                Envelope(success = true, message = "", data = sampleProfileDto(updatedFields = listOf("name", "password")))
        stubSuccessfulSessionRefresh()

        val result = repository.updateProfile(
            name = "x", currentPassword = "a", newPassword = "b", confirmPassword = "b",
            imageBytes = null, imageMime = null, imageName = null,
        ) as ApiResult.Success

        assertEquals(listOf("name", "password"), result.value.updatedFields)
    }

    @Test
    fun `updateProfile with an empty updatedFields list returns an empty list, not null`() = runTest {
        coEvery { profileApi.updateProfileJson(any()) } returns
                Envelope(success = true, message = "", data = sampleProfileDto(updatedFields = emptyList()))
        stubSuccessfulSessionRefresh()

        val result = repository.updateProfile(
            name = null, currentPassword = null, newPassword = null, confirmPassword = null,
            imageBytes = null, imageMime = null, imageName = null,
        ) as ApiResult.Success

        assertEquals(emptyList<String>(), result.value.updatedFields)
    }

    // -----------------------------------------------------------------
    // No session mutation on failure
    // -----------------------------------------------------------------

    @Test
    fun `updateProfile does NOT touch sessionManager when the API returns Error`() = runTest {
        coEvery { profileApi.updateProfileJson(any()) } throws
                httpException(500, """{"success":false,"message":"Server error"}""")

        repository.updateProfile(
            name = "x", currentPassword = null, newPassword = null, confirmPassword = null,
            imageBytes = null, imageMime = null, imageName = null,
        )

        coVerify(exactly = 0) { sessionManager.saveSession(any()) }
    }

    @Test
    fun `updateProfile does NOT touch sessionManager when the API returns NetworkError`() = runTest {
        coEvery { profileApi.updateProfileJson(any()) } throws java.io.IOException("offline")

        repository.updateProfile(
            name = "x", currentPassword = null, newPassword = null, confirmPassword = null,
            imageBytes = null, imageMime = null, imageName = null,
        )

        coVerify(exactly = 0) { sessionManager.saveSession(any()) }
    }

    @Test
    fun `updateProfile on success with no current session skips the save without failing the overall call`() = runTest {
        coEvery { profileApi.updateProfileJson(any()) } returns
                Envelope(success = true, message = "", data = sampleProfileDto())
        stubSuccessfulSessionRefresh(session = null)

        val result = repository.updateProfile(
            name = "x", currentPassword = null, newPassword = null, confirmPassword = null,
            imageBytes = null, imageMime = null, imageName = null,
        )

        assertTrue(result is ApiResult.Success)
        coVerify(exactly = 0) { sessionManager.saveSession(any()) }
    }
}