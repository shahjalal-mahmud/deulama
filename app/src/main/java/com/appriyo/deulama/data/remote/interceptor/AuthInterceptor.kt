package com.appriyo.deulama.data.remote.interceptor

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Placeholder. Once SessionManager (data/local/datastore) actually
 * stores a real JWT, read it here and attach:
 *
 *   Authorization: Bearer <token>
 *
 * to every outgoing request that needs it. Not wired into
 * NetworkModule's OkHttpClient yet — this is scaffolding only.
 */
class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        return chain.proceed(request)
    }
}