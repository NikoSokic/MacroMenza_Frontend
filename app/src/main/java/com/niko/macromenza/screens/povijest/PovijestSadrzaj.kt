package com.niko.macromenza.screens.povijest

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.niko.macromenza.R
import com.niko.macromenza.model.Obrok
import com.niko.macromenza.model.PovijestDana
import com.niko.macromenza.screens.ObrociDana
import com.niko.macromenza.ui.components.screenBottomPadding
import com.niko.macromenza.ui.components.screenTopPadding
import com.niko.macromenza.ui.components.skalaPritiska
import com.niko.macromenza.ui.theme.Kasnjenje
import com.niko.macromenza.ui.theme.MacroDimenzije
import com.niko.macromenza.ui.theme.MacroMenzaTheme
import com.niko.macromenza.ui.theme.MacroOblici
import com.niko.macromenza.ui.theme.MacroOmjeri
import com.niko.macromenza.ui.theme.MacroRazmaci
import com.niko.macromenza.ui.theme.Opruge
import com.niko.macromenza.ui.theme.POMAK_TJEDNA_DP
import com.niko.macromenza.ui.theme.POMAK_ULAZA_DP
import com.niko.macromenza.ui.theme.Trajanje
import com.niko.macromenza.ui.theme.Ublazavanje
import com.niko.macromenza.ui.theme.macroBoje
import com.niko.macromenza.ui.theme.opruga
import com.niko.macromenza.ui.theme.rememberSmanjeniPokret
import com.niko.macromenza.util.formatGrami
import com.niko.macromenza.util.formatKcal
import com.niko.macromenza.util.formatKcalTisuce
import com.niko.macromenza.util.formatMakro
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Nakon ovoliko ms ulazni slijed je odigran (cijeli slijed traje 610 ms). */
private const val TRAJANJE_ULAZA = 1000L

/** Redoslijed elemenata u ulaznom slijedu: naslov, izbor, navigacija, sadržaj. */
private const val ULAZ_NASLOV = 0
private const val ULAZ_IZBOR = 1
private const val ULAZ_NAVIGACIJA = 2
private const val ULAZ_SADRZAJ = 3

/** Radnje ekrana; PovijestScreen ih spaja na ViewModel i navigaciju, previewi ostavljaju prazne. */
@Immutable
class PovijestAkcije(
    val onTab: (PovijestTab) -> Unit = {},
    val onPrethodniTjedan: () -> Unit = {},
    val onSljedeciTjedan: () -> Unit = {},
    val onOvajTjedan: () -> Unit = {},
    val onPonovi: () -> Unit = {},
    val onUcitajPrimjer: () -> Unit = {},
    val onUkloniPrimjer: () -> Unit = {},
    val onOtvoriDan: (String) -> Unit = {},
    val onPrethodniDan: () -> Unit = {},
    val onSljedeciDan: () -> Unit = {},
    val onUredi: (Obrok) -> Unit = {},
    val onObrisi: (Obrok) -> Unit = {}
)

/**
 * Tab "Povijest" bez ViewModela (design-system/macromenza/pages/povijest.md).
 * [puniUlaz] je istina samo pri prvom otvaranju u sesiji: naslov, izbor, navigacija i sadržaj ulaze stupnjevano.
 */
@Composable
fun PovijestSadrzaj(
    stanje: TjedanStanje,
    odabraniTab: PovijestTab,
    primjerUTijeku: Boolean,
    odabraniDatum: String?,
    povijestDana: PovijestDana?,
    obrociDana: List<Obrok>,
    akcije: PovijestAkcije,
    modifier: Modifier = Modifier,
    puniUlaz: Boolean = true,
    snackbarHost: @Composable BoxScope.() -> Unit = {}
) {
    val smanjeno = rememberSmanjeniPokret()
    val pregledAlata = LocalInspectionMode.current
    val scope = rememberCoroutineScope()

    var ulazOdigran by rememberSaveable { mutableStateOf(false) }
    val animirajUlaz = puniUlaz && !ulazOdigran && !smanjeno && !pregledAlata
    LaunchedEffect(Unit) {
        delay(TRAJANJE_ULAZA)
        ulazOdigran = true
    }

    val tjedan = (stanje as? TjedanStanje.Sadrzaj)?.tjedan

    // Listanje: -1 = sadržaj otišao ulijevo, 0 = na mjestu, 1 = udesno. Jedna vrijednost, pa je svaki novi dodir
    // nastavak od trenutne pozicije; ViewModel prekida stari zahtjev, pa stiže samo zadnji tjedan.
    val listanje = remember { Animatable(0f) }
    var smjer by remember { mutableIntStateOf(0) }
    var cekaTjedan by remember { mutableStateOf(false) }

    fun listaj(s: Int, radnja: () -> Unit) {
        if (!smanjeno) {
            smjer = s
            cekaTjedan = true
            scope.launch { listanje.animateTo(-s.toFloat(), Opruge.slijeganje()) }
        }
        radnja()
    }

    // Granica ne pokreće izlaz: inače bi sadržaj ostao skriven jer novi tjedan ne dolazi.
    val prethodni by rememberUpdatedState { if (tjedan?.imaPrethodni == true) listaj(-1, akcije.onPrethodniTjedan) }
    val sljedeci by rememberUpdatedState { if (tjedan?.imaSljedeci == true) listaj(1, akcije.onSljedeciTjedan) }
    val ovaj = { if (tjedan?.jeTekuci == false) listaj(1, akcije.onOvajTjedan) }

    LaunchedEffect(tjedan?.pocetak) {
        if (tjedan != null && cekaTjedan) {
            cekaTjedan = false
            listanje.snapTo(smjer.toFloat())
            listanje.animateTo(0f, Opruge.slijeganje())
        }
    }

    val pragListanja = with(LocalDensity.current) { MacroDimenzije.pragListanja.toPx() }
    val naTjednu = odabraniTab == PovijestTab.TJEDNI && tjedan != null

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                // Povlačenje lijevo/desno na tabu "Tjedan" mijenja tjedan (desno = prethodni); strelice rade isto.
                .pointerInput(naTjednu) {
                    if (!naTjednu) return@pointerInput
                    var pomak = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { pomak = 0f },
                        onDragEnd = {
                            when {
                                pomak > pragListanja -> prethodni()
                                pomak < -pragListanja -> sljedeci()
                            }
                        }
                    ) { _, dragAmount ->
                        pomak += dragAmount
                    }
                },
            contentPadding = PaddingValues(
                top = screenTopPadding(),
                bottom = screenBottomPadding()
            )
        ) {
            item(key = "naslov") {
                Text(
                    text = stringResource(R.string.pov_naslov),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .padding(horizontal = MacroRazmaci.rubEkrana)
                        .semantics { heading() }
                        .ulaz(ULAZ_NASLOV, animirajUlaz)
                )
            }
            item(key = "izbor") {
                IzborTaba(
                    odabrani = odabraniTab,
                    smanjeno = smanjeno,
                    onOdabir = akcije.onTab,
                    modifier = Modifier
                        .padding(start = MacroRazmaci.rubEkrana, end = MacroRazmaci.rubEkrana, top = MacroRazmaci.l)
                        .ulaz(ULAZ_IZBOR, animirajUlaz)
                )
            }

            if (odabraniTab == PovijestTab.TJEDNI) {
                when (stanje) {
                    TjedanStanje.Ucitavanje -> item(key = "kostur") {
                        Kostur(smanjeno, prijelaz(smanjeno).ulaz(ULAZ_NAVIGACIJA, animirajUlaz))
                    }

                    TjedanStanje.Greska -> item(key = "greska") {
                        Greska(akcije.onPonovi, prijelaz(smanjeno).ulaz(ULAZ_NAVIGACIJA, animirajUlaz))
                    }

                    is TjedanStanje.Sadrzaj -> {
                        val t = stanje.tjedan
                        item(key = "navigacija") {
                            NavigacijaTjedna(
                                tjedan = t,
                                onPrethodni = prethodni,
                                onSljedeci = sljedeci,
                                onOvajTjedan = ovaj,
                                modifier = prijelaz(smanjeno).ulaz(ULAZ_NAVIGACIJA, animirajUlaz)
                            )
                        }

                        // Sve ispod navigacije lista zajedno.
                        fun Modifier.sadrzajTjedna() = this
                            .padding(horizontal = MacroRazmaci.rubEkrana)
                            .listanjeTjedna(listanje)
                            .ulaz(ULAZ_SADRZAJ, animirajUlaz)

                        if (t.imaPrimjer) {
                            item(key = "primjer") {
                                PrimjerTraka(
                                    uTijeku = primjerUTijeku,
                                    onUkloni = akcije.onUkloniPrimjer,
                                    modifier = prijelaz(smanjeno).sadrzajTjedna()
                                )
                            }
                        }

                        if (t.jePrazan) {
                            item(key = "prazno") {
                                PrazanTjedan(
                                    mozePrimjer = t.mozePrimjer,
                                    primjerUTijeku = primjerUTijeku,
                                    onUcitajPrimjer = akcije.onUcitajPrimjer,
                                    modifier = prijelaz(smanjeno)
                                        .sadrzajTjedna()
                                        .padding(top = MacroRazmaci.s)
                                )
                            }
                        } else {
                            item(key = "graf") {
                                TjedniGrafCard(
                                    tjedan = t,
                                    onOtvoriDan = akcije.onOtvoriDan,
                                    modifier = prijelaz(smanjeno)
                                        .sadrzajTjedna()
                                        .padding(top = MacroRazmaci.s)
                                )
                            }
                            item(key = "statistika") {
                                TjednaStatistikaCard(
                                    tjedan = t,
                                    modifier = prijelaz(smanjeno)
                                        .sadrzajTjedna()
                                        .padding(top = MacroRazmaci.l)
                                )
                            }
                        }
                    }
                }
            } else {
                item(key = "dan_izbor") {
                    DaySelector(
                        datum = odabraniDatum,
                        onPrevious = akcije.onPrethodniDan,
                        onNext = akcije.onSljedeciDan,
                        modifier = prijelaz(smanjeno)
                            .padding(horizontal = MacroRazmaci.xs, vertical = MacroRazmaci.s)
                    )
                }

                povijestDana?.let { dan ->
                    val imaObroka = dan.obroci.any { (_, stavke) -> stavke.isNotEmpty() }

                    if (!imaObroka) {
                        item(key = "dan_prazno") {
                            EmptyDayCard(prijelaz(smanjeno).padding(horizontal = MacroRazmaci.rubEkrana))
                        }
                    } else {
                        item(key = "dan_zbroj") {
                            DailySummaryCard(
                                kalorije = dan.ukupnoKalorije,
                                proteini = dan.ukupnoProteini,
                                uh = dan.ukupnoUgljikohidrati,
                                masti = dan.ukupnoMasti,
                                modifier = prijelaz(smanjeno).padding(horizontal = MacroRazmaci.rubEkrana)
                            )
                        }
                        item(key = "dan_obroci") {
                            Box(
                                prijelaz(smanjeno).padding(start = MacroRazmaci.rubEkrana, end = MacroRazmaci.rubEkrana, top = MacroRazmaci.l)
                            ) {
                                ObrociDana(
                                    obroci = obrociDana,
                                    onUredi = akcije.onUredi,
                                    onObrisi = akcije.onObrisi
                                )
                            }
                        }
                    }
                }
            }
        }

        snackbarHost()
    }
}

/** Promjena stanja ili taba: stavke izblijede i klize na mjesto umjesto skoka. */
private fun LazyItemScope.prijelaz(smanjeno: Boolean): Modifier =
    if (smanjeno) Modifier else Modifier.animateItem(
        fadeInSpec = tween(Trajanje.kratko, easing = Ublazavanje.usporavanje),
        placementSpec = Opruge.slijeganje(),
        fadeOutSpec = tween(Trajanje.kratko, easing = Ublazavanje.ubrzavanje)
    )

/** Pomak i alfa sadržaja tjedna pri listanju; vrijednost se čita u fazi crtanja. */
private fun Modifier.listanjeTjedna(listanje: Animatable<Float, *>): Modifier = graphicsLayer {
    val v = listanje.value
    translationX = v * POMAK_TJEDNA_DP.dp.toPx()
    alpha = (1f - abs(v)).coerceIn(0f, 1f)
}

/** Prvi ulaz u sesiji: izblijeđivanje + pomak 12 dp nakon prijelaza ekrana, stupnjevano (samo graphicsLayer). */
private fun Modifier.ulaz(indeks: Int, aktivno: Boolean): Modifier = composed {
    val napredak = remember { Animatable(if (aktivno) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (napredak.value < 1f) {
            delay((Kasnjenje.nakonPrijelaza + indeks * Kasnjenje.korak).toLong())
            napredak.animateTo(1f, tween(Trajanje.srednje, easing = Ublazavanje.usporavanje))
        }
    }
    graphicsLayer {
        alpha = napredak.value
        translationY = (1f - napredak.value) * POMAK_ULAZA_DP.dp.toPx()
    }
}

/** Segmentirani izbor Tjedan/Dan: odabir je stanje (secondaryContainer), indikator klizi kroz graphicsLayer. */
@Composable
private fun IzborTaba(
    odabrani: PovijestTab,
    smanjeno: Boolean,
    onOdabir: (PovijestTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val shema = MaterialTheme.colorScheme
    val polozaj by animateFloatAsState(
        targetValue = if (odabrani == PovijestTab.DNEVNI) 1f else 0f,
        animationSpec = opruga(Opruge.blaga(), smanjeno),
        label = "indikatorTaba"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(MacroDimenzije.dodirniCilj + MacroRazmaci.s)
            .background(shema.surface, MacroOblici.pilula)
            .border(MacroDimenzije.linija, shema.outlineVariant, MacroOblici.pilula)
            .padding(MacroRazmaci.xs)
    ) {
        Box(
            Modifier
                .fillMaxWidth(0.5f)
                .fillMaxHeight()
                .graphicsLayer { translationX = polozaj * size.width }
                .background(shema.secondaryContainer, MacroOblici.pilula)
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .selectableGroup()
        ) {
            listOf(
                PovijestTab.TJEDNI to R.string.pov_tab_tjedan,
                PovijestTab.DNEVNI to R.string.pov_tab_dan
            ).forEach { (tab, tekst) ->
                val odabran = tab == odabrani
                val boja by animateColorAsState(
                    targetValue = if (odabran) shema.onSecondaryContainer else shema.onSurfaceVariant,
                    animationSpec = tween(if (smanjeno) 0 else Trajanje.kratko),
                    label = "bojaTaba"
                )
                val izvor = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(MacroOblici.pilula)
                        .selectable(
                            selected = odabran,
                            onClick = { if (!odabran) onOdabir(tab) },
                            role = Role.Tab,
                            interactionSource = izvor,
                            indication = null
                        )
                        .skalaPritiska(izvor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(tekst), style = MaterialTheme.typography.labelLarge, color = boja)
                }
            }
        }
    }
}

/** ‹ raspon › kao gornja traka na Home; "Ovaj tjedan" je link samo kad prikazani tjedan nije tekući. */
@Composable
private fun NavigacijaTjedna(
    tjedan: TjedanUi,
    onPrethodni: () -> Unit,
    onSljedeci: () -> Unit,
    onOvajTjedan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shema = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = MacroRazmaci.xs, end = MacroRazmaci.xs, top = MacroRazmaci.s),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrethodni, enabled = tjedan.imaPrethodni) {
            Icon(Icons.Rounded.ChevronLeft, contentDescription = stringResource(R.string.pov_prethodni_tjedan))
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = tjedan.raspon,
                style = MaterialTheme.typography.titleMedium,
                color = shema.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() }
            )
            // Ista visina u oba slučaja, pa se raspon ne pomiče pri prelasku na tekući tjedan.
            Box(
                modifier = Modifier.heightIn(min = MacroDimenzije.dodirniCilj),
                contentAlignment = Alignment.Center
            ) {
                if (tjedan.jeTekuci) {
                    Text(
                        text = stringResource(R.string.pov_ovaj_tjedan),
                        style = MaterialTheme.typography.labelMedium,
                        color = shema.onSurfaceVariant
                    )
                } else {
                    val izvor = remember { MutableInteractionSource() }
                    TextButton(
                        onClick = onOvajTjedan,
                        interactionSource = izvor,
                        contentPadding = PaddingValues(horizontal = MacroRazmaci.m),
                        modifier = Modifier
                            .heightIn(min = MacroDimenzije.dodirniCilj)
                            .skalaPritiska(izvor)
                    ) {
                        Text(stringResource(R.string.pov_ovaj_tjedan), style = MaterialTheme.typography.labelLarge, color = shema.primary)
                    }
                }
            }
        }
        IconButton(onClick = onSljedeci, enabled = tjedan.imaSljedeci) {
            Icon(Icons.Rounded.ChevronRight, contentDescription = stringResource(R.string.pov_sljedeci_tjedan))
        }
    }
}

/** Tihi red bez pozadine; "Ukloni primjer" je onemogućen dok traje učitavanje ili uklanjanje. */
@Composable
private fun PrimjerTraka(
    uTijeku: Boolean,
    onUkloni: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shema = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Info, contentDescription = null, tint = shema.onSurfaceVariant, modifier = Modifier.size(MacroDimenzije.ikonaSitna))
        Spacer(Modifier.width(MacroRazmaci.s))
        Text(
            text = stringResource(R.string.pov_primjer_prikazan),
            style = MaterialTheme.typography.bodyMedium,
            color = shema.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        val izvor = remember { MutableInteractionSource() }
        TextButton(
            onClick = onUkloni,
            enabled = !uTijeku,
            interactionSource = izvor,
            contentPadding = PaddingValues(horizontal = MacroRazmaci.s),
            modifier = Modifier
                .heightIn(min = MacroDimenzije.dodirniCilj)
                .skalaPritiska(izvor)
        ) {
            Text(
                text = stringResource(R.string.pov_ukloni_primjer),
                style = MaterialTheme.typography.labelLarge,
                color = if (uTijeku) shema.onSurface.copy(alpha = 0.38f) else shema.primary
            )
        }
    }
}

@Composable
private fun PrazanTjedan(
    mozePrimjer: Boolean,
    primjerUTijeku: Boolean,
    onUcitajPrimjer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shema = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(MacroDimenzije.obrubIsprekidan, shema.outlineVariant, MacroOblici.kartica)
            .padding(MacroRazmaci.xl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.pov_prazno_naslov),
            style = MaterialTheme.typography.titleMedium,
            color = shema.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(MacroRazmaci.xs))
        Text(
            text = stringResource(R.string.pov_prazno_tekst),
            style = MaterialTheme.typography.bodyMedium,
            color = shema.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (mozePrimjer) {
            Spacer(Modifier.height(MacroRazmaci.l))
            val izvor = remember { MutableInteractionSource() }
            FilledTonalButton(
                onClick = onUcitajPrimjer,
                enabled = !primjerUTijeku,
                interactionSource = izvor,
                shape = MacroOblici.gumb,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = shema.secondaryContainer,
                    contentColor = shema.onSecondaryContainer
                ),
                modifier = Modifier
                    .heightIn(min = MacroDimenzije.dodirniCilj)
                    .skalaPritiska(izvor)
            ) {
                Text(stringResource(R.string.pov_ucitaj_primjer), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** Učitavanje: kostur navigacije, reda od 7 tanjura i traka (bez spinnera), s blagim pulsiranjem. */
@Composable
private fun Kostur(smanjeno: Boolean, modifier: Modifier = Modifier) {
    val boje = MaterialTheme.macroBoje
    val opis = stringResource(R.string.pov_ucitavanje)
    val puls = if (smanjeno) null else rememberInfiniteTransition(label = "kostur").animateFloat(
        initialValue = 0.55f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(Trajanje.disanje / 2), RepeatMode.Reverse),
        label = "puls"
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MacroRazmaci.rubEkrana)
            .graphicsLayer { alpha = puls?.value ?: 1f }
            .semantics { contentDescription = opis }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = MacroRazmaci.s),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(Modifier.size(MacroDimenzije.dodirniCilj).background(boje.kostur, CircleShape))
            Box(
                Modifier
                    .fillMaxWidth(MacroOmjeri.kosturNaziv)
                    .height(MacroDimenzije.kosturNaziv)
                    .background(boje.kostur, MacroOblici.traka)
            )
            Box(Modifier.size(MacroDimenzije.dodirniCilj).background(boje.kostur, CircleShape))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = MacroRazmaci.xl),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            repeat(7) {
                Box(Modifier.size(MacroDimenzije.tanjurTjedanMax).background(boje.kostur, CircleShape))
            }
        }
        Spacer(Modifier.height(MacroRazmaci.iznadSekcije))
        repeat(3) {
            Box(
                Modifier
                    .padding(top = MacroRazmaci.l)
                    .fillMaxWidth()
                    .height(MacroDimenzije.oznakaCiljaVisina)
                    .background(boje.kostur, MacroOblici.traka)
            )
        }
    }
}

@Composable
private fun Greska(onPonovi: () -> Unit, modifier: Modifier = Modifier) {
    val shema = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(MacroRazmaci.rubEkrana)
            .background(shema.surface, MacroOblici.pladanj)
            .border(MacroDimenzije.linija, shema.outlineVariant, MacroOblici.pladanj)
            .padding(MacroRazmaci.xl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Rounded.CloudOff, contentDescription = null, tint = shema.onSurfaceVariant, modifier = Modifier.size(MacroDimenzije.ikona))
        Spacer(Modifier.height(MacroRazmaci.m))
        Text(
            stringResource(R.string.pov_greska_naslov),
            style = MaterialTheme.typography.titleMedium,
            color = shema.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(MacroRazmaci.xs))
        Text(
            stringResource(R.string.pov_greska_tekst),
            style = MaterialTheme.typography.bodyMedium,
            color = shema.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(MacroRazmaci.l))
        val izvor = remember { MutableInteractionSource() }
        OutlinedButton(
            onClick = onPonovi,
            interactionSource = izvor,
            shape = MacroOblici.gumb,
            modifier = Modifier
                .heightIn(min = MacroDimenzije.dodirniCilj)
                .skalaPritiska(izvor)
        ) {
            Text(stringResource(R.string.pov_pokusaj_ponovno), style = MaterialTheme.typography.labelLarge, color = shema.onSurface)
        }
    }
}

// ---------- Stare kartice: struktura ostaje do dijelova 2–4 (pages/povijest.md), boje su na tokenima ----------

/**
 * Stupčasti graf kalorija po danu s isprekidanom linijom cilja. Prvi dodir na stupac pokazuje kcal,
 * drugi dodir na isti stupac (ili "Otvori dan") otvara karticu "Dan". Dani u budućnosti se ne mogu odabrati.
 */
@Composable
private fun TjedniGrafCard(
    tjedan: TjedanUi,
    onOtvoriDan: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val dani = tjedan.dani
    val oznakeDana = stringArrayResource(R.array.pov_oznake_dana)
    val naziviDana = stringArrayResource(R.array.pov_kratki_dani)

    var odabraniIndeks by rememberSaveable(tjedan.pocetak) {
        mutableStateOf<Int?>(null)
    }

    val shema = MaterialTheme.colorScheme
    val bojaUCilju = MaterialTheme.macroBoje.lukIspuna
    val bojaIzvanCilja = shema.onSurfaceVariant.copy(alpha = 0.35f)
    val bojaObrisa = shema.onSurfaceVariant.copy(alpha = 0.45f)
    val bojaCilja = shema.onSurface.copy(alpha = 0.55f)
    val bojaOdabira = shema.secondaryContainer
    val bojaOznake = shema.onSurface
    val bojaPodloge = shema.surface

    val textMeasurer = rememberTextMeasurer()
    val stilOznake = MaterialTheme.typography.labelMedium.copy(
        color = bojaOznake,
        fontWeight = FontWeight.Bold
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MacroOblici.pladanj,
        color = shema.surface
    ) {

        Column(
            modifier = Modifier.padding(20.dp)
        ) {

            Text(
                text = stringResource(R.string.pov_kalorije_po_danima),
                style = MaterialTheme.typography.titleMedium,
                color = shema.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .pointerInput(dani) {
                        detectTapGestures { pozicija ->
                            val indeks = (pozicija.x / (size.width / 7f)).toInt().coerceIn(0, 6)
                            val dan = dani.getOrNull(indeks) ?: return@detectTapGestures

                            if (dan.uBuducnosti) return@detectTapGestures

                            if (odabraniIndeks == indeks) {
                                onOtvoriDan(dan.datum.toString())
                            } else {
                                odabraniIndeks = indeks
                            }
                        }
                    }
            ) {
                val maxVrijednost = maxOf(
                    dani.maxOfOrNull { it.makro.kcal } ?: 0.0,
                    dani.maxOfOrNull { it.ciljKcal ?: 0.0 } ?: 0.0,
                    1.0
                ) * 1.12

                val prostorOznake = 22.dp.toPx()
                val visinaGrafa = size.height - prostorOznake
                val sirinaSlota = size.width / 7f
                val sirinaStupca = minOf(sirinaSlota * 0.56f, 30.dp.toPx())
                val radijus = CornerRadius(8.dp.toPx())
                val crtica = 6.dp.toPx()
                val razmak = 5.dp.toPx()

                fun y(vrijednost: Double): Float =
                    size.height - (vrijednost / maxVrijednost * visinaGrafa).toFloat()

                dani.forEachIndexed { indeks, dan ->
                    val lijevo = indeks * sirinaSlota + (sirinaSlota - sirinaStupca) / 2f

                    if (indeks == odabraniIndeks) {
                        drawRoundRect(
                            color = bojaOdabira,
                            topLeft = Offset(indeks * sirinaSlota + 2.dp.toPx(), 0f),
                            size = Size(sirinaSlota - 4.dp.toPx(), size.height),
                            cornerRadius = CornerRadius(10.dp.toPx())
                        )
                    }

                    if (dan.imaUnos) {
                        val visina = maxOf(size.height - y(dan.makro.kcal), 4.dp.toPx())

                        drawRoundRect(
                            color = if (dan.uCilju) bojaUCilju else bojaIzvanCilja,
                            topLeft = Offset(lijevo, size.height - visina),
                            size = Size(sirinaStupca, visina),
                            cornerRadius = radijus
                        )
                    } else {
                        val visina = 10.dp.toPx()
                        val debljina = 1.5.dp.toPx()
                        val vrhStupca = size.height - visina

                        drawRoundRect(
                            color = bojaObrisa,
                            topLeft = Offset(lijevo + debljina / 2f, vrhStupca + debljina / 2f),
                            size = Size(sirinaStupca - debljina, visina - debljina),
                            cornerRadius = CornerRadius(4.dp.toPx()),
                            style = Stroke(width = debljina)
                        )
                    }

                    // Cilj se crta po danu (preporuka se može promijeniti usred tjedna); faza drži crtice u nizu.
                    dan.ciljKcal?.let { cilj ->
                        val yCilja = y(cilj)
                        drawLine(
                            color = bojaCilja,
                            start = Offset(indeks * sirinaSlota, yCilja),
                            end = Offset((indeks + 1) * sirinaSlota, yCilja),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(crtica, razmak),
                                (indeks * sirinaSlota) % (crtica + razmak)
                            )
                        )
                    }
                }

                // Oznaka se crta zadnja, s podlogom, da je linija cilja ne prekriži.
                odabraniIndeks?.let { indeks ->
                    val dan = dani.getOrNull(indeks) ?: return@let
                    val tekst = if (dan.imaUnos) formatKcalTisuce(dan.makro.kcal) else "0"
                    val izmjereno = textMeasurer.measure(tekst, stilOznake)
                    val ruba = 4.dp.toPx()
                    val sirina = izmjereno.size.width + 2 * ruba
                    val visina = izmjereno.size.height.toFloat()
                    val vrhStupca = if (dan.imaUnos) {
                        size.height - maxOf(size.height - y(dan.makro.kcal), 4.dp.toPx())
                    } else {
                        size.height - 10.dp.toPx()
                    }
                    val x = (indeks * sirinaSlota + (sirinaSlota - sirina) / 2f).coerceIn(0f, size.width - sirina)
                    val yOznake = (vrhStupca - visina - 4.dp.toPx()).coerceAtLeast(0f)

                    drawRoundRect(
                        color = bojaPodloge,
                        topLeft = Offset(x, yOznake),
                        size = Size(sirina, visina),
                        cornerRadius = CornerRadius(6.dp.toPx())
                    )
                    drawText(izmjereno, topLeft = Offset(x + ruba, yOznake))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                dani.forEachIndexed { indeks, dan ->
                    Text(
                        text = oznakeDana.getOrElse(indeks) { "" },
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (dan.jeDanas || indeks == odabraniIndeks) FontWeight.Bold else FontWeight.Normal,
                        color = if (dan.jeDanas) shema.onSurface else shema.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LegendaGrafa(
                bojaUCilju = bojaUCilju,
                bojaIzvanCilja = bojaIzvanCilja,
                bojaObrisa = bojaObrisa,
                bojaCilja = bojaCilja
            )

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(color = shema.outlineVariant)

            Spacer(modifier = Modifier.height(8.dp))

            val odabrani = odabraniIndeks?.let { dani.getOrNull(it) }

            if (odabrani == null) {
                Text(
                    text = stringResource(R.string.pov_dodirni_stupac),
                    style = MaterialTheme.typography.bodySmall,
                    color = shema.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 10.dp)
                )
            } else {
                OdabraniDanRed(
                    dan = odabrani,
                    naziv = naziviDana.getOrElse(odabraniIndeks ?: 0) { "" },
                    onOtvori = { onOtvoriDan(odabrani.datum.toString()) }
                )
            }
        }
    }
}

@Composable
private fun OdabraniDanRed(
    dan: DanTjednaUi,
    naziv: String,
    onOtvori: () -> Unit
) {
    val shema = MaterialTheme.colorScheme

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.pov_dan_datum, naziv, dan.datum.dayOfMonth, dan.datum.monthValue),
                style = MaterialTheme.typography.titleSmall,
                color = shema.onSurface
            )

            val cilj = dan.ciljKcal?.let { stringResource(R.string.pov_dan_cilj, formatKcalTisuce(it)) } ?: ""

            Text(
                text = if (dan.imaUnos) stringResource(R.string.pov_dan_kcal, formatKcalTisuce(dan.makro.kcal), cilj)
                else stringResource(R.string.pov_dan_nema_unosa, cilj),
                style = MaterialTheme.typography.bodySmall,
                color = if (dan.uCilju) shema.onSurface else shema.onSurfaceVariant
            )
        }

        TextButton(onClick = onOtvori) {
            Text(
                text = stringResource(R.string.pov_otvori_dan),
                style = MaterialTheme.typography.labelLarge,
                color = shema.primary
            )

            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = shema.primary,
                modifier = Modifier.size(MacroDimenzije.ikonaMala)
            )
        }
    }
}

@Composable
private fun LegendaGrafa(
    bojaUCilju: Color,
    bojaIzvanCilja: Color,
    bojaObrisa: Color,
    bojaCilja: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StavkaLegende(tekst = stringResource(R.string.pov_legenda_u_cilju)) {
            Box(Modifier.size(10.dp).clip(MacroOblici.traka).background(bojaUCilju))
        }
        StavkaLegende(tekst = stringResource(R.string.pov_legenda_izvan_cilja)) {
            Box(Modifier.size(10.dp).clip(MacroOblici.traka).background(bojaIzvanCilja))
        }
        StavkaLegende(tekst = stringResource(R.string.pov_legenda_bez_unosa)) {
            Canvas(Modifier.size(10.dp)) {
                drawRoundRect(
                    color = bojaObrisa,
                    cornerRadius = CornerRadius(3.dp.toPx()),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }
        StavkaLegende(tekst = stringResource(R.string.pov_legenda_cilj)) {
            Canvas(Modifier.size(width = 14.dp, height = 10.dp)) {
                drawLine(
                    color = bojaCilja,
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width, size.height / 2f),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 2.dp.toPx()))
                )
            }
        }
    }
}

@Composable
private fun StavkaLegende(
    tekst: String,
    oznaka: @Composable () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        oznaka()
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = tekst,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Dani u cilju i prosjek kalorija i makronutrijenata preko dana s unosom, uz prosječni cilj tih dana. */
@Composable
private fun TjednaStatistikaCard(tjedan: TjedanUi, modifier: Modifier = Modifier) {
    val prosjek = tjedan.prosjek ?: return
    val cilj = tjedan.prosjekCilj
    val boje = MaterialTheme.macroBoje

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MacroOblici.pladanj,
        color = boje.heroPozadina,
        border = BorderStroke(MacroDimenzije.linija, boje.heroRub)
    ) {
        Column(
            modifier = Modifier.padding(22.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.pov_u_cilju),
                        color = boje.naHerojuPrigusen,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = stringResource(R.string.pov_u_cilju_vrijednost, tjedan.daniUCilju, tjedan.daniSUnosom),
                        color = boje.naHeroju,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.pov_prosjecno_dnevno),
                        color = boje.naHerojuPrigusen,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = stringResource(R.string.pov_kcal, formatKcalTisuce(prosjek.kcal)),
                        color = boje.naHeroju,
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Text(
                        text = cilj?.let { stringResource(R.string.pov_cilj_kcal, formatKcalTisuce(it.kcal)) }
                            ?: stringResource(R.string.pov_bez_cilja),
                        color = boje.naHerojuPrigusen,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Text(
                text = stringResource(R.string.pov_napomena_cilja),
                color = boje.naHerojuPrigusen,
                style = MaterialTheme.typography.labelSmall
            )

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(color = boje.naHeroju.copy(alpha = 0.18f))

            Spacer(modifier = Modifier.height(14.dp))

            TjednaMakroTraka(
                naziv = stringResource(R.string.kom_proteini),
                vrijednost = prosjek.proteini,
                cilj = cilj?.proteini,
                boja = boje.proteini
            )

            Spacer(modifier = Modifier.height(12.dp))

            TjednaMakroTraka(
                naziv = stringResource(R.string.kom_ugljikohidrati),
                vrijednost = prosjek.ugljikohidrati,
                cilj = cilj?.ugljikohidrati,
                boja = boje.ugljikohidrati
            )

            Spacer(modifier = Modifier.height(12.dp))

            TjednaMakroTraka(
                naziv = stringResource(R.string.kom_masti),
                vrijednost = prosjek.masti,
                cilj = cilj?.masti,
                boja = boje.masti
            )

            if (cilj == null) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.pov_nema_cilja),
                    color = boje.naHerojuPrigusen,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun TjednaMakroTraka(
    naziv: String,
    vrijednost: Double,
    cilj: Double?,
    boja: Color
) {
    val boje = MaterialTheme.macroBoje

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = naziv,
                color = boje.naHeroju,
                style = MaterialTheme.typography.titleSmall
            )

            Text(
                text = if (cilj != null) stringResource(R.string.pov_makro_vrijednost, formatMakro(vrijednost), formatGrami(cilj))
                else stringResource(R.string.pov_makro_bez_cilja, formatMakro(vrijednost)),
                color = boje.naHeroju,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (cilj != null && cilj > 0) {
            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(MacroOblici.traka)
                    .background(boje.lukStaza)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((vrijednost / cilj).toFloat().coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(MacroOblici.traka)
                        .background(boja)
                )
            }
        }
    }
}

@Composable
private fun DaySelector(
    datum: String?,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shema = MaterialTheme.colorScheme

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        IconButton(onClick = onPrevious) {
            Icon(
                imageVector = Icons.Rounded.ChevronLeft,
                contentDescription = stringResource(R.string.pov_prethodni_dan),
                tint = shema.onSurface
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Text(
                text = datum ?: stringResource(R.string.pov_odaberi_datum),
                style = MaterialTheme.typography.titleMedium,
                color = shema.onSurface
            )

            Text(
                text = stringResource(R.string.pov_dnevni_pregled),
                style = MaterialTheme.typography.bodySmall,
                color = shema.onSurfaceVariant
            )
        }

        IconButton(onClick = onNext) {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = stringResource(R.string.pov_sljedeci_dan),
                tint = shema.onSurface
            )
        }
    }
}

@Composable
private fun DailySummaryCard(
    kalorije: Double,
    proteini: Double,
    uh: Double,
    masti: Double,
    modifier: Modifier = Modifier
) {
    val boje = MaterialTheme.macroBoje

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MacroOblici.pladanj,
        color = boje.heroPozadina,
        border = BorderStroke(MacroDimenzije.linija, boje.heroRub)
    ) {

        Column(
            modifier = Modifier.padding(22.dp)
        ) {

            Text(
                text = stringResource(R.string.pov_ukupni_dnevni_unos),
                color = boje.naHerojuPrigusen
            )

            Text(
                text = stringResource(R.string.pov_kcal, formatKcal(kalorije)),
                style = MaterialTheme.typography.headlineLarge,
                color = boje.naHeroju
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DailyMacro(title = stringResource(R.string.kom_proteini), value = proteini)
                DailyMacro(title = stringResource(R.string.kom_ugljikohidrati), value = uh)
                DailyMacro(title = stringResource(R.string.kom_masti), value = masti)
            }
        }
    }
}

@Composable
private fun DailyMacro(
    title: String,
    value: Double
) {
    val boje = MaterialTheme.macroBoje

    Column {
        Text(
            text = stringResource(R.string.pov_makro_bez_cilja, formatMakro(value)),
            color = boje.naHeroju,
            style = MaterialTheme.typography.titleSmall
        )

        Text(
            text = title,
            color = boje.naHerojuPrigusen,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun EmptyDayCard(modifier: Modifier = Modifier) {
    val shema = MaterialTheme.colorScheme

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MacroOblici.pladanj,
        color = shema.surface,
        border = BorderStroke(MacroDimenzije.linija, shema.outlineVariant)
    ) {

        Column(
            modifier = Modifier.padding(20.dp)
        ) {

            Text(
                text = stringResource(R.string.pov_dan_prazno_naslov),
                style = MaterialTheme.typography.titleMedium,
                color = shema.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.pov_dan_prazno_tekst),
                color = shema.onSurfaceVariant
            )
        }
    }
}

// ---------- Previewi (lažni podaci iz PovijestPrimjeri.kt) ----------

@Composable
private fun PreviewOkvir(tamna: Boolean, stanje: TjedanStanje) {
    MacroMenzaTheme(darkTheme = tamna) {
        PovijestSadrzaj(
            stanje = stanje,
            odabraniTab = PovijestTab.TJEDNI,
            primjerUTijeku = false,
            odabraniDatum = null,
            povijestDana = null,
            obrociDana = emptyList(),
            akcije = PovijestAkcije(),
            puniUlaz = false
        )
    }
}

private fun sadrzaj(tjedan: com.niko.macromenza.model.Tjedan) = tjedanStanje(tjedan, greska = false, danas = DANAS_PRIMJERA)

private const val VISINA_PREVIEWA = 1100

@Preview(name = "Svijetla · učitavanje", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewSvijetlaUcitavanje() = PreviewOkvir(false, TjedanStanje.Ucitavanje)

@Preview(name = "Svijetla · greška", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewSvijetlaGreska() = PreviewOkvir(false, TjedanStanje.Greska)

@Preview(name = "Svijetla · prazan tjedan", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewSvijetlaPrazno() = PreviewOkvir(false, sadrzaj(PRIMJER_TJEDAN_PRAZNO))

@Preview(name = "Svijetla · primjer tjedna", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewSvijetlaPrimjer() = PreviewOkvir(false, sadrzaj(PRIMJER_TJEDAN_PRIMJER))

@Preview(name = "Svijetla · normalan tjedan", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewSvijetlaNormalno() = PreviewOkvir(false, sadrzaj(PRIMJER_TJEDAN_NORMALNO))

@Preview(name = "Tamna · učitavanje", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewTamnaUcitavanje() = PreviewOkvir(true, TjedanStanje.Ucitavanje)

@Preview(name = "Tamna · greška", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewTamnaGreska() = PreviewOkvir(true, TjedanStanje.Greska)

@Preview(name = "Tamna · prazan tjedan", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewTamnaPrazno() = PreviewOkvir(true, sadrzaj(PRIMJER_TJEDAN_PRAZNO))

@Preview(name = "Tamna · primjer tjedna", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewTamnaPrimjer() = PreviewOkvir(true, sadrzaj(PRIMJER_TJEDAN_PRIMJER))

@Preview(name = "Tamna · normalan tjedan", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewTamnaNormalno() = PreviewOkvir(true, sadrzaj(PRIMJER_TJEDAN_NORMALNO))

@Preview(name = "Svijetla · normalan tjedan · fontScale 1,3", widthDp = 360, heightDp = 1400, fontScale = 1.3f)
@Composable
private fun PreviewVelikiFontNormalno() = PreviewOkvir(false, sadrzaj(PRIMJER_TJEDAN_NORMALNO))

@Preview(name = "Tamna · prazan tjedan · fontScale 1,3", widthDp = 360, heightDp = VISINA_PREVIEWA, fontScale = 1.3f)
@Composable
private fun PreviewVelikiFontPrazno() = PreviewOkvir(true, sadrzaj(PRIMJER_TJEDAN_PRAZNO))
