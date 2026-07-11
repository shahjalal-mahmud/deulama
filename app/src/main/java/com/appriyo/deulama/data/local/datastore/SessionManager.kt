package com.appriyo.deulama.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.appriyo.deulama.domain.model.Session
import com.appriyo.deulama.domain.model.User
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.sessionDataStore by preferencesDataStore(name = "session")

/**
 * Persists the JWT and a cached copy of the user profile.
 *
 * Two layers:
 *  - **DataStore (preferences)** — durable storage, survives kill/reboot.
 *  - **In-memory AtomicReferences** — synchronous read for the OkHttp
 *    AuthInterceptor so it never blocks the network thread.
 *
 * The cache starts empty and is primed asynchronously from a caller-
 * supplied scope (see [prime]). Until priming completes, `currentToken()`
 * returns null and any protected request will go out unauthenticated;
 * the AuthInterceptor handles that case correctly.
 *
 * NOTE: still uses plain DataStore for the JWT. The plan calls for
 * EncryptedSharedPreferences eventually — swap the storage mechanism
 * later without touching call sites.
 */
class SessionManager(private val context: Context) {

    private object Keys {
        val TOKEN = stringPreferencesKey("jwt_token")
        val USER_ID = intPreferencesKey("user_id")
        val FULL_NAME = stringPreferencesKey("full_name")
        val EMAIL = stringPreferencesKey("email")
        val PROFILE_IMAGE = stringPreferencesKey("profile_image")
        val CREATED_AT = stringPreferencesKey("created_at")
    }

    private val tokenCache = AtomicReference<String?>(null)
    private val userCache = AtomicReference<User?>(null)

    /** Reads the persisted session once and primes the in-memory cache. */
    fun prime(scope: CoroutineScope) {
        scope.launch {
            val session = context.sessionDataStore.data.first().toSession()
            tokenCache.set(session?.token)
            userCache.set(session?.user)
        }
    }

    val tokenFlow: Flow<String?> =
        context.sessionDataStore.data.map { it[Keys.TOKEN] }

    val sessionFlow: Flow<Session?> =
        context.sessionDataStore.data.map { it.toSession() }

    // ---- Synchronous accessors (read from in-memory cache) ----

    fun currentToken(): String? = tokenCache.get()

    fun currentSession(): Session? {
        val token = tokenCache.get() ?: return null
        val user = userCache.get() ?: return null
        return Session(user, token)
    }

    // ---- Writes (update both cache and DataStore atomically per write) ----

    suspend fun saveSession(session: Session) {
        tokenCache.set(session.token)
        userCache.set(session.user)
        context.sessionDataStore.edit { prefs ->
            prefs[Keys.TOKEN] = session.token
            prefs[Keys.USER_ID] = session.user.userId
            prefs[Keys.FULL_NAME] = session.user.fullName
            prefs[Keys.EMAIL] = session.user.email
            session.user.profileImage?.let { prefs[Keys.PROFILE_IMAGE] = it }
            prefs[Keys.CREATED_AT] = session.user.createdAt
        }
    }

    suspend fun clear() {
        tokenCache.set(null)
        userCache.set(null)
        context.sessionDataStore.edit { it.clear() }
    }

    private fun Preferences.toSession(): Session? {
        val token = this[Keys.TOKEN] ?: return null
        val id = this[Keys.USER_ID] ?: return null
        val name = this[Keys.FULL_NAME] ?: return null
        val email = this[Keys.EMAIL] ?: return null
        return Session(
            user = User(
                userId = id,
                fullName = name,
                email = email,
                profileImage = this[Keys.PROFILE_IMAGE],
                createdAt = this[Keys.CREATED_AT].orEmpty(),
            ),
            token = token,
        )
    }
}