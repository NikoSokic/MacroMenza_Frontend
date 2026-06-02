package com.niko.macromenza.model

data class RegistracijaProfilRequest(
    val supabaseUid: String,
    val email: String,
    val ime: String,
    val prezime: String,
    val spol: String,
    val visina: Int,
    val dob: Int,
    val masa: Double,
    val razinaAktivnosti: String,
    val tipCilja: String
)