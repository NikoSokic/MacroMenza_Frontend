package com.niko.macromenza.model

import com.google.gson.annotations.SerializedName

data class Obrok(
    @SerializedName("id")
    val id: Long,

    @SerializedName("datum")
    val datum: String,

    @SerializedName("vrijeme")
    val vrijeme: String,

    @SerializedName("tipObroka")
    val tipObroka: String,

    @SerializedName("izvor")
    val izvor: String,

    @SerializedName("stavke")
    val stavke: List<ObrokStavka>,

    @SerializedName("ukupnoKalorije")
    val ukupnoKalorije: Double,

    @SerializedName("ukupnoProteini")
    val ukupnoProteini: Double,

    @SerializedName("ukupnoUgljikohidrati")
    val ukupnoUgljikohidrati: Double,

    @SerializedName("ukupnoMasti")
    val ukupnoMasti: Double
)

data class ObrokStavka(
    @SerializedName("idJelo")
    val idJelo: Long,

    @SerializedName("naziv")
    val naziv: String,

    @SerializedName("kolicina")
    val kolicina: Double,

    @SerializedName("kalorije")
    val kalorije: Double,

    @SerializedName("proteini")
    val proteini: Double,

    @SerializedName("ugljikohidrati")
    val ugljikohidrati: Double,

    @SerializedName("masti")
    val masti: Double
)
