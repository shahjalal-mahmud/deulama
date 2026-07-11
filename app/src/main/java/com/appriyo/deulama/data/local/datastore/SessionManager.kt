package com.appriyo.deulama.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore by preferencesDataStore(name = "session")

/**
 * Wraps a plain DataStore<Preferences> for now. NOTE: per the plan
 * (section 6), the real JWT should eventually live in
 * EncryptedSharedPreferences or a Keystore-backed DataStore — this is
 * just getting the plumbing wired up so the rest of the app has
 * something to inject. Swap the storage mechanism later without
 * touching call sites.
 */
class SessionManager(private val context: Context) {

    private object Keys {
        val TOKEN = stringPreferencesKey("jwt_token")
    }

    val tokenFlow: Flow<String?> =
        context.sessionDataStore.data.map { prefs -> prefs[Keys.TOKEN] }

    suspend fun saveToken(token: String) {
        context.sessionDataStore.edit { prefs -> prefs[Keys.TOKEN] = token }
    }

    suspend fun clear() {
        context.sessionDataStore.edit { prefs -> prefs.remove(Keys.TOKEN) }
    }
}