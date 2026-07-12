package com.appriyo.deulama.di

import com.appriyo.deulama.data.local.datastore.AppPrefs
import com.appriyo.deulama.data.local.datastore.SessionManager
import com.appriyo.deulama.data.local.db.databaseModule
import com.appriyo.deulama.data.remote.networkModule
import com.appriyo.deulama.data.repository.AuthRepositoryImpl
import com.appriyo.deulama.data.repository.DramaRepositoryImpl
import com.appriyo.deulama.data.repository.EngagementFailureBus
import com.appriyo.deulama.data.repository.EngagementSyncService
import com.appriyo.deulama.data.repository.FavoritesRepositoryImpl
import com.appriyo.deulama.data.repository.GenreStatsRepositoryImpl
import com.appriyo.deulama.data.repository.NoOpFailureBus
import com.appriyo.deulama.data.repository.ProfileRepositoryImpl
import com.appriyo.deulama.data.repository.RecommendationsRepositoryImpl
import com.appriyo.deulama.data.repository.SwipeRepositoryImpl
import com.appriyo.deulama.data.repository.WatchLaterRepositoryImpl
import com.appriyo.deulama.data.repository.WatchedRepositoryImpl
import com.appriyo.deulama.domain.repository.AuthRepository
import com.appriyo.deulama.domain.repository.DramaRepository
import com.appriyo.deulama.domain.repository.FavoritesRepository
import com.appriyo.deulama.domain.repository.GenreStatsRepository
import com.appriyo.deulama.domain.repository.ProfileRepository
import com.appriyo.deulama.domain.repository.RecommendationsRepository
import com.appriyo.deulama.domain.repository.SwipeRepository
import com.appriyo.deulama.domain.repository.WatchLaterRepository
import com.appriyo.deulama.domain.repository.WatchedRepository
import com.appriyo.deulama.presentation.auth.AuthViewModel
import com.appriyo.deulama.presentation.details.DramaDetailsViewModel
import com.appriyo.deulama.presentation.discover.DiscoverViewModel
import com.appriyo.deulama.presentation.discover.SwipeDeckViewModel
import com.appriyo.deulama.presentation.genre.GenreStatsViewModel
import com.appriyo.deulama.presentation.home.HomeViewModel
import com.appriyo.deulama.presentation.profile.EditProfileViewModel
import com.appriyo.deulama.presentation.recommendations.RecommendationsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

private val appOnlyModule = module {
    single { SessionManager(get()) }
    single { AppPrefs(get()) }

    // App-scope used by the sync service to replay anon-mode queues
    // without blocking the UI thread.
    single<CoroutineScope> {
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    // Default no-op failure bus — keeps tests / previews simple. The
    // screen collects events through `EngagementSyncService` so the
    // bus is mainly a hook for direct repository callers.
    single<EngagementFailureBus> { NoOpFailureBus() }

    single<AuthRepository> { AuthRepositoryImpl(get(), get(), get()) }
    single<DramaRepository> { DramaRepositoryImpl(get(), get()) }
    single<SwipeRepository> { SwipeRepositoryImpl(get(), get()) }
    single<FavoritesRepository> {
        FavoritesRepositoryImpl(get(), get(), get(), get(), get())
    }
    single<WatchLaterRepository> {
        WatchLaterRepositoryImpl(get(), get(), get(), get(), get())
    }
    single<WatchedRepository> {
        WatchedRepositoryImpl(get(), get(), get(), get(), get())
    }
    single<RecommendationsRepository> {
        RecommendationsRepositoryImpl(get(), get())
    }
    single<GenreStatsRepository> {
        GenreStatsRepositoryImpl(get(), get())
    }
    single<ProfileRepository> {
        ProfileRepositoryImpl(get(), get(), get())
    }

    // Phase-4 sync-on-login — wired to start() from HangugDeulamaApp.
    single {
        EngagementSyncService(
            sessionManager = get(),
            swipeApi = get(),
            favoritesApi = get(),
            watchLaterApi = get(),
            watchedApi = get(),
            swipeDao = get(),
            favoriteDao = get(),
            watchLaterDao = get(),
            watchedDao = get(),
            json = get(),
            scope = get(),
        )
    }

    viewModel { AuthViewModel(get()) }
    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { DiscoverViewModel(get()) }
    viewModel { SwipeDeckViewModel(get(), get(), get(), get()) }
    viewModel { DramaDetailsViewModel(get(), get(), get(), get()) }
    viewModel { RecommendationsViewModel(get(), get(), get()) }
    viewModel { GenreStatsViewModel(get(), get()) }
    viewModel { EditProfileViewModel(get(), get()) }
}

val appModules = listOf(
    networkModule,
    databaseModule,
    appOnlyModule,
)
