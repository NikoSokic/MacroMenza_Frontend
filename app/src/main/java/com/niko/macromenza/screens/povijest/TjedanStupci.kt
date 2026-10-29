package com.niko.macromenza.screens.povijest

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
import com.niko.macromenza.R
import com.niko.macromenza.screens.dnevnipregled.naslovDatuma
import com.niko.macromenza.ui.components.skalaPritiska
import com.niko.macromenza.ui.theme.Kasnjenje
import com.niko.macromenza.ui.theme.MacroDimenzije
import com.niko.macromenza.ui.theme.MacroOblici
import com.niko.macromenza.ui.theme.MacroOmjeri
import com.niko.macromenza.ui.theme.MacroRazmaci
import com.niko.macromenza.ui.theme.Opruge
import com.niko.macromenza.ui.theme.Trajanje
import com.niko.macromenza.ui.theme.macroBoje
import com.niko.macromenza.ui.theme.opruga
import com.niko.macromenza.ui.theme.ulazniTween
import com.niko.macromenza.util.formatKcal
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val BROJ_DANA = 7

/** Pojas cilja ±10 % (isti prag kao "u cilju" na backendu). */
private const val POJAS_DOLJE = 0.9f
private const val POJAS_GORE = 1.1f

/** Tjedan bez cilja: najveći dan stoji na 1 / 1,1 visine grafa. */
private const val PROSTOR_BEZ_CILJA = 1.1

/** Dio visine sličice u legendi koji zauzima pojas, odnosno baza stupca preko cilja. */
private const val LEGENDA_POJAS = 0.5f
private const val LEGENDA_BAZA = 0.5f

/**
 * Vrh ljestvice u kcal: 125 % najvećeg cilja u tjednu, proširen do dana koji ga premašuje (stupac se ne reže).
 * Bez cilja: najveći dan tjedna × 1,1.
 */
fun vrhLjestvice(dani: List<DanTjednaUi>): Double {
    val maxKcal = dani.filter { it.imaUnos }.maxOfOrNull { it.makro.kcal } ?: 0.0
    val maxCilj = dani.mapNotNull { it.ciljKcal?.takeIf { c -> c > 0.0 } }.maxOrNull()
    val vrh = if (maxCilj != null) maxOf(maxCilj * MacroOmjeri.trakaMaksimum, maxKcal) else maxKcal * PROSTOR_BEZ_CILJA
    return maxOf(vrh, 1.0)
}

/**
 * Tjedan kao stupčasti graf s pojasom cilja (pages/povijest.md, "Tjedan: stupci s pojasom cilja"). Crta Canvas u svakom
 * od 7 stupaca dana, a stupac je i dodirni cilj i čvor za čitač zaslona. Prvi dodir odabire dan, drugi (ili "Otvori dan")
 * otvara karticu Dan. [kasnjenjeUlaza] je trenutak u ulaznom slijedu ekrana kad stupci počinju rasti.
 */
@Composable
fun TjedanStupci(
    tjedan: TjedanUi,
    animirajUlaz: Boolean,
    kasnjenjeUlaza: Int,
    smanjeno: Boolean,
    onOtvoriDan: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    pocetnoOdabrani: Int? = null
) {
    val dani = tjedan.dani
    val shema = MaterialTheme.colorScheme
    val tip = MaterialTheme.typography
    val oznakeDana = stringArrayResource(R.array.pov_oznake_dana)
    var odabrani by rememberSaveable(tjedan.pocetak) { mutableStateOf(pocetnoOdabrani) }
    val vrh = vrhLjestvice(dani)
    val imaCilj = dani.any { (it.ciljKcal ?: 0.0) > 0.0 }
    val ciljTjedna = tjedan.prosjekCilj?.kcal?.takeIf { it > 0.0 }
        ?: dani.lastOrNull { (it.ciljKcal ?: 0.0) > 0.0 }?.ciljKcal

    val sazetak = stringResource(R.string.pov_tjedan_opis, tjedan.raspon, tjedan.daniUCilju, tjedan.daniSUnosom)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MacroRazmaci.s),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                stringResource(R.string.pov_graf_jedinica),
                style = tip.bodySmall,
                color = shema.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (ciljTjedna != null) {
                Text(
                    stringResource(R.string.pov_graf_cilj, formatKcal(ciljTjedna)),
                    style = tip.bodySmall,
                    color = shema.onSurfaceVariant
                )
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = MacroRazmaci.xs)
                .semantics { contentDescription = sazetak }
        ) {
            PilulaOdabira(
                odabrani = odabrani,
                sirinaStupca = maxWidth / BROJ_DANA,
                smanjeno = smanjeno,
                modifier = Modifier.matchParentSize()
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                dani.forEachIndexed { indeks, dan ->
                    StupacDana(
                        dan = dan,
                        indeks = indeks,
                        oznaka = oznakeDana.getOrElse(indeks) { "" },
                        odabran = odabrani == indeks,
                        vrh = vrh,
                        animirajUlaz = animirajUlaz,
                        kasnjenje = kasnjenjeUlaza + indeks * Kasnjenje.tanjuri,
                        smanjeno = smanjeno,
                        onClick = {
                            if (odabrani == indeks) onOtvoriDan(dan.datum) else odabrani = indeks
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (imaCilj) {
            Legenda(modifier = Modifier.padding(top = MacroRazmaci.m, start = MacroRazmaci.s, end = MacroRazmaci.s))
        }

        HorizontalDivider(
            color = shema.outlineVariant,
            modifier = Modifier.padding(top = MacroRazmaci.m, start = MacroRazmaci.s, end = MacroRazmaci.s)
        )

        Crossfade(
            targetState = odabrani?.let { dani.getOrNull(it) },
            animationSpec = tween(if (smanjeno) 0 else Trajanje.kratko),
            label = "odabraniDan"
        ) { dan ->
            OdabraniDan(
                dan = dan,
                onOtvori = { dan?.let { onOtvoriDan(it.datum) } },
                modifier = Modifier.padding(horizontal = MacroRazmaci.s)
            )
        }
    }
}

/** Pilula iza odabranog stupca: klizi kroz graphicsLayer.translationX (Opruge.blaga), pri prvom odabiru samo izblijedi. */
@Composable
private fun PilulaOdabira(odabrani: Int?, sirinaStupca: Dp, smanjeno: Boolean, modifier: Modifier = Modifier) {
    val polozaj = remember { Animatable((odabrani ?: 0).toFloat()) }
    var prethodni by remember { mutableStateOf(odabrani) }
    LaunchedEffect(odabrani) {
        val cilj = odabrani
        if (cilj == null) {
            prethodni = null
            return@LaunchedEffect
        }
        if (prethodni == null) polozaj.snapTo(cilj.toFloat())
        else polozaj.animateTo(cilj.toFloat(), opruga(Opruge.blaga(), smanjeno))
        prethodni = cilj
    }
    val alfa by animateFloatAsState(
        targetValue = if (odabrani != null) 1f else 0f,
        animationSpec = tween(if (smanjeno) 0 else Trajanje.kratko),
        label = "alfaPilule"
    )

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .width(sirinaStupca)
                .fillMaxHeight()
                .graphicsLayer {
                    translationX = polozaj.value * sirinaStupca.toPx()
                    alpha = alfa
                }
                .padding(horizontal = MacroRazmaci.xxs)
                .background(MaterialTheme.colorScheme.secondaryContainer, MacroOblici.pilula)
        )
    }
}

@Composable
private fun StupacDana(
    dan: DanTjednaUi,
    indeks: Int,
    oznaka: String,
    odabran: Boolean,
    vrh: Double,
    animirajUlaz: Boolean,
    kasnjenje: Int,
    smanjeno: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shema = MaterialTheme.colorScheme
    val boje = MaterialTheme.macroBoje
    val tip = MaterialTheme.typography
    val stanje = dan.stanje

    // Udjeli visine grafa; nova vrijednost (drugi tjedan, druga ljestvica) prelazi od trenutne.
    val ciljniUdio = if (dan.imaUnos) (dan.makro.kcal / vrh).toFloat().coerceIn(0f, 1f) else 0f
    val ciljniUdioCilja = dan.ciljKcal?.takeIf { it > 0.0 }?.let { (it / vrh).toFloat().coerceIn(0f, 1f) }
    val udio = remember { Animatable(ciljniUdio) }
    LaunchedEffect(ciljniUdio) { udio.animateTo(ciljniUdio, opruga(Opruge.blaga(), smanjeno)) }
    val udioCilja = remember { Animatable(ciljniUdioCilja ?: 0f) }
    LaunchedEffect(ciljniUdioCilja) {
        if (ciljniUdioCilja != null) udioCilja.animateTo(ciljniUdioCilja, opruga(Opruge.blaga(), smanjeno))
    }

    // Rast od osnovne linije samo pri prvom ulazu u sesiji; broj izblijedi kad stupac gotovo naraste.
    val rast = remember { Animatable(if (animirajUlaz) 0f else 1f) }
    val alfaBroja = remember { Animatable(if (animirajUlaz) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (rast.value < 1f) {
            delay(kasnjenje.toLong())
            launch {
                delay(Trajanje.kratko.toLong())
                alfaBroja.animateTo(1f, ulazniTween(Trajanje.kratko, smanjeno = smanjeno))
            }
            rast.animateTo(1f, Opruge.slijeganje())
        }
    }

    val bojaPojasa = shema.surfaceVariant
    val bojaCilja = shema.onSurfaceVariant
    val bojaOsnove = shema.outlineVariant
    val bojaPozadine = shema.background
    val bojaStupca = boje.lukIspuna
    val visnja = boje.visnja

    val opis = opisDana(dan)
    val izvor = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .clickable(
                enabled = stanje != StanjeDana.BUDUCNOST,
                interactionSource = izvor,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .semantics {
                contentDescription = opis
                selected = odabran
            }
            .skalaPritiska(izvor)
            .padding(vertical = MacroRazmaci.s),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clearAndSetSemantics { },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Broj stoji iznad vrha stupca: mjesto mu je rezervirano iznad grafa, a pomiče se u njega (graphicsLayer).
            OznakaVrijednosti(
                dan = dan,
                modifier = Modifier
                    .zIndex(1f)
                    .graphicsLayer {
                        val visinaGrafa = MacroDimenzije.grafTjednaVisina.toPx()
                        val prazan = MacroDimenzije.stupacTjednaPrazan.toPx()
                        translationY = visinaGrafa - visinaStupca(stanje, udio.value, rast.value, visinaGrafa, prazan)
                        alpha = alfaBroja.value
                    }
            )
            Spacer(Modifier.height(MacroRazmaci.xs))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MacroDimenzije.grafTjednaVisina)
                    .clipToBounds()
            ) {
                val w = size.width
                val h = size.height
                val crtica = MacroDimenzije.crtica.toPx()
                // Pojas i linija cilja crtaju se po danu, pa se lome kad se preporuka promijeni usred tjedna;
                // faza crtica nastavlja se iz stupca u stupac, pa linija izgleda neprekinuto.
                if (ciljniUdioCilja != null) {
                    val c = udioCilja.value
                    val gore = (h * (1f - c * POJAS_GORE)).coerceAtLeast(0f)
                    val dolje = h * (1f - c * POJAS_DOLJE)
                    drawRect(bojaPojasa, topLeft = Offset(0f, gore), size = Size(w, dolje - gore))
                    val y = h * (1f - c)
                    drawLine(
                        color = bojaCilja,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = MacroDimenzije.obrubIsprekidan.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(crtica, crtica), (indeks * w) % (2 * crtica))
                    )
                }
                val linija = MacroDimenzije.linija.toPx()
                drawLine(bojaOsnove, Offset(0f, h - linija / 2), Offset(w, h - linija / 2), linija)

                val sirina = MacroDimenzije.stupacTjedna.toPx().coerceAtMost(w)
                val x = (w - sirina) / 2
                val vis = visinaStupca(stanje, udio.value, rast.value, h, MacroDimenzije.stupacTjednaPrazan.toPx())
                when (stanje) {
                    StanjeDana.U_CILJU, StanjeDana.BEZ_CILJA -> stupac(x, h - vis, sirina, vis, bojaStupca)
                    StanjeDana.ISPOD_CILJA -> {
                        stupac(x, h - vis, sirina, vis, bojaPojasa)
                        obrisStupca(x, h - vis, sirina, vis, bojaStupca, Stroke(MacroDimenzije.obrisStupca.toPx()))
                    }
                    StanjeDana.PREKO_CILJA -> {
                        // Baza do cilja, razmak u boji pozadine, pa višnja do stvarne vrijednosti (kao luk na Home).
                        val hCilja = h * udioCilja.value
                        val razmak = MacroDimenzije.prekoracenjeRub.toPx()
                        if (vis <= hCilja) {
                            stupac(x, h - vis, sirina, vis, bojaStupca)
                        } else {
                            drawRect(bojaStupca, topLeft = Offset(x, h - hCilja), size = Size(sirina, hCilja))
                            val vrhRazmaka = maxOf(h - vis, h - hCilja - razmak)
                            drawRect(bojaPozadine, topLeft = Offset(x, vrhRazmaka), size = Size(sirina, h - hCilja - vrhRazmaka))
                            stupac(x, h - vis, sirina, vis - hCilja - razmak, visnja)
                        }
                    }
                    StanjeDana.BEZ_UNOSA -> obrisStupca(
                        x, h - vis, sirina, vis, bojaCilja,
                        Stroke(
                            MacroDimenzije.obrubIsprekidan.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(crtica / 2, crtica / 2))
                        )
                    )
                    StanjeDana.BUDUCNOST -> Unit
                }
            }

            Spacer(Modifier.height(MacroRazmaci.xs))
            Text(
                text = oznaka,
                style = if (odabran) tip.labelMedium.copy(fontWeight = FontWeight.SemiBold) else tip.labelMedium,
                color = when {
                    odabran -> shema.onSurface
                    stanje == StanjeDana.BUDUCNOST -> shema.onSurfaceVariant.copy(alpha = MacroOmjeri.onemoguceno)
                    dan.jeDanas -> shema.onSurface
                    else -> shema.onSurfaceVariant
                }
            )
            Box(
                Modifier
                    .padding(top = MacroRazmaci.xxs)
                    .size(MacroDimenzije.oznakaDanas)
                    .then(if (dan.jeDanas) Modifier.background(shema.onSurface, MacroOblici.traka) else Modifier)
            )
        }
    }
}

/** Trenutna visina stupca u px: udio ljestvice × rast pri ulazu; dan bez unosa ima kratki obris, budućnost ništa. */
private fun visinaStupca(stanje: StanjeDana, udio: Float, rast: Float, visinaGrafa: Float, prazan: Float): Float = when (stanje) {
    StanjeDana.BUDUCNOST -> 0f
    StanjeDana.BEZ_UNOSA -> prazan
    else -> (udio * rast * visinaGrafa).coerceIn(0f, visinaGrafa)
}

/** Pun stupac zaobljen samo gore (MacroOblici.stupacTjedna). */
private fun DrawScope.stupac(lijevo: Float, vrh: Float, sirina: Float, visina: Float, boja: Color, stil: DrawStyle = Fill) {
    if (visina <= 0f || sirina <= 0f) return
    val oblik = MacroOblici.stupacTjedna.createOutline(Size(sirina, visina), layoutDirection, this)
    translate(lijevo, vrh) { drawOutline(oblik, boja, style = stil) }
}

/** Obris stupca: crta se pola debljine unutar ruba, pa ne izlazi izvan stupca. */
private fun DrawScope.obrisStupca(lijevo: Float, vrh: Float, sirina: Float, visina: Float, boja: Color, stil: Stroke) {
    val pola = stil.width / 2
    stupac(lijevo + pola, vrh + pola, sirina - 2 * pola, visina - pola, boja, stil)
}

/** Broj kcal iznad stupca; ikona stanja stoji iznad broja (uz broj ne stane pri fontScale 1,3). */
@Composable
private fun OznakaVrijednosti(dan: DanTjednaUi, modifier: Modifier = Modifier) {
    val shema = MaterialTheme.colorScheme
    val boje = MaterialTheme.macroBoje
    val stanje = dan.stanje
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        // Mjesto za ikonu je uvijek tu, pa grafovi svih stupaca počinju na istoj visini.
        Box(modifier = Modifier.size(MacroDimenzije.ikonaSitna), contentAlignment = Alignment.Center) {
            when (stanje) {
                StanjeDana.U_CILJU -> Icon(Icons.Rounded.Check, contentDescription = null, tint = shema.onSurface, modifier = Modifier.fillMaxSize())
                StanjeDana.PREKO_CILJA -> Icon(Icons.Rounded.ArrowUpward, contentDescription = null, tint = boje.visnja, modifier = Modifier.fillMaxSize())
                else -> Unit
            }
        }
        Text(
            text = when (stanje) {
                StanjeDana.BEZ_UNOSA -> stringResource(R.string.pov_bez_vrijednosti)
                StanjeDana.BUDUCNOST -> ""
                else -> formatKcal(dan.makro.kcal)
            },
            style = MaterialTheme.typography.labelSmall,
            color = when (stanje) {
                StanjeDana.U_CILJU -> shema.onSurface
                StanjeDana.PREKO_CILJA -> boje.visnja
                else -> shema.onSurfaceVariant
            },
            maxLines = 1,
            softWrap = false
        )
    }
}

/** Rečenica za čitač zaslona prema tablici stanja u specu. */
@Composable
private fun opisDana(dan: DanTjednaUi): String {
    val datum = naslovDatuma(dan.datum)
    val kcal = formatKcal(dan.makro.kcal)
    val cilj = dan.ciljKcal?.let { formatKcal(it) }.orEmpty()
    val osnovno = when (dan.stanje) {
        StanjeDana.ISPOD_CILJA -> stringResource(R.string.pov_dan_ispod, datum, kcal, cilj)
        StanjeDana.U_CILJU -> stringResource(R.string.pov_dan_u_cilju, datum, kcal, cilj)
        StanjeDana.PREKO_CILJA -> stringResource(R.string.pov_dan_preko, datum, kcal, cilj)
        StanjeDana.BEZ_CILJA -> stringResource(R.string.pov_dan_bez_cilja, datum, kcal)
        StanjeDana.BEZ_UNOSA -> stringResource(R.string.pov_dan_bez_unosa, datum)
        StanjeDana.BUDUCNOST -> stringResource(R.string.pov_dan_buducnost, datum)
    }
    return if (dan.jeDanas) stringResource(R.string.pov_dan_danas, osnovno) else osnovno
}

/** "Cilj ±10 % · U cilju · Preko cilja · Ispod cilja" sa sličicama kao u grafu; prelama se pri velikom fontu. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Legenda(modifier: Modifier = Modifier) {
    val shema = MaterialTheme.colorScheme
    val boje = MaterialTheme.macroBoje
    val bojaPojasa = shema.surfaceVariant
    val bojaCilja = shema.onSurfaceVariant
    val bojaPozadine = shema.background
    val bojaStupca = boje.lukIspuna
    val visnja = boje.visnja
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { },
        horizontalArrangement = Arrangement.spacedBy(MacroRazmaci.l, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(MacroRazmaci.xs)
    ) {
        StavkaLegende(stringResource(R.string.pov_legenda_cilj)) {
            val crtica = MacroDimenzije.crtica.toPx()
            val visinaPojasa = size.height * LEGENDA_POJAS
            drawRect(bojaPojasa, topLeft = Offset(0f, (size.height - visinaPojasa) / 2), size = Size(size.width, visinaPojasa))
            drawLine(
                color = bojaCilja,
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = MacroDimenzije.obrubIsprekidan.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(crtica / 2, crtica / 2))
            )
        }
        StavkaLegende(stringResource(R.string.pov_legenda_u_cilju)) {
            stupac(0f, 0f, size.width, size.height, bojaStupca)
        }
        StavkaLegende(stringResource(R.string.pov_legenda_preko)) {
            val baza = size.height * LEGENDA_BAZA
            val razmak = MacroDimenzije.xxsRub.toPx()
            drawRect(bojaStupca, topLeft = Offset(0f, size.height - baza), size = Size(size.width, baza))
            drawRect(bojaPozadine, topLeft = Offset(0f, size.height - baza - razmak), size = Size(size.width, razmak))
            stupac(0f, 0f, size.width, size.height - baza - razmak, visnja)
        }
        StavkaLegende(stringResource(R.string.pov_legenda_ispod)) {
            stupac(0f, 0f, size.width, size.height, bojaPojasa)
            obrisStupca(0f, 0f, size.width, size.height, bojaStupca, Stroke(MacroDimenzije.obrisStupca.toPx()))
        }
    }
}

@Composable
private fun StavkaLegende(tekst: String, slicica: DrawScope.() -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(MacroDimenzije.ikonaSitna), onDraw = slicica)
        Spacer(Modifier.width(MacroRazmaci.xs))
        Text(tekst, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Red ispod grafa: odabrani dan (datum, kcal, status, "Otvori dan ›") ili savjet. Iste je visine u oba slučaja. */
@Composable
private fun OdabraniDan(dan: DanTjednaUi?, onOtvori: () -> Unit, modifier: Modifier = Modifier) {
    val shema = MaterialTheme.colorScheme
    val tip = MaterialTheme.typography
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MacroDimenzije.dodirniCilj + MacroRazmaci.l)
            .padding(start = MacroRazmaci.s),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (dan == null) {
            Text(stringResource(R.string.pov_odabir_savjet), style = tip.bodySmall, color = shema.onSurfaceVariant)
            return@Row
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(naslovDatuma(dan.datum), style = tip.titleSmall, color = shema.onSurface)
            val kcal = when (dan.stanje) {
                StanjeDana.BEZ_UNOSA -> null
                StanjeDana.BEZ_CILJA -> stringResource(R.string.pov_kcal, formatKcal(dan.makro.kcal))
                else -> stringResource(R.string.pov_odabrani_kcal, formatKcal(dan.makro.kcal), formatKcal(dan.ciljKcal ?: 0.0))
            }
            val status = when (dan.stanje) {
                StanjeDana.ISPOD_CILJA -> stringResource(R.string.pov_status_ispod)
                StanjeDana.U_CILJU -> stringResource(R.string.pov_status_u_cilju)
                StanjeDana.PREKO_CILJA -> stringResource(R.string.pov_status_preko)
                StanjeDana.BEZ_UNOSA -> stringResource(R.string.pov_status_bez_unosa)
                StanjeDana.BEZ_CILJA, StanjeDana.BUDUCNOST -> null
            }
            val razmak = stringResource(R.string.pov_status_razmak)
            val bojaStatusa = if (dan.stanje == StanjeDana.PREKO_CILJA) MaterialTheme.macroBoje.visnja else shema.onSurfaceVariant
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = shema.onSurfaceVariant)) {
                        if (kcal != null) append(kcal)
                        if (kcal != null && status != null) append(razmak)
                    }
                    if (status != null) withStyle(SpanStyle(color = bojaStatusa)) { append(status) }
                },
                style = tip.bodySmall
            )
        }
        val izvor = remember { MutableInteractionSource() }
        TextButton(
            onClick = onOtvori,
            interactionSource = izvor,
            contentPadding = PaddingValues(start = MacroRazmaci.m, end = MacroRazmaci.s),
            modifier = Modifier
                .heightIn(min = MacroDimenzije.dodirniCilj)
                .skalaPritiska(izvor)
        ) {
            Text(stringResource(R.string.pov_otvori_dan), style = tip.labelLarge, color = shema.primary)
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = shema.primary, modifier = Modifier.size(MacroDimenzije.ikonaMala))
        }
    }
}
