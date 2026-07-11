package com.appriyo.deulama.data.remote

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Events the auth pipeline can broadcast to the rest of the app. Right
 * now the only one is "your stored token was rejected — force-log-out",
 * which is emitted by [com.appriyo.deulama.data.remote.interceptor.AuthInterceptor]
 * when it sees an HTTP 401 on a request that did carry a Bearer token.
 *
 * Subscribe once at the NavGraph root to react globally.
 */
sealed interface AuthEvent {
    /** Server rejected our stored token. UI should drop back to Login. */
    data object SessionExpired : AuthEvent
}

/**
 * Process-wide bus for [AuthEvent]. Implemented with a buffered
 * MutableSharedFlow so `emit` is fire-and-forget from the OkHttp
 * thread and never blocks the network dispatcher.
 */
object AuthEventBus {
    private val _events = MutableSharedFlow<AuthEvent>(
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<AuthEvent> = _events.asSharedFlow()

    fun emit(event: AuthEvent) {
        _events.tryEmit(event)
    }
}