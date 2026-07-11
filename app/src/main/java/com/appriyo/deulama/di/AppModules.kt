package com.appriyo.deulama.di

import com.appriyo.deulama.data.local.datastore.SessionManager
import com.appriyo.deulama.data.remote.networkModule
import com.appriyo.deulama.data.repository.AuthRepositoryImpl
import com.appriyo.deulama.data.repository.DramaRepositoryImpl
import com.appriyo.deulama.domain.repository.AuthRepository
import com.appriyo.deulama.domain.repository.DramaRepository
import com.appriyo.deulama.presentation.auth.AuthViewModel
import com.appriyo.deulama.presentation.details.DramaDetailsViewModel
import com.appriyo.deulama.presentation.discover.DiscoverViewModel
import com.appriyo.deulama.presentation.home.HomeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

private val appOnlyModule = module {
    single { SessionManager(get()) }

    single<AuthRepository> { AuthRepositoryImpl(get(), get(), get()) }
    single<DramaRepository> { DramaRepositoryImpl(get(), get()) }

    viewModel { AuthViewModel(get()) }
    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { DiscoverViewModel(get()) }
    viewModel { DramaDetailsViewModel(get()) }
}

val appModules = listOf(
    networkModule,
    appOnlyModule,
)