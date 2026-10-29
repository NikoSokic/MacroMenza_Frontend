package com.niko.macromenza.screens.dnevnipregled

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.niko.macromenza.R
import com.niko.macromenza.model.Savjet
import com.niko.macromenza.ui.components.MakroTrake
import com.niko.macromenza.ui.components.ObrokRed
import com.niko.macromenza.ui.components.ObrokUi
import com.niko.macromenza.ui.components.Pladanj
import com.niko.macromenza.ui.components.TanjurUi
import com.niko.macromenza.ui.components.screenBottomPadding
import com.niko.macromenza.ui.components.skalaPritiska
import com.niko.macromenza.ui.theme.Kasnjenje
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
import com.niko.macromenza.ui.theme.rememberSmanjeniPokret
import com.niko.macromenza.util.formatGrami
import com.niko.macromenza.util.formatKcal
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Granice "u cilju" (kao u Povijesti: ±10 %). */
private const val CILJ_DONJA = 0.9
private const val CILJ_GORNJA = 1.1

/** Nakon ovoliko ms ulazni slijed je odigran (cijeli slijed traje < 900 ms). */
private const val TRAJANJE_ULAZA = 1000L

/** Indeks prvog reda obroka u LazyColumn (hero, makro, naslov, pladanj, naslov). */
private const val PRVI_RED_OBROKA = 5

fun jeCiljDosegnut(pregled: DnevniPregledUi): Boolean {
    val cilj = pregled.cilj?.kcal ?: return false
    if (cilj <= 0) return false
    val omjer = pregled.pojedeno.kcal / cilj
    return omjer in CILJ_DONJA..CILJ_GORNJA
}

/** Ekran dnevnog pregleda bez ViewModela; HomeScreen spaja podatke, a previewi koriste lažne podatke. */
@Composable
fun DnevniPregledSadrzaj(
    stanje: DnevniPregledStanje,
    datum: LocalDate,
    proslava: Boolean,
    onProslavaGotova: () -> Unit,
    onPrethodniDan: () -> Unit,
    onPostaviCilj: () -> Unit,
    onDodajObrok: (tip: String?) -> Unit,
    onSlikaIliOpis: () -> Unit,
    onUredi: (ObrokUi) -> Unit,
    onObrisi: (ObrokUi) -> Unit,
    onJeloSavjeta: (Long) -> Unit,
    onOdakle: () -> Unit,
    onPonovi: () -> Unit,
    modifier: Modifier = Modifier,
    pocetnoProsireni: List<Long> = emptyList(),
    snackbarHost: @Composable BoxScope.() -> Unit = {}
) {
    val smanjeno = rememberSmanjeniPokret()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.statusBarsPadding()) {
            GornjaTraka(datum = datum, onPrethodniDan = onPrethodniDan)

            val kljuc = when (stanje) {
                DnevniPregledStanje.Ucitavanje -> 0
                DnevniPregledStanje.Greska -> 1
                is DnevniPregledStanje.Sadrzaj -> 2
            }
            Crossfade(targetState = kljuc, animationSpec = tween(if (smanjeno) 0 else Trajanje.kratko), label = "stanje") { k ->
                when {
                    k == 0 -> Kostur(smanjeno)
                    k == 1 -> Greska(onPonovi)
                    stanje is DnevniPregledStanje.Sadrzaj -> Pregled(
                        pregled = stanje.pregled,
                        smanjeno = smanjeno,
                        proslava = proslava,
                        onProslavaGotova = onProslavaGotova,
                        onPostaviCilj = onPostaviCilj,
                        onDodajObrok = onDodajObrok,
                        onUredi = onUredi,
                        onObrisi = onObrisi,
                        onJeloSavjeta = onJeloSavjeta,
                        onOdakle = onOdakle,
                        pocetnoProsireni = pocetnoProsireni
                    )
                }
            }
        }

        if (stanje is DnevniPregledStanje.Sadrzaj) {
            Akcije(
                onDodajObrok = { onDodajObrok(null) },
                onSlikaIliOpis = onSlikaIliOpis,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = MacroRazmaci.rubEkrana, bottom = screenBottomPadding())
            )
        }

        snackbarHost()
    }
}

@Composable
private fun Pregled(
    pregled: DnevniPregledUi,
    smanjeno: Boolean,
    proslava: Boolean,
    onProslavaGotova: () -> Unit,
    onPostaviCilj: () -> Unit,
    onDodajObrok: (String?) -> Unit,
    onUredi: (ObrokUi) -> Unit,
    onObrisi: (ObrokUi) -> Unit,
    onJeloSavjeta: (Long) -> Unit,
    onOdakle: () -> Unit,
    pocetnoProsireni: List<Long>
) {
    val pregledAlata = LocalInspectionMode.current
    var ulazOdigran by rememberSaveable { mutableStateOf(false) }
    val animirajUlaz = !ulazOdigran && !smanjeno && !pregledAlata
    LaunchedEffect(Unit) {
        delay(TRAJANJE_ULAZA)
        ulazOdigran = true
    }

    var prosireni by rememberSaveable { mutableStateOf(pocetnoProsireni) }
    val stanjeListe = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LazyColumn(
        state = stanjeListe,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = MacroRazmaci.rubEkrana,
            end = MacroRazmaci.rubEkrana,
            top = MacroRazmaci.xs,
            bottom = screenBottomPadding() + MacroDimenzije.prostorZaFab
        )
    ) {
        item(key = "hero") {
            HeroKalorija(
                pojedeno = pregled.pojedeno.kcal,
                cilj = pregled.cilj?.kcal,
                popunjeniUtori = pregled.popunjeniUtori,
                ukupnoUtora = TIPOVI_UTORA.size,
                animirajUlaz = animirajUlaz,
                smanjeno = smanjeno,
                onPostaviCilj = onPostaviCilj
            )
        }
        item(key = "makro") {
            MakroTrake(
                pojedeno = pregled.pojedeno,
                cilj = pregled.cilj,
                animirajUlaz = animirajUlaz,
                smanjeno = smanjeno,
                modifier = Modifier.padding(top = MacroRazmaci.xl)
            )
        }
        item(key = "naslov_pladanj") {
            NaslovSekcije(stringResource(R.string.dp_tvoj_pladanj)) {
                if (jeCiljDosegnut(pregled)) CiljDosegnut()
            }
        }
        item(key = "pladanj") {
            Pladanj(
                tanjuri = pregled.tanjuri,
                danJePrazan = pregled.jePrazan,
                animirajUlaz = animirajUlaz,
                smanjeno = smanjeno,
                proslava = proslava,
                onProslavaGotova = onProslavaGotova,
                onTanjur = { tanjur ->
                    if (tanjur.makro == null) {
                        onDodajObrok(tanjur.tip)
                    } else {
                        val indeks = pregled.obroci.indexOfFirst { it.tip == tanjur.tip }
                        if (indeks >= 0) {
                            val id = pregled.obroci[indeks].id
                            if (id !in prosireni) prosireni = prosireni + id
                            scope.launch { stanjeListe.animateScrollToItem(PRVI_RED_OBROKA + indeks) }
                        }
                    }
                }
            )
        }
        item(key = "naslov_obroci") {
            NaslovSekcije(stringResource(R.string.dp_danas_jedes))
        }

        if (pregled.jePrazan) {
            item(key = "prazno") {
                PrazanDan(modifier = Modifier.ulazReda(0, animirajUlaz))
            }
        } else {
            itemsIndexed(pregled.obroci, key = { _, obrok -> obrok.id }) { indeks, obrok ->
                ObrokRed(
                    obrok = obrok,
                    prosiren = obrok.id in prosireni,
                    smanjeno = smanjeno,
                    onProsiri = {
                        prosireni = if (obrok.id in prosireni) prosireni - obrok.id else prosireni + obrok.id
                    },
                    onUredi = { onUredi(obrok) },
                    onObrisi = { onObrisi(obrok) },
                    modifier = Modifier
                        .padding(bottom = MacroRazmaci.s)
                        .then(
                            // animateItem(): brisanje, poništavanje i novi obroci klize na mjesto umjesto skoka
                            if (smanjeno) Modifier else Modifier.animateItem(
                                fadeInSpec = tween(Trajanje.kratko),
                                placementSpec = Opruge.slijeganje(),
                                fadeOutSpec = tween(Trajanje.kratko, easing = Ublazavanje.ubrzavanje)
                            )
                        )
                        .ulazReda(indeks, animirajUlaz)
                )
            }
        }

        pregled.savjet?.let { savjet ->
            item(key = "savjet") {
                SavjetDana(
                    savjet = savjet,
                    onJelo = onJeloSavjeta,
                    modifier = Modifier
                        .padding(top = MacroRazmaci.l)
                        .ulazReda(pregled.obroci.size + 1, animirajUlaz)
                )
            }
        }
        item(key = "podnozje") {
            Podnozje(onOdakle = onOdakle)
        }
    }
}

/** Ulaz reda pri prvom učitavanju: izblijeđivanje + pomak 12 dp, stupnjevano (samo graphicsLayer). */
private fun Modifier.ulazReda(indeks: Int, aktivno: Boolean): Modifier = composed {
    val napredak = remember { Animatable(if (aktivno) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (napredak.value < 1f) {
            delay((Kasnjenje.redoviPocetak + indeks * Kasnjenje.redovi).toLong())
            napredak.animateTo(1f, tween(Trajanje.srednje, easing = Ublazavanje.usporavanje))
        }
    }
    graphicsLayer {
        alpha = napredak.value
        translationY = (1f - napredak.value) * POMAK_ULAZA_DP.dp.toPx()
    }
}

@Composable
private fun GornjaTraka(datum: LocalDate, onPrethodniDan: () -> Unit) {
    val shema = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MacroRazmaci.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrethodniDan) {
            Icon(Icons.Rounded.ChevronLeft, contentDescription = stringResource(R.string.dp_prethodni_dan), tint = shema.onSurface)
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .semantics(mergeDescendants = true) { heading() },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.dp_danas).uppercase(),
                style = MaterialTheme.macroTipografija.nadnaslov,
                color = shema.onSurfaceVariant
            )
            Text(
                text = naslovDatuma(datum),
                style = MaterialTheme.typography.titleMedium,
                color = shema.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        // Budući dani nemaju unos: strelica je onemogućena, a drži naslov točno u sredini.
        IconButton(onClick = {}, enabled = false) {
            Icon(Icons.Rounded.ChevronRight, contentDescription = stringResource(R.string.dp_sljedeci_dan))
        }
    }
}

@Composable
private fun NaslovSekcije(tekst: String, dodatak: @Composable () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = MacroRazmaci.iznadSekcije, bottom = MacroRazmaci.ispodNaslovaSekcije),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = tekst,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() }
        )
        dodatak()
    }
}

@Composable
private fun CiljDosegnut() {
    val shema = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .background(shema.secondaryContainer, MacroOblici.cip)
            .padding(horizontal = MacroRazmaci.s, vertical = MacroRazmaci.xs)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = shema.onSecondaryContainer, modifier = Modifier.size(MacroDimenzije.ikonaSitna))
        Spacer(Modifier.width(MacroRazmaci.xs))
        Text(stringResource(R.string.dp_cilj_dosegnut), style = MaterialTheme.typography.labelMedium, color = shema.onSecondaryContainer)
    }
}

@Composable
private fun PrazanDan(modifier: Modifier = Modifier) {
    val shema = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(MacroDimenzije.obrubIsprekidan, shema.outlineVariant, MacroOblici.kartica)
            .padding(MacroRazmaci.xl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.dp_prazno_naslov),
            style = MaterialTheme.typography.titleLarge,
            color = shema.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(MacroRazmaci.xs))
        Text(
            text = stringResource(R.string.dp_prazno_tekst),
            style = MaterialTheme.typography.bodyMedium,
            color = shema.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SavjetDana(savjet: Savjet, onJelo: (Long) -> Unit, modifier: Modifier = Modifier) {
    val shema = MaterialTheme.colorScheme
    val tip = MaterialTheme.typography
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(shema.surface, MacroOblici.kartica)
            .border(MacroDimenzije.linija, shema.outlineVariant, MacroOblici.kartica)
            .padding(MacroRazmaci.l)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Lightbulb, contentDescription = null, tint = shema.secondary, modifier = Modifier.size(MacroDimenzije.ikonaMala))
            Spacer(Modifier.width(MacroRazmaci.s))
            Text(stringResource(R.string.dp_savjet_dana).uppercase(), style = MaterialTheme.macroTipografija.nadnaslov, color = shema.onSurfaceVariant)
        }
        Spacer(Modifier.height(MacroRazmaci.s))
        Text(savjet.naslov, style = tip.titleMedium, color = shema.onSurface)
        Spacer(Modifier.height(MacroRazmaci.xs))
        Text(savjet.tekst, style = tip.bodyMedium, color = shema.onSurfaceVariant)
        savjet.jela.take(3).forEach { jelo ->
            val izvor = remember { MutableInteractionSource() }
            Surface(
                onClick = { onJelo(jelo.idJelo) },
                interactionSource = izvor,
                shape = MacroOblici.cip,
                color = shema.background,
                modifier = Modifier
                    .padding(top = MacroRazmaci.s)
                    .fillMaxWidth()
                    .heightIn(min = MacroDimenzije.dodirniCilj)
                    .skalaPritiska(izvor)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = MacroRazmaci.m, vertical = MacroRazmaci.s),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(jelo.naziv, style = tip.titleSmall, color = shema.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(
                            stringResource(R.string.dp_jelo_savjet, formatKcal(jelo.kalorije), formatGrami(jelo.proteini)),
                            style = tip.bodySmall,
                            color = shema.onSurfaceVariant
                        )
                    }
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = shema.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun Podnozje(onOdakle: () -> Unit) {
    val shema = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = MacroRazmaci.xl),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            Icons.Rounded.Info,
            contentDescription = null,
            tint = shema.onSurfaceVariant,
            modifier = Modifier
                .padding(top = MacroRazmaci.xxs)
                .size(MacroDimenzije.ikonaSitna)
        )
        Spacer(Modifier.width(MacroRazmaci.s))
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.dp_okvirno), style = MaterialTheme.typography.bodySmall, color = shema.onSurfaceVariant)
            val izvor = remember { MutableInteractionSource() }
            TextButton(
                onClick = onOdakle,
                interactionSource = izvor,
                contentPadding = PaddingValues(),
                modifier = Modifier
                    .heightIn(min = MacroDimenzije.dodirniCilj)
                    .skalaPritiska(izvor)
            ) {
                Text(stringResource(R.string.dp_odakle), style = MaterialTheme.typography.labelLarge, color = shema.primary)
            }
        }
    }
}

@Composable
private fun Akcije(onDodajObrok: () -> Unit, onSlikaIliOpis: () -> Unit, modifier: Modifier = Modifier) {
    val shema = MaterialTheme.colorScheme
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MacroRazmaci.m)
    ) {
        val izvorSlike = remember { MutableInteractionSource() }
        SmallFloatingActionButton(
            onClick = onSlikaIliOpis,
            interactionSource = izvorSlike,
            containerColor = shema.secondaryContainer,
            contentColor = shema.onSecondaryContainer,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.loweredElevation(),
            modifier = Modifier
                .size(MacroDimenzije.dodirniCilj)
                .skalaPritiska(izvorSlike)
        ) {
            Icon(Icons.Rounded.PhotoCamera, contentDescription = stringResource(R.string.dp_slika_ili_opis))
        }
        val izvor = remember { MutableInteractionSource() }
        ExtendedFloatingActionButton(
            onClick = onDodajObrok,
            interactionSource = izvor,
            containerColor = shema.primary,
            contentColor = shema.onPrimary,
            shape = MacroOblici.gumb,
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.dp_dodaj_obrok), style = MaterialTheme.typography.labelLarge) },
            modifier = Modifier
                .heightIn(min = MacroDimenzije.fabVisina)
                .skalaPritiska(izvor)
        )
    }
}

/** Učitavanje: kostur u obliku luka, traka, pladnja i redova (bez spinnera), s blagim pulsiranjem. */
@Composable
private fun Kostur(smanjeno: Boolean) {
    val boje = MaterialTheme.macroBoje
    val opis = stringResource(R.string.dp_ucitavanje)
    val puls = if (smanjeno) null else rememberInfiniteTransition(label = "kostur").animateFloat(
        initialValue = 0.55f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(Trajanje.disanje / 2), RepeatMode.Reverse),
        label = "puls"
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = MacroRazmaci.rubEkrana)
            .graphicsLayer { alpha = puls?.value ?: 1f }
            .semantics { contentDescription = opis }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = MacroRazmaci.xs)
                .background(boje.heroPozadina, MacroOblici.pladanj)
                .border(MacroDimenzije.linija, boje.heroRub, MacroOblici.pladanj)
                .padding(MacroDimenzije.heroUnutarnji),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(MacroDimenzije.lukSirina, MacroDimenzije.lukVisina)) {
                val debljina = MacroDimenzije.lukDebljina.toPx()
                val promjer = size.width - debljina
                drawArc(
                    boje.kostur, MacroOmjeri.lukPocetak, MacroOmjeri.lukRaspon, false,
                    Offset(debljina / 2, debljina / 2), Size(promjer, promjer),
                    style = Stroke(debljina, cap = StrokeCap.Round)
                )
            }
        }
        repeat(3) {
            Box(
                Modifier
                    .padding(top = MacroRazmaci.l)
                    .fillMaxWidth()
                    .height(MacroDimenzije.oznakaCiljaVisina)
                    .background(boje.kostur, MacroOblici.traka)
            )
        }
        Spacer(Modifier.height(MacroRazmaci.iznadSekcije))
        Pladanj(
            tanjuri = TIPOVI_UTORA.map { TanjurUi(it, null) },
            danJePrazan = false,
            animirajUlaz = false,
            smanjeno = true,
            proslava = false,
            onProslavaGotova = {},
            onTanjur = {}
        )
        repeat(2) {
            Box(
                Modifier
                    .padding(top = MacroRazmaci.m)
                    .fillMaxWidth()
                    .height(MacroDimenzije.dodirniCilj + MacroRazmaci.l)
                    .background(boje.kostur, MacroOblici.kartica)
            )
        }
    }
}

@Composable
private fun Greska(onPonovi: () -> Unit) {
    val shema = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
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
            stringResource(R.string.dp_greska_naslov),
            style = MaterialTheme.typography.titleMedium,
            color = shema.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(MacroRazmaci.xs))
        Text(
            stringResource(R.string.dp_greska_tekst),
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
            Text(stringResource(R.string.kom_pokusaj_ponovno), style = MaterialTheme.typography.labelLarge, color = shema.onSurface)
        }
    }
}

// ---------- Previewi (lažni podaci iz DnevniPregledPrimjeri.kt) ----------

@Composable
private fun PreviewOkvir(tamna: Boolean, stanje: DnevniPregledStanje, prosireni: List<Long> = emptyList()) {
    MacroMenzaTheme(darkTheme = tamna) {
        DnevniPregledSadrzaj(
            stanje = stanje,
            datum = LocalDate.of(2026, 10, 26),
            proslava = false,
            onProslavaGotova = {},
            onPrethodniDan = {},
            onPostaviCilj = {},
            onDodajObrok = {},
            onSlikaIliOpis = {},
            onUredi = {},
            onObrisi = {},
            onJeloSavjeta = {},
            onOdakle = {},
            onPonovi = {},
            pocetnoProsireni = prosireni
        )
    }
}

private const val VISINA_PREVIEWA = 1900

@Preview(name = "Svijetla · prazno", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewSvijetlaPrazno() = PreviewOkvir(false, DnevniPregledStanje.Sadrzaj(PRIMJER_PRAZNO))

@Preview(name = "Svijetla · normalno", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewSvijetlaNormalno() = PreviewOkvir(false, DnevniPregledStanje.Sadrzaj(PRIMJER_NORMALNO))

@Preview(name = "Svijetla · preko cilja", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewSvijetlaPrekoCilja() = PreviewOkvir(false, DnevniPregledStanje.Sadrzaj(PRIMJER_PREKO_CILJA))

@Preview(name = "Tamna · prazno", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewTamnaPrazno() = PreviewOkvir(true, DnevniPregledStanje.Sadrzaj(PRIMJER_PRAZNO))

@Preview(name = "Tamna · normalno", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewTamnaNormalno() = PreviewOkvir(true, DnevniPregledStanje.Sadrzaj(PRIMJER_NORMALNO))

@Preview(name = "Tamna · preko cilja", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewTamnaPrekoCilja() = PreviewOkvir(true, DnevniPregledStanje.Sadrzaj(PRIMJER_PREKO_CILJA))

/** Proširena večera pokazuje prelamanje dugog naziva ("Puretina u umaku od gljiva s njokima"). */
@Preview(name = "Svijetla · preko cilja · fontScale 1,3", widthDp = 360, heightDp = 2300, fontScale = 1.3f)
@Composable
private fun PreviewVelikiFont() = PreviewOkvir(false, DnevniPregledStanje.Sadrzaj(PRIMJER_PREKO_CILJA), prosireni = listOf(2L, 4L))

@Preview(name = "Svijetla · učitavanje", widthDp = 360, heightDp = 900)
@Composable
private fun PreviewUcitavanje() = PreviewOkvir(false, DnevniPregledStanje.Ucitavanje)

@Preview(name = "Tamna · greška", widthDp = 360, heightDp = 900)
@Composable
private fun PreviewGreska() = PreviewOkvir(true, DnevniPregledStanje.Greska)
