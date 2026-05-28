package com.niko.macromenza.model

import com.google.gson.annotations.SerializedName

data class Mjerenje(
    val id: Long? = null,

    val idKorisnik: Long,

    val masa: Double,

    @SerializedName("razina_aktivnosti")
    val razinaAktivnosti: String,

    @SerializedName("tip_cilja")
    val tipCilja: String,

    val datum: String? = null
)