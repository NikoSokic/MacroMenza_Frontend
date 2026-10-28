package com.niko.macromenza.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.niko.macromenza.R
import com.niko.macromenza.model.IzracunPreporuke
import com.niko.macromenza.ui.components.Makro
import com.niko.macromenza.ui.components.makroKratko
import com.niko.macromenza.ui.theme.MacroDimenzije
import com.niko.macromenza.ui.theme.MacroRazmaci
import com.niko.macromenza.util.formatKcalTisuce
import com.niko.macromenza.util.formatMakro

/**
 * Objašnjenje "Kako smo izračunali" s korisnikovim brojevima: Mifflin-St Jeor BMR × aktivnost ± cilj,
 * zatim proteini (2 g/kg), masti (25 % kcal / 9) i ugljikohidrati (ostatak / 4).
 * Bez kartice (pages/profil.md § Postavke ciljeva): koraci su redovi s dividerima, rezultat u onSurface.
 * Koristi ga i onboarding.
 */
@Composable
fun KakoSmoIzracunaliKartica(
    izracun: IzracunPreporuke,
    masa: Double,
    naslov: String = stringResource(R.string.kom_kako_smo_izracunali),
    modifier: Modifier = Modifier
) {
    val shema = MaterialTheme.colorScheme
    val tip = MaterialTheme.typography
    val bmr = formatKcalTisuce(izracun.bmr)
    val tdee = formatKcalTisuce(izracun.tdee)
    val cilj = formatKcalTisuce(izracun.ciljKalorije.toDouble())

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = naslov,
            style = tip.titleMedium,
            color = shema.onSurface,
            modifier = Modifier.semantics { heading() }
        )

        Column(modifier = Modifier.padding(top = MacroRazmaci.s)) {
            Korak(
                naziv = stringResource(R.string.kom_izracun_bmr),
                racun = stringResource(R.string.kom_izracun_bmr_racun, izracun.bmrFormula, bmr)
            )
            Korak(
                naziv = stringResource(R.string.kom_izracun_potrosnja),
                racun = stringResource(R.string.kom_izracun_potrosnja_racun, bmr, formatKolicina3(izracun.faktorAktivnosti), tdee)
            )
            Korak(
                naziv = stringResource(R.string.kom_izracun_prilagodba),
                racun = when {
                    izracun.korekcijaCilja < 0 ->
                        stringResource(R.string.kom_izracun_minus, tdee, formatKcalTisuce(-izracun.korekcijaCilja), cilj)
                    izracun.korekcijaCilja > 0 ->
                        stringResource(R.string.kom_izracun_plus, tdee, formatKcalTisuce(izracun.korekcijaCilja), cilj)
                    else -> stringResource(R.string.kom_izracun_bez_korekcije, cilj)
                }
            )
            Korak(
                naziv = stringResource(R.string.kom_izracun_proteini),
                racun = stringResource(R.string.kom_izracun_proteini_racun, formatMakro(masa), izracun.proteini)
            )
            Korak(
                naziv = stringResource(R.string.kom_izracun_masti),
                racun = stringResource(R.string.kom_izracun_masti_racun, cilj, izracun.masti)
            )
            Korak(
                naziv = stringResource(R.string.kom_izracun_uh),
                racun = stringResource(R.string.kom_izracun_uh_racun, cilj, izracun.proteini, izracun.masti, izracun.ugljikohidrati),
                zadnji = true
            )
        }

        Text(
            text = stringResource(R.string.kom_izracun_cilj, cilj),
            style = tip.titleMedium,
            color = shema.onSurface,
            modifier = Modifier.padding(top = MacroRazmaci.l)
        )
        Text(
            text = makroKratko(
                Makro(
                    kcal = izracun.ciljKalorije.toDouble(),
                    proteini = izracun.proteini.toDouble(),
                    masti = izracun.masti.toDouble(),
                    ugljikohidrati = izracun.ugljikohidrati.toDouble()
                )
            ),
            style = tip.bodyMedium,
            color = shema.onSurface
        )
        Text(
            text = stringResource(R.string.kom_izracun_zaokruzeno),
            style = tip.bodySmall,
            color = shema.onSurfaceVariant,
            modifier = Modifier.padding(top = MacroRazmaci.xs)
        )
    }
}

/** Korak računa: naziv u bodyMedium, račun u bodySmall (Hanken, tablične znamenke). */
@Composable
private fun Korak(naziv: String, racun: String, zadnji: Boolean = false) {
    val shema = MaterialTheme.colorScheme
    Column(modifier = Modifier.padding(vertical = MacroRazmaci.s)) {
        Text(text = naziv, style = MaterialTheme.typography.bodyMedium, color = shema.onSurface)
        Text(text = racun, style = MaterialTheme.typography.bodySmall, color = shema.onSurfaceVariant)
    }
    if (!zadnji) {
        HorizontalDivider(thickness = MacroDimenzije.linija, color = shema.outlineVariant)
    }
}

/** Faktor aktivnosti s do tri decimale, npr. 1.375 -> "1,375", 1.55 -> "1,55". */
private fun formatKolicina3(vrijednost: Double): String =
    String.format(java.util.Locale.forLanguageTag("hr-HR"), "%.3f", vrijednost).trimEnd('0').trimEnd(',')
