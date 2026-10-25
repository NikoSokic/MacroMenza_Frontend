package com.niko.macromenza.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.niko.macromenza.model.MjerenjeMase
import com.niko.macromenza.model.Preporuka
import com.niko.macromenza.ui.theme.MacroGreen
import com.niko.macromenza.util.formatKcalTisuce
import com.niko.macromenza.util.formatMasa
import com.niko.macromenza.util.formatPromjenaMase
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs

private val ZAGREB = ZoneId.of("Europe/Zagreb")
private val FORMAT_DATUMA = DateTimeFormatter.ofPattern("d. M. yyyy.")

/** Margina osi Y iznad najveće i ispod najmanje mase (kg). */
private const val MARGINA_Y = 0.5

private enum class RazdobljeGrafa(val oznaka: String, val dana: Long?, val uRazdoblju: String) {
    DANA_30("30 dana", 30, "u 30 dana"),
    DANA_90("90 dana", 90, "u 90 dana"),
    GODINA("godina", 365, "u godinu dana"),
    SVE("sve", null, "")
}

/** Graf mase s odabirom razdoblja, promjenom mase u razdoblju i ciljem kcal ako se promijenio. */
@Composable
fun GrafMaseKartica(
    mase: List<MjerenjeMase>,
    preporuke: List<Preporuka>
) {
    var razdoblje by rememberSaveable { mutableStateOf(RazdobljeGrafa.DANA_30) }

    val danas = LocalDate.now(ZAGREB)
    val sveTocke = remember(mase) { mase.map { LocalDate.parse(it.datum) to it.masa } }

    val pocetak: LocalDate? = razdoblje.dana?.let { danas.minusDays(it - 1) }
    val tocke = if (pocetak == null) sveTocke else sveTocke.filter { !it.first.isBefore(pocetak) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {

            Text(
                text = "Masa kroz vrijeme",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RazdobljeGrafa.entries.forEach { r ->
                    FilterChip(
                        selected = razdoblje == r,
                        onClick = { razdoblje = r },
                        label = { Text(r.oznaka) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                            selectedLabelColor = MacroGreen
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (tocke.size < 2) {
                Text(
                    text = "Za graf trebaju barem dva mjerenja u odabranom razdoblju.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                GrafMase(
                    tocke = tocke,
                    odDatuma = pocetak ?: tocke.first().first,
                    doDatuma = maxOf(danas, tocke.last().first),
                    kljuc = razdoblje
                )

                Spacer(modifier = Modifier.height(14.dp))

                val promjena = tocke.last().second - tocke.first().second
                val opisRazdoblja = razdoblje.uRazdoblju.ifEmpty {
                    "u ${ChronoUnit.DAYS.between(tocke.first().first, tocke.last().first)} dana"
                }

                Text(
                    text = "${formatPromjenaMase(promjena)} $opisRazdoblja",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            PromjenaCilja(preporuke = preporuke, pocetak = pocetak)
        }
    }
}

@Composable
private fun GrafMase(
    tocke: List<Pair<LocalDate, Double>>,
    odDatuma: LocalDate,
    doDatuma: LocalDate,
    kljuc: RazdobljeGrafa
) {
    var odabrana by remember(kljuc, tocke) { mutableStateOf<Int?>(null) }

    val bojaLinije = MacroGreen
    val bojaMreze = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
    val bojaPodloge = MaterialTheme.colorScheme.surfaceVariant
    val bojaTocke = MaterialTheme.colorScheme.surface

    val textMeasurer = rememberTextMeasurer()
    val stilOsi = MaterialTheme.typography.labelSmall.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    val stilOznake = MaterialTheme.typography.labelMedium.copy(
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold
    )

    val minMasa = tocke.minOf { it.second } - MARGINA_Y
    val maxMasa = tocke.maxOf { it.second } + MARGINA_Y
    val ukupnoDana = ChronoUnit.DAYS.between(odDatuma, doDatuma).coerceAtLeast(1).toFloat()

    // Lijevi prostor za oznake osi Y; računa se jednom po rasponu.
    val sirinaOsi = remember(minMasa, maxMasa) {
        textMeasurer.measure(formatMasa(maxMasa).removeSuffix(" kg"), stilOsi).size.width.toFloat()
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .pointerInput(tocke, odDatuma, doDatuma) {
                detectTapGestures { pozicija ->
                    val lijevo = sirinaOsi + 8.dp.toPx()
                    val desno = size.width - 8.dp.toPx()
                    val najbliza = tocke.indices.minByOrNull { i ->
                        val dani = ChronoUnit.DAYS.between(odDatuma, tocke[i].first).toFloat()
                        abs(lijevo + dani / ukupnoDana * (desno - lijevo) - pozicija.x)
                    }
                    odabrana = if (najbliza == odabrana) null else najbliza
                }
            }
    ) {
        val lijevo = sirinaOsi + 8.dp.toPx()
        val desno = size.width - 8.dp.toPx()
        val vrh = 28.dp.toPx()
        val dno = size.height - 6.dp.toPx()

        fun x(datum: LocalDate): Float =
            lijevo + ChronoUnit.DAYS.between(odDatuma, datum).toFloat() / ukupnoDana * (desno - lijevo)

        fun y(masa: Double): Float =
            dno - ((masa - minMasa) / (maxMasa - minMasa)).toFloat() * (dno - vrh)

        // Tri vodoravne linije mreže s oznakama mase (dno, sredina, vrh raspona).
        listOf(minMasa, (minMasa + maxMasa) / 2, maxMasa).forEach { vrijednost ->
            val yLinije = y(vrijednost)
            drawLine(
                color = bojaMreze,
                start = Offset(lijevo, yLinije),
                end = Offset(desno, yLinije),
                strokeWidth = 1.dp.toPx()
            )
            val oznaka = textMeasurer.measure(formatMasa(vrijednost).removeSuffix(" kg"), stilOsi)
            drawText(oznaka, topLeft = Offset(0f, yLinije - oznaka.size.height / 2f))
        }

        val putanja = Path()
        tocke.forEachIndexed { i, (datum, masa) ->
            if (i == 0) putanja.moveTo(x(datum), y(masa)) else putanja.lineTo(x(datum), y(masa))
        }
        drawPath(
            path = putanja,
            color = bojaLinije,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        tocke.forEachIndexed { i, (datum, masa) ->
            val sredina = Offset(x(datum), y(masa))
            val radijus = if (i == odabrana) 6.dp.toPx() else 4.dp.toPx()
            drawCircle(color = bojaLinije, radius = radijus, center = sredina)
            drawCircle(color = bojaTocke, radius = radijus - 2.dp.toPx(), center = sredina)
        }

        // Oznaka odabrane točke crta se zadnja, s podlogom, iznad točke.
        odabrana?.let { i ->
            val (datum, masa) = tocke.getOrNull(i) ?: return@let
            val izmjereno = textMeasurer.measure("${datum.format(FORMAT_DATUMA)} · ${formatMasa(masa)}", stilOznake)
            val ruba = 6.dp.toPx()
            val sirina = izmjereno.size.width + 2 * ruba
            val visina = izmjereno.size.height + ruba
            val xOznake = (x(datum) - sirina / 2f).coerceIn(0f, size.width - sirina)
            val yOznake = (y(masa) - visina - 10.dp.toPx()).let { if (it < 0f) y(masa) + 10.dp.toPx() else it }

            drawRoundRect(
                color = bojaPodloge,
                topLeft = Offset(xOznake, yOznake),
                size = Size(sirina, visina),
                cornerRadius = CornerRadius(8.dp.toPx())
            )
            drawText(izmjereno, topLeft = Offset(xOznake + ruba, yOznake + ruba / 2f))
        }
    }

    Spacer(modifier = Modifier.height(6.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = odDatuma.format(FORMAT_DATUMA),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = doDatuma.format(FORMAT_DATUMA),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Trenutni cilj kcal, samo ako se razlikuje od cilja na početku razdoblja ([pocetak] null = prvi cilj ikad). */
@Composable
private fun PromjenaCilja(
    preporuke: List<Preporuka>,
    pocetak: LocalDate?
) {
    val trenutna = preporuke.maxByOrNull { it.id ?: 0 } ?: return

    val prije = if (pocetak == null) {
        preporuke.minByOrNull { it.id ?: 0 }
    } else {
        preporuke
            .filter { p -> runCatching { LocalDate.parse(p.datum).isBefore(pocetak) }.getOrDefault(false) }
            .maxByOrNull { it.id ?: 0 }
    } ?: return

    if (Math.round(prije.kalorije) == Math.round(trenutna.kalorije)) return

    Spacer(modifier = Modifier.height(6.dp))

    Text(
        text = "Cilj: ${formatKcalTisuce(trenutna.kalorije)} kcal (prije ${formatKcalTisuce(prije.kalorije)} kcal)",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
