package com.niko.macromenza.screens.povijest

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.niko.macromenza.R
import com.niko.macromenza.ui.components.MakroTrake
import com.niko.macromenza.ui.components.TablicniBroj
import com.niko.macromenza.ui.theme.MacroRazmaci
import com.niko.macromenza.ui.theme.Opruge
import com.niko.macromenza.ui.theme.Trajanje
import com.niko.macromenza.ui.theme.macroBoje
import com.niko.macromenza.ui.theme.macroTipografija
import com.niko.macromenza.ui.theme.opruga
import com.niko.macromenza.ui.theme.ulazniTween
import com.niko.macromenza.util.formatKcal
import kotlin.math.roundToInt

/** Prosjek iznad ovoga (dio prosječnog cilja) smatra se prekoračenjem; isti prag kao "u cilju" gore (±10 %). */
private const val PREKO_CILJA = 1.1

/**
 * Sažetak tjedna (pages/povijest.md, "Sažetak tjedna"): dani u cilju, prosjek prema prosječnom cilju i trake P/M/U.
 * Bez kartice. [kasnjenjeUlaza] je trenutak u ulaznom slijedu ekrana kad brojevi kreću brojati od nule.
 */
@Composable
fun SazetakTjedna(
    tjedan: TjedanUi,
    animirajUlaz: Boolean,
    kasnjenjeUlaza: Int,
    smanjeno: Boolean,
    modifier: Modifier = Modifier
) {
    val prosjek = tjedan.prosjek ?: return
    val cilj = tjedan.prosjekCilj
    val shema = MaterialTheme.colorScheme
    val boje = MaterialTheme.macroBoje
    val tip = MaterialTheme.typography

    val prekoCilja = cilj != null && cilj.kcal > 0 && prosjek.kcal > cilj.kcal * PREKO_CILJA

    val daniUCilju by animiranaVrijednost(tjedan.daniUCilju.toFloat(), animirajUlaz, kasnjenjeUlaza, smanjeno)
    val kcal by animiranaVrijednost(prosjek.kcal.toFloat(), animirajUlaz, kasnjenjeUlaza, smanjeno)

    val opisDani = stringResource(R.string.pov_sazetak_dani_opis, tjedan.daniUCilju, tjedan.daniSUnosom)
    val prosjekTekst = formatKcal(prosjek.kcal)
    val opisKcal = when {
        cilj == null -> stringResource(R.string.pov_sazetak_kcal_opis_bez_cilja, prosjekTekst)
        prekoCilja -> stringResource(R.string.pov_sazetak_kcal_opis_preko, prosjekTekst, formatKcal(cilj.kcal))
        else -> stringResource(R.string.pov_sazetak_kcal_opis, prosjekTekst, formatKcal(cilj.kcal))
    }
    val oznakaKcal = when {
        cilj == null -> stringResource(R.string.pov_kcal_dnevno)
        prekoCilja -> stringResource(R.string.pov_kcal_dnevno_preko)
        else -> stringResource(R.string.pov_kcal_dnevno_cilj, formatKcal(cilj.kcal))
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.pov_sazetak_naslov),
            style = tip.headlineSmall,
            color = shema.onBackground,
            modifier = Modifier
                .padding(top = MacroRazmaci.iznadSekcije, bottom = MacroRazmaci.ispodNaslovaSekcije)
                .semantics { heading() }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MacroRazmaci.l)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clearAndSetSemantics { contentDescription = opisDani }
            ) {
                TablicniBroj(
                    tekst = { "${daniUCilju.roundToInt()} / ${tjedan.daniSUnosom}" },
                    stil = MaterialTheme.macroTipografija.brojSrednji,
                    boja = shema.onSurface,
                    maxZnamenki = 2
                )
                Text(
                    text = pluralStringResource(R.plurals.pov_dana_u_cilju, tjedan.daniSUnosom),
                    style = tip.labelMedium,
                    color = shema.onSurfaceVariant
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clearAndSetSemantics { contentDescription = opisKcal }
            ) {
                TablicniBroj(
                    tekst = { formatKcal(kcal.toDouble()) },
                    stil = MaterialTheme.macroTipografija.brojSrednji,
                    boja = if (prekoCilja) boje.visnja else shema.onSurface
                )
                Text(
                    text = oznakaKcal,
                    style = tip.labelMedium,
                    color = if (prekoCilja) boje.visnja else shema.onSurfaceVariant
                )
            }
        }

        MakroTrake(
            pojedeno = prosjek,
            cilj = cilj,
            animirajUlaz = animirajUlaz,
            smanjeno = smanjeno,
            modifier = Modifier.padding(top = MacroRazmaci.l)
        )

        Text(
            text = stringResource(if (cilj != null) R.string.pov_napomena_cilja else R.string.pov_nema_cilja),
            style = tip.bodySmall,
            color = shema.onSurfaceVariant,
            modifier = Modifier.padding(top = MacroRazmaci.l)
        )
    }
}

/**
 * Prvi ulaz u sesiji: broji od nule uz [Trajanje.ulaz] s usporavanjem. Svaka sljedeća promjena (npr. drugi tjedan)
 * prelazi od trenutne vrijednosti uz [Opruge.blaga], ne od nule. Smanjeno: odmah završna vrijednost.
 */
@Composable
private fun animiranaVrijednost(cilj: Float, animirajUlaz: Boolean, kasnjenje: Int, smanjeno: Boolean): State<Float> {
    val vrijednost = remember { Animatable(if (animirajUlaz) 0f else cilj) }
    var prviPut by remember { mutableStateOf(true) }
    LaunchedEffect(cilj) {
        val spec = if (prviPut && animirajUlaz) ulazniTween<Float>(Trajanje.ulaz, kasnjenje, smanjeno)
        else opruga(Opruge.blaga(), smanjeno)
        prviPut = false
        vrijednost.animateTo(cilj, spec)
    }
    return vrijednost.asState()
}
