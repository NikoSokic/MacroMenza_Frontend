package com.niko.macromenza.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.niko.macromenza.R
import com.niko.macromenza.model.Jelo
import com.niko.macromenza.ui.components.GumbSpremi
import com.niko.macromenza.ui.components.KoracnikPorcija
import com.niko.macromenza.ui.components.Makro
import com.niko.macromenza.ui.components.TablicniBroj
import com.niko.macromenza.ui.components.kcalIMakroKratko
import com.niko.macromenza.ui.components.makroKratko
import com.niko.macromenza.ui.theme.MacroDimenzije
import com.niko.macromenza.ui.theme.MacroMenzaTheme
import com.niko.macromenza.ui.theme.MacroOblici
import com.niko.macromenza.ui.theme.MacroOmjeri
import com.niko.macromenza.ui.theme.MacroRazmaci
import com.niko.macromenza.ui.theme.Opruge
import com.niko.macromenza.ui.theme.POMAK_ULAZA_DP
import com.niko.macromenza.ui.theme.Trajanje
import com.niko.macromenza.ui.theme.Ublazavanje
import com.niko.macromenza.ui.theme.macroBoje
import com.niko.macromenza.ui.theme.macroTipografija
import com.niko.macromenza.ui.theme.opruga
import com.niko.macromenza.ui.theme.rememberSmanjeniPokret
import com.niko.macromenza.util.formatKcal
import com.niko.macromenza.viewmodel.OdabranaStavka
import com.niko.macromenza.viewmodel.SazetakObroka

/**
 * Stalni sažetak obroka pri dnu ekrana (porculan na pladnju, rub `outlineVariant`, bez sjene).
 * Tap ili povlačenje prema gore otvara listu odabranih stavki; lista izblijedi i klizne kroz graphicsLayer,
 * a visina se mijenja bez animacije. [onVisina] javlja visinu zatvorenog bara, da ekran može ostaviti prostor ispod liste.
 */
@Composable
fun SazetakObrokaBar(
    sazetak: SazetakObroka,
    stavke: List<OdabranaStavka>,
    spremanjeUTijeku: Boolean,
    onPlus: (Jelo) -> Unit,
    onMinus: (Long) -> Unit,
    onUkloni: (Long) -> Unit,
    onSpremi: () -> Unit,
    onVisina: (Dp) -> Unit,
    modifier: Modifier = Modifier,
    tekstSpremi: String = stringResource(R.string.uo_spremi),
    /** Samo za previewe: lista je odmah otvorena. */
    pocetnoOtvoren: Boolean = false
) {
    val shema = MaterialTheme.colorScheme
    val smanjeno = rememberSmanjeniPokret()
    val prazno = stavke.isEmpty()
    var otvoren by remember { mutableStateOf(pocetnoOtvoren) }

    // Zadnja stavka uklonjena: lista se zatvara.
    LaunchedEffect(prazno) {
        if (prazno) otvoren = false
    }

    BackHandler(enabled = otvoren) { otvoren = false }

    // Lista ostaje u kompoziciji dok ne izblijedi; zatim nestaje, a bar odmah poprimi novu visinu.
    val pregled = LocalInspectionMode.current
    val pojava = remember { Animatable(if (otvoren) 1f else 0f) }
    var listaPrikazana by remember { mutableStateOf(otvoren && !prazno) }
    LaunchedEffect(otvoren, prazno) {
        if (otvoren && !prazno) {
            listaPrikazana = true
            pojava.animateTo(1f, opruga(Opruge.slijeganje(), smanjeno || pregled))
        } else {
            pojava.animateTo(0f, if (smanjeno) snap() else tween(Trajanje.kratko, easing = Ublazavanje.ubrzavanje))
            listaPrikazana = false
        }
    }

    val density = LocalDensity.current
    val pragPx = with(density) { MacroDimenzije.pragPovlacenjaSazetka.toPx() }
    val pomakPx = with(density) { POMAK_ULAZA_DP.dp.toPx() }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MacroRazmaci.rubEkrana),
        shape = MacroOblici.pladanj,
        color = shema.surface,
        border = BorderStroke(MacroDimenzije.linija, shema.outlineVariant)
    ) {
        Column {
            if (listaPrikazana) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = MacroDimenzije.sazetakListaMaks)
                        .graphicsLayer {
                            alpha = pojava.value
                            translationY = (1f - pojava.value) * pomakPx
                        },
                    contentPadding = PaddingValues(
                        start = MacroRazmaci.l,
                        end = MacroRazmaci.xs,
                        top = MacroRazmaci.m
                    )
                ) {
                    itemsIndexed(stavke, key = { _, s -> s.jelo.id }) { i, stavka ->
                        StavkaSazetka(
                            stavka = stavka,
                            crta = i > 0,
                            smanjeno = smanjeno,
                            onPlus = { onPlus(stavka.jelo) },
                            onMinus = { onMinus(stavka.jelo.id) },
                            onUkloni = { onUkloni(stavka.jelo.id) },
                            modifier = Modifier.animateItem(
                                fadeInSpec = if (smanjeno) null else tween(Trajanje.kratko),
                                placementSpec = if (smanjeno) null else Opruge.slijeganje<IntOffset>(),
                                fadeOutSpec = if (smanjeno) null else tween(Trajanje.kratko)
                            )
                        )
                    }
                }
                HorizontalDivider(color = shema.outlineVariant, thickness = MacroDimenzije.linija)
            }

            val opisRadnje = stringResource(if (otvoren) R.string.uo_sazetak_sakrij else R.string.uo_sazetak_prikazi)
            val stanje = stringResource(if (otvoren) R.string.uo_sazetak_otvoren else R.string.uo_sazetak_zatvoren)
            Column(
                modifier = Modifier
                    .onSizeChanged { onVisina(with(density) { it.height.toDp() }) }
                    .pointerInput(prazno) {
                        if (prazno) return@pointerInput

                        var povuceno = 0f
                        detectVerticalDragGestures(
                            onDragStart = { povuceno = 0f },
                            onDragEnd = {
                                if (povuceno <= -pragPx) otvoren = true
                                else if (povuceno >= pragPx) otvoren = false
                            },
                            onDragCancel = { povuceno = 0f },
                            onVerticalDrag = { _, delta -> povuceno += delta }
                        )
                    }
                    .clickable(enabled = !prazno, onClickLabel = opisRadnje) { otvoren = !otvoren }
                    .then(if (prazno) Modifier else Modifier.semantics { stateDescription = stanje })
                    .padding(start = MacroRazmaci.l, end = MacroRazmaci.m, top = MacroRazmaci.s, bottom = MacroRazmaci.m)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(MacroDimenzije.rucka)
                        .background(shema.onSurfaceVariant.copy(alpha = MacroOmjeri.rucka), CircleShape)
                )

                Spacer(modifier = Modifier.height(MacroRazmaci.s))

                if (prazno) {
                    PrazanSazetak(tekstSpremi)
                } else {
                    PuniSazetak(
                        sazetak = sazetak,
                        smanjeno = smanjeno,
                        spremanjeUTijeku = spremanjeUTijeku,
                        tekstSpremi = tekstSpremi,
                        onSpremi = onSpremi
                    )
                }
            }
        }
    }
}

@Composable
private fun PrazanSazetak(tekstSpremi: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MacroDimenzije.dodirniCilj),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.uo_sazetak_prazno),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.width(MacroRazmaci.s))

        GumbSpremi(tekst = tekstSpremi, onClick = {}, enabled = false, punaSirina = false)
    }
}

@Composable
private fun PuniSazetak(
    sazetak: SazetakObroka,
    smanjeno: Boolean,
    spremanjeUTijeku: Boolean,
    tekstSpremi: String,
    onSpremi: () -> Unit
) {
    val shema = MaterialTheme.colorScheme
    val tip = MaterialTheme.typography

    // Zbroj prelazi oprugom kad se doda ili makne stavka; prvi prikaz je odmah na vrijednosti.
    val kcal = remember { Animatable(sazetak.kalorije.toFloat()) }
    LaunchedEffect(sazetak.kalorije) { kcal.animateTo(sazetak.kalorije.toFloat(), opruga(Opruge.blaga(), smanjeno)) }

    val makro = Makro(sazetak.kalorije, sazetak.proteini, sazetak.masti, sazetak.ugljikohidrati)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                TablicniBroj(
                    tekst = { formatKcal(kcal.value.toDouble()) },
                    stil = MaterialTheme.macroTipografija.brojSrednji,
                    boja = shema.onSurface
                )
                Spacer(Modifier.width(MacroRazmaci.xs))
                Text(
                    text = stringResource(R.string.uo_sazetak_kcal),
                    style = tip.bodySmall,
                    color = shema.onSurfaceVariant
                )
            }

            Text(
                text = stringResource(
                    R.string.uo_sazetak_stavke_makro,
                    pluralStringResource(R.plurals.kom_stavki, sazetak.brojStavki, sazetak.brojStavki),
                    makroKratko(makro)
                ),
                style = tip.bodySmall,
                color = shema.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(MacroRazmaci.s))

        GumbSpremi(
            tekst = tekstSpremi,
            onClick = onSpremi,
            sprema = spremanjeUTijeku,
            punaSirina = false
        )
    }

    val nakon = sazetak.nakonObroka
    if (nakon != null) {
        val cilj = sazetak.ciljKalorije
        val prelazi = cilj != null && nakon > cilj

        Spacer(modifier = Modifier.height(MacroRazmaci.xs))

        Text(
            text = buildString {
                append(
                    if (cilj != null) stringResource(R.string.uo_nakon_obroka, formatKcal(nakon), formatKcal(cilj))
                    else stringResource(R.string.uo_nakon_obroka_bez_cilja, formatKcal(nakon))
                )
                if (prelazi) {
                    append(stringResource(R.string.uo_razdjelnik))
                    append(stringResource(R.string.uo_preko_cilja))
                }
            },
            color = if (prelazi) MaterialTheme.macroBoje.visnja else shema.onSurfaceVariant,
            style = tip.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StavkaSazetka(
    stavka: OdabranaStavka,
    crta: Boolean,
    smanjeno: Boolean,
    onPlus: () -> Unit,
    onMinus: () -> Unit,
    onUkloni: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shema = MaterialTheme.colorScheme
    val k = stavka.kolicina
    val makro = with(stavka.jelo) { Makro(kalorije * k, proteini * k, masti * k, ugljikohidrati * k) }

    Column(modifier = modifier.fillMaxWidth()) {
        if (crta) HorizontalDivider(color = shema.outlineVariant, thickness = MacroDimenzije.linija)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = MacroRazmaci.s),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = stavka.jelo.naziv,
                modifier = Modifier
                    .weight(1f)
                    .padding(top = MacroRazmaci.m),
                style = MaterialTheme.typography.bodyLarge,
                color = shema.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            IconButton(onClick = onUkloni, modifier = Modifier.size(MacroDimenzije.dodirniCilj)) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.uo_ukloni_stavku, stavka.jelo.naziv),
                    tint = shema.onSurfaceVariant,
                    modifier = Modifier.size(MacroDimenzije.ikonaMala)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = MacroRazmaci.m, bottom = MacroRazmaci.s),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = kcalIMakroKratko(makro),
                modifier = Modifier.weight(1f, fill = false),
                style = MaterialTheme.typography.bodySmall,
                color = shema.onSurfaceVariant
            )

            Spacer(Modifier.width(MacroRazmaci.s))

            KoracnikPorcija(
                kolicina = k,
                onMinus = onMinus,
                onPlus = onPlus,
                smanjeno = smanjeno
            )
        }
    }
}

// ---------- Previewi (⚠ lažni podaci: prava jela iz menze, vrijednosti okvirne i ručno upisane) ----------

private fun primjerJela(id: Long, naziv: String, kcal: Double, p: Double, m: Double, u: Double) =
    Jelo(id, naziv, null, kcal, p, u, m, null, null, null, null)

private val STAVKE_PRIMJERA = listOf(
    OdabranaStavka(primjerJela(2, "Juha od krumpira", 172.0, 4.0, 7.0, 23.0), 1.0),
    OdabranaStavka(primjerJela(37, "Sarma", 452.0, 22.0, 29.0, 26.0), 1.5),
    OdabranaStavka(primjerJela(181, "Salata od svj. kupusa", 125.0, 2.0, 10.0, 7.0), 0.5)
)

private fun sazetakPrimjera(pojedeno: Double) = SazetakObroka(
    brojStavki = STAVKE_PRIMJERA.size,
    kalorije = STAVKE_PRIMJERA.sumOf { it.jelo.kalorije * it.kolicina },
    proteini = STAVKE_PRIMJERA.sumOf { it.jelo.proteini * it.kolicina },
    ugljikohidrati = STAVKE_PRIMJERA.sumOf { it.jelo.ugljikohidrati * it.kolicina },
    masti = STAVKE_PRIMJERA.sumOf { it.jelo.masti * it.kolicina },
    pojedenoKalorije = pojedeno,
    ciljKalorije = 2400.0
)

@Composable
private fun PreviewSazetak(tamna: Boolean, otvoren: Boolean, pojedeno: Double = 980.0, prazno: Boolean = false) {
    MacroMenzaTheme(darkTheme = tamna) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(vertical = MacroRazmaci.l)
        ) {
            SazetakObrokaBar(
                sazetak = if (prazno) SazetakObroka() else sazetakPrimjera(pojedeno),
                stavke = if (prazno) emptyList() else STAVKE_PRIMJERA,
                spremanjeUTijeku = false,
                onPlus = {},
                onMinus = {},
                onUkloni = {},
                onSpremi = {},
                onVisina = {},
                pocetnoOtvoren = otvoren
            )
        }
    }
}

@Preview(name = "Sažetak · svijetla · zatvoren", widthDp = 360)
@Composable
private fun PreviewSazetakSvijetlaZatvoren() = PreviewSazetak(tamna = false, otvoren = false)

@Preview(name = "Sažetak · tamna · zatvoren", widthDp = 360)
@Composable
private fun PreviewSazetakTamnaZatvoren() = PreviewSazetak(tamna = true, otvoren = false)

@Preview(name = "Sažetak · svijetla · otvoren", widthDp = 360)
@Composable
private fun PreviewSazetakSvijetlaOtvoren() = PreviewSazetak(tamna = false, otvoren = true)

@Preview(name = "Sažetak · tamna · otvoren", widthDp = 360)
@Composable
private fun PreviewSazetakTamnaOtvoren() = PreviewSazetak(tamna = true, otvoren = true)

@Preview(name = "Sažetak · svijetla · preko cilja", widthDp = 360)
@Composable
private fun PreviewSazetakSvijetlaPreko() = PreviewSazetak(tamna = false, otvoren = false, pojedeno = 1980.0)

@Preview(name = "Sažetak · tamna · preko cilja", widthDp = 360)
@Composable
private fun PreviewSazetakTamnaPreko() = PreviewSazetak(tamna = true, otvoren = false, pojedeno = 1980.0)

@Preview(name = "Sažetak · svijetla · prazno", widthDp = 360)
@Composable
private fun PreviewSazetakSvijetlaPrazno() = PreviewSazetak(tamna = false, otvoren = false, prazno = true)

@Preview(name = "Sažetak · otvoren · fontScale 1,3", widthDp = 360, fontScale = 1.3f)
@Composable
private fun PreviewSazetakOtvorenVelikiFont() = PreviewSazetak(tamna = false, otvoren = true)

@Preview(name = "Sažetak · preko cilja · fontScale 1,3", widthDp = 360, fontScale = 1.3f)
@Composable
private fun PreviewSazetakPrekoVelikiFont() = PreviewSazetak(tamna = true, otvoren = false, pojedeno = 1980.0)
