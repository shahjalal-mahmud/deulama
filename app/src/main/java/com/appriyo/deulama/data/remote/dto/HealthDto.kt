package com.appriyo.deulama.data.remote.dto

import kotlinx.serialization.Serializable

/** Payload shape of `data` in the GET /api/health envelope. */
@Serializable
data class HealthDto(
    val status: String,
    val time: String,
    val app: String,
)