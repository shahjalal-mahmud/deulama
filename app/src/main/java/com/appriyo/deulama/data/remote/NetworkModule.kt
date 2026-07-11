package com.appriyo.deulama.data.remote

import com.appriyo.deulama.BuildConfig
import com.appriyo.deulama.data.remote.api.AuthApi
import com.appriyo.deulama.data.remote.api.DramaApi
import com.appriyo.deulama.data.remote.api.HealthApi
import com.appriyo.deulama.data.remote.interceptor.AuthInterceptor
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module
import retrofit2.Retrofit

/**
 * Shared JSON config — exposed as a Koin singleton so repositories
 * can use it to parse error bodies (e.g. for the safeApiCall helper).
 */
internal val hangugJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}

val networkModule = module {

    single<Json> { hangugJson }

    single { AuthInterceptor(get(), AuthEventBus) }

    single<OkHttpClient> {
        OkHttpClient.Builder()
            // Auth goes in BEFORE the logging interceptor so the log
            // headers show the attached Authorization header.
            .addInterceptor(get<AuthInterceptor>())
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            // Use HEADERS (not BODY) so errorBody isn't
                            // consumed by the logger before safeApiCall
                            // can read it on a non-2xx response.
                            level = HttpLoggingInterceptor.Level.HEADERS
                        }
                    )
                }
            }
            .build()
    }

    single<Retrofit> {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(get())
            .addConverterFactory(
                hangugJson.asConverterFactory("application/json".toMediaType())
            )
            .build()
    }

    single<HealthApi> { get<Retrofit>().create(HealthApi::class.java) }
    single<AuthApi> { get<Retrofit>().create(AuthApi::class.java) }
    single<DramaApi> { get<Retrofit>().create(DramaApi::class.java) }
}