package com.appriyo.deulama.di

import com.appriyo.deulama.data.local.datastore.SessionManager
import com.appriyo.deulama.data.remote.networkModule
import com.appriyo.deulama.presentation.home.HomeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

private val appOnlyModule = module {
    single { SessionManager(get()) }
    viewModel { HomeViewModel(get()) }
}

val appModules = listOf(
    networkModule,
    appOnlyModule,
)