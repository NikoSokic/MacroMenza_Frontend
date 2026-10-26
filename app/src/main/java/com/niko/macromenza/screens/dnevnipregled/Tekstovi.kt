package com.niko.macromenza.screens.dnevnipregled



import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.niko.macromenza.R
import java.time.LocalDate

/** "Ponedjeljak, 26. 10." */
@Composable
fun naslovDatuma(datum: LocalDate): String {
    val dani = stringArrayResource(R.array.dp_dani_u_tjednu)
    return stringResource(R.string.dp_datum, dani[datum.dayOfWeek.value - 1], datum.dayOfMonth, datum.monthValue)
}
