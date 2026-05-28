package com.niko.macromenza.model

data class PovijestDana(
    val datum: String,
    val ukupnoKalorije: Double,
    val ukupnoProteini: Double,
    val ukupnoUgljikohidrati: Double,
    val ukupnoMasti: Double,
    val obroci: Map<String, List<StavkaObroka>>
)