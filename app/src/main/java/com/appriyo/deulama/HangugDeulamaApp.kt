package com.appriyo.deulama

import android.app.Application
import com.appriyo.deulama.data.local.datastore.SessionManager
import com.appriyo.deulama.data.repository.EngagementSyncService
import com.appriyo.deulama.di.appModules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class HangugDeulamaApp : Application() {

    // App-scope used to prime the SessionManager cache off the main thread.
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@HangugDeulamaApp)
            modules(appModules)
        }

        get<SessionManager>().prime(appScope)

        // Phase-4: kick the anon → server sync observer. It subscribes
        // to SessionManager.sessionFlow and replays queued engagement
        // rows once a non-null session appears (i.e. registration or
        // login completes).
        get<EngagementSyncService>().start()
    }
}
