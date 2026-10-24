package com.niko.macromenza.model

import com.google.gson.annotations.SerializedName

data class Jelo(
    val id: Long,
    val naziv: String,
    val opis: String?,
    val kalorije: Double,
    val proteini: Double,
    val ugljikohidrati: Double,
    val masti: Double,

    @SerializedName("sifra")
    val sifra: Int?,

    @SerializedName("kategorija")
    val kategorija: String?,

    @SerializedName("cijenaEur")
    val cijenaEur: Double?,

    @SerializedName("napomena")
    val napomena: String?
)
