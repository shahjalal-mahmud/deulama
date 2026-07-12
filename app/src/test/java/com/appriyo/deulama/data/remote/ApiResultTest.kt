package com.appriyo.deulama.data.remote

import com.appriyo.deulama.data.remote.dto.Envelope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/**
 * Section A of UNIT_TEST_PLAN.md — backs `data/remote/ApiResult.kt`.
 *
 * This is the single chokepoint the fix for Bug #14 (blank-message
 * fallback in EngagementRepositoryImpls) depends on, so every branch of
 * `safeApiCall` is pinned here even where the audit marked it ✅ verified
 * — a future change to this file is exactly the kind of thing that could
 * silently break #14's fix without these tests.
 *
 * All tests are PROTECTIVE — currently PASS — unless noted otherwise.
 * `Dummy` is a throwaway payload type so this file has zero dependency
 * on any real feature DTO.
 */
class ApiResultTest {

    private val json = Json { ignoreUnknownKeys = true }

    private data class Dummy(val id: Int)

    /** Builds a retrofit2.HttpException carrying [bodyJson] as the raw error body. */
    private fun httpException(status: Int, bodyJson: String): HttpException {
        val body = bodyJson.toResponseBody("application/json".toMediaType())
        val response = Response.error<Any>(status, body)
        return HttpException(response)
    }

    // -----------------------------------------------------------------
    // Envelope-level success/failure (2xx, no HttpException thrown)
    // -----------------------------------------------------------------

    @Test
    fun `safeApiCall returns Success when envelope is success=true with non-null data`() = runTest {
        val result = safeApiCall(json) { Envelope(success = true, message = "", data = Dummy(1)) }
        assertEquals(ApiResult.Success(Dummy(1)), result)
    }

    @Test
    fun `safeApiCall returns Error with envelope message when envelope is success=false`() = runTest {
        val result = safeApiCall<Dummy>(json) {
            Envelope(success = false, message = "Nope.", data = null)
        }
        val error = result as ApiResult.Error
        assertEquals(200, error.httpStatus)
        assertEquals("Nope.", error.message)
    }

    @Test
    fun `safeApiCall returns Error with fallback when envelope success=false and message is blank`() = runTest {
        val result = safeApiCall<Dummy>(json) {
            Envelope(success = false, message = "", data = null)
        }
        val error = result as ApiResult.Error
        assertEquals("Request failed", error.message)
    }

    @Test
    fun `safeApiCall returns Error Empty response body when envelope success=true with null data and blank message`() = runTest {
        val result = safeApiCall<Dummy>(json) {
            Envelope(success = true, message = "", data = null)
        }
        val error = result as ApiResult.Error
        assertEquals(200, error.httpStatus)
        assertEquals("Empty response body", error.message)
    }

    @Test
    fun `safeApiCall keeps the envelope message when data is null but message is non-blank`() = runTest {
        val result = safeApiCall<Dummy>(json) {
            Envelope(success = true, message = "No content yet.", data = null)
        }
        val error = result as ApiResult.Error
        assertEquals("No content yet.", error.message)
    }

    @Test
    fun `safeApiCall extracts code from envelope errors when success=false`() = runTest {
        val errors = buildJsonObject { put("code", JsonPrimitive("auth.user_not_found")) }
        val result = safeApiCall<Dummy>(json) {
            Envelope(success = false, message = "Account no longer exists.", data = null, errors = errors)
        }
        val error = result as ApiResult.Error
        assertEquals("auth.user_not_found", error.code)
    }

    // -----------------------------------------------------------------
    // HTTP 422 -> ValidationError
    // -----------------------------------------------------------------

    @Test
    fun `safeApiCall returns ValidationError with field map for HTTP 422 with field errors`() = runTest {
        val body = """{"success":false,"message":"Validation failed","errors":{"email":["Email is required."]}}"""
        val result = safeApiCall<Dummy>(json) { throw httpException(422, body) }
        val validation = result as ApiResult.ValidationError
        assertEquals(listOf("Email is required."), validation.fieldErrors["email"])
    }

    @Test
    fun `safeApiCall returns ValidationError with empty map for HTTP 422 without field errors`() = runTest {
        val body = """{"success":false,"message":"Validation failed"}"""
        val result = safeApiCall<Dummy>(json) { throw httpException(422, body) }
        val validation = result as ApiResult.ValidationError
        assertTrue(validation.fieldErrors.isEmpty())
    }

    @Test
    fun `safeApiCall returns ValidationError with empty map when the 422 body is unparseable`() = runTest {
        val result = safeApiCall<Dummy>(json) { throw httpException(422, "not json") }
        val validation = result as ApiResult.ValidationError
        assertTrue(validation.fieldErrors.isEmpty())
    }

    // -----------------------------------------------------------------
    // Other non-2xx -> Error
    // -----------------------------------------------------------------

    @Test
    fun `safeApiCall returns Error using the parsed message for a non-422 HTTP failure such as 401`() = runTest {
        val body = """{"success":false,"message":"Invalid email or password."}"""
        val result = safeApiCall<Dummy>(json) { throw httpException(401, body) }
        val error = result as ApiResult.Error
        assertEquals(401, error.httpStatus)
        assertEquals("Invalid email or password.", error.message)
    }

    @Test
    fun `safeApiCall returns Error with HTTP-status fallback when errorBody is empty and unparseable`() = runTest {
        val result = safeApiCall<Dummy>(json) { throw httpException(500, "") }
        val error = result as ApiResult.Error
        assertEquals(500, error.httpStatus)
        assertEquals("Request failed (HTTP 500)", error.message)
    }

    @Test
    fun `safeApiCall returns Error with HTTP-status fallback when the parsed message is blank`() = runTest {
        val body = """{"success":false,"message":""}"""
        val result = safeApiCall<Dummy>(json) { throw httpException(409, body) }
        val error = result as ApiResult.Error
        assertEquals("Request failed (HTTP 409)", error.message)
    }

    @Test
    fun `safeApiCall extracts code from the error envelope on a non-422 failure`() = runTest {
        val body = """{"success":false,"message":"Account no longer exists.","errors":{"code":"auth.user_not_found"}}"""
        val result = safeApiCall<Dummy>(json) { throw httpException(404, body) }
        val error = result as ApiResult.Error
        assertEquals("auth.user_not_found", error.code)
    }

    // -----------------------------------------------------------------
    // Exceptional paths
    // -----------------------------------------------------------------

    @Test
    fun `safeApiCall wraps IOException as NetworkError`() = runTest {
        val ioException = IOException("no route to host")
        val result = safeApiCall<Dummy>(json) { throw ioException }
        val networkError = result as ApiResult.NetworkError
        assertEquals(ioException, networkError.cause)
    }

    @Test
    fun `safeApiCall wraps generic Throwable as NetworkError`() = runTest {
        val throwable = IllegalStateException("boom")
        val result = safeApiCall<Dummy>(json) { throw throwable }
        val networkError = result as ApiResult.NetworkError
        assertEquals(throwable, networkError.cause)
    }

    @Test
    fun `safeApiCall rethrows CancellationException instead of swallowing it`() = runTest {
        var thrown: Throwable? = null
        try {
            safeApiCall<Dummy>(json) { throw CancellationException("scope cancelled") }
        } catch (e: CancellationException) {
            thrown = e
        }
        assertTrue(thrown is CancellationException)
    }

    // -----------------------------------------------------------------
    // treatAlreadyAppliedAsSuccess
    // -----------------------------------------------------------------

    @Test
    fun `treatAlreadyAppliedAsSuccess converts Error 409 to Success`() {
        val input: ApiResult<Unit> = ApiResult.Error(httpStatus = 409, message = "Already exists")
        assertEquals(ApiResult.Success(Unit), input.treatAlreadyAppliedAsSuccess())
    }

    @Test
    fun `treatAlreadyAppliedAsSuccess converts Error 404 to Success`() {
        val input: ApiResult<Unit> = ApiResult.Error(httpStatus = 404, message = "Not found")
        assertEquals(ApiResult.Success(Unit), input.treatAlreadyAppliedAsSuccess())
    }

    @Test
    fun `treatAlreadyAppliedAsSuccess passes non-409-404 Error through unchanged`() {
        val input: ApiResult<Unit> = ApiResult.Error(httpStatus = 500, message = "Server error")
        assertEquals(input, input.treatAlreadyAppliedAsSuccess())
    }

    @Test
    fun `treatAlreadyAppliedAsSuccess leaves Success unchanged`() {
        val input: ApiResult<Unit> = ApiResult.Success(Unit)
        assertEquals(input, input.treatAlreadyAppliedAsSuccess())
    }

    @Test
    fun `treatAlreadyAppliedAsSuccess leaves ValidationError unchanged`() {
        val input: ApiResult<Unit> = ApiResult.ValidationError(emptyMap())
        assertEquals(input, input.treatAlreadyAppliedAsSuccess())
    }

    @Test
    fun `treatAlreadyAppliedAsSuccess leaves NetworkError unchanged`() {
        val input: ApiResult<Unit> = ApiResult.NetworkError(IOException("offline"))
        assertEquals(input, input.treatAlreadyAppliedAsSuccess())
    }

    // -----------------------------------------------------------------
    // extractFieldErrors / extractErrorCode (internal helpers)
    // -----------------------------------------------------------------

    @Test
    fun `extractFieldErrors coerces primitive values to single-element lists`() {
        val errors = buildJsonObject { put("email", JsonPrimitive("Email is required.")) }
        val result = extractFieldErrors(errors)
        assertEquals(listOf("Email is required."), result["email"])
    }

    @Test
    fun `extractFieldErrors reads array values as multi-element lists`() {
        val errors = json.parseToJsonElement(
            """{"password":["Too short.","Must contain a number."]}""",
        )
        val result = extractFieldErrors(errors)
        assertEquals(listOf("Too short.", "Must contain a number."), result["password"])
    }

    @Test
    fun `extractFieldErrors skips the code key`() {
        val errors = json.parseToJsonElement(
            """{"code":"auth.user_not_found","email":["Bad email."]}""",
        )
        val result = extractFieldErrors(errors)
        assertTrue("code" !in result)
        assertEquals(listOf("Bad email."), result["email"])
    }

    @Test
    fun `extractFieldErrors returns empty map when errors is not a JsonObject`() {
        val errors = JsonPrimitive("not an object")
        assertTrue(extractFieldErrors(errors).isEmpty())
    }

    @Test
    fun `extractErrorCode returns null when no code field present`() {
        val errors = buildJsonObject { put("email", JsonPrimitive("Bad email.")) }
        assertNull(extractErrorCode(errors))
    }

    @Test
    fun `extractErrorCode returns the code value when present`() {
        val errors = buildJsonObject { put("code", JsonPrimitive("auth.user_not_found")) }
        assertEquals("auth.user_not_found", extractErrorCode(errors))
    }

    @Test
    fun `extractErrorCode returns null when errors is not a JsonObject`() {
        assertNull(extractErrorCode(JsonPrimitive("nope")))
    }
}