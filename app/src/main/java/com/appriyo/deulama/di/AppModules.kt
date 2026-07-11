package com.appriyo.deulama.di

import com.appriyo.deulama.data.local.datastore.AppPrefs
import com.appriyo.deulama.data.local.datastore.SessionManager
import com.appriyo.deulama.data.remote.networkModule
import com.appriyo.deulama.data.repository.AuthRepositoryImpl
import com.appriyo.deulama.data.repository.DramaRepositoryImpl
import com.appriyo.deulama.data.repository.FavoritesRepositoryImpl
import com.appriyo.deulama.data.repository.SwipeRepositoryImpl
import com.appriyo.deulama.data.repository.WatchLaterRepositoryImpl
import com.appriyo.deulama.data.repository.WatchedRepositoryImpl
import com.appriyo.deulama.domain.repository.AuthRepository
import com.appriyo.deulama.domain.repository.DramaRepository
import com.appriyo.deulama.domain.repository.FavoritesRepository
import com.appriyo.deulama.domain.repository.SwipeRepository
import com.appriyo.deulama.domain.repository.WatchLaterRepository
import com.appriyo.deulama.domain.repository.WatchedRepository
import com.appriyo.deulama.presentation.auth.AuthViewModel
import com.appriyo.deulama.presentation.details.DramaDetailsViewModel
import com.appriyo.deulama.presentation.discover.DiscoverViewModel
import com.appriyo.deulama.presentation.discover.SwipeDeckViewModel
import com.appriyo.deulama.presentation.home.HomeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

private val appOnlyModule = module {
    single { SessionManager(get()) }
    single { AppPrefs(get()) }

    single<AuthRepository> { AuthRepositoryImpl(get(), get(), get()) }
    single<DramaRepository> { DramaRepositoryImpl(get(), get()) }
    single<SwipeRepository> { SwipeRepositoryImpl(get(), get()) }
    single<FavoritesRepository> { FavoritesRepositoryImpl(get(), get()) }
    single<WatchLaterRepository> { WatchLaterRepositoryImpl(get(), get()) }
    single<WatchedRepository> { WatchedRepositoryImpl(get(), get()) }

    viewModel { AuthViewModel(get()) }
    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { DiscoverViewModel(get()) }
    viewModel { SwipeDeckViewModel(get(), get(), get(), get()) }
    viewModel { DramaDetailsViewModel(get()) }
}

val appModules = listOf(
    networkModule,
    appOnlyModule,
)