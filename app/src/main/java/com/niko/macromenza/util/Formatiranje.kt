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

/** Masa na jednu decimalu s jedinicom, npr. 72.4 -> "72,4 kg". */
fun formatMasa(vrijednost: Double): String = String.format(HR, "%.1f kg", vrijednost)

/** Promjena mase s predznakom, npr. -1.2 -> "−1,2 kg", 0.3 -> "+0,3 kg". */
fun formatPromjenaMase(vrijednost: Double): String {
    val zaokruzeno = Math.round(vrijednost * 10) / 10.0
    val iznos = String.format(HR, "%.1f kg", kotlin.math.abs(zaokruzeno))
    return when {
        zaokruzeno < 0 -> "−$iznos"
        zaokruzeno > 0 -> "+$iznos"
        else -> iznos
    }
}

/** Unos mase: znamenke, najviše jedan zarez ili točka i najviše jedna decimala; inače null (unos se odbija). */
fun filtrirajUnosMase(unos: String): String? {
    if (!unos.all { it.isDigit() || it == ',' || it == '.' }) return null
    val separatori = unos.count { it == ',' || it == '.' }
    if (separatori > 1) return null
    val decimale = unos.substringAfter(',', "").ifEmpty { unos.substringAfter('.', "") }
    if (decimale.length > 1) return null
    if (unos.substringBefore(',').substringBefore('.').length > 3) return null
    return unos
}

/** Masa iz unosa s decimalnim zarezom ili točkom, npr. "72,4" -> 72.4. */
fun parsirajMasu(unos: String): Double? = unos.trim().replace(',', '.').toDoubleOrNull()
