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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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
import androidx.compose.ui.unit.dp
import com.niko.macromenza.R
import com.niko.macromenza.screens.dnevnipregled.naslovDatuma
import com.niko.macromenza.ui.components.Tanjur
import com.niko.macromenza.ui.components.skalaPritiska
import com.niko.macromenza.ui.theme.Kasnjenje
import com.niko.macromenza.ui.theme.MacroDimenzije
import com.niko.macromenza.ui.theme.MacroOblici
import com.niko.macromenza.ui.theme.MacroOmjeri
import com.niko.macromenza.ui.theme.MacroRazmaci
import com.niko.macromenza.ui.theme.Opruge
import com.niko.macromenza.ui.theme.POMAK_TANJURA_DP
import com.niko.macromenza.ui.theme.SKALA_TANJURA
import com.niko.macromenza.ui.theme.Trajanje
import com.niko.macromenza.ui.theme.macroBoje
import com.niko.macromenza.ui.theme.opruga
import com.niko.macromenza.util.formatKcal
import java.time.LocalDate
import kotlinx.coroutines.delay

private const val BROJ_DANA = 7

/** Prigušenje slova i obrisa dana u budućnosti (kao onemogućeni elementi u M3). */
private const val ALFA_BUDUCNOSTI = 0.38f

/** Veličine jednog stupca; na uskom zaslonu sve se razmjerno smanjuje da okvir stane u stupac. */
private class VelicineTanjura(val max: Dp, val min: Dp, val prazan: Dp, val okvir: Dp) {
    val cilj: Dp get() = max / MacroOmjeri.trakaMaksimum
}

/**
 * Tjedan kao red od 7 tanjura (pages/povijest.md, "Tjedan: red od 7 tanjura"). Veličina tanjura je kcal dana na ljestvici
 * do 125 % cilja, a cilj je krug na 80 % najvećeg promjera. Prvi dodir odabire dan, drugi (ili "Otvori dan") otvara karticu Dan.
 * [kasnjenjeUlaza] je trenutak u ulaznom slijedu ekrana kad tanjuri počinju sjedati.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TjedanTanjuri(
    tjedan: TjedanUi,
    animirajUlaz: Boolean,
    kasnjenjeUlaza: Int,
    smanjeno: Boolean,
    onOtvoriDan: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    pocetnoOdabrani: Int? = null
) {
    val dani = tjedan.dani
    val oznakeDana = stringArrayResource(R.array.pov_oznake_dana)
    var odabrani by rememberSaveable(tjedan.pocetak) { mutableStateOf(pocetnoOdabrani) }
    val maxKcalTjedna = maxOf(dani.maxOfOrNull { it.makro.kcal } ?: 0.0, 1.0)

    val sazetak = stringResource(R.string.pov_tjedan_opis, tjedan.raspon, tjedan.daniUCilju, tjedan.daniSUnosom)

    Column(modifier = modifier.fillMaxWidth()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = sazetak }
        ) {
            val sirinaStupca = maxWidth / BROJ_DANA
            val idealniOkvir = MacroDimenzije.tanjurTjedanMax + (MacroDimenzije.xxsRub + MacroDimenzije.prekoracenjeRub) * 2
            val skala = minOf(1f, sirinaStupca / idealniOkvir)
            val velicine = VelicineTanjura(
                max = MacroDimenzije.tanjurTjedanMax * skala,
                min = MacroDimenzije.tanjurTjedanMin * skala,
                prazan = MacroDimenzije.tanjurTjedanPrazan * skala,
                okvir = idealniOkvir * skala
            )

            PilulaOdabira(odabrani = odabrani, smanjeno = smanjeno, modifier = Modifier.matchParentSize())

            Row(modifier = Modifier.fillMaxWidth()) {
                dani.forEachIndexed { indeks, dan ->
                    StupacDana(
                        dan = dan,
                        oznaka = oznakeDana.getOrElse(indeks) { "" },
                        odabran = odabrani == indeks,
                        velicine = velicine,
                        maxKcalTjedna = maxKcalTjedna,
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

        if (dani.any { it.stanje == StanjeDana.U_CILJU || it.stanje == StanjeDana.PREKO_CILJA }) {
            Legenda(modifier = Modifier.padding(top = MacroRazmaci.m, start = MacroRazmaci.s, end = MacroRazmaci.s))
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
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

/** Pilula iza odabranog stupca: klizi između stupaca (Opruge.blaga), pri prvom odabiru samo izblijedi. */
@Composable
private fun PilulaOdabira(odabrani: Int?, smanjeno: Boolean, modifier: Modifier = Modifier) {
    val boja = MaterialTheme.colorScheme.secondaryContainer
    val polozaj = remember { Animatable((odabrani ?: 0).toFloat()) }
    var prethodni by remember { mutableStateOf(odabrani) }
    LaunchedEffect(odabrani) {
        val cilj = odabrani ?: return@LaunchedEffect
        if (prethodni == null) polozaj.snapTo(cilj.toFloat())
        else polozaj.animateTo(cilj.toFloat(), opruga(Opruge.blaga(), smanjeno))
        prethodni = cilj
    }
    val alfa by animateFloatAsState(
        targetValue = if (odabrani != null) 1f else 0f,
        animationSpec = tween(if (smanjeno) 0 else Trajanje.kratko),
        label = "alfaPilule"
    )

    Canvas(modifier = modifier) {
        if (alfa <= 0f) return@Canvas
        val sirina = size.width / BROJ_DANA
        val uvlaka = MacroRazmaci.xxs.toPx()
        val w = sirina - 2 * uvlaka
        drawRoundRect(
            color = boja.copy(alpha = boja.alpha * alfa),
            topLeft = Offset(polozaj.value * sirina + uvlaka, 0f),
            size = Size(w, size.height),
            cornerRadius = CornerRadius(w / 2)
        )
    }
}

@Composable
private fun StupacDana(
    dan: DanTjednaUi,
    oznaka: String,
    odabran: Boolean,
    velicine: VelicineTanjura,
    maxKcalTjedna: Double,
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

    val ciljniPromjer = promjerDana(dan, maxKcalTjedna, velicine)
    // Ista opruga kao u Tanjuru, pa prsten preko cilja prati rub tanjura.
    val promjer = remember { Animatable(ciljniPromjer.value) }
    LaunchedEffect(ciljniPromjer) { promjer.animateTo(ciljniPromjer.value, opruga(Opruge.blaga(), smanjeno)) }

    // Slijetanje samo pri prvom ulazu u sesiji; nova vrijednost tjedna tanjur ne spušta ponovno.
    val sletanje = remember { Animatable(if (animirajUlaz && dan.imaUnos) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (sletanje.value < 1f) {
            delay(kasnjenje.toLong())
            sletanje.animateTo(1f, Opruge.slijeganje())
        }
    }

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
            modifier = Modifier.clearAndSetSemantics { },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = oznaka,
                style = if (dan.jeDanas) tip.labelMedium.copy(fontWeight = FontWeight.SemiBold) else tip.labelMedium,
                color = when {
                    dan.jeDanas -> shema.onSurface
                    stanje == StanjeDana.BUDUCNOST -> shema.onSurfaceVariant.copy(alpha = ALFA_BUDUCNOSTI)
                    else -> shema.onSurfaceVariant
                }
            )
            Box(
                Modifier
                    .padding(top = MacroRazmaci.xxs)
                    .size(MacroDimenzije.oznakaDanas)
                    .then(if (dan.jeDanas) Modifier.background(shema.onSurface, MacroOblici.traka) else Modifier)
            )
            Spacer(Modifier.height(MacroRazmaci.xs))

            Box(modifier = Modifier.size(velicine.okvir), contentAlignment = Alignment.Center) {
                val bojaCilja = shema.onSurfaceVariant
                val bojaUCilju = boje.lukIspuna
                val bojaBuducnosti = shema.outlineVariant
                val visnja = boje.visnja
                Canvas(modifier = Modifier.fillMaxSize()) {
                    when (stanje) {
                        StanjeDana.BEZ_UNOSA -> isprekidaniKrug(bojaCilja, velicine.prazan.toPx())
                        StanjeDana.BUDUCNOST -> isprekidaniKrug(bojaBuducnosti, velicine.prazan.toPx())
                        StanjeDana.U_CILJU -> drawCircle(
                            color = bojaUCilju,
                            radius = velicine.cilj.toPx() / 2,
                            style = Stroke(MacroDimenzije.obrubIsprekidan.toPx())
                        )
                        StanjeDana.ISPOD_CILJA, StanjeDana.PREKO_CILJA -> isprekidaniKrug(bojaCilja, velicine.cilj.toPx())
                        StanjeDana.BEZ_CILJA -> Unit
                    }
                    if (stanje == StanjeDana.PREKO_CILJA) {
                        // Prsten u višnji, odvojen od ruba tanjura razmakom u boji pozadine (kao luk na Home).
                        val debljina = MacroDimenzije.prekoracenjeRub.toPx()
                        val r = promjer.value.dp.toPx() / 2 + MacroDimenzije.xxsRub.toPx() + debljina / 2
                        drawCircle(visnja.copy(alpha = sletanje.value.coerceIn(0f, 1f)), r, style = Stroke(debljina))
                    }
                }
                if (dan.imaUnos) {
                    Tanjur(
                        makro = dan.makro,
                        promjer = ciljniPromjer,
                        okvir = velicine.max,
                        smanjeno = smanjeno,
                        punjenjeRuba = animirajUlaz,
                        prikaziKcal = false,
                        modifier = Modifier.graphicsLayer {
                            val s = sletanje.value
                            alpha = s.coerceIn(0f, 1f)
                            translationY = (1f - s) * -POMAK_TANJURA_DP.dp.toPx()
                            val skala = SKALA_TANJURA + (1f - SKALA_TANJURA) * s
                            scaleX = skala
                            scaleY = skala
                        }
                    )
                }
            }

            Spacer(Modifier.height(MacroRazmaci.xs))
            Text(
                text = when (stanje) {
                    StanjeDana.BEZ_UNOSA -> stringResource(R.string.pov_bez_vrijednosti)
                    StanjeDana.BUDUCNOST -> ""
                    else -> formatKcal(dan.makro.kcal)
                },
                style = tip.labelSmall,
                color = when (stanje) {
                    StanjeDana.U_CILJU -> shema.onSurface
                    StanjeDana.PREKO_CILJA -> boje.visnja
                    else -> shema.onSurfaceVariant
                },
                maxLines = 1,
                softWrap = false
            )
            // Mjesto za ikonu je uvijek tu, pa su svi stupci jednako visoki.
            Box(modifier = Modifier.size(MacroDimenzije.ikonaSitna), contentAlignment = Alignment.Center) {
                when (stanje) {
                    StanjeDana.U_CILJU -> Icon(Icons.Rounded.Check, contentDescription = null, tint = boje.lukIspuna, modifier = Modifier.fillMaxSize())
                    StanjeDana.PREKO_CILJA -> Icon(Icons.Rounded.ArrowUpward, contentDescription = null, tint = boje.visnja, modifier = Modifier.fillMaxSize())
                    else -> Unit
                }
            }
        }
    }
}

private fun DrawScope.isprekidaniKrug(boja: Color, promjer: Float) {
    val debljina = MacroDimenzije.obrubIsprekidan.toPx()
    val crtica = MacroDimenzije.crtica.toPx()
    drawCircle(
        color = boja,
        radius = promjer / 2 - debljina / 2,
        style = Stroke(debljina, pathEffect = PathEffect.dashPathEffect(floatArrayOf(crtica, crtica)))
    )
}

/** Ljestvica do 125 % cilja dana; dan bez cilja mjeri se prema najvećem kcal u tjednu. */
private fun promjerDana(dan: DanTjednaUi, maxKcalTjedna: Double, velicine: VelicineTanjura): Dp {
    val ljestvica = dan.ciljKcal?.takeIf { it > 0 }?.let { it * MacroOmjeri.trakaMaksimum } ?: maxKcalTjedna
    val udio = (dan.makro.kcal / ljestvica).toFloat().coerceIn(0f, 1f)
    return (velicine.max * udio).coerceIn(velicine.min, velicine.max)
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
        StanjeDana.PREKO_CILJA -> stringResource(
            R.string.pov_dan_preko, datum, kcal, cilj, formatKcal(dan.makro.kcal - (dan.ciljKcal ?: 0.0))
        )
        StanjeDana.BEZ_CILJA -> stringResource(R.string.pov_dan_bez_cilja, datum, kcal)
        StanjeDana.BEZ_UNOSA -> stringResource(R.string.pov_dan_bez_unosa, datum)
        StanjeDana.BUDUCNOST -> stringResource(R.string.pov_dan_buducnost, datum)
    }
    return if (dan.jeDanas) stringResource(R.string.pov_dan_danas, osnovno) else osnovno
}

/** "○ Cilj · ✓ U cilju · ↑ Preko cilja"; prelama se pri velikom fontu. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Legenda(modifier: Modifier = Modifier) {
    val shema = MaterialTheme.colorScheme
    val boje = MaterialTheme.macroBoje
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MacroRazmaci.l, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(MacroRazmaci.xs)
    ) {
        StavkaLegende(stringResource(R.string.pov_legenda_cilj)) {
            Canvas(Modifier.size(MacroDimenzije.ikonaSitna)) { isprekidaniKrug(shema.onSurfaceVariant, size.minDimension) }
        }
        StavkaLegende(stringResource(R.string.pov_legenda_u_cilju)) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = boje.lukIspuna, modifier = Modifier.size(MacroDimenzije.ikonaSitna))
        }
        StavkaLegende(stringResource(R.string.pov_legenda_preko)) {
            Icon(Icons.Rounded.ArrowUpward, contentDescription = null, tint = boje.visnja, modifier = Modifier.size(MacroDimenzije.ikonaSitna))
        }
    }
}

@Composable
private fun StavkaLegende(tekst: String, oznaka: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        oznaka()
        Spacer(Modifier.width(MacroRazmaci.xs))
        Text(tekst, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Red ispod tanjura: odabrani dan (datum, kcal, status, "Otvori dan ›") ili savjet. Iste je visine u oba slučaja. */
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
