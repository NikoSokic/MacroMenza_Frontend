package com.niko.macromenza.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object SupabaseAuthInstance {

    private const val SUPABASE_URL = "https://nncjmtrxderijntybnhb.supabase.co/"
    const val SUPABASE_ANON_KEY = "sb_publishable_vs4HOEwugnBnhHtMBUV7gw_nO16Wjah"

    val api: SupabaseAuthApi by lazy {
        Retrofit.Builder()
            .baseUrl(SUPABASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SupabaseAuthApi::class.java)
    }
}