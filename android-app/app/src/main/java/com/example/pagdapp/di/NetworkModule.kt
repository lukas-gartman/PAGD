package com.example.pagdapp.di

import com.example.pagdapp.data.remote.api.IGoogleApi
import com.example.pagdapp.data.remote.api.IPAGDApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {


    private const val PAGD_BASE_URL = "https://lukas.tottes.net"
    private const val GOOGLE_BASE_URL = "https://maps.googleapis.com/maps/api/"

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }


        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("PAGDRetrofit")
    fun providePAGDRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(PAGD_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @Named("GoogleServiceRetrofit")
    fun provideGoogleServiceRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(GOOGLE_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun providePAGDApi(@Named("PAGDRetrofit") retrofit: Retrofit): IPAGDApi {
        return retrofit.create(IPAGDApi::class.java)
    }

    @Provides
    @Singleton
    fun provideIGoogleApi(@Named("GoogleServiceRetrofit") retrofit: Retrofit): IGoogleApi {
        return retrofit.create(IGoogleApi::class.java)
    }

}