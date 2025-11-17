package com.diabeat.di

import com.diabeat.data.api.OpenFoodFactsApi
import com.diabeat.data.api.USDAFoodDataApi
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class USDARetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class OpenFoodFactsRetrofit

@Module
@InstallIn(SingletonComponent::class)
object FoodModule {
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
    
    /**
     * USDA FoodData Central API
     * Base URL: https://api.nal.usda.gov/fdc/
     */
    @Provides
    @Singleton
    @USDARetrofit
    fun provideUSDARetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.nal.usda.gov/fdc/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
    
    @Provides
    @Singleton
    fun provideUSDAFoodDataApi(@USDARetrofit retrofit: Retrofit): USDAFoodDataApi {
        return retrofit.create(USDAFoodDataApi::class.java)
    }
    
    /**
     * OpenFoodFacts API
     * Base URL: https://world.openfoodfacts.org/
     */
    @Provides
    @Singleton
    @OpenFoodFactsRetrofit
    fun provideOpenFoodFactsRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://world.openfoodfacts.org/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
    
    @Provides
    @Singleton
    fun provideOpenFoodFactsApi(@OpenFoodFactsRetrofit retrofit: Retrofit): OpenFoodFactsApi {
        return retrofit.create(OpenFoodFactsApi::class.java)
    }
}
