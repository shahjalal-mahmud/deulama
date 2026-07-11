package com.appriyo.deulama

import android.app.Application
import com.appriyo.deulama.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class HangugDeulamaApp : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@HangugDeulamaApp)
            modules(appModules)
        }
    }
}