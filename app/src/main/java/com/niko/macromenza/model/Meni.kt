package com.niko.macromenza.model

import com.google.gson.annotations.SerializedName

data class MeniDTO(
    @SerializedName("id")
    val id: Long,

    @SerializedName("sifra")
    val sifra: String,

    @SerializedName("tip")
    val tip: String,

    @SerializedName("vegetarijanski")
    val vegetarijanski: Boolean,

    @SerializedName("redniBroj")
    val redniBroj: Int?,

    @SerializedName("kalorije")
    val kalorije: Double,

    @SerializedName("proteini")
    val proteini: Double,

    @SerializedName("ugljikohidrati")
    val ugljikohidrati: Double,

    @SerializedName("masti")
    val masti: Double,

    @SerializedName("stavke")
    val stavke: List<MeniStavkaDTO>
)

data class MeniStavkaDTO(
    @SerializedName("idJelo")
    val idJelo: Long,

    @SerializedName("oznaka")
    val oznaka: String,

    @SerializedName("nazivJela")
    val nazivJela: String,

    @SerializedName("kolicina")
    val kolicina: Double,

    @SerializedName("priblizno")
    val priblizno: Boolean,

    @SerializedName("kalorije")
    val kalorije: Double,

    @SerializedName("proteini")
    val proteini: Double,

    @SerializedName("ugljikohidrati")
    val ugljikohidrati: Double,

    @SerializedName("masti")
    val masti: Double
)
