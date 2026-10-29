package com.niko.macromenza.api

import com.niko.macromenza.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitInstance {

    private const val BASE_URL = BuildConfig.BASE_URL

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
            .create(ApiService::class.java)
    }

    /**
     * Za spore pozive: "Učitaj primjer tjedna" na backendu traje oko 16 s (21 obrok, pojedinačni upisi u udaljenu bazu),
     * pa bi zadani timeout od 10 s prekinuo vezu dok backend još uspješno piše podatke.
     */
    val sporiApi: ApiService get() = prepoznavanjeApi

    // Prepoznavanje obroka zove vanjski AI servis, pa treba duži timeout od zadanih 10 s.
    val prepoznavanjeApi: ApiService by lazy {
        val klijent = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .callTimeout(70, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(klijent)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
            .create(ApiService::class.java)
    }
}
