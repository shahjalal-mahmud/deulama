package com.appriyo.deulama.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Error-only mirror of [Envelope] — same shape, no generic `data`.
 * Used by [com.appriyo.deulama.data.remote.safeApiCall] to parse
 * non-2xx error bodies without having to know the success type.
 */
@Serializable
data class EnvelopeErrorDto(
    val success: Boolean = false,
    val message: String = "",
    val errors: JsonElement? = null,
)