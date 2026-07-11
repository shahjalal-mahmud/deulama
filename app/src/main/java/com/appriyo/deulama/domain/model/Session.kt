package com.appriyo.deulama.domain.model

/**
 * A persisted login session: the user plus the JWT used to authenticate
 * subsequent API calls. Produced by AuthRepository.login/register and
 * read by the rest of the app to decide whether we're signed in.
 */
data class Session(
    val user: User,
    val token: String,
)