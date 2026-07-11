package com.appriyo.deulama.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * The one envelope shape every endpoint in api.md returns, success or
 * error. Reuse this generic wrapper for every future Retrofit service —
 * don't invent a new response shape per-endpoint.
 *
 * `errors` is either a `{ field: [messages] }` validation map or a
 * single `{ code: "..." }` domain-error marker (e.g.
 * "auth.user_not_found"), so it's typed as a raw JsonElement and parsed
 * by the caller depending on which shape is expected.
 */
@Serializable
data class Envelope<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null,
    val errors: JsonElement? = null,
)