package com.niko.macromenza.screens.dnevnipregled

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.niko.macromenza.R
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
import com.niko.macromenza.ui.theme.Ublazavanje
import com.niko.macromenza.ui.theme.macroBoje
import com.niko.macromenza.ui.theme.macroTipografija
import com.niko.macromenza.ui.theme.opruga
import com.niko.macromenza.util.formatKcal
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

private const val BROJ_ISKRI = 14
private const val BROJ_ZRNA = 18

/**
 * "Tvoj pladanj": pladanj od oraha (crtan u Canvasu) s utorom za doručak, ručak i večeru (+ tanjurić za užinu).
 * Tanjur je bijeli krug čiji je rub podijeljen po udjelu P/M/U; promjer prati udio obroka u danas unesenim kcal.
 */
@Composable
fun Pladanj(
    tanjuri: List<TanjurUi>,
    danJePrazan: Boolean,
    animirajUlaz: Boolean,
    smanjeno: Boolean,
    proslava: Boolean,
    onProslavaGotova: () -> Unit,
    onTanjur: (TanjurUi) -> Unit,
    modifier: Modifier = Modifier
) {
    val boje = MaterialTheme.macroBoje

    val ukupnoKcal = tanjuri.sumOf { it.makro?.kcal ?: 0.0 }
    val najveciUdio = tanjuri.maxOfOrNull { (it.makro?.kcal ?: 0.0) / ukupnoKcal.coerceAtLeast(1.0) }?.coerceAtLeast(0.0001) ?: 1.0

    val iskre = remember { Animatable(0f) }
    val skok = remember { Animatable(1f) }
    val zadnjiOnProslavaGotova by rememberUpdatedState(onProslavaGotova)
    LaunchedEffect(proslava) {
        if (!proslava || smanjeno) return@LaunchedEffect
        launch { skok.animateTo(1f, Opruge.razigrana(), initialVelocity = 2.4f) }
        iskre.snapTo(0f)
        iskre.animateTo(1f, tween(Trajanje.proslava, easing = Ublazavanje.usporavanje))
        iskre.snapTo(0f)
        zadnjiOnProslavaGotova()
    }

    // Prazni utori lagano "dišu" samo na praznom danu.
    val disanje: State<Float>? = if (danJePrazan && !smanjeno) {
        rememberInfiniteTransition(label = "disanjeUtora").animateFloat(
            initialValue = 0.45f,
            targetValue = 0.8f,
            animationSpec = infiniteRepeatable(tween(Trajanje.disanje), RepeatMode.Reverse),
            label = "disanje"
        )
    } else null

    val opisUtora = tanjuri.map { t ->
        val naziv = nazivTipa(t.tip)
        if (t.makro != null) stringResource(R.string.dp_utor_pun, naziv, formatKcal(t.makro.kcal))
        else stringResource(R.string.dp_utor_prazan, naziv)
    }
    val opisPladnja = stringResource(R.string.dp_pladanj_opis, opisUtora.joinToString(", "))

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MacroOblici.pladanj)
            .drawBehind {
                drawRect(boje.orah)
                // Zrno drva: tanke, blago nagnute linije
                val nagib = size.height * 0.08f
                for (i in 0 until BROJ_ZRNA) {
                    val x = size.width * (i + 0.5f) / BROJ_ZRNA + sin(i * 1.7f) * MacroDimenzije.zrnoNjihanje.toPx()
                    val boja = if (i % 3 == 0) boje.pladanjZrnoTamno else boje.pladanjZrnoSvijetlo
                    val debljina = if (i % 3 == 0) MacroDimenzije.zrnoDebelo.toPx() else MacroDimenzije.zrnoTanko.toPx()
                    drawLine(boja, Offset(x, 0f), Offset(x + nagib, size.height), debljina)
                }
                // Udubljeni unutarnji dio pladnja
                val okvir = MacroDimenzije.pladanjOkvir.toPx()
                val polumjer = CornerRadius(MacroDimenzije.utorPolumjer.toPx())
                drawRoundRect(boje.pladanjUdubljenje, Offset(okvir, okvir), Size(size.width - 2 * okvir, size.height - 2 * okvir), polumjer)
                drawRoundRect(
                    boje.pladanjRub, Offset(okvir, okvir), Size(size.width - 2 * okvir, size.height - 2 * okvir), polumjer,
                    style = Stroke(MacroDimenzije.linija.toPx())
                )
            }
            .drawWithContent {
                drawContent()
                val t = iskre.value
                if (t > 0f && t < 1f) {
                    // Krem iskre se šire od ruba pladnja prema van i nestaju.
                    val sredina = Offset(size.width / 2, size.height / 2)
                    for (i in 0 until BROJ_ISKRI) {
                        val kut = (i.toFloat() / BROJ_ISKRI) * 2f * Math.PI.toFloat() + 0.3f
                        val polazni = Offset(sredina.x + cos(kut) * size.width * 0.38f, sredina.y + sin(kut) * size.height * 0.36f)
                        val pomak = MacroDimenzije.iskraPomak.toPx() * t
                        drawCircle(
                            color = boje.naOrahu.copy(alpha = (1f - t) * 0.9f),
                            radius = MacroDimenzije.iskraPolumjer.toPx() * (1f - t * 0.5f),
                            center = Offset(polazni.x + cos(kut) * pomak, polazni.y + sin(kut) * pomak)
                        )
                    }
                }
            }
            .semantics { contentDescription = opisPladnja }
            .padding(MacroDimenzije.pladanjOkvir + MacroRazmaci.s)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            tanjuri.forEachIndexed { i, tanjur ->
                val udio = ((tanjur.makro?.kcal ?: 0.0) / ukupnoKcal.coerceAtLeast(1.0) / najveciUdio).toFloat().coerceIn(0f, 1f)
                val promjer = if (tanjur.jeUzina) lerp(MacroDimenzije.tanjurUzinaMin, MacroDimenzije.tanjurUzinaMax, udio)
                else lerp(MacroDimenzije.tanjurMin, MacroDimenzije.tanjurMax, udio)
                Utor(
                    tanjur = tanjur,
                    promjer = promjer,
                    indeks = i,
                    animirajUlaz = animirajUlaz,
                    smanjeno = smanjeno,
                    disanje = disanje,
                    skok = { skok.value },
                    onClick = { onTanjur(tanjur) },
                    modifier = Modifier.weight(if (tanjur.jeUzina) 0.75f else 1f)
                )
            }
        }

        Spacer(Modifier.height(MacroRazmaci.s))
        Legenda()
    }
}

@Composable
private fun Utor(
    tanjur: TanjurUi,
    promjer: Dp,
    indeks: Int,
    animirajUlaz: Boolean,
    smanjeno: Boolean,
    disanje: State<Float>?,
    skok: () -> Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val boje = MaterialTheme.macroBoje
    val gustoca = LocalDensity.current
    val pun = tanjur.makro != null
    val maxPromjer = if (tanjur.jeUzina) MacroDimenzije.tanjurUzinaMax else MacroDimenzije.tanjurMax

    // Sve vrijednosti crteža su Animatable i čitaju se u fazi crtanja (bez rekompozicije po kadru).
    val promjerAnim = remember { Animatable(promjer.value) }
    val (p, m, u) = tanjur.makro?.udjeli() ?: Triple(0f, 0f, 0f)
    val udioP = remember { Animatable(p) }
    val udioM = remember { Animatable(m) }
    val udioU = remember { Animatable(u) }
    val sletanje = remember { Animatable(if (pun && !animirajUlaz) 1f else 0f) }
    var prviPut by remember { mutableStateOf(true) }

    LaunchedEffect(promjer) { promjerAnim.animateTo(promjer.value, opruga(Opruge.blaga(), smanjeno)) }
    LaunchedEffect(p, m, u) {
        launch { udioP.animateTo(p, opruga(Opruge.blaga(), smanjeno)) }
        launch { udioM.animateTo(m, opruga(Opruge.blaga(), smanjeno)) }
        launch { udioU.animateTo(u, opruga(Opruge.blaga(), smanjeno)) }
    }
    LaunchedEffect(pun) {
        if (pun) {
            if (sletanje.value < 1f) {
                if (prviPut && animirajUlaz) delay((indeks * Kasnjenje.tanjuri).toLong())
                sletanje.animateTo(1f, opruga(Opruge.slijeganje(), smanjeno))
            }
        } else {
            sletanje.snapTo(0f)
        }
        prviPut = false
    }

    val naziv = nazivTipa(tanjur.tip)
    val opisKlika = if (pun) stringResource(R.string.dp_utor_pun, naziv, formatKcal(tanjur.makro!!.kcal))
    else stringResource(R.string.dp_dodaj_u, naziv)
    val izvor = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .clip(MacroOblici.utor)
            .clickable(interactionSource = izvor, indication = null, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = opisKlika }
            .skalaPritiska(izvor)
            .padding(vertical = MacroRazmaci.xs),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(MacroDimenzije.utorVisina),
            contentAlignment = Alignment.Center
        ) {
            // Prazan utor: isprekidani obris s ikonom "+" (nestaje dok tanjur slijeće)
            Canvas(modifier = Modifier.size(MacroDimenzije.tanjurPrazan)) {
                val alfa = (1f - sletanje.value).coerceIn(0f, 1f) * (disanje?.value ?: 0.7f)
                if (alfa > 0f) {
                    drawCircle(
                        color = boje.naOrahu.copy(alpha = alfa),
                        radius = size.minDimension / 2 - MacroDimenzije.obrubIsprekidan.toPx(),
                        style = Stroke(
                            width = MacroDimenzije.obrubIsprekidan.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(MacroDimenzije.crtica.toPx(), MacroDimenzije.crtica.toPx()))
                        )
                    )
                }
            }
            if (!pun) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    tint = boje.naOrahu.copy(alpha = disanje?.value ?: 0.8f),
                    modifier = Modifier.size(MacroDimenzije.ikona)
                )
            }

            // Tanjur
            Box(
                modifier = Modifier
                    .size(maxPromjer)
                    .graphicsLayer {
                        val s = sletanje.value
                        val skokSada = skok()
                        alpha = s.coerceIn(0f, 1f)
                        translationY = (1f - s) * -POMAK_TANJURA_DP.dp.toPx()
                        val skala = (SKALA_TANJURA + (1f - SKALA_TANJURA) * s) * skokSada
                        scaleX = skala
                        scaleY = skala
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val d = promjerAnim.value.dp.toPx()
                    val r = d / 2
                    val sredina = Offset(size.width / 2, size.height / 2)
                    drawCircle(boje.sjenaTanjura, r, sredina + Offset(0f, MacroDimenzije.sjenaTanjura.toPx()))
                    drawCircle(boje.tanjur, r, sredina)
                    drawCircle(
                        boje.tanjurUdubljenje, r * MacroOmjeri.udubljenjeTanjura, sredina,
                        style = Stroke(MacroDimenzije.obrubIsprekidan.toPx())
                    )

                    val debljinaRuba = d * MacroOmjeri.rubTanjura
                    val rRuba = r - debljinaRuba / 2 - debljinaRuba * 0.25f
                    val razmak = 360f * MacroOmjeri.razmakSegmenata
                    var kut = -90f
                    listOf(udioP.value to boje.tanjurProteini, udioM.value to boje.tanjurMasti, udioU.value to boje.tanjurUgljikohidrati)
                        .forEach { (udio, boja) ->
                            val raspon = 360f * udio
                            if (raspon > razmak) {
                                drawArc(
                                    color = boja,
                                    startAngle = kut + razmak / 2,
                                    sweepAngle = raspon - razmak,
                                    useCenter = false,
                                    topLeft = Offset(sredina.x - rRuba, sredina.y - rRuba),
                                    size = Size(rRuba * 2, rRuba * 2),
                                    style = Stroke(debljinaRuba, cap = StrokeCap.Butt)
                                )
                            }
                            kut += raspon
                        }
                }
                if (pun) {
                    val velicina = with(gustoca) { (promjer * MacroOmjeri.kcalTanjura).toSp() }
                    Text(
                        text = formatKcal(tanjur.makro!!.kcal),
                        style = MaterialTheme.macroTipografija.kcalTanjura.copy(fontSize = velicina, lineHeight = velicina),
                        color = boje.tanjurTekst,
                        maxLines = 1
                    )
                }
            }
        }

        Text(
            text = naziv,
            style = MaterialTheme.typography.labelLarge,
            color = boje.naOrahu,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = if (pun) tanjur.vrijeme.orEmpty() else stringResource(R.string.dp_dodaj),
            style = MaterialTheme.typography.labelMedium,
            color = boje.naOrahuPrigusen,
            maxLines = 1
        )
    }
}

/** Legenda se prelama u novi red pri velikom fontu. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Legenda() {
    val boje = MaterialTheme.macroBoje
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MacroRazmaci.m, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(MacroRazmaci.xs)
    ) {
        listOf(
            R.string.dp_proteini to boje.tanjurProteini,
            R.string.dp_masti to boje.tanjurMasti,
            R.string.dp_ugljikohidrati to boje.tanjurUgljikohidrati
        ).forEach { (tekst, boja) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Točka u malom tanjuru, kao na rubu pravog tanjura (vidljiva i na tamnom orahu)
                Canvas(modifier = Modifier.size(MacroDimenzije.legendaTocka + MacroDimenzije.xxsRub)) {
                    drawCircle(boje.tanjur)
                    drawCircle(boja, radius = size.minDimension / 2 - MacroDimenzije.xxsRub.toPx())
                }
                Spacer(Modifier.width(MacroRazmaci.xs))
                Text(
                    text = stringResource(tekst),
                    style = MaterialTheme.typography.labelSmall,
                    color = boje.naOrahuPrigusen,
                    maxLines = 1
                )
            }
        }
    }
}
