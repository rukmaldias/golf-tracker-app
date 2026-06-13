package com.rapsodo.golf.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.github.aakira.napier.Napier

import com.rapsodo.golf.data.remote.PlayerApi
import com.rapsodo.golf.data.remote.createPlayerApi
import com.rapsodo.golf.domain.logger.AppLogger

import de.jensklingenberg.ktorfit.Ktorfit
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun provideHttpClient(): HttpClient {
        Napier.d(tag = AppLogger.TAG) { "Creating HttpClient" }
        return HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }

    @Provides @Singleton
    fun provideKtorfit(client: HttpClient): Ktorfit {
        Napier.d(tag = AppLogger.TAG) { "Creating Ktorfit with baseUrl: https://6a2c265d3e2b60ab038f87fb.mockapi.io/api/v1/" }
        return Ktorfit.Builder()
            .baseUrl("https://6a2c265d3e2b60ab038f87fb.mockapi.io/api/v1/")
            .httpClient(client)
            .build()
    }

    @Provides @Singleton
    fun providePlayerApi(ktorfit: Ktorfit): PlayerApi {
        Napier.d(tag = AppLogger.TAG) { "Creating PlayerApi" }
        return ktorfit.createPlayerApi()
    }
}