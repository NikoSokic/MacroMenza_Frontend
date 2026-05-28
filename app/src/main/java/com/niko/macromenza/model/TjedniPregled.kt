package com.niko.macromenza.model

data class TjedniPregled(
    val datumOd: String,
    val datumDo: String,
    val ukupnoKalorije: Double,
    val ukupnoProteini: Double,
    val ukupnoUgljikohidrati: Double,
    val ukupnoMasti: Double,
    val prosjekKalorija: Double,
    val dani: List<DnevniPregled>
)