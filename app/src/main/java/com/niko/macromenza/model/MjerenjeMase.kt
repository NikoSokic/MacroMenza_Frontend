package com.niko.macromenza.model

/** Točka grafa mase iz GET /api/mjerenja (zadnje mjerenje dana). */
data class MjerenjeMase(
    val id: Long,
    val datum: String,
    val masa: Double
)
