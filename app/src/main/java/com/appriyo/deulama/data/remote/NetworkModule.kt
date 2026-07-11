package com.appriyo.deulama.data.remote

import com.appriyo.deulama.BuildConfig
import com.appriyo.deulama.data.remote.api.HealthApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

private val hangugJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}

val networkModule = module {

    single<OkHttpClient> {
        OkHttpClient.Builder()
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BODY
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
}