package com.niko.macromenza.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niko.macromenza.model.DanPregled
import com.niko.macromenza.ui.theme.MacroDark
import com.niko.macromenza.ui.theme.MacroGreen
import com.niko.macromenza.ui.theme.MacroLightGreen
import com.niko.macromenza.util.formatKcalTisuce

private val TIPOVI_OBROKA = listOf("DORUCAK", "RUCAK", "UZINA", "VECERA")

// Četiri nijanse zelene iz palete aplikacije (MacroGreen, MacroLightGreen + dvije izvedene)
private val BojaDorucak = lerp(MacroLightGreen, Color.White, 0.40f)
private val BojaRucak = MacroGreen
private val BojaUzina = MacroLightGreen
private val BojaVecera = lerp(MacroGreen, MacroDark, 0.40f)

private fun bojaTipa(tip: String): Color = when (tip) {
    "DORUCAK" -> BojaDorucak
    "RUCAK" -> BojaRucak
    "UZINA" -> BojaUzina
    else -> BojaVecera
}

private fun nazivTipa(tip: String): String = when (tip) {
    "DORUCAK" -> "Doručak"
    "RUCAK" -> "Ručak"
    "UZINA" -> "Užina"
    "VECERA" -> "Večera"
    else -> tip
}

/** 1 obrok, 2 obroka, 5 obroka, 21 obrok. */
private fun tekstBrojaObroka(n: Int): String =
    if (n % 10 == 1 && n % 100 != 11) "$n obrok" else "$n obroka"

/**
 * Prsten dnevnih kalorija: luk po tipu obroka, proporcionalan udjelu u cilju; preostali dio cilja je prigušen.
 * Bez cilja prsten se puni samo pojedenim i nudi "Postavi cilj".
 */
@Composable
fun PrstenKalorija(
    danPregled: DanPregled,
    onPostaviCilj: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cilj = danPregled.cilj?.kalorije
    val pojedeno = danPregled.pojedeno.kalorije

    // Animira se kalorije svakog tipa; fiksna četiri poziva u istom redoslijedu.
    val kalorijePoTipu = TIPOVI_OBROKA.map { tip ->
        val kcal = danPregled.poTipu.firstOrNull { it.tipObroka == tip }?.kalorije ?: 0.0
        animateFloatAsState(
            targetValue = kcal.toFloat(),
            animationSpec = tween(durationMillis = 800),
            label = "prsten_$tip"
        ).value
    }

    // Preko cilja prsten je pun, a omjeri među obrocima ostaju.
    val nazivnik = maxOf(cilj?.toFloat() ?: 0f, kalorijePoTipu.sum())
    val track = MacroGreen.copy(alpha = 0.14f)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Dnevne kalorije",
                modifier = Modifier.align(Alignment.Start),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier.size(220.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val debljina = 22.dp.toPx()
                    val promjer = Size(size.width - debljina, size.height - debljina)
                    val vrh = Offset(debljina / 2, debljina / 2)

                    drawArc(
                        color = track,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = vrh,
                        size = promjer,
                        style = Stroke(width = debljina)
                    )

                    if (nazivnik <= 0f) return@Canvas

                    val brojLukova = kalorijePoTipu.count { it > 0f }
                    val razmak = if (brojLukova > 1) 3f else 0f
                    var pocetak = -90f

                    TIPOVI_OBROKA.forEachIndexed { i, tip ->
                        val kut = kalorijePoTipu[i] / nazivnik * 360f
                        if (kut <= 0f) return@forEachIndexed

                        drawArc(
                            color = bojaTipa(tip),
                            startAngle = pocetak + razmak / 2,
                            sweepAngle = (kut - razmak).coerceAtLeast(0.5f),
                            useCenter = false,
                            topLeft = vrh,
                            size = promjer,
                            style = Stroke(width = debljina, cap = StrokeCap.Butt)
                        )
                        pocetak += kut
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (cilj != null) {
                        Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold)) {
                                    append(formatKcalTisuce(pojedeno))
                                }
                                withStyle(SpanStyle(fontSize = 13.sp)) {
                                    append(" / ${formatKcalTisuce(cilj)} kcal")
                                }
                            },
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        val razlika = cilj - pojedeno
                        Text(
                            text = if (razlika >= 0) {
                                "ostalo ${formatKcalTisuce(razlika)} kcal"
                            } else {
                                "preko ${formatKcalTisuce(-razlika)} kcal"
                            },
                            color = if (razlika >= 0) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    } else {
                        Text(
                            text = "${formatKcalTisuce(pojedeno)} kcal",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "pojedeno danas",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            TIPOVI_OBROKA.forEach { tip ->
                val red = danPregled.poTipu.firstOrNull { it.tipObroka == tip }

                LegendaRed(
                    boja = bojaTipa(tip),
                    naziv = nazivTipa(tip),
                    kalorije = red?.kalorije ?: 0.0,
                    brojObroka = red?.brojObroka ?: 0
                )
            }

            if (cilj == null) {
                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onPostaviCilj,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MacroGreen)
                ) {
                    Text(
                        text = "Postavi cilj",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendaRed(
    boja: Color,
    naziv: String,
    kalorije: Double,
    brojObroka: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(boja, CircleShape)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = naziv,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )

        Text(
            text = tekstBrojaObroka(brojObroka),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )

        Text(
            text = "${formatKcalTisuce(kalorije)} kcal",
            modifier = Modifier.width(84.dp),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}
