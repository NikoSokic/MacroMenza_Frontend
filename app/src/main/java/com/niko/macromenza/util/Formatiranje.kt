package com.niko.macromenza.util

import java.util.Locale

private val HR = Locale.forLanguageTag("hr-HR")

/** Kalorije bez decimala, npr. 1350,25 -> "1350". */
fun formatKcal(vrijednost: Double): String = String.format(HR, "%.0f", vrijednost)

/** Makronutrijenti na jednu decimalu s decimalnim zarezom, npr. 36,04 -> "36,0". */
fun formatMakro(vrijednost: Double): String = String.format(HR, "%.1f", vrijednost)

/** Grami bez decimala, npr. 67,5 -> "68". */
fun formatGrami(vrijednost: Double): String = String.format(HR, "%.0f", vrijednost)

/** Cijena na dvije decimale s decimalnim zarezom, npr. 1.99 -> "1,99". */
fun formatCijena(vrijednost: Double): String = String.format(HR, "%.2f", vrijednost)

/** Broj porcija: cijeli broj bez decimala, inače jedna decimala, npr. 1.0 -> "1", 1.5 -> "1,5". */
fun formatKolicina(vrijednost: Double): String =
    if (vrijednost % 1.0 == 0.0) String.format(HR, "%.0f", vrijednost)
    else String.format(HR, "%.1f", vrijednost)
