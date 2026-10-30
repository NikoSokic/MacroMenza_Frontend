package com.niko.macromenza.api

import com.niko.macromenza.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitInstance {

    private const val BASE_URL = BuildConfig.BASE_URL

    /**
     * Token na svakom zahtjevu i osvježavanje pri 401. Duži timeouti zbog Rendera: besplatni servis se uspava
     * pa prvi poziv nakon pauze traje i do minutu.
     */
    private fun klijent(builder: OkHttpClient.Builder): OkHttpClient =
        builder
            .addInterceptor(TokenInterceptor())
            .authenticator(TokenAuthenticator())
            .build()

    val api: ApiService by lazy {
        val klijent = klijent(
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
        )

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(klijent)
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
        val klijent = klijent(
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .callTimeout(100, TimeUnit.SECONDS)
        )

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
