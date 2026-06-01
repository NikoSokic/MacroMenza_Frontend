package com.niko.macromenza.model

import com.google.gson.annotations.SerializedName

data class KonzumacijaRequest(
    val idKorisnik: Long,
    val idJelo: Long,
    val kolicina: Int,

    @SerializedName("tip_obroka")
    val tipObroka: String
)