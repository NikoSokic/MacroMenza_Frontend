package com.niko.macromenza.model

data class Jelo(
    val id: Long,
    val naziv: String,
    val opis: String?,
    val kalorije: Double,
    val proteini: Double,
    val ugljikohidrati: Double,
    val masti: Double
)
