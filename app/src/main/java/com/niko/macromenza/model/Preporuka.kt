package com.niko.macromenza.model

data class Preporuka(
    val id: Long? = null,
    val idKorisnik: Long,
    val kalorije: Double,
    val proteini: Double,
    val ugljikohidrati: Double,
    val masti: Double,
    val datum: String
)