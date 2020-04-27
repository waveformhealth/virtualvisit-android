package com.waveformhealth.di

import com.waveformhealth.api.WaveformApiService
import com.waveformhealth.repo.WaveformServiceRepository
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
class AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY

        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideWaveformServiceRepository(waveformApiService: WaveformApiService): WaveformServiceRepository {
        return WaveformServiceRepository(waveformApiService)
    }

    @Singleton
    @Provides
    fun provideWaveformService(okHttpClient: OkHttpClient): WaveformApiService {
        return Retrofit.Builder()
            .baseUrl("https://twilio-test-wf.herokuapp.com")
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
            .create(WaveformApiService::class.java)
    }
}