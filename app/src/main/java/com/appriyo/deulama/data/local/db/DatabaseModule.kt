package com.appriyo.deulama.data.local.db

import androidx.room.Room
import com.appriyo.deulama.data.local.db.dao.LocalFavoriteDao
import com.appriyo.deulama.data.local.db.dao.LocalSwipeDao
import com.appriyo.deulama.data.local.db.dao.LocalWatchLaterDao
import com.appriyo.deulama.data.local.db.dao.LocalWatchedDao
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Builds the singleton [AppDatabase] and exposes its DAOs.
 *
 * Uses `fallbackToDestructiveMigration()` because the engagement
 * queues are intentionally ephemeral — a schema bump is allowed to
 * wipe them since the worst case is a user losing their pending
 * anonymous taps.
 *
 * If the queues grow more important, swap this for explicit Migration
 * objects before any release build ships.
 */
val databaseModule = module {

    single<AppDatabase> {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "hangug-deulama.db",
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    single<LocalSwipeDao> { get<AppDatabase>().localSwipeDao() }
    single<LocalFavoriteDao> { get<AppDatabase>().localFavoriteDao() }
    single<LocalWatchLaterDao> { get<AppDatabase>().localWatchLaterDao() }
    single<LocalWatchedDao> { get<AppDatabase>().localWatchedDao() }
}
