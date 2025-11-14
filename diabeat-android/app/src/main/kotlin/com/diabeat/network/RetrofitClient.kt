package com.diabeat.network

import android.content.Context
import com.diabeat.R
import com.diabeat.utils.TokenManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private var BASE_URL = "http://10.0.2.2:8000/" // 默认模拟器地址
    private lateinit var _applicationContext: Context
    
    // 将 apiService 声明为 lateinit var 以便在测试中重新赋值
    lateinit var apiService: ApiService

    init {
        // 初始化时设置默认的 apiService，可能需要一个默认的 Context 或延迟初始化
        // 为了测试方便，我们假设在 init 之前或之后会调用 init(context) 或 setTestingApiService
    }

    fun init(context: Context) {
        _applicationContext = context.applicationContext // 保存 Application Context
        BASE_URL = context.resources.getString(R.string.api_base_url)
        if (!BASE_URL.endsWith("/")) {
            BASE_URL += "/"
        }
        rebuildRetrofit()
    }

    // 用于测试时注入模拟的 ApiService
    fun setTestingApiService(testApiService: ApiService) {
        apiService = testApiService
    }
    
    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val token = TokenManager.getToken(_applicationContext)
        
        val requestBuilder = original.newBuilder()
        if (token != null) {
            requestBuilder.header("Authorization", "Bearer $token")
        }
        
        chain.proceed(requestBuilder.build())
    }
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    @Volatile
    private var retrofit: Retrofit? = null
    
    private fun rebuildRetrofit() {
        android.util.Log.d("RetrofitClient", "Rebuilding Retrofit with BASE_URL=$BASE_URL")
        retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        apiService = retrofit!!.create(ApiService::class.java) // 每次重建时更新 apiService
    }
    
    // 移除之前的 apiService getter
    // val apiService:
    //     get() {
    //         val instance = retrofit ?: run {
    //             rebuildRetrofit()
    //             retrofit!!
    //         }
    //         return instance.create(ApiService::class.java)
    //     }
}

