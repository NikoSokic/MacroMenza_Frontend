package com.niko.macromenza.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit

private const val ZNAMENKE = "0123456789"

/**
 * Broj kojemu svaka znamenka stoji u ćeliji širine najšire znamenke, pa se tijekom brojanja ne pomiče
 * (Fraunces nema značajku "tnum"). [tekst] je lambda: brojanje rekomponira samo ovu komponentu.
 * Ako [maxZnamenki] ćelija ne stane u [maxSirina], font se proporcionalno smanji (npr. fontScale 1,3).
 */
@Composable
fun TablicniBroj(
    tekst: () -> String,
    stil: TextStyle,
    boja: Color,
    modifier: Modifier = Modifier,
    maxZnamenki: Int = 4,
    maxSirina: Dp = Dp.Unspecified
) {
    val mjerac = rememberTextMeasurer()
    val gustoca = LocalDensity.current

    val (stilBroja, sirinaCelije) = remember(stil, maxZnamenki, maxSirina, gustoca) {
        fun najsira(s: TextStyle) = ZNAMENKE.maxOf { mjerac.measure(it.toString(), s).size.width }

        var s = stil
        var celija = najsira(s)
        if (maxSirina != Dp.Unspecified) {
            val dostupno = with(gustoca) { maxSirina.toPx() }
            val potrebno = celija * maxZnamenki
            if (potrebno > dostupno) {
                val omjer = dostupno / potrebno
                s = stil.copy(
                    fontSize = stil.fontSize * omjer,
                    lineHeight = if (stil.lineHeight == TextUnit.Unspecified) stil.lineHeight else stil.lineHeight * omjer
                )
                celija = najsira(s)
            }
        }
        s to with(gustoca) { celija.toDp() }
    }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        tekst().forEach { znak ->
            if (znak.isDigit()) {
                Box(modifier = Modifier.width(sirinaCelije), contentAlignment = Alignment.Center) {
                    Text(text = znak.toString(), style = stilBroja, color = boja, maxLines = 1, softWrap = false)
                }
            } else {
                Text(text = znak.toString(), style = stilBroja, color = boja, maxLines = 1, softWrap = false)
            }
        }
    }
}
