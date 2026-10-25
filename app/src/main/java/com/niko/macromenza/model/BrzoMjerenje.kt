package com.niko.macromenza.model

data class BrzoMjerenjeRequest(
    val idKorisnik: Long,
    val masa: Double
)

/** Odgovor POST /api/mjerenja/brzo: spremljeno današnje mjerenje i nova preporuka. */
data class BrzoMjerenjeOdgovor(
    val mjerenje: MjerenjeMase,
    val preporuka: Preporuka
)
