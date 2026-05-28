package com.niko.macromenza.model

data class ProfilPodaci(
    val id: Long? = null,
    val idKorisnik: Long,
    val visina: Int,
    val spol: String,
    val dob: Int
)