package com.niko.macromenza.model

data class Korisnik(
    val id: Long? = null,
    val supabaseUid: String? = null,
    val ime: String,
    val prezime: String,
    val email: String,
    val lozinka_hash: String
)