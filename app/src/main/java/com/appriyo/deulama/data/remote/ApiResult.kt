package com.appriyo.deulama.data.remote

import com.appriyo.deulama.data.remote.dto.Envelope
import com.appriyo.deulama.data.remote.dto.EnvelopeErrorDto
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import retrofit2.HttpException

/**
 * Unified result type for every repository call that hits the API.
 * Repositories return one of these instead of throwing, so the UI
 * layer can render a banner / per-field error without try/catch noise.
 *
 *  - Success         — body was 2xx, envelope.success=true, data is non-null.
 *  - ValidationError — HTTP 422. Field-keyed map of error messages.
 *  - Error           — any other non-2xx (401, 409, 500, ...). `code` is
 *                      the domain-error marker if the server provided one.
 *  - NetworkError    — couldn't reach the server at all (no HTTP response).
 */
sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>
    data class ValidationError(val fieldErrors: Map<String, List<String>>) : ApiResult<Nothing>
    data class Error(
        val httpStatus: Int,
        val message: String,
        val code: String? = null,
    ) : ApiResult<Nothing>
    data class NetworkError(val cause: Throwable) : ApiResult<Nothing>
}

/**
 * Wraps any suspend Retrofit call and translates failures into the
 * unified ApiResult shape.
 *
 * Usage:
 *   val result = safeApiCall(json) { authApi.login(LoginRequest(email, password)) }
 *
 * Behaviour:
 *  - HTTP 2xx with `success=true` and a non-null `data` → Success(data).
 *  - HTTP 422 → ValidationError(field → [messages]).
 *  - HTTP 401 / 409 / other non-2xx → Error(status, message, code?).
 *  - IOException (no connectivity, DNS, timeout) → NetworkError.
 *  - Anything else → NetworkError (wrapped).
 */
suspend fun <T> safeApiCall(
    json: Json,
    block: suspend () -> Envelope<T>,
): ApiResult<T> = try {
    val envelope = block()
    if (envelope.success) {
        val data = envelope.data
        if (data != null) {
            ApiResult.Success(data)
        } else {
            ApiResult.Error(httpStatus = 200, message = envelope.message.ifBlank { "Empty response body" })
        }
    } else {
        ApiResult.Error(
            httpStatus = 200,
            message = envelope.message.ifBlank { "Request failed" },
            code = envelope.errors?.let { extractErrorCode(it) },
        )
    }
} catch (e: HttpException) {
    val status = e.code()
    val rawBody = e.response()?.errorBody()?.string().orEmpty()
    val envelope = runCatching { json.decodeFromString(EnvelopeErrorDto.serializer(), rawBody) }.getOrNull()

    when (status) {
        422 -> {
            val fieldMap = envelope?.errors?.let { extractFieldErrors(it) } ?: emptyMap()
            ApiResult.ValidationError(fieldMap)
        }
        else -> ApiResult.Error(
            httpStatus = status,
            message = envelope?.message?.takeIf { it.isNotBlank() }
                ?: "Request failed (HTTP $status)",
            code = envelope?.errors?.let { extractErrorCode(it) },
        )
    }
} catch (e: CancellationException) {
    throw e
} catch (e: IOException) {
    ApiResult.NetworkError(e)
} catch (e: Throwable) {
    ApiResult.NetworkError(e)
}

/**
 * Same as [safeApiCall] but for endpoints whose response body the
 * caller doesn't actually need — only the success/failure decision.
 * Used by favorites / watch-later / watched mutations where we'd
 * otherwise have to thread a phantom `JsonElement` through the
 * repository just to throw it away.
 *
 * Like [safeApiCall], `200` and `201` are both treated as Success
 * (the API normalises everything via the envelope's `success` flag).
 */
suspend fun safeApiCallRaw(
    json: Json,
    block: suspend () -> Envelope<JsonElement>,
): ApiResult<Unit> = safeApiCall<JsonElement>(json, block).mapToUnit()

/** Functor map for [ApiResult]. Passes the value through `transform` on Success. */
inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(value))
    is ApiResult.ValidationError -> this
    is ApiResult.Error -> this
    is ApiResult.NetworkError -> this
}

/**
 * Discard the success value. Exists so call sites read as
 * `safeApiCallRaw(json) { ... }` without having to `.let { ... }`.
 */
private fun <T> ApiResult<T>.mapToUnit(): ApiResult<Unit> = when (this) {
    is ApiResult.Success -> ApiResult.Success(Unit)
    is ApiResult.ValidationError -> this
    is ApiResult.Error -> this
    is ApiResult.NetworkError -> this
}

/**
 * Collapse 409 / 404 responses into Success — the desired state is
 * already true, so a double-tap shouldn't surface an error toast.
 * Anything else passes through unchanged.
 */
fun ApiResult<Unit>.treatAlreadyAppliedAsSuccess(): ApiResult<Unit> = when (this) {
    is ApiResult.Error -> if (httpStatus == 409 || httpStatus == 404) {
        ApiResult.Success(Unit)
    } else {
        this
    }
    else -> this
}

/** The minimum drama id accepted by any endpoint. */
internal const val MIN_DRAMA_ID = 1

/**
 * The API returns validation errors as
 *   { "email": ["Email must be valid."], "password": ["Too short."] }
 * i.e. a JSON object whose values are arrays of strings. Some keys
 * might accidentally be primitives — coerce safely.
 */
internal fun extractFieldErrors(errors: JsonElement): Map<String, List<String>> {
    val obj = errors as? JsonObject ?: return emptyMap()
    val out = mutableMapOf<String, List<String>>()
    for ((key, value) in obj) {
        if (key == "code") continue
        val messages = when (value) {
            is JsonPrimitive -> listOf(value.content)
            is JsonObject -> listOf(value.toString())
            else -> {
                runCatching {
                    value.jsonArray.map { (it as JsonPrimitive).content }
                }.getOrElse { listOf(value.toString()) }
            }
        }
        out[key] = messages
    }
    return out
}

/**
 * The API returns domain errors as
 *   { "code": "auth.user_not_found" }
 * alongside the human-readable `message`. Returns null if not present.
 */
internal fun extractErrorCode(errors: JsonElement): String? {
    val obj = errors as? JsonObject ?: return null
    val codeEl = obj["code"] as? JsonPrimitive ?: return null
    return runCatching { codeEl.content }.getOrNull()
}