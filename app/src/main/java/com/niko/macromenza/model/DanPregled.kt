package com.niko.macromenza.model

/** Odgovor GET api/obroci/dan-pregled. [cilj] je null kad korisnik nema preporuku. */
data class DanPregled(
    val cilj: NutritivneVrijednosti?,
    val pojedeno: NutritivneVrijednosti,
    val poTipu: List<KalorijeTipa>,
    val savjet: Savjet
)

data class NutritivneVrijednosti(
    val kalorije: Double,
    val proteini: Double,
    val ugljikohidrati: Double,
    val masti: Double
)

data class KalorijeTipa(
    val tipObroka: String,
    val kalorije: Double,
    val brojObroka: Int
)

data class Savjet(
    val naslov: String,
    val tekst: String,
    val jela: List<SavjetJelo>
)

data class SavjetJelo(
    val idJelo: Long,
    val naziv: String,
    val kalorije: Double,
    val proteini: Double
)
