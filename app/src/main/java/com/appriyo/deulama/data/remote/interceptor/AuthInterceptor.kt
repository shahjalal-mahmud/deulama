package com.appriyo.deulama.data.remote.interceptor

import com.appriyo.deulama.data.local.datastore.SessionManager
import com.appriyo.deulama.data.remote.AuthEvent
import com.appriyo.deulama.data.remote.AuthEventBus
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Reads the cached JWT synchronously and attaches it as
 * `Authorization: Bearer <token>`. On the way back, an authenticated
 * request that gets 401 fires [AuthEvent.SessionExpired] so the nav
 * graph can force-log-out.
 */
class AuthInterceptor(
    private val sessionManager: SessionManager,
    private val authEventBus: AuthEventBus = AuthEventBus,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = sessionManager.currentToken()

        // Skip the builder allocation entirely when there's nothing to add.
        val request = if (token == null || original.header("Authorization") != null) {
            original
        } else {
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }

        val response = chain.proceed(request)

        // Only signal session-expired when WE sent a token — an anonymous
        // 401 (login/register) is normal and must not trigger global logout.
        if (response.code == 401 && token != null) {
            authEventBus.emit(AuthEvent.SessionExpired)
        }

        return response
    }
}