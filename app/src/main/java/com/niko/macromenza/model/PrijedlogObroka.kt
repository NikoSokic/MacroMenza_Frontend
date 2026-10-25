package com.niko.macromenza.model

import com.google.gson.annotations.SerializedName

/** Odgovor `POST /api/prepoznavanje`; vrijednosti stavke su za cijelu količinu. */
data class PrijedlogObroka(
    @SerializedName("stavke") val stavke: List<PrijedlogStavka> = emptyList(),
    @SerializedName("neprepoznato") val neprepoznato: List<String> = emptyList(),
    @SerializedName("napomena") val napomena: String? = null,
    @SerializedName("model") val model: String? = null
)

data class PrijedlogStavka(
    @SerializedName("idJelo") val idJelo: Long,
    @SerializedName("naziv") val naziv: String,
    @SerializedName("kolicina") val kolicina: Double,
    // VISOKA, SREDNJA ili NISKA
    @SerializedName("sigurnost") val sigurnost: String,
    @SerializedName("kalorije") val kalorije: Double,
    @SerializedName("proteini") val proteini: Double,
    @SerializedName("ugljikohidrati") val ugljikohidrati: Double,
    @SerializedName("masti") val masti: Double
)
