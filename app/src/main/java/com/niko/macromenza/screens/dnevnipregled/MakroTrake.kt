package com.niko.macromenza.screens.dnevnipregled

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.niko.macromenza.R
import com.niko.macromenza.ui.theme.Kasnjenje
import com.niko.macromenza.ui.theme.MacroDimenzije
import com.niko.macromenza.ui.theme.MacroOmjeri
import com.niko.macromenza.ui.theme.MacroRazmaci
import com.niko.macromenza.ui.theme.Opruge
import com.niko.macromenza.ui.theme.Trajanje
import com.niko.macromenza.ui.theme.macroBoje
import com.niko.macromenza.ui.theme.opruga
import com.niko.macromenza.ui.theme.ulazniTween
import com.niko.macromenza.util.formatGrami

/** Tri tanke trake (MacroFactor): ljestvica do 125 % cilja, oznaka cilja na 80 %, višak u višnji. */
@Composable
fun MakroTrake(
    pojedeno: Makro,
    cilj: Makro?,
    animirajUlaz: Boolean,
    smanjeno: Boolean,
    modifier: Modifier = Modifier
) {
    val boje = MaterialTheme.macroBoje
    val redovi = listOf(
        Triple(stringResource(R.string.dp_proteini), pojedeno.proteini to cilj?.proteini, boje.proteini),
        Triple(stringResource(R.string.dp_masti), pojedeno.masti to cilj?.masti, boje.masti),
        Triple(stringResource(R.string.dp_ugljikohidrati), pojedeno.ugljikohidrati to cilj?.ugljikohidrati, boje.ugljikohidrati)
    )

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MacroRazmaci.l)) {
        redovi.forEachIndexed { i, (naziv, vrijednosti, boja) ->
            MakroTraka(
                naziv = naziv,
                vrijednost = vrijednosti.first,
                cilj = vrijednosti.second,
                boja = boja,
                kasnjenje = Kasnjenje.trake + i * Kasnjenje.korak,
                animirajUlaz = animirajUlaz,
                smanjeno = smanjeno
            )
        }
    }
}

@Composable
private fun MakroTraka(
    naziv: String,
    vrijednost: Double,
    cilj: Double?,
    boja: Color,
    kasnjenje: Int,
    animirajUlaz: Boolean,
    smanjeno: Boolean
) {
    val boje = MaterialTheme.macroBoje
    val shema = MaterialTheme.colorScheme
    val tip = MaterialTheme.typography

    // Udio na ljestvici 0..1 (1 = 125 % cilja)
    val ciljniUdio = if (cilj != null && cilj > 0) (vrijednost / (cilj * MacroOmjeri.trakaMaksimum)).toFloat().coerceIn(0f, 1f) else 0f
    val udio = remember { Animatable(if (animirajUlaz) 0f else ciljniUdio) }
    var prviPut by remember { mutableStateOf(true) }
    LaunchedEffect(ciljniUdio) {
        val spec = if (prviPut && animirajUlaz) ulazniTween<Float>(Trajanje.srednje, kasnjenje, smanjeno)
        else opruga(Opruge.blaga(), smanjeno)
        prviPut = false
        udio.animateTo(ciljniUdio, spec)
    }

    val opis = if (cilj != null) stringResource(R.string.dp_makro_opis, naziv, formatGrami(vrijednost), formatGrami(cilj))
    else stringResource(R.string.dp_makro_opis_bez_cilja, naziv, formatGrami(vrijednost))

    Column(modifier = Modifier.clearAndSetSemantics { contentDescription = opis }) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(naziv, style = tip.titleSmall, color = shema.onSurface, modifier = Modifier.weight(1f))
            // "71 / 160 g": pojedena vrijednost istaknuta, ostatak prigušen
            val puni = if (cilj != null) stringResource(R.string.dp_makro_vrijednost, formatGrami(vrijednost), formatGrami(cilj))
            else stringResource(R.string.dp_makro_bez_cilja, formatGrami(vrijednost))
            val istaknuto = formatGrami(vrijednost)
            val vrijednostTekst = buildAnnotatedString {
                withStyle(SpanStyle(color = shema.onSurface, fontWeight = FontWeight.Bold)) { append(istaknuto) }
                withStyle(SpanStyle(color = shema.onSurfaceVariant)) { append(puni.removePrefix(istaknuto)) }
            }
            Text(vrijednostTekst, style = tip.bodySmall)
        }
        if (cilj != null) {
            Spacer(Modifier.height(MacroRazmaci.s))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MacroDimenzije.oznakaCiljaVisina)
            ) {
                val visinaTrake = MacroDimenzije.trakaVisina.toPx()
                val y = (size.height - visinaTrake) / 2
                val polumjer = CornerRadius(visinaTrake / 2)
                val oznakaX = size.width / MacroOmjeri.trakaMaksimum

                drawRoundRect(shema.surfaceVariant, Offset(0f, y), Size(size.width, visinaTrake), polumjer)

                val kraj = size.width * udio.value
                if (kraj > 0f) {
                    drawRoundRect(boja, Offset(0f, y), Size(kraj.coerceAtMost(oznakaX), visinaTrake), polumjer)
                    if (kraj > oznakaX) {
                        drawRoundRect(boje.visnja, Offset(oznakaX, y), Size(kraj - oznakaX, visinaTrake), polumjer)
                    }
                }

                val sirinaOznake = MacroDimenzije.oznakaCiljaSirina.toPx()
                drawRoundRect(
                    color = shema.onSurface.copy(alpha = 0.6f),
                    topLeft = Offset(oznakaX - sirinaOznake / 2, 0f),
                    size = Size(sirinaOznake, size.height),
                    cornerRadius = CornerRadius(sirinaOznake / 2)
                )
            }
        }
    }
}
