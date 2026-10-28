package com.niko.macromenza.screens.povijest

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.niko.macromenza.R
import com.niko.macromenza.ui.components.ObrokUi
import java.time.LocalDate
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

/** Sažetak ulazi s navigacijom: brojevi broje 650 ms od 240 ms, pa je ulaz gotov u 890 ms (< 900). */
private const val ULAZ_SAZETAK = ULAZ_NAVIGACIJA

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
    val onUredi: (ObrokUi) -> Unit = {},
    val onObrisi: (ObrokUi) -> Unit = {}
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
    odabraniDan: LocalDate?,
    danStanje: DanStanje,
    danas: LocalDate,
    akcije: PovijestAkcije,
    modifier: Modifier = Modifier,
    puniUlaz: Boolean = true,
    /** Samo za previewe: dan odabran u redu tanjura. */
    pocetnoOdabraniDan: Int? = null,
    /** Samo za previewe: prošireni redovi obroka na kartici Dan. */
    pocetnoProsireni: List<Long> = emptyList(),
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

    // Listanje dana radi kao listanje tjedna: stari dan izlazi u smjeru, a dok novi ne stigne ostaje prikazan
    // (izblijeđen), pa između dva dana nema kostura ni skoka.
    val listanjeDana = remember { Animatable(0f) }
    var smjerDana by remember { mutableIntStateOf(0) }
    var cekaDan by remember { mutableStateOf(false) }
    var zadnjiDan by remember { mutableStateOf<DanUi?>(null) }
    val ucitaniDan = (danStanje as? DanStanje.Sadrzaj)?.dan

    fun listajDan(s: Int, radnja: () -> Unit) {
        if (!smanjeno && zadnjiDan != null) {
            smjerDana = s
            cekaDan = true
            scope.launch { listanjeDana.animateTo(-s.toFloat(), Opruge.slijeganje()) }
        }
        radnja()
    }

    LaunchedEffect(ucitaniDan) {
        if (ucitaniDan != null) zadnjiDan = ucitaniDan
    }
    LaunchedEffect(ucitaniDan?.datum) {
        if (ucitaniDan != null && cekaDan) {
            cekaDan = false
            listanjeDana.snapTo(smjerDana.toFloat())
            listanjeDana.animateTo(0f, Opruge.slijeganje())
        }
    }
    val prikazDana = zadnjiDan.let { zadnji ->
        if (cekaDan && ucitaniDan == null && zadnji != null) DanStanje.Sadrzaj(zadnji) else danStanje
    }
    var prosireni by rememberSaveable { mutableStateOf(pocetnoProsireni) }

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
                        fun Modifier.sadrzajTjedna(rub: Dp = MacroRazmaci.rubEkrana, ulazIndeks: Int = ULAZ_SADRZAJ) = this
                            .padding(horizontal = rub)
                            .listanjeTjedna(listanje)
                            .ulaz(ulazIndeks, animirajUlaz)

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
                            item(key = "tanjuri") {
                                // Red ide do 8 dp od ruba da svaki od 7 stupaca ima ≥ 48 dp (pages/povijest.md).
                                TjedanTanjuri(
                                    tjedan = t,
                                    animirajUlaz = animirajUlaz,
                                    kasnjenjeUlaza = Kasnjenje.nakonPrijelaza + ULAZ_SADRZAJ * Kasnjenje.korak,
                                    smanjeno = smanjeno,
                                    onOtvoriDan = { akcije.onOtvoriDan(it.toString()) },
                                    pocetnoOdabrani = pocetnoOdabraniDan,
                                    modifier = prijelaz(smanjeno)
                                        .sadrzajTjedna(rub = MacroRazmaci.s)
                                        .padding(top = MacroRazmaci.s)
                                )
                            }
                            item(key = "sazetak") {
                                SazetakTjedna(
                                    tjedan = t,
                                    animirajUlaz = animirajUlaz,
                                    kasnjenjeUlaza = Kasnjenje.nakonPrijelaza + ULAZ_SAZETAK * Kasnjenje.korak,
                                    smanjeno = smanjeno,
                                    modifier = prijelaz(smanjeno).sadrzajTjedna(ulazIndeks = ULAZ_SAZETAK)
                                )
                            }
                        }
                    }
                }
            } else {
                item(key = "dan_zaglavlje") {
                    odabraniDan?.let { datum ->
                        ZaglavljeDana(
                            datum = datum,
                            danas = danas,
                            onPrethodni = { listajDan(-1, akcije.onPrethodniDan) },
                            onSljedeci = { if (datum.isBefore(danas)) listajDan(1, akcije.onSljedeciDan) },
                            modifier = prijelaz(smanjeno)
                        )
                    }
                }
                stavkeDana(
                    stanje = prikazDana,
                    prosireni = prosireni,
                    smanjeno = smanjeno,
                    onProsiri = { id -> prosireni = if (id in prosireni) prosireni - id else prosireni + id },
                    onUredi = akcije.onUredi,
                    onObrisi = akcije.onObrisi,
                    ukras = { prijelaz(smanjeno).listanjeTjedna(listanjeDana) }
                )
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

/** Pomak i alfa sadržaja tjedna (ili dana) pri listanju; vrijednost se čita u fazi crtanja. */
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

// ---------- Previewi (lažni podaci iz PovijestPrimjeri.kt) ----------

@Composable
private fun PreviewOkvir(tamna: Boolean, stanje: TjedanStanje, odabraniDan: Int? = null) {
    MacroMenzaTheme(darkTheme = tamna) {
        PovijestSadrzaj(
            stanje = stanje,
            odabraniTab = PovijestTab.TJEDNI,
            primjerUTijeku = false,
            odabraniDan = null,
            danStanje = DanStanje.Ucitavanje,
            danas = DANAS_PRIMJERA,
            akcije = PovijestAkcije(),
            puniUlaz = false,
            pocetnoOdabraniDan = odabraniDan
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

@Preview(name = "Svijetla · tjedan bez cilja", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewSvijetlaBezCilja() = PreviewOkvir(false, sadrzaj(PRIMJER_TJEDAN_BEZ_CILJA))

@Preview(name = "Tamna · tjedan bez cilja", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewTamnaBezCilja() = PreviewOkvir(true, sadrzaj(PRIMJER_TJEDAN_BEZ_CILJA))

@Preview(name = "Svijetla · odabran dan (preko cilja)", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewSvijetlaOdabran() = PreviewOkvir(false, sadrzaj(PRIMJER_TJEDAN_NORMALNO), ODABRANI_DAN_PRIMJERA)

@Preview(name = "Tamna · odabran dan (preko cilja)", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewTamnaOdabran() = PreviewOkvir(true, sadrzaj(PRIMJER_TJEDAN_NORMALNO), ODABRANI_DAN_PRIMJERA)

@Preview(name = "Svijetla · 320 dp · odabran dan", widthDp = 320, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewUskiOdabran() = PreviewOkvir(false, sadrzaj(PRIMJER_TJEDAN_NORMALNO), ODABRANI_DAN_PRIMJERA)

@Preview(name = "Tamna · 320 dp · fontScale 1,3", widthDp = 320, heightDp = 1400, fontScale = 1.3f)
@Composable
private fun PreviewUskiVelikiFont() = PreviewOkvir(true, sadrzaj(PRIMJER_TJEDAN_NORMALNO), ODABRANI_DAN_PRIMJERA)

// Sažetak tjedna: u cilju, preko cilja i bez cilja (svijetla/tamna), uz fontScale 1,3.

@Preview(name = "Svijetla · sažetak u cilju", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewSvijetlaSazetakUCilju() = PreviewOkvir(false, sadrzaj(PRIMJER_TJEDAN_U_CILJU))

@Preview(name = "Tamna · sažetak u cilju", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewTamnaSazetakUCilju() = PreviewOkvir(true, sadrzaj(PRIMJER_TJEDAN_U_CILJU))

@Preview(name = "Svijetla · sažetak preko cilja", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewSvijetlaSazetakPreko() = PreviewOkvir(false, sadrzaj(PRIMJER_TJEDAN_PREKO_CILJA))

@Preview(name = "Tamna · sažetak preko cilja", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewTamnaSazetakPreko() = PreviewOkvir(true, sadrzaj(PRIMJER_TJEDAN_PREKO_CILJA))

@Preview(name = "Svijetla · sažetak u cilju · fontScale 1,3", widthDp = 360, heightDp = 1700, fontScale = 1.3f)
@Composable
private fun PreviewVelikiFontSazetakUCilju() = PreviewOkvir(false, sadrzaj(PRIMJER_TJEDAN_U_CILJU))

@Preview(name = "Tamna · sažetak preko cilja · fontScale 1,3", widthDp = 360, heightDp = 1700, fontScale = 1.3f)
@Composable
private fun PreviewVelikiFontSazetakPreko() = PreviewOkvir(true, sadrzaj(PRIMJER_TJEDAN_PREKO_CILJA))

@Preview(name = "Svijetla · sažetak bez cilja · fontScale 1,3", widthDp = 360, heightDp = 1700, fontScale = 1.3f)
@Composable
private fun PreviewVelikiFontSazetakBezCilja() = PreviewOkvir(false, sadrzaj(PRIMJER_TJEDAN_BEZ_CILJA))
