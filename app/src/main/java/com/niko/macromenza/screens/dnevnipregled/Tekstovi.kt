package com.niko.macromenza.screens.dnevnipregled

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.niko.macromenza.R
import com.niko.macromenza.util.formatGrami
import com.niko.macromenza.util.formatKolicina
import java.time.LocalDate

@Composable
fun nazivTipa(tip: String): String = when (tip) {
    "DORUCAK" -> stringResource(R.string.dp_tip_dorucak)
    "RUCAK" -> stringResource(R.string.dp_tip_rucak)
    "UZINA" -> stringResource(R.string.dp_tip_uzina)
    "VECERA" -> stringResource(R.string.dp_tip_vecera)
    else -> tip
}

/** "Ponedjeljak, 26. 10." */
@Composable
fun naslovDatuma(datum: LocalDate): String {
    val dani = stringArrayResource(R.array.dp_dani_u_tjednu)
    return stringResource(R.string.dp_datum, dani[datum.dayOfWeek.value - 1], datum.dayOfMonth, datum.monthValue)
}

/** "52P 38M 120U" */
@Composable
fun makroKratko(makro: Makro): String = stringResource(
    R.string.dp_makro_kratko,
    formatGrami(makro.proteini),
    formatGrami(makro.masti),
    formatGrami(makro.ugljikohidrati)
)

/** "1 porcija", "0,5 porcije", "2 porcije", "5 porcija" (decimalni broj uvijek uz "porcije"). */
@Composable
fun tekstPorcije(kolicina: Double): String {
    val broj = formatKolicina(kolicina)
    if (kolicina % 1.0 != 0.0) return stringResource(R.string.dp_porcija_dvije, broj)
    val n = kolicina.toLong()
    return when {
        n % 10 == 1L && n % 100 != 11L -> stringResource(R.string.dp_porcija_jedna, broj)
        n % 10 in 2..4 && n % 100 !in 12..14 -> stringResource(R.string.dp_porcija_dvije, broj)
        else -> stringResource(R.string.dp_porcija_vise, broj)
    }
}
