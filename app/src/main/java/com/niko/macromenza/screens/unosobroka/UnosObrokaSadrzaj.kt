package com.niko.macromenza.screens.unosobroka

import android.os.SystemClock
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.niko.macromenza.R
import com.niko.macromenza.screens.dnevnipregled.naslovDatuma
import com.niko.macromenza.model.CestoJedenoJelo
import com.niko.macromenza.model.Jelo
import com.niko.macromenza.model.MeniDTO
import com.niko.macromenza.ui.components.nazivTipa
import com.niko.macromenza.ui.components.skalaPritiska
import com.niko.macromenza.ui.theme.Kasnjenje
import com.niko.macromenza.ui.theme.MAKS_ANIMIRANIH_REDOVA
import com.niko.macromenza.ui.theme.MacroDimenzije
import com.niko.macromenza.ui.theme.MacroMenzaTheme
import com.niko.macromenza.ui.theme.MacroOblici
import com.niko.macromenza.ui.theme.MacroRazmaci
import com.niko.macromenza.ui.theme.Opruge
import com.niko.macromenza.ui.theme.Trajanje
import com.niko.macromenza.ui.theme.Ublazavanje
import com.niko.macromenza.ui.theme.macroBoje
import com.niko.macromenza.ui.theme.macroTipografija
import com.niko.macromenza.ui.theme.rememberSmanjeniPokret
import com.niko.macromenza.util.formatKcal
import com.niko.macromenza.viewmodel.MakroFilter
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val TIPOVI_OBROKA = listOf("DORUCAK", "RUCAK", "UZINA", "VECERA")
private val FORMAT_VREMENA = DateTimeFormatter.ofPattern("HH:mm")

/** "Često jedeš" ide prije menija kad ima barem ovoliko jela (nakon makro filtera). */
private const val CESTO_PRVO_OD = 3

/**
 * Redni broj ulaza elemenata liste; gradi se redom kojim se elementi deklariraju u LazyColumnu.
 * Pamti i koja je sekcija prva (ona ima manji razmak iznad, jer stoji odmah ispod filtera).
 */
private class BrojacUlaza {
    private var n = 0
    private var prva = true

    fun sljedeci(): Int = n++

    /** Rezervira [broj] uzastopnih brojeva i vraća prvi. */
    fun rezerviraj(broj: Int): Int = n.also { n += broj }

    fun jePrvaSekcija(): Boolean = prva.also { prva = false }
}

/** Prije "Prikaži sve menije" vidi se ovoliko menija. */
private const val MENIJA_SAZETO = 3

private const val REDOVA_KOSTURA = 7

/**
 * Prvi dio unosa obroka (smjer A "Pladanj"), bez ViewModela: gornja traka, pretraga i filteri su
 * fiksni na vrhu, a ispod se lista (meniji → Često jedeš → jela po kategorijama s ljepljivim naslovima).
 * Sažetak obroka, dijalozi i sheetovi dolaze kroz [prekrivanje].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UnosObrokaSadrzaj(
    ui: UnosObrokaUi,
    akcije: UnosObrokaAkcije,
    donjiRazmak: Dp,
    modifier: Modifier = Modifier,
    /** True samo pri prvom ulazu u sesiji: puni stupnjevani slijed; inače sve odmah (ekran izblijedi u prijelazu). */
    puniUlaz: Boolean = true,
    prekrivanje: @Composable BoxScope.() -> Unit = {}
) {
    val shema = MaterialTheme.colorScheme
    val pretraga = ui.upit.isNotBlank()
    val stanjeListe = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Ulaz: dok traje (prvi prikaz liste), novi redovi izblijeđuju stupnjevano; poslije toga samo animateItem.
    // Previewi (statični kadar) i smanjeni pokret odmah prikazuju završno stanje.
    val smanjeno = rememberSmanjeniPokret() || LocalInspectionMode.current
    val pocetak = remember { SystemClock.uptimeMillis() }
    var ulazTraje by remember { mutableStateOf(puniUlaz) }
    val bezUlaza = smanjeno || !puniUlaz
    val jelaStigla = ui.jela is StanjeJela.Sadrzaj
    LaunchedEffect(jelaStigla) {
        if (jelaStigla) {
            delay((Kasnjenje.nakonPrijelaza + Kasnjenje.unosRedovi + MAKS_ANIMIRANIH_REDOVA * Kasnjenje.redovi + Trajanje.srednje).toLong())
            ulazTraje = false
        }
    }
    val pokret = PokretUnosa(aktivan = ulazTraje, smanjeno = smanjeno, pocetak = pocetak)

    // Traka filtera se skuplja pri listanju prema dolje i vraća pri listanju prema gore; pretraga ostaje prikvačena.
    val trake = remember(smanjeno) { SkupljanjeTrake(scope, smanjeno) { stanjeListe.canScrollForward } }

    // Novi upit, kategorija ili filter daju novu listu: kreće se od vrha, inače prvi rezultat ostaje skriven.
    LaunchedEffect(ui.upit, ui.kategorija, ui.makroFilter) {
        stanjeListe.scrollToItem(0)
        trake.prikazi()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(shema.background)
    ) {
        Column(modifier = Modifier.statusBarsPadding().nestedScroll(trake.veza)) {
            GornjaTraka(ui, akcije)

            if (ui.ucitavanjeObroka) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MacroRazmaci.rubEkrana),
                    color = shema.secondary,
                    trackColor = shema.surfaceVariant
                )
            }

            PoljePretrage(
                upit = ui.upit,
                prikaziSliku = !ui.uredivanje,
                slikaOmogucena = !ui.spremanjeUTijeku,
                onUpit = akcije.onUpit,
                onSlikaIliOpis = akcije.onSlikaIliOpis,
                modifier = Modifier.ulazZaglavlja(0, bezUlaza)
            )

            FilterTraka(
                filter = ui.makroFilter,
                kategorije = ui.kategorije,
                kategorija = ui.kategorija,
                onFilteri = akcije.onFilteri,
                onKategorija = akcije.onKategorija,
                modifier = Modifier
                    .skupljajuca(trake)
                    // Skupljena traka je nevidljiva i čitaču zaslona (vraća se listanjem prema gore).
                    .then(if (trake.skupljena) Modifier.clearAndSetSemantics { } else Modifier)
                    .ulazZaglavlja(1, bezUlaza)
                    .padding(top = MacroRazmaci.s, bottom = MacroRazmaci.xs)
            )

            LazyColumn(
                state = stanjeListe,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = donjiRazmak)
            ) {
                // Svaki element dobiva redni broj ulaza (najviše MAKS_ANIMIRANIH_REDOVA animira, ostali su odmah tu).
                val brojac = BrojacUlaza()
                if (!pretraga && ui.kategorija == null) {
                    // Makro filter vrijedi i za "Često jedeš"; s dovoljno jela ono je najbrži put pa ide prvo.
                    val cesto = ui.cesto.filter { ui.makroFilter.propusta(it.jelo) }
                    if (cesto.size >= CESTO_PRVO_OD) {
                        sekcijaCesto(cesto, ui, akcije, pokret, brojac)
                        sekcijaMenija(ui, akcije, pokret, brojac)
                    } else {
                        sekcijaMenija(ui, akcije, pokret, brojac)
                        sekcijaCesto(cesto, ui, akcije, pokret, brojac)
                    }
                }
                sekcijaJela(ui, akcije, pretraga, pokret, brojac)
            }
        }

        prekrivanje()
    }
}

// ---------- Gornja traka ----------

@Composable
private fun GornjaTraka(ui: UnosObrokaUi, akcije: UnosObrokaAkcije) {
    val shema = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MacroRazmaci.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = akcije.onNatrag) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.uo_natrag), tint = shema.onSurface)
        }
        Spacer(Modifier.weight(1f))
        NapredakTekst(
            nakonObroka = ui.nakonObroka,
            cilj = ui.ciljKalorije,
            datum = ui.datum,
            danas = ui.danas,
            modifier = Modifier.padding(end = MacroRazmaci.m)
        )
    }

    Column(modifier = Modifier.padding(horizontal = MacroRazmaci.rubEkrana)) {
        if (ui.uredivanje) {
            Text(
                text = stringResource(R.string.uo_uredivanje).uppercase(),
                style = MaterialTheme.macroTipografija.nadnaslov,
                color = shema.onSurfaceVariant,
                modifier = Modifier.padding(top = MacroRazmaci.xs)
            )
        }
        // Izbornik uređivanja stoji uz naziv vrste obroka.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) {
                TipCip(tip = ui.tip, onTip = akcije.onTip)
            }
            if (ui.mozeBrisati) {
                IzbornikUredivanja(enabled = !ui.spremanjeUTijeku, onObrisi = akcije.onObrisi)
            }
        }
        DatumIVrijeme(ui, akcije)
    }
}

/** Vrsta obroka kao naslov ekrana; dodir otvara izbor vrste. */
@Composable
private fun TipCip(tip: String, onTip: (String) -> Unit) {
    val shema = MaterialTheme.colorScheme
    var otvoren by remember { mutableStateOf(false) }
    val naziv = nazivTipa(tip)
    val opis = stringResource(R.string.uo_promijeni_tip, naziv)
    val izvor = remember { MutableInteractionSource() }

    Box {
        Row(
            modifier = Modifier
                .heightIn(min = MacroDimenzije.dodirniCilj)
                .clip(MacroOblici.cip)
                .clickable(interactionSource = izvor, indication = null, role = Role.Button) { otvoren = true }
                .clearAndSetSemantics { contentDescription = opis }
                .skalaPritiska(izvor),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = naziv,
                style = MaterialTheme.typography.headlineSmall,
                color = shema.onBackground,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(Modifier.width(MacroRazmaci.xs))
            Icon(Icons.Rounded.ExpandMore, contentDescription = null, tint = shema.onSurfaceVariant)
        }
        DropdownMenu(
            expanded = otvoren,
            onDismissRequest = { otvoren = false },
            containerColor = shema.surface,
            shape = MacroOblici.kartica
        ) {
            TIPOVI_OBROKA.forEach { t ->
                DropdownMenuItem(
                    text = { Text(nazivTipa(t), style = MaterialTheme.typography.bodyLarge) },
                    trailingIcon = if (t == tip) {
                        { Icon(Icons.Rounded.Check, contentDescription = null, tint = shema.onSurface) }
                    } else null,
                    onClick = {
                        otvoren = false
                        onTip(t)
                    },
                    modifier = Modifier.heightIn(min = MacroDimenzije.dodirniCilj)
                )
            }
        }
    }
}

/**
 * Datum i vrijeme kao običan tekst ispod naslova ("Ponedjeljak, 24. 10. · 12:40"); svaki dio otvara svoj birač.
 * Novi unos za danas piše "Danas"; uređivanje uvijek piše puni datum s danom u tjednu.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DatumIVrijeme(ui: UnosObrokaUi, akcije: UnosObrokaAkcije) {
    val datumTekst = tekstDatuma(ui)
    val vrijemeTekst = ui.vrijeme.format(FORMAT_VREMENA)

    FlowRow(verticalArrangement = Arrangement.Center) {
        DioDatuma(datumTekst, stringResource(R.string.uo_datum_opis, datumTekst), akcije.onDatum)
        Text(
            text = stringResource(R.string.uo_razdjelnik),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .clearAndSetSemantics { }
        )
        DioDatuma(vrijemeTekst, stringResource(R.string.uo_vrijeme_opis, vrijemeTekst), akcije.onVrijeme)
    }
}

@Composable
private fun tekstDatuma(ui: UnosObrokaUi): String {
    val datum = ui.datum
    if (!ui.uredivanje && datum == ui.danas) return stringResource(R.string.uo_danas)
    if (datum.year == ui.danas.year) return naslovDatuma(datum)
    val dani = stringArrayResource(R.array.dp_dani_u_tjednu)
    return stringResource(R.string.uo_datum_godina, dani[datum.dayOfWeek.value - 1], datum.dayOfMonth, datum.monthValue, datum.year)
}

@Composable
private fun DioDatuma(tekst: String, opis: String, onClick: () -> Unit) {
    val izvor = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .heightIn(min = MacroDimenzije.dodirniCilj)
            .clip(MacroOblici.cip)
            .clickable(interactionSource = izvor, indication = null, role = Role.Button, onClick = onClick)
            .clearAndSetSemantics { contentDescription = opis }
            .skalaPritiska(izvor),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(tekst, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

/**
 * Tihi zbroj desno u gornjem redu: "1510 / 2400 kcal" i ispod "danas s ovim obrokom" (ili "taj dan" za drugi datum).
 * Broj prelazi na novi iznos uz Opruge.blaga, boja (višnja preko cilja) uz Trajanje.kratko.
 */
@Composable
private fun NapredakTekst(nakonObroka: Double?, cilj: Double?, datum: LocalDate, danas: LocalDate, modifier: Modifier = Modifier) {
    val shema = MaterialTheme.colorScheme
    val smanjeno = rememberSmanjeniPokret()
    val pojedeno = nakonObroka ?: return
    val prekoCilja = cilj != null && pojedeno > cilj

    val prikazano by animateFloatAsState(
        targetValue = pojedeno.toFloat(),
        animationSpec = if (smanjeno) snap() else Opruge.blaga(),
        label = "napredakKcal"
    )
    val boja by animateColorAsState(
        targetValue = if (prekoCilja) MaterialTheme.macroBoje.visnja else shema.onSurface,
        animationSpec = if (smanjeno) snap() else tween(Trajanje.kratko, easing = Ublazavanje.usporavanje),
        label = "napredakBoja"
    )

    val tekst = if (cilj != null) stringResource(R.string.uo_napredak, formatKcal(prikazano.toDouble()), formatKcal(cilj))
    else stringResource(R.string.uo_napredak_bez_cilja, formatKcal(prikazano.toDouble()))
    // Čitač zaslona dobiva završni iznos, ne vrijednost usred prijelaza.
    val opis = if (cilj != null) stringResource(R.string.uo_napredak_opis, formatKcal(pojedeno), formatKcal(cilj))
    else stringResource(R.string.uo_napredak_opis_bez_cilja, formatKcal(pojedeno))

    Column(
        modifier = modifier.clearAndSetSemantics { contentDescription = opis },
        horizontalAlignment = Alignment.End
    ) {
        Text(text = tekst, style = MaterialTheme.typography.labelLarge, color = boja)
        Text(
            text = if (datum == danas) stringResource(R.string.uo_danas_s_obrokom)
            else stringResource(R.string.uo_datum_s_obrokom, datum.dayOfMonth, datum.monthValue),
            style = MaterialTheme.typography.labelSmall,
            color = shema.onSurfaceVariant
        )
    }
}

@Composable
private fun IzbornikUredivanja(enabled: Boolean, onObrisi: () -> Unit) {
    val shema = MaterialTheme.colorScheme
    var otvoren by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { otvoren = true }, enabled = enabled) {
            Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.uo_vise_opcija))
        }
        DropdownMenu(
            expanded = otvoren,
            onDismissRequest = { otvoren = false },
            containerColor = shema.surface,
            shape = MacroOblici.kartica
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.uo_obrisi_obrok), color = shema.error) },
                leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = shema.error) },
                onClick = {
                    otvoren = false
                    onObrisi()
                },
                modifier = Modifier.heightIn(min = MacroDimenzije.dodirniCilj)
            )
        }
    }
}

// ---------- Pretraga ----------

@Composable
private fun PoljePretrage(
    upit: String,
    prikaziSliku: Boolean,
    slikaOmogucena: Boolean,
    onUpit: (String) -> Unit,
    onSlikaIliOpis: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shema = MaterialTheme.colorScheme
    OutlinedTextField(
        value = upit,
        onValueChange = onUpit,
        placeholder = { Text(stringResource(R.string.uo_pretrazi), style = MaterialTheme.typography.bodyLarge) },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = when {
            upit.isNotEmpty() -> {
                {
                    IconButton(onClick = { onUpit("") }) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.uo_ocisti_pretragu))
                    }
                }
            }
            prikaziSliku -> {
                {
                    IconButton(onClick = onSlikaIliOpis, enabled = slikaOmogucena) {
                        Icon(Icons.Rounded.PhotoCamera, contentDescription = stringResource(R.string.uo_slika_ili_opis))
                    }
                }
            }
            else -> null
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge,
        shape = MacroOblici.pilula,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = shema.surface,
            unfocusedContainerColor = shema.surface,
            focusedBorderColor = shema.secondary,
            unfocusedBorderColor = shema.outlineVariant,
            cursorColor = shema.onSurface,
            focusedLeadingIconColor = shema.onSurfaceVariant,
            unfocusedLeadingIconColor = shema.onSurfaceVariant,
            focusedTrailingIconColor = shema.onSurfaceVariant,
            unfocusedTrailingIconColor = shema.onSurfaceVariant,
            focusedPlaceholderColor = shema.onSurfaceVariant,
            unfocusedPlaceholderColor = shema.onSurfaceVariant
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MacroRazmaci.rubEkrana)
            .padding(top = MacroRazmaci.s)
    )
}

// ---------- Sekcije liste ----------

private fun LazyListScope.sekcijaMenija(ui: UnosObrokaUi, akcije: UnosObrokaAkcije, pokret: PokretUnosa, brojac: BrojacUlaza) {
    // Ručak i večera sami biraju tip menija. Doručak i užina nemaju menije u menzi: sažeti su u jedan red,
    // a dodir na njega otvara menije za ručak (čipom se prebacuje na večeru, ponovnim dodirom zatvara).
    val tipOdredjen = ui.tip == "RUCAK" || ui.tip == "VECERA"
    val tipFiltera = if (tipOdredjen) ui.tip else ui.tipMenija
    val prva = brojac.jePrvaSekcija()

    if (!tipOdredjen && ui.tipMenija == null) {
        val ulaz = brojac.sljedeci()
        item(key = "meniji-redak") {
            RedakMenija(
                prva = prva,
                onClick = { akcije.onTipMenija("RUCAK") },
                modifier = pomakReda(pokret).ulazUnosa(ulaz, pokret)
            )
        }
        return
    }

    val naslovUlaz = brojac.sljedeci()
    item(key = "meniji-naslov") {
        NaslovSekcije(
            tekst = stringResource(
                when {
                    !tipOdredjen -> R.string.uo_meniji
                    tipFiltera == "RUCAK" -> R.string.uo_meniji_rucak
                    else -> R.string.uo_meniji_vecera
                }
            ),
            prva = prva,
            modifier = pomakReda(pokret).ulazUnosa(naslovUlaz, pokret, pomak = false)
        ) {
            MacroCip(
                oznaka = stringResource(R.string.uo_vegetarijanski),
                odabran = ui.samoVegetarijanski,
                onClick = { akcije.onVegetarijanski(!ui.samoVegetarijanski) }
            )
        }
    }

    if (!tipOdredjen) {
        item(key = "meniji-tip") {
            IzborTipaMenija(tipMenija = ui.tipMenija, onTipMenija = akcije.onTipMenija, modifier = pomakReda(pokret))
        }
    }

    when (val stanje = ui.meniji) {
        StanjeMenija.Ucitavanje -> item(key = "meniji-kostur") {
            KosturRedova(broj = 2, opis = stringResource(R.string.uo_ucitavanje_menija))
        }

        is StanjeMenija.Greska -> item(key = "meniji-greska") {
            KratkaGreska(poruka = stanje.poruka, onPonovi = akcije.onPonoviMenije, modifier = pomakReda(pokret))
        }

        is StanjeMenija.Sadrzaj -> {
            val prikazani = stanje.meniji.filter {
                (tipFiltera == null || it.tip == tipFiltera) && (!ui.samoVegetarijanski || it.vegetarijanski)
            }
            if (prikazani.isEmpty()) {
                item(key = "meniji-prazno") {
                    TihaPoruka(stringResource(R.string.uo_nema_menija_filter), modifier = pomakReda(pokret))
                }
            } else {
                val baza = brojac.rezerviraj(minOf(prikazani.size, MENIJA_SAZETO))
                item(key = "meniji-lista") {
                    ListaMenija(prikazani, akcije.onDodajMeni, pokret, baza)
                }
            }
        }
    }
}

/** Doručak i užina: jedan red "Meniji za ručak/večeru ›" umjesto cijele sekcije. */
@Composable
private fun RedakMenija(prva: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shema = MaterialTheme.colorScheme
    val izvor = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = if (prva) MacroRazmaci.s else MacroRazmaci.xl)
            .heightIn(min = MacroDimenzije.dodirniCilj)
            .clickable(interactionSource = izvor, indication = null, role = Role.Button, onClick = onClick)
            .skalaPritiska(izvor)
            .padding(horizontal = MacroRazmaci.rubEkrana),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.uo_meniji_rucak_vecera),
            style = MaterialTheme.typography.bodyLarge,
            color = shema.onSurface,
            modifier = Modifier.weight(1f)
        )
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = shema.onSurfaceVariant)
    }
}

/** Za doručak i užinu: koji se meniji prikazuju; ponovni dodir na odabrani čip zatvara menije. */
@Composable
private fun IzborTipaMenija(tipMenija: String?, onTipMenija: (String?) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(horizontal = MacroRazmaci.rubEkrana),
        horizontalArrangement = Arrangement.spacedBy(MacroRazmaci.s)
    ) {
        listOf("RUCAK", "VECERA").forEach { t ->
            MacroCip(
                oznaka = nazivTipa(t),
                odabran = tipMenija == t,
                onClick = { onTipMenija(if (tipMenija == t) null else t) }
            )
        }
    }
}

@Composable
private fun ListaMenija(meniji: List<MeniDTO>, onDodaj: (MeniDTO) -> Unit, pokret: PokretUnosa, ulazBaza: Int) {
    var svi by rememberSaveable { mutableStateOf(false) }
    val vidljivi = if (svi) meniji else meniji.take(MENIJA_SAZETO)

    Column {
        vidljivi.forEachIndexed { i, meni ->
            MeniRed(
                meni = meni,
                onDodaj = { onDodaj(meni) },
                crta = i > 0,
                modifier = Modifier.ulazUnosa(ulazBaza + i, pokret)
            )
        }
        if (meniji.size > MENIJA_SAZETO) {
            TextButton(
                onClick = { svi = !svi },
                contentPadding = PaddingValues(horizontal = MacroRazmaci.rubEkrana),
                modifier = Modifier.heightIn(min = MacroDimenzije.dodirniCilj)
            ) {
                Text(
                    text = if (svi) stringResource(R.string.uo_prikazi_manje)
                    else stringResource(R.string.uo_prikazi_sve_menije, meniji.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun LazyListScope.sekcijaCesto(
    cesto: List<CestoJedenoJelo>,
    ui: UnosObrokaUi,
    akcije: UnosObrokaAkcije,
    pokret: PokretUnosa,
    brojac: BrojacUlaza
) {
    if (cesto.isEmpty()) return
    val prva = brojac.jePrvaSekcija()

    val naslovUlaz = brojac.sljedeci()
    item(key = "cesto-naslov") {
        NaslovSekcije(
            tekst = stringResource(R.string.uo_cesto),
            podnaslov = stringResource(R.string.uo_cesto_tekst),
            prva = prva,
            modifier = pomakReda(pokret).ulazUnosa(naslovUlaz, pokret, pomak = false)
        )
    }
    val baza = brojac.rezerviraj(cesto.size)
    itemsIndexed(cesto, key = { _, c -> "c-${c.jelo.id}" }) { i, c ->
        JeloRed(
            modifier = pomakReda(pokret).ulazUnosa(baza + i, pokret),
            naziv = c.jelo.naziv,
            makro = c.jelo.uMakro(),
            kolicina = ui.kolicine[c.jelo.id] ?: 0.0,
            onDodaj = { akcije.onDodaj(c.jelo) },
            onDetalj = { akcije.onDetalj(c.jelo) },
            dodatak = stringResource(R.string.uo_cesto_puta, c.brojPuta),
            crta = i > 0
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.sekcijaJela(
    ui: UnosObrokaUi,
    akcije: UnosObrokaAkcije,
    pretraga: Boolean,
    pokret: PokretUnosa,
    brojac: BrojacUlaza
) {
    // Bez pretrage i kategorije ova sekcija dolazi nakon menija i "Često jedeš", pa ima svoj naslov.
    if (!pretraga && ui.kategorija == null) {
        val naslovUlaz = brojac.sljedeci()
        val prva = brojac.jePrvaSekcija()
        item(key = "jela-naslov") {
            NaslovSekcije(
                tekst = stringResource(R.string.uo_sva_jela),
                prva = prva,
                modifier = pomakReda(pokret).ulazUnosa(naslovUlaz, pokret, pomak = false)
            )
        }
    } else {
        item(key = "jela-razmak") { Spacer(Modifier.height(MacroRazmaci.s)) }
    }

    when (val stanje = ui.jela) {
        StanjeJela.Ucitavanje -> item(key = "jela-kostur") {
            KosturRedova(broj = REDOVA_KOSTURA, opis = stringResource(R.string.uo_ucitavanje))
        }

        StanjeJela.Greska -> item(key = "jela-greska") {
            GreskaJela(onPonovi = akcije.onPonoviJela)
        }

        is StanjeJela.Sadrzaj -> {
            if (stanje.jela.isEmpty()) {
                item(key = "jela-prazno") {
                    BezRezultata(
                        modifier = pomakReda(pokret),
                        upit = ui.upit.trim().takeIf { pretraga },
                        imaFiltera = ui.makroFilter.aktivan || ui.kategorija != null,
                        onPonistiFiltere = {
                            akcije.onMakroFilter(MakroFilter())
                            akcije.onKategorija(null)
                        }
                    )
                }
                return
            }

            // Pretraga: jedna lista po relevantnosti, kategorija kao prigušeni dodatak u retku.
            if (pretraga) {
                val baza = brojac.rezerviraj(stanje.jela.size)
                val rangirana = rangirajPoUpitu(stanje.jela, ui.upit, ui.cesto)
                itemsIndexed(rangirana, key = { _, j -> "j-${j.id}" }) { i, jelo ->
                    RedJela(
                        jelo = jelo,
                        crta = i > 0,
                        ui = ui,
                        akcije = akcije,
                        dodatak = jelo.kategorija,
                        modifier = pomakReda(pokret).ulazUnosa(baza + i, pokret)
                    )
                }
                return
            }

            // Pregledavanje: naslovi kategorija prianjaju uz vrh liste.
            grupirajPoKategoriji(stanje.jela, ui.kategorije).forEach { skupina ->
                val naslovUlaz = brojac.sljedeci()
                // Ljepljivi naslov: samo izblijeđivanje (bez pomaka), da se pri prianjanju uz vrh ne trzne.
                stickyHeader(key = "kat-${skupina.kategorija}") {
                    NaslovKategorije(
                        tekst = skupina.kategorija ?: stringResource(R.string.uo_ostalo),
                        modifier = pomakReda(pokret, naslov = true).ulazUnosa(naslovUlaz, pokret, pomak = false)
                    )
                }
                val baza = brojac.rezerviraj(skupina.jela.size)
                itemsIndexed(skupina.jela, key = { _, j -> "j-${j.id}" }) { i, jelo ->
                    RedJela(
                        jelo = jelo,
                        crta = i > 0,
                        ui = ui,
                        akcije = akcije,
                        modifier = pomakReda(pokret).ulazUnosa(baza + i, pokret)
                    )
                }
            }
        }
    }
}

@Composable
private fun RedJela(
    jelo: Jelo,
    crta: Boolean,
    ui: UnosObrokaUi,
    akcije: UnosObrokaAkcije,
    modifier: Modifier,
    dodatak: String? = null
) {
    JeloRed(
        modifier = modifier,
        dodatak = dodatak,
        naziv = jelo.naziv,
        makro = jelo.uMakro(),
        kolicina = ui.kolicine[jelo.id] ?: 0.0,
        onDodaj = { akcije.onDodaj(jelo) },
        onDetalj = { akcije.onDetalj(jelo) },
        pogodak = pogodakUpita(jelo.naziv, ui.upit),
        crta = crta
    )
}

// ---------- Stanja ----------

@Composable
private fun TihaPoruka(tekst: String, modifier: Modifier = Modifier) {
    Text(
        text = tekst,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = MacroRazmaci.rubEkrana)
    )
}

@Composable
private fun KratkaGreska(poruka: String, onPonovi: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = MacroRazmaci.rubEkrana)) {
        Text(poruka, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        TextButton(
            onClick = onPonovi,
            contentPadding = PaddingValues(horizontal = MacroRazmaci.xs),
            modifier = Modifier.heightIn(min = MacroDimenzije.dodirniCilj)
        ) {
            Text(
                stringResource(R.string.uo_pokusaj_ponovno),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun BezRezultata(upit: String?, imaFiltera: Boolean, onPonistiFiltere: () -> Unit, modifier: Modifier = Modifier) {
    val shema = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MacroRazmaci.rubEkrana, vertical = MacroRazmaci.xl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (upit != null) stringResource(R.string.uo_nema_rezultata, upit)
            else stringResource(R.string.uo_nema_jela_filter),
            style = MaterialTheme.typography.titleMedium,
            color = shema.onSurface,
            textAlign = TextAlign.Center
        )
        if (imaFiltera) {
            TextButton(
                onClick = onPonistiFiltere,
                modifier = Modifier
                    .padding(top = MacroRazmaci.xs)
                    .heightIn(min = MacroDimenzije.dodirniCilj)
            ) {
                Text(stringResource(R.string.uo_ponisti_filtere), style = MaterialTheme.typography.labelLarge, color = shema.primary)
            }
        }
    }
}

@Composable
private fun GreskaJela(onPonovi: () -> Unit) {
    val shema = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MacroRazmaci.rubEkrana, vertical = MacroRazmaci.xl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Rounded.CloudOff, contentDescription = null, tint = shema.onSurfaceVariant, modifier = Modifier.size(MacroDimenzije.ikona))
        Spacer(Modifier.height(MacroRazmaci.m))
        Text(
            stringResource(R.string.uo_greska_jela_naslov),
            style = MaterialTheme.typography.titleMedium,
            color = shema.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(MacroRazmaci.xs))
        Text(
            stringResource(R.string.uo_greska_jela_tekst),
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
            Text(stringResource(R.string.uo_pokusaj_ponovno), style = MaterialTheme.typography.labelLarge, color = shema.onSurface)
        }
    }
}

// ---------- Previewi (⚠ lažni podaci iz UnosObrokaPrimjeri.kt) ----------

@Composable
private fun PreviewUnos(tamna: Boolean, ui: UnosObrokaUi) {
    MacroMenzaTheme(darkTheme = tamna) {
        UnosObrokaSadrzaj(ui = ui, akcije = UnosObrokaAkcije(), donjiRazmak = MacroRazmaci.xl)
    }
}

private const val VISINA_PREVIEWA = 1400

@Preview(name = "Unos obroka · svijetla · zadano", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewUnosSvijetlaZadano() = PreviewUnos(false, PRIMJER_ZADANO)

@Preview(name = "Unos obroka · tamna · zadano", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewUnosTamnaZadano() = PreviewUnos(true, PRIMJER_ZADANO)

@Preview(name = "Unos obroka · svijetla · pretraga", widthDp = 360, heightDp = 900)
@Composable
private fun PreviewUnosSvijetlaPretraga() = PreviewUnos(false, PRIMJER_PRETRAGA)

@Preview(name = "Unos obroka · tamna · pretraga", widthDp = 360, heightDp = 900)
@Composable
private fun PreviewUnosTamnaPretraga() = PreviewUnos(true, PRIMJER_PRETRAGA)

@Preview(name = "Unos obroka · svijetla · bez rezultata", widthDp = 360, heightDp = 640)
@Composable
private fun PreviewUnosSvijetlaBezRezultata() = PreviewUnos(false, PRIMJER_BEZ_REZULTATA)

@Preview(name = "Unos obroka · tamna · bez rezultata", widthDp = 360, heightDp = 640)
@Composable
private fun PreviewUnosTamnaBezRezultata() = PreviewUnos(true, PRIMJER_BEZ_REZULTATA)

@Preview(name = "Unos obroka · svijetla · uređivanje", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewUnosSvijetlaUredivanje() = PreviewUnos(false, PRIMJER_UREDIVANJE)

@Preview(name = "Unos obroka · tamna · uređivanje", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewUnosTamnaUredivanje() = PreviewUnos(true, PRIMJER_UREDIVANJE)

@Preview(name = "Unos obroka · doručak bez menija · učitavanje", widthDp = 360, heightDp = 900)
@Composable
private fun PreviewUnosUcitavanje() = PreviewUnos(false, PRIMJER_UCITAVANJE)

@Preview(name = "Unos obroka · greška", widthDp = 360, heightDp = 900)
@Composable
private fun PreviewUnosGreska() = PreviewUnos(true, PRIMJER_GRESKA)

@Preview(name = "Unos obroka · fontScale 1,3", widthDp = 360, heightDp = VISINA_PREVIEWA, fontScale = 1.3f)
@Composable
private fun PreviewUnosVelikiFont() = PreviewUnos(false, PRIMJER_UREDIVANJE)
