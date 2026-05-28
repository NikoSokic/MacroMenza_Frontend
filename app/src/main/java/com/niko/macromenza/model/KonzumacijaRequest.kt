package com.niko.macromenza.model

import com.google.gson.annotations.SerializedName

data class KonzumacijaRequest(
    val idKorisnik: Long = 1,
    val idJelo: Long,
    val kolicina: Int,

    @SerializedName("tip_obroka")
    val tipObroka: String
)