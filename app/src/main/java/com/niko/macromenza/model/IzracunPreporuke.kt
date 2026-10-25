package com.niko.macromenza.model

/** Zahtjev za POST api/preporuke/pregled; ništa se ne sprema. */
data class IzracunPreporukeRequest(
    val spol: String,
    val dob: Int,
    val visina: Int,
    val masa: Double,
    val razinaAktivnosti: String,
    val tipCilja: String
)

/**
 * Koraci izračuna dnevne preporuke. [ciljKalorije], [proteini], [masti] i [ugljikohidrati] su zaokruženi
 * jednako kao spremljena preporuka.
 */
data class IzracunPreporuke(
    val bmr: Double,
    val bmrFormula: String,
    val faktorAktivnosti: Double,
    val tdee: Double,
    val korekcijaCilja: Double,
    val ciljKalorije: Int,
    val proteini: Int,
    val masti: Int,
    val ugljikohidrati: Int
)

/** Odgovor GET api/izvori-podataka. */
data class IzvoriPodataka(
    val brojJela: Int,
    val brojNamirnica: Int,
    val brojMenija: Int,
    val izvorNormativa: String
)
