package com.niko.macromenza.screens.dnevnipregled

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import com.niko.macromenza.R
import com.niko.macromenza.ui.components.TablicniBroj
import com.niko.macromenza.ui.components.skalaPritiska
import com.niko.macromenza.ui.theme.MacroDimenzije
import com.niko.macromenza.ui.theme.MacroOblici
import com.niko.macromenza.ui.theme.MacroOmjeri
import com.niko.macromenza.ui.theme.MacroRazmaci
import com.niko.macromenza.ui.theme.Opruge
import com.niko.macromenza.ui.theme.Trajanje
import com.niko.macromenza.ui.theme.macroBoje
import com.niko.macromenza.ui.theme.macroTipografija
import com.niko.macromenza.ui.theme.opruga
import com.niko.macromenza.ui.theme.ulazniTween
import com.niko.macromenza.util.formatKcal
import kotlin.math.roundToInt

/**
 * Hero: luk od 260° s otvorom dolje. Luk, "Uneseno" i "Ostaje" čitaju istu animiranu vrijednost pojedenih kcal,
 * pa završavaju zajedno; "Ostaje" zato broji unatrag od cilja. Iznad cilja preko ispune se crta segment višnje
 * (s rubom u boji pozadine), a natpis postaje "Preko cilja".
 */
@Composable
fun HeroKalorija(
    pojedeno: Double,
    cilj: Double?,
    popunjeniUtori: Int,
    ukupnoUtora: Int,
    animirajUlaz: Boolean,
    smanjeno: Boolean,
    onPostaviCilj: () -> Unit,
    modifier: Modifier = Modifier
) {
    val boje = MaterialTheme.macroBoje
    val kcal = remember { Animatable(if (animirajUlaz) 0f else pojedeno.toFloat()) }
    var prviPut by remember { mutableStateOf(true) }

    LaunchedEffect(pojedeno) {
        val spec = if (prviPut && animirajUlaz) ulazniTween<Float>(Trajanje.ulaz, smanjeno = smanjeno)
        else opruga(Opruge.blaga(), smanjeno)
        prviPut = false
        kcal.animateTo(pojedeno.toFloat(), spec)
    }

    val opis = when {
        cilj == null -> stringResource(R.string.dp_luk_opis_bez_cilja, formatKcal(pojedeno))
        pojedeno > cilj -> stringResource(R.string.dp_luk_opis_preko, formatKcal(pojedeno - cilj), formatKcal(pojedeno), formatKcal(cilj))
        else -> stringResource(R.string.dp_luk_opis, formatKcal(cilj - pojedeno), formatKcal(cilj), formatKcal(pojedeno))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(boje.heroPozadina, MacroOblici.pladanj)
            .border(MacroDimenzije.linija, boje.heroRub, MacroOblici.pladanj)
            .padding(MacroDimenzije.heroUnutarnji)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val mjerac = rememberTextMeasurer()
            val gustoca = LocalDensity.current
            val stilStrane = MaterialTheme.macroTipografija.brojSrednji
            val potrebnaStrana = remember(stilStrane, gustoca) {
                with(gustoca) { mjerac.measure("0000", stilStrane).size.width.toDp() } + MacroRazmaci.s
            }
            val dostupnaStrana = (maxWidth - MacroDimenzije.lukSirina) / 2
            val uneseno: @Composable () -> Unit = {
                StatStrana(
                    oznaka = stringResource(R.string.dp_uneseno),
                    vrijednost = { kcal.value.roundToInt().toString() }
                )
            }
            val obroka: @Composable () -> Unit = {
                val tekst = stringResource(R.string.dp_obroka_vrijednost, popunjeniUtori, ukupnoUtora)
                StatStrana(oznaka = stringResource(R.string.dp_obroka), vrijednost = { tekst })
            }
            val luk: @Composable () -> Unit = {
                Luk(kcal = kcal, cilj = cilj, onPostaviCilj = onPostaviCilj)
            }

            if (dostupnaStrana >= potrebnaStrana) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clearAndSetSemantics { contentDescription = opis },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { uneseno() }
                    luk()
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { obroka() }
                }
            } else {
                // Uski ekran ili veliki font: statistike idu ispod luka.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clearAndSetSemantics { contentDescription = opis },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    luk()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        uneseno()
                        obroka()
                    }
                }
            }
        }
    }
}

@Composable
private fun Luk(
    kcal: Animatable<Float, *>,
    cilj: Double?,
    onPostaviCilj: () -> Unit
) {
    val boje = MaterialTheme.macroBoje
    val ciljF = cilj?.toFloat()
    val prekoCilja by remember(ciljF) { derivedStateOf { ciljF != null && kcal.value > ciljF } }

    Box(modifier = Modifier.size(MacroDimenzije.lukSirina, MacroDimenzije.lukVisina)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val debljina = MacroDimenzije.lukDebljina.toPx()
            val promjer = size.width - debljina
            val pocetak = Offset(debljina / 2, debljina / 2)
            val okvir = Size(promjer, promjer)
            val stil = Stroke(width = debljina, cap = StrokeCap.Round)

            drawArc(boje.lukStaza, MacroOmjeri.lukPocetak, MacroOmjeri.lukRaspon, false, pocetak, okvir, style = stil)

            if (ciljF != null && ciljF > 0f) {
                val vrijednost = kcal.value
                val udio = (vrijednost / ciljF).coerceIn(0f, 1f)
                if (udio > 0f) {
                    drawArc(boje.lukIspuna, MacroOmjeri.lukPocetak, MacroOmjeri.lukRaspon * udio, false, pocetak, okvir, style = stil)
                }
                val visak = ((vrijednost - ciljF) / ciljF).coerceIn(0f, 1f)
                if (visak > 0f) {
                    val rub = Stroke(width = debljina + 2 * MacroDimenzije.prekoracenjeRub.toPx(), cap = StrokeCap.Round)
                    drawArc(boje.heroPozadina, MacroOmjeri.lukPocetak, MacroOmjeri.lukRaspon * visak, false, pocetak, okvir, style = rub)
                    drawArc(boje.visnja, MacroOmjeri.lukPocetak, MacroOmjeri.lukRaspon * visak, false, pocetak, okvir, style = stil)
                }
            }
        }

        Column(
            modifier = Modifier
                .align(BiasAlignment(0f, MacroOmjeri.lukSredisteBias))
                .width(MacroDimenzije.lukUnutarnjiPromjer),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val tip = MaterialTheme.typography
            if (ciljF == null) {
                Text(stringResource(R.string.dp_uneseno), style = tip.labelMedium, color = boje.naHerojuPrigusen)
                TablicniBroj(
                    tekst = { kcal.value.roundToInt().toString() },
                    stil = MaterialTheme.macroTipografija.brojLuka,
                    boja = boje.naHeroju,
                    maxSirina = MacroDimenzije.lukUnutarnjiPromjer
                )
                val izvor = remember { MutableInteractionSource() }
                TextButton(
                    onClick = onPostaviCilj,
                    interactionSource = izvor,
                    modifier = Modifier
                        .heightIn(min = MacroDimenzije.dodirniCilj)
                        .skalaPritiska(izvor)
                ) {
                    Text(stringResource(R.string.dp_postavi_cilj), style = tip.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (prekoCilja) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowUpward,
                            contentDescription = null,
                            tint = boje.visnja,
                            modifier = Modifier.size(MacroDimenzije.ikonaSitna)
                        )
                        Spacer(Modifier.width(MacroRazmaci.xxs))
                    }
                    Text(
                        text = stringResource(if (prekoCilja) R.string.dp_preko_cilja else R.string.dp_ostaje),
                        style = tip.labelMedium,
                        color = if (prekoCilja) boje.visnja else boje.naHerojuPrigusen
                    )
                }
                TablicniBroj(
                    tekst = {
                        val v = kcal.value
                        (if (v > ciljF) v - ciljF else ciljF - v).roundToInt().toString()
                    },
                    stil = MaterialTheme.macroTipografija.brojLuka,
                    boja = boje.naHeroju,
                    maxSirina = MacroDimenzije.lukUnutarnjiPromjer
                )
                Text(
                    text = stringResource(R.string.dp_od_kcal, formatKcal(cilj)),
                    style = tip.labelMedium,
                    color = boje.naHerojuPrigusen,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun StatStrana(oznaka: String, vrijednost: () -> String) {
    val boje = MaterialTheme.macroBoje
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = MacroRazmaci.s)
    ) {
        Text(oznaka, style = MaterialTheme.typography.labelMedium, color = boje.naHerojuPrigusen, maxLines = 1)
        TablicniBroj(tekst = vrijednost, stil = MaterialTheme.macroTipografija.brojSrednji, boja = boje.naHeroju)
    }
}
