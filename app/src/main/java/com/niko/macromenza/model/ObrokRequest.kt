package com.niko.macromenza.model

import com.google.gson.annotations.SerializedName

data class ObrokRequest(
    @SerializedName("idKorisnik")
    val idKorisnik: Long,

    // "2026-10-25"
    @SerializedName("datum")
    val datum: String,

    // "13:05"
    @SerializedName("vrijeme")
    val vrijeme: String,

    @SerializedName("tipObroka")
    val tipObroka: String,

    @SerializedName("izvor")
    val izvor: String = "RUCNO",

    @SerializedName("stavke")
    val stavke: List<StavkaObrokaRequest>
)

data class StavkaObrokaRequest(
    @SerializedName("idJelo")
    val idJelo: Long,

    @SerializedName("kolicina")
    val kolicina: Double
)
