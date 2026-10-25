package com.niko.macromenza.model

import com.google.gson.annotations.SerializedName

/** Detalj jela (GET api/jela/{id}/detalj); vrijednosti su po porciji, sastojci sortirani po gramima silazno. */
data class JeloDetalj(
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
    val napomena: String?,

    @SerializedName("masaG")
    val masaG: Double?,

    @SerializedName("sastojci")
    val sastojci: List<JeloSastojak>
)

data class JeloSastojak(
    @SerializedName("nazivURecepturi")
    val nazivURecepturi: String,

    @SerializedName("namirnicaKljuc")
    val namirnicaKljuc: String,

    val grami: Double,
    val kalorije: Double,
    val proteini: Double,
    val ugljikohidrati: Double,
    val masti: Double
)
