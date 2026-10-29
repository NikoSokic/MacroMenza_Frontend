package com.niko.macromenza.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.niko.macromenza.R
import com.niko.macromenza.ui.theme.Kasnjenje
import com.niko.macromenza.ui.theme.MacroDimenzije
import com.niko.macromenza.ui.theme.MacroOmjeri
import com.niko.macromenza.ui.theme.Trajanje
import com.niko.macromenza.ui.theme.macroBoje
import com.niko.macromenza.ui.theme.ulazniTween

/** Udjeli segmenata luka: proteini 30 %, masti 30 %, ugljikohidrati 40 % (kao ikona aplikacije). */
private val UDJELI_SEGMENATA = floatArrayOf(0.3f, 0.3f, 0.4f)

/**
 * Logo za Welcome: „M" u Frauncesu na porculanskom krugu, okružen otvorenim lukom u bojama
 * makronutrijenata (P / M / UH). Ukras, pa je skriven čitaču zaslona. Pri prvom ulazu se luk nacrta.
 */
@Composable
fun MonogramMakro(animirajUlaz: Boolean, smanjeno: Boolean, modifier: Modifier = Modifier) {
    val shema = MaterialTheme.colorScheme
    val boje = MaterialTheme.macroBoje
    val napredak = remember { Animatable(if (animirajUlaz && !smanjeno) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (napredak.value < 1f) {
            napredak.animateTo(1f, ulazniTween(Trajanje.ulaz, Kasnjenje.nakonPrijelaza, smanjeno))
        }
    }
    val bojeSegmenata = listOf(boje.proteini, boje.masti, boje.ugljikohidrati)

    Box(
        modifier = modifier
            .size(MacroDimenzije.monogram)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(MacroDimenzije.monogram)) {
            val debljina = MacroDimenzije.monogramLuk.toPx()
            val stil = Stroke(width = debljina, cap = StrokeCap.Round)
            val okvir = Size(size.width - debljina, size.height - debljina)
            val pocetak = Offset(debljina / 2f, debljina / 2f)
            val razmak = MacroOmjeri.lukRaspon * MacroOmjeri.razmakSegmenata
            val raspolozivo = MacroOmjeri.lukRaspon - razmak * (UDJELI_SEGMENATA.size - 1)
            val ukupnoNacrtano = MacroOmjeri.lukRaspon * napredak.value

            var kut = MacroOmjeri.lukPocetak
            var pomaknuto = 0f
            UDJELI_SEGMENATA.forEachIndexed { i, udio ->
                val raspon = raspolozivo * udio
                val vidljivo = (ukupnoNacrtano - pomaknuto).coerceIn(0f, raspon)
                if (vidljivo > 0f) {
                    drawArc(bojeSegmenata[i], kut, vidljivo, false, pocetak, okvir, style = stil)
                }
                kut += raspon + razmak
                pomaknuto += raspon + razmak
            }
        }
        Box(
            modifier = Modifier
                .size(MacroDimenzije.monogramKrug)
                .background(shema.surface, CircleShape)
                .border(MacroDimenzije.linija, shema.outlineVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.kom_znak_aplikacije),
                style = MaterialTheme.typography.displayMedium,
                color = shema.onSurface
            )
        }
    }
}
