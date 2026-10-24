package com.niko.macromenza.model

data class StavkaObroka(
    val nazivJela: String,
    val tipObroka: String,
    val kolicina: Double,
    val kalorije: Double,
    val proteini: Double,
    val ugljikohidrati: Double,
    val masti: Double
)