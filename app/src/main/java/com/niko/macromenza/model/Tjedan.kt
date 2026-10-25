package com.niko.macromenza.model

import com.google.gson.annotations.SerializedName

/**
 * Odgovor GET api/obroci/tjedan: tjedan pon–ned, točno 7 dana. [prosjek] je null kad tjedan nema unosa,
 * [prosjekCilj] i kad nijedan dan s unosom nema cilj.
 */
data class Tjedan(
    val pocetak: String,
    val kraj: String,
    val dani: List<TjedanDan>,
    val daniSUnosom: Int,
    val daniUCilju: Int,
    val prosjek: NutritivneVrijednosti?,
    val prosjekCilj: NutritivneVrijednosti?,
    val imaPrethodni: Boolean,
    val imaSljedeci: Boolean
)

/** Dan tjedna; [ciljKalorije] je null kad za taj dan nema preporuke. */
data class TjedanDan(
    val datum: String,
    val kalorije: Double,
    val proteini: Double,
    val ugljikohidrati: Double,
    val masti: Double,
    val ciljKalorije: Double?,
    val imaUnos: Boolean,
    @SerializedName("uCilju")
    val uCilju: Boolean
)
