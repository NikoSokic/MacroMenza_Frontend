package com.niko.macromenza.model

data class DnevniPregled(
    val datum: String,
    val kalorije: Double,
    val proteini: Double,
    val ugljikohidrati: Double,
    val masti: Double
)