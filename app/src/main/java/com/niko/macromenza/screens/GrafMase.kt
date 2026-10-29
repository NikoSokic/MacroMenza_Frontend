package com.niko.macromenza.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.util.lerp
import com.niko.macromenza.R
import com.niko.macromenza.model.MjerenjeMase
import com.niko.macromenza.model.Preporuka
import com.niko.macromenza.ui.components.MacroCip
import com.niko.macromenza.ui.theme.Kasnjenje
import com.niko.macromenza.ui.theme.MacroDimenzije
import com.niko.macromenza.ui.theme.MacroOblici
import com.niko.macromenza.ui.theme.MacroRazmaci
import com.niko.macromenza.ui.theme.Opruge
import com.niko.macromenza.ui.theme.Trajanje
import com.niko.macromenza.ui.theme.macroBoje
import com.niko.macromenza.ui.theme.opruga
import com.niko.macromenza.ui.theme.rememberSmanjeniPokret
import com.niko.macromenza.ui.theme.ulazniTween
import com.niko.macromenza.util.formatKcal
import com.niko.macromenza.util.formatMasa
import com.niko.macromenza.util.formatPromjenaMase
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.hypot

private val ZAGREB = ZoneId.of("Europe/Zagreb")
private val FORMAT_KRATKO = DateTimeFormatter.ofPattern("d. M.")
private val FORMAT_PUNO = DateTimeFormatter.ofPattern("d. M. yyyy.")

/** Margina osi Y iznad najveće i ispod najmanje mase (kg). */
private const val MARGINA_Y = 0.5

/** Ispod ovog raspona mase (kg) oznake osi Y imaju decimalu. */
private const val RASPON_ZA_DECIMALU = 2.0

private enum class RazdobljeGrafa(val oznaka: Int, val dana: Long?, val uRazdoblju: Int?) {
    DANA_30(R.string.mj_razdoblje_30, 30, R.string.mj_u_30_dana),
    DANA_90(R.string.mj_razdoblje_90, 90, R.string.mj_u_90_dana),
    GODINA(R.string.mj_razdoblje_godina, 365, R.string.mj_u_godinu),
    SVE(R.string.mj_razdoblje_sve, null, null)
}

/** "78,4" (masa bez jedinice, decimalni zarez). */
internal fun masaBezJedinice(masa: Double): String = formatMasa(masa).removeSuffix(" kg")

/** "27. 10." u tekućoj godini, inače "27. 10. 2025." */
internal fun kratkiDatum(datum: LocalDate): String =
    datum.format(if (datum.year == LocalDate.now(ZAGREB).year) FORMAT_KRATKO else FORMAT_PUNO)

/** Isprekidani obris (outlineVariant) za prazna stanja: prazno mjerenje i graf bez dovoljno točaka. */
@Composable
internal fun Modifier.isprekidaniObris(): Modifier {
    val boja = MaterialTheme.colorScheme.outlineVariant
    return drawBehind {
        val debljina = MacroDimenzije.obrubIsprekidan.toPx()
        val crtica = MacroDimenzije.crtica.toPx()
        val polumjer = MacroRazmaci.l.toPx()
        drawRoundRect(
            color = boja,
            topLeft = Offset(debljina / 2, debljina / 2),
            size = Size(size.width - debljina, size.height - debljina),
            cornerRadius = CornerRadius(polumjer),
            style = Stroke(debljina, pathEffect = PathEffect.dashPathEffect(floatArrayOf(crtica, crtica)))
        )
    }
}

/**
 * Graf mase s odabirom razdoblja (čipovi), promjenom mase u razdoblju i ciljem kcal ako se promijenio.
 * [animirajUlaz]: prvi ulaz u sesiji, linija se iscrta s lijeva nadesno.
 */
@Composable
fun GrafMaseKartica(
    mase: List<MjerenjeMase>,
    preporuke: List<Preporuka>,
    modifier: Modifier = Modifier,
    animirajUlaz: Boolean = false
) {
    var razdoblje by rememberSaveable { mutableStateOf(RazdobljeGrafa.DANA_30) }

    val danas = LocalDate.now(ZAGREB)
    val sveTocke = remember(mase) { mase.map { LocalDate.parse(it.datum) to it.masa } }

    val pocetak: LocalDate? = razdoblje.dana?.let { danas.minusDays(it - 1) }
    val tocke = remember(sveTocke, pocetak) {
        if (pocetak == null) sveTocke else sveTocke.filter { !it.first.isBefore(pocetak) }
    }

    val shema = MaterialTheme.colorScheme
    val tip = MaterialTheme.typography

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(MacroRazmaci.s)
        ) {
            RazdobljeGrafa.entries.forEach { r ->
                MacroCip(oznaka = stringResource(r.oznaka), odabran = razdoblje == r, onClick = { razdoblje = r })
            }
        }

        if (tocke.size < 2) {
            Box(
                modifier = Modifier
                    .padding(top = MacroRazmaci.m)
                    .fillMaxWidth()
                    .height(MacroDimenzije.grafMaseVisina)
                    .isprekidaniObris()
                    .padding(MacroRazmaci.xl),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.mj_premalo_tocaka),
                    style = tip.bodyMedium,
                    color = shema.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            val promjena = tocke.last().second - tocke.first().second
            val opisRazdoblja = razdoblje.uRazdoblju?.let { stringResource(it) }
                ?: ChronoUnit.DAYS.between(tocke.first().first, tocke.last().first).toInt().let {
                    pluralStringResource(R.plurals.mj_u_n_dana, it, it)
                }
            val sazetak = stringResource(R.string.mj_promjena_razdoblja, formatPromjenaMase(promjena), opisRazdoblja)
            val odDatuma = pocetak ?: tocke.first().first
            val doDatuma = maxOf(danas, tocke.last().first)

            GrafMase(
                tocke = tocke,
                odDatuma = odDatuma,
                doDatuma = doDatuma,
                opis = stringResource(
                    R.string.mj_graf_opis,
                    sazetak,
                    masaBezJedinice(tocke.minOf { it.second }),
                    masaBezJedinice(tocke.maxOf { it.second })
                ),
                animirajUlaz = animirajUlaz,
                modifier = Modifier.padding(top = MacroRazmaci.m)
            )

            // Predznak promjene se ne boji: smjer nije ni dobar ni loš sam po sebi (ovisi o cilju).
            Text(
                text = sazetak,
                style = tip.bodyMedium,
                color = shema.onSurfaceVariant,
                modifier = Modifier.padding(top = MacroRazmaci.m)
            )
        }

        PromjenaCilja(preporuke = preporuke, pocetak = pocetak)
    }
}

/** Zadnji nacrtani položaji točaka (px), da prijelaz na novo razdoblje krene od onoga što se vidi. */
private class StanjeLinije {
    var zadnje: List<Offset> = emptyList()
    var izvor: List<Offset> = emptyList()
    var tocke: List<Pair<LocalDate, Double>>? = null
}

@Composable
private fun GrafMase(
    tocke: List<Pair<LocalDate, Double>>,
    odDatuma: LocalDate,
    doDatuma: LocalDate,
    opis: String,
    animirajUlaz: Boolean,
    modifier: Modifier = Modifier
) {
    val smanjeno = rememberSmanjeniPokret()
    var odabrana by remember(tocke) { mutableStateOf<Int?>(null) }

    val shema = MaterialTheme.colorScheme
    val bojaLinije = MaterialTheme.macroBoje.lukIspuna
    val bojaMreze = shema.outlineVariant
    val bojaPozadine = shema.background
    val bojaPodloge = shema.surface

    val mjerac = rememberTextMeasurer()
    val stilOsi = MaterialTheme.typography.labelSmall.copy(color = shema.onSurfaceVariant)
    val stilOznake = MaterialTheme.typography.labelMedium.copy(color = shema.onSurface, fontWeight = FontWeight.SemiBold)

    val minRaw = tocke.minOf { it.second }
    val maxRaw = tocke.maxOf { it.second }
    val minMasa = minRaw - MARGINA_Y
    val maxMasa = maxRaw + MARGINA_Y
    val decimala = maxRaw - minRaw < RASPON_ZA_DECIMALU
    val ukupnoDana = ChronoUnit.DAYS.between(odDatuma, doDatuma).coerceAtLeast(1).toFloat()

    fun oznakaOsi(v: Double): String = if (decimala) masaBezJedinice(v) else Math.round(v).toString()

    // Lijevi prostor za oznake osi Y; računa se jednom po rasponu.
    val sirinaOsi = remember(minMasa, maxMasa, decimala, stilOsi) {
        listOf(minMasa, maxMasa).maxOf { mjerac.measure(oznakaOsi(it), stilOsi).size.width }.toFloat()
    }

    // Ulaz: linija se iscrta s lijeva (put po duljini). Promjena razdoblja: točke prelaze oprugom od nacrtanog.
    val iscrtavanje = remember { Animatable(if (animirajUlaz && !smanjeno) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (iscrtavanje.value < 1f) iscrtavanje.animateTo(1f, ulazniTween(Trajanje.ulaz, Kasnjenje.nakonPrijelaza, smanjeno))
    }
    val stanje = remember { StanjeLinije() }
    if (stanje.tocke !== tocke) {
        stanje.izvor = stanje.zadnje
        stanje.tocke = tocke
    }
    val prijelaz = remember(tocke) { Animatable(if (stanje.izvor.isEmpty()) 1f else 0f) }
    LaunchedEffect(tocke) {
        if (prijelaz.value < 1f) prijelaz.animateTo(1f, opruga(Opruge.blaga(), smanjeno))
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(MacroDimenzije.grafMaseVisina)
                .semantics { contentDescription = opis }
                .pointerInput(tocke, odDatuma, doDatuma, sirinaOsi) {
                    detectTapGestures { pozicija ->
                        val lijevo = sirinaOsi + MacroRazmaci.s.toPx()
                        val desno = size.width - MacroRazmaci.s.toPx()
                        val najbliza = tocke.indices.minByOrNull { i ->
                            val dani = ChronoUnit.DAYS.between(odDatuma, tocke[i].first).toFloat()
                            abs(lijevo + dani / ukupnoDana * (desno - lijevo) - pozicija.x)
                        }
                        odabrana = if (najbliza == odabrana) null else najbliza
                    }
                }
        ) {
            val lijevo = sirinaOsi + MacroRazmaci.s.toPx()
            val desno = size.width - MacroRazmaci.s.toPx()
            val vrh = MacroRazmaci.xxl.toPx()
            val dno = size.height - MacroRazmaci.s.toPx()

            fun x(datum: LocalDate): Float =
                lijevo + ChronoUnit.DAYS.between(odDatuma, datum).toFloat() / ukupnoDana * (desno - lijevo)

            fun y(masa: Double): Float =
                dno - ((masa - minMasa) / (maxMasa - minMasa)).toFloat() * (dno - vrh)

            // Tri vodoravne linije mreže s oznakama mase (dno, sredina, vrh raspona).
            listOf(minMasa, (minMasa + maxMasa) / 2, maxMasa).forEach { vrijednost ->
                val yLinije = y(vrijednost)
                drawLine(bojaMreze, Offset(lijevo, yLinije), Offset(desno, yLinije), MacroDimenzije.linija.toPx())
                val oznaka = mjerac.measure(oznakaOsi(vrijednost), stilOsi)
                drawText(oznaka, topLeft = Offset(0f, yLinije - oznaka.size.height / 2f))
            }

            // Ciljni položaji; tijekom prijelaza y kreće s prethodne linije na istom x.
            val t = prijelaz.value
            val izvor = stanje.izvor
            val polozaji = tocke.map { (datum, masa) ->
                val xi = x(datum)
                val yi = y(masa)
                Offset(xi, if (t < 1f && izvor.isNotEmpty()) lerp(yNaLiniji(izvor, xi), yi, t) else yi)
            }
            stanje.zadnje = polozaji

            val put = Path()
            val duljine = FloatArray(polozaji.size)
            polozaji.forEachIndexed { i, p ->
                if (i == 0) put.moveTo(p.x, p.y)
                else {
                    put.lineTo(p.x, p.y)
                    duljine[i] = duljine[i - 1] + hypot(p.x - polozaji[i - 1].x, p.y - polozaji[i - 1].y)
                }
            }
            val ukupno = duljine.last().coerceAtLeast(1f)
            val iscrtano = iscrtavanje.value * ukupno
            val crta = if (iscrtavanje.value >= 1f) put else Path().also { dio ->
                PathMeasure().apply { setPath(put, false) }.getSegment(0f, iscrtano, dio, true)
            }
            drawPath(
                path = crta,
                color = bojaLinije,
                style = Stroke(MacroDimenzije.grafLinija.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Točke se pojave kad do njih dođe linija; zadnja je puna, ostale imaju ispunu u boji pozadine.
            val zadnji = polozaji.lastIndex
            polozaji.forEachIndexed { i, p ->
                if (duljine[i] > iscrtano + 0.5f) return@forEachIndexed
                val polumjer = (if (i == odabrana) MacroDimenzije.grafTockaOdabrana else MacroDimenzije.grafTocka).toPx()
                drawCircle(bojaLinije, polumjer, p)
                if (i != zadnji) drawCircle(bojaPozadine, polumjer - MacroDimenzije.grafTockaRub.toPx(), p)
            }

            // Zadnja točka nosi svoju vrijednost; odabrana točka (dodir) dobiva oznaku s podlogom i datumom.
            val oznacena = odabrana ?: zadnji.takeIf { iscrtavanje.value >= 1f }
            oznacena?.let { i ->
                val (datum, masa) = tocke.getOrNull(i) ?: return@let
                val p = polozaji[i]
                val tekst = if (odabrana != null) "${datum.format(FORMAT_PUNO)} · ${formatMasa(masa)}" else masaBezJedinice(masa)
                val izmjereno = mjerac.measure(tekst, stilOznake)
                val ruba = MacroRazmaci.s.toPx()
                val sirina = izmjereno.size.width + 2 * ruba
                val visina = izmjereno.size.height + ruba
                val xOznake = (p.x - sirina / 2f).coerceIn(0f, size.width - sirina)
                val razmak = MacroRazmaci.m.toPx()
                val yOznake = (p.y - visina - razmak).let { if (it < 0f) p.y + razmak else it }

                if (odabrana != null) {
                    val oblik = CornerRadius(MacroRazmaci.s.toPx())
                    drawRoundRect(bojaPodloge, Offset(xOznake, yOznake), Size(sirina, visina), oblik)
                    drawRoundRect(
                        bojaMreze, Offset(xOznake, yOznake), Size(sirina, visina), oblik,
                        style = Stroke(MacroDimenzije.linija.toPx())
                    )
                }
                drawText(izmjereno, topLeft = Offset(xOznake + ruba, yOznake + ruba / 2f))
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = MacroRazmaci.xs),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = kratkiDatum(odDatuma), style = MaterialTheme.typography.labelSmall, color = shema.onSurfaceVariant)
            Text(text = kratkiDatum(doDatuma), style = MaterialTheme.typography.labelSmall, color = shema.onSurfaceVariant)
        }
    }
}

/** y prethodne linije na [x] (linearno između susjednih točaka, izvan raspona prva ili zadnja). */
private fun yNaLiniji(linija: List<Offset>, x: Float): Float {
    if (x <= linija.first().x) return linija.first().y
    if (x >= linija.last().x) return linija.last().y
    val i = linija.indexOfFirst { it.x >= x }.coerceAtLeast(1)
    val a = linija[i - 1]
    val b = linija[i]
    val raspon = b.x - a.x
    return if (raspon == 0f) b.y else lerp(a.y, b.y, (x - a.x) / raspon)
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

    Text(
        text = stringResource(
            R.string.mj_promjena_cilja,
            formatKcal(trenutna.kalorije),
            formatKcal(prije.kalorije)
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = MacroRazmaci.xs)
    )
}
