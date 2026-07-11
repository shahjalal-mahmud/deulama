package com.appriyo.deulama.domain.repository

import com.appriyo.deulama.data.remote.ApiResult
import com.appriyo.deulama.domain.model.Session
import com.appriyo.deulama.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Auth-related operations the presentation layer is allowed to call.
 * All network calls return [ApiResult] so the UI can render banners
 * and per-field errors without try/catch noise.
 */
interface AuthRepository {

    /**
     * Hot flow of the persisted session. Emits `null` when the user is
     * signed out and a populated [Session] once one is available.
     * Cold-start friendly — emits immediately on collect.
     */
    val sessionFlow: Flow<Session?>

    /**
     * Synchronous snapshot of the current session (used to seed the UI
     * before the first flow emission arrives). Returns null when signed
     * out.
     */
    fun currentSession(): Session?

    /**
     * POST /api/auth/register. On success, also persists the token +
     * user returned in the response (register auto-logs the user in).
     */
    suspend fun register(
        fullName: String,
        email: String,
        password: String,
        passwordConfirmation: String,
    ): ApiResult<User>

    /**
     * POST /api/auth/login. On success, persists the token + user.
     *
     * NOTE: per api.md, the server returns the same "Invalid email or
     * password." message for both unknown email and wrong password —
     * callers must not branch on which case it was.
     */
    suspend fun login(email: String, password: String): ApiResult<User>

    /**
     * Clears the persisted session. Idempotent — safe to call when
     * already signed out. The UI layer is responsible for also
     * navigating back to the Login screen.
     */
    suspend fun logout()
}