package com.echosafe.data.remote

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object BackendConfig {

    /**
     * Set your active Ngrok URL here when backend is running on a remote laptop.
     * Example: "https://a1b2-102-132-21-4.ngrok-free.app/api/"
     * Or local fallback: "http://10.0.2.2:5000/api/" (for local emulator)
     */
    var BASE_URL: String = "https://275a-146-141-14-63.ngrok-free.app/api/"
        set(value) {
            field = if (value.endsWith("/")) value else "$value/"
            retrofitInstance = null
        }

    var jwtToken: String? = null

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val requestBuilder = original.newBuilder()
            .header("ngrok-skip-browser-warning", "true") // Bypass Ngrok warning landing page
            .header("User-Agent", "EchoSafe-Android-Client/1.2")

        jwtToken?.let { token ->
            if (token.isNotEmpty()) {
                requestBuilder.header("Authorization", "Bearer $token")
            }
        }

        chain.proceed(requestBuilder.build())
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private var retrofitInstance: Retrofit? = null

    fun getRetrofit(): Retrofit {
        if (retrofitInstance == null) {
            retrofitInstance = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofitInstance!!
    }

    val apiService: EchoSafeApiService
        get() = getRetrofit().create(EchoSafeApiService::class.java)
}
