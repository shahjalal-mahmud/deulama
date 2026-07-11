package com.appriyo.deulama.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appPrefsDataStore by preferencesDataStore(name = "app_prefs")

/**
 * Tiny preferences store for first-launch / one-time UX flags.
 * Separate from [SessionManager] so logging out doesn't reset the
 * "user has seen the coach mark" state.
 */
class AppPrefs(private val context: Context) {

    private object Keys {
        val SWIPE_COACH_MARK_SEEN = booleanPreferencesKey("swipe_coach_mark_seen")
    }

    /** Hot flow — emits the current value on collect. */
    val swipeCoachMarkSeen: Flow<Boolean> =
        context.appPrefsDataStore.data.map { it[Keys.SWIPE_COACH_MARK_SEEN] ?: false }

    suspend fun markSwipeCoachMarkSeen() {
        context.appPrefsDataStore.edit { it[Keys.SWIPE_COACH_MARK_SEEN] = true }
    }
}
