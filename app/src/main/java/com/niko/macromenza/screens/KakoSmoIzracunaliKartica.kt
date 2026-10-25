package com.niko.macromenza.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niko.macromenza.model.IzracunPreporuke
import com.niko.macromenza.ui.theme.MacroGreen
import com.niko.macromenza.util.formatKcalTisuce

import com.niko.macromenza.util.formatMakro

/**
 * Kartica "Kako smo izračunali" s korisnikovim brojevima: Mifflin-St Jeor BMR × aktivnost ± cilj,
 * zatim proteini (2 g/kg), masti (25 % kcal / 9) i ugljikohidrati (ostatak / 4).
 */
@Composable
fun KakoSmoIzracunaliKartica(
    izracun: IzracunPreporuke,
    masa: Double,
    naslov: String = "Kako smo izračunali",
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = naslov,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(14.dp))

            Korak(
                naziv = "1. BMR (Mifflin-St Jeor)",
                racun = "BMR = ${izracun.bmrFormula} = ${formatKcalTisuce(izracun.bmr)} kcal"
            )

            Korak(
                naziv = "2. Dnevna potrošnja",
                racun = "${formatKcalTisuce(izracun.bmr)} × ${formatKolicina3(izracun.faktorAktivnosti)} " +
                    "= ${formatKcalTisuce(izracun.tdee)} kcal"
            )

            Korak(
                naziv = "3. Prilagodba cilju",
                racun = when {
                    izracun.korekcijaCilja < 0 ->
                        "${formatKcalTisuce(izracun.tdee)} − ${formatKcalTisuce(-izracun.korekcijaCilja)} " +
                            "= ${formatKcalTisuce(izracun.ciljKalorije.toDouble())} kcal"
                    izracun.korekcijaCilja > 0 ->
                        "${formatKcalTisuce(izracun.tdee)} + ${formatKcalTisuce(izracun.korekcijaCilja)} " +
                            "= ${formatKcalTisuce(izracun.ciljKalorije.toDouble())} kcal"
                    else -> "Održavanje, bez korekcije = ${formatKcalTisuce(izracun.ciljKalorije.toDouble())} kcal"
                }
            )

            Korak(
                naziv = "4. Proteini (2 g po kg)",
                racun = "2 × ${formatMakro(masa)} kg = ${izracun.proteini} g"
            )

            Korak(
                naziv = "5. Masti (25 % kalorija)",
                racun = "${formatKcalTisuce(izracun.ciljKalorije.toDouble())} × 0,25 ÷ 9 = ${izracun.masti} g"
            )

            Korak(
                naziv = "6. Ugljikohidrati (ostatak)",
                racun = "(${formatKcalTisuce(izracun.ciljKalorije.toDouble())} − ${izracun.proteini} × 4 − " +
                    "${izracun.masti} × 9) ÷ 4 = ${izracun.ugljikohidrati} g",
                zadnji = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Cilj: ${formatKcalTisuce(izracun.ciljKalorije.toDouble())} kcal · " +
                    "P ${izracun.proteini} g · UH ${izracun.ugljikohidrati} g · M ${izracun.masti} g",
                color = MacroGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Brojevi su zaokruženi, pa se račun može razlikovati za 1–2 jedinice.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun Korak(naziv: String, racun: String, zadnji: Boolean = false) {
    Text(
        text = naziv,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp
    )

    Spacer(modifier = Modifier.height(2.dp))

    Text(
        text = racun,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    )

    if (!zadnji) {
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.09f))
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/** Faktor aktivnosti s do tri decimale, npr. 1.375 -> "1,375", 1.55 -> "1,55". */
private fun formatKolicina3(vrijednost: Double): String =
    String.format(java.util.Locale.forLanguageTag("hr-HR"), "%.3f", vrijednost).trimEnd('0').trimEnd(',')
