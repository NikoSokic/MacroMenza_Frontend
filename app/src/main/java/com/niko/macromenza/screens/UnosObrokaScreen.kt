package com.niko.macromenza.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.gson.Gson
import com.niko.macromenza.R
import com.niko.macromenza.model.Jelo
import com.niko.macromenza.model.MeniDTO
import com.niko.macromenza.screens.unosobroka.FilterRasponaSheet
import com.niko.macromenza.ui.components.PotvrdaBrisanjaObrokaDialog
import com.niko.macromenza.screens.unosobroka.StanjeJela
import com.niko.macromenza.screens.unosobroka.StanjeMenija
import com.niko.macromenza.screens.unosobroka.UnosObrokaAkcije
import com.niko.macromenza.screens.unosobroka.UnosObrokaSadrzaj
import com.niko.macromenza.screens.unosobroka.UnosObrokaUi
import com.niko.macromenza.session.UserSessionManager
import com.niko.macromenza.ui.components.screenBottomPadding
import com.niko.macromenza.ui.theme.MacroRazmaci
import com.niko.macromenza.ui.theme.UlazUSesiji
import com.niko.macromenza.util.PrijedlogObrokaStore
import com.niko.macromenza.viewmodel.JelaViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val ZAGREB = ZoneId.of("Europe/Zagreb")
private val FORMAT_VREMENA = DateTimeFormatter.ofPattern("HH:mm")

/** Ako prva jela ne stignu u ovom roku (bez greške), kostur se zamjenjuje praznim stanjem. */
private const val ROK_ZA_JELA_MS = 8_000L

/** Hrvatski naziv vrste obroka izvan kompozicije (snackbar). */
private fun nazivTipa(context: Context, tip: String): String = when (tip) {
    "DORUCAK" -> context.getString(R.string.kom_tip_dorucak)
    "RUCAK" -> context.getString(R.string.kom_tip_rucak)
    "UZINA" -> context.getString(R.string.kom_tip_uzina)
    "VECERA" -> context.getString(R.string.kom_tip_vecera)
    else -> tip
}

/** "Ručak 12" za snackbar (izvan kompozicije). */
private fun nazivMenija(context: Context, meni: MeniDTO): String {
    val tip = nazivTipa(context, meni.tip)
    return meni.redniBroj?.let { context.getString(R.string.uo_meni_naziv, tip, it.toString()) } ?: tip
}

/** Tipično vrijeme obroka u menzi, za unos vrste koja nije trenutna (npr. ručak upisan navečer). */
private fun tipicnoVrijeme(tip: String): LocalTime? = when (tip) {
    "DORUCAK" -> LocalTime.of(8, 0)
    "RUCAK" -> LocalTime.of(12, 30)
    "UZINA" -> LocalTime.of(16, 0)
    "VECERA" -> LocalTime.of(18, 30)
    else -> null
}

/**
 * Početno vrijeme novog obroka (datum je danas): "sada" ako vrsta odgovara trenutnom dobu dana, inače tipično
 * vrijeme te vrste, ali nikad u budućnosti: ako je tipično vrijeme kasnije od sada, uzima se "sada".
 * Datum se ne mijenja automatski.
 */
private fun pocetnoVrijeme(tip: String): LocalTime {
    val sada = LocalTime.now(ZAGREB).truncatedTo(ChronoUnit.MINUTES)
    if (zadaniTipZaVrijeme(sada) == tip) return sada
    val tipicno = tipicnoVrijeme(tip) ?: return sada
    return if (tipicno.isAfter(sada)) sada else tipicno
}

/**
 * Unos i uređivanje obroka (smjer A "Pladanj"). Ovaj composable spaja JelaViewModel (bez promjena) s
 * [UnosObrokaSadrzaj]; ovdje ostaju navigacija, dijalozi, pickeri, sheetovi i sažetak obroka (paket 2).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnosObrokaScreen(
    tipObroka: String,
    navController: NavController,
    // Zadan kod uređivanja spremljenog obroka; null je novi unos.
    idObrok: Long? = null,
    // Jelo iz prečaca "Često jedeš" na kartici Unos: dodaje se u obrok čim je poznato (jedna porcija).
    dodajJeloId: Long? = null,
    viewModel: JelaViewModel = viewModel()
) {
    val uredivanje = idObrok != null
    val jela by viewModel.jela.collectAsState()
    val search by viewModel.upit.collectAsState()
    val odabranaKategorija by viewModel.kategorija.collectAsState()
    val kategorije by viewModel.kategorije.collectAsState()
    val cestoJedeno by viewModel.cestoJedeno.collectAsState()
    val makroFilter by viewModel.makroFilter.collectAsState()
    val greska by viewModel.greska.collectAsState()
    val spremanjeUTijeku by viewModel.spremanjeUTijeku.collectAsState()
    val spremljeniObrok by viewModel.spremljeniObrok.collectAsState()
    val greskaSpremanja by viewModel.greskaSpremanja.collectAsState()
    val meniji by viewModel.meniji.collectAsState()
    val menijiUcitavanje by viewModel.menijiUcitavanje.collectAsState()
    val greskaMenija by viewModel.greskaMenija.collectAsState()
    val tipMenija by viewModel.tipMenija.collectAsState()
    val samoVegetarijanski by viewModel.samoVegetarijanski.collectAsState()
    val odabir by viewModel.odabir.collectAsState()
    val sazetak by viewModel.sazetak.collectAsState()
    val datum by viewModel.datum.collectAsState()
    val uredivaniObrok by viewModel.uredivaniObrok.collectAsState()
    val ucitavanjeObroka by viewModel.ucitavanjeObroka.collectAsState()
    val greskaUcitavanja by viewModel.greskaUcitavanja.collectAsState()
    val azuriraniObrok by viewModel.azuriraniObrok.collectAsState()
    val obrisaniObrok by viewModel.obrisaniObrok.collectAsState()

    val danas = remember { LocalDate.now(ZAGREB) }
    var visinaBara by remember { mutableStateOf(0.dp) }
    // Kod uređivanja vrijeme dolazi iz obroka (vidi LaunchedEffect(uredivaniObrok)).
    var vrijeme by remember {
        mutableStateOf(
            if (uredivanje) LocalTime.now(ZAGREB).truncatedTo(ChronoUnit.MINUTES)
            else pocetnoVrijeme(tipObroka.uppercase())
        )
    }
    var tip by remember { mutableStateOf(tipObroka.uppercase()) }
    var tipRucno by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var detaljJelo by remember { mutableStateOf<Jelo?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Svaka stavka ide u odabir s količinom iz menija (zbraja se s već odabranom);
    // "Poništi" vraća odabir na stanje prije dodavanja.
    fun dodajMeni(meni: MeniDTO) {
        val staro = viewModel.dodajMeni(meni)

        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val rezultat = snackbarHostState.showSnackbar(
                message = context.getString(R.string.uo_dodan_meni, nazivMenija(context, meni)),
                actionLabel = context.getString(R.string.uo_ponisti),
                duration = SnackbarDuration.Short
            )
            if (rezultat == SnackbarResult.ActionPerformed) {
                viewModel.vratiOdabir(staro)
            }
        }
    }

    val sessionManager = remember {
        UserSessionManager(context)
    }

    val prijavljeniKorisnikId by
    sessionManager.korisnikId.collectAsState(initial = null)

    LaunchedEffect(Unit) {
        viewModel.ucitajKategorije()
        viewModel.ucitajJela()
        // Meniji su sada sekcija na vrhu liste (prije zasebna kartica), pa se učitavaju odmah.
        viewModel.ucitajMenije()
    }

    // Prečac s kartice Unos: jelo se traži među "Često jedeš", a ako tamo nije, među svim jelima.
    var precacDodan by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(cestoJedeno, jela) {
        val id = dodajJeloId ?: return@LaunchedEffect
        if (precacDodan || uredivanje) return@LaunchedEffect
        val jelo = cestoJedeno.firstOrNull { it.jelo.id == id }?.jelo
            ?: jela.firstOrNull { it.id == id }
            ?: return@LaunchedEffect
        viewModel.povecaj(jelo)
        precacDodan = true
    }

    // Puni ulazni slijed samo pri prvom otvaranju unosa u ovoj sesiji; poslije kratko izblijeđivanje u prijelazu.
    val puniUlaz = remember { UlazUSesiji.jePrvi(UlazUSesiji.UNOS_OBROKA) }
    SideEffect { UlazUSesiji.oznaci(UlazUSesiji.UNOS_OBROKA) }

    LaunchedEffect(prijavljeniKorisnikId) {
        prijavljeniKorisnikId?.let {
            viewModel.postaviKorisnika(it)
            idObrok?.let { id -> viewModel.ucitajObrok(id) }
        }
    }

    // Vrijeme i tip žive u ekranu, pa ih postavlja ekran kad obrok stigne; tip se više ne mijenja s vremenom.
    LaunchedEffect(uredivaniObrok) {
        val obrok = uredivaniObrok ?: return@LaunchedEffect
        vrijeme = LocalTime.parse(obrok.vrijeme, FORMAT_VREMENA)
        tip = obrok.tipObroka
        tipRucno = true
    }

    // Prijedlog iz ekrana "Slika ili opis" stiže pri otvaranju i pri povratku na ovaj ekran.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (uredivanje) return@LifecycleEventEffect
        val cekajuci = PrijedlogObrokaStore.preuzmi() ?: return@LifecycleEventEffect
        viewModel.primijeniPrijedlog(cekajuci.prijedlog, cekajuci.izvor)
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(context.getString(R.string.uo_prijedlog_dodan))
        }
    }

    LaunchedEffect(spremljeniObrok) {
        val obrok = spremljeniObrok ?: return@LaunchedEffect
        viewModel.ocistiRezultatSpremanja()
        navController.navigate("obrok_spremljen/${obrok.tipObroka}")
    }

    // Promjene spremljene: natrag na prikaz dana, koji se pri povratku sam ponovno učita.
    LaunchedEffect(azuriraniObrok) {
        azuriraniObrok ?: return@LaunchedEffect
        viewModel.ocistiRezultatSpremanja()
        navController.popBackStack()
    }

    LaunchedEffect(greskaUcitavanja) {
        val poruka = greskaUcitavanja ?: return@LaunchedEffect
        viewModel.ocistiGreskuUcitavanja()
        snackbarHostState.showSnackbar(poruka)
    }

    val imaPromjena = uredivanje && uredivaniObrok != null &&
        viewModel.imaPromjena(vrijeme.format(FORMAT_VREMENA), tip)
    var showOdbaciDialog by remember { mutableStateOf(false) }
    var showBrisanjeDialog by remember { mutableStateOf(false) }

    // Obrok obrisan: povratak na prikaz dana, koji nudi "Poništi" (obrok se predaje preko savedStateHandle).
    LaunchedEffect(obrisaniObrok) {
        val obrok = obrisaniObrok ?: return@LaunchedEffect
        navController.previousBackStackEntry?.savedStateHandle
            ?.set(KLJUC_OBRISANI_OBROK, Gson().toJson(obrok))
        viewModel.ocistiRezultatSpremanja()
        navController.popBackStack()
    }

    // Prije BackHandlera sažetka, pa otvorena lista sažetka i dalje prva hvata "natrag".
    BackHandler(enabled = imaPromjena) { showOdbaciDialog = true }

    LaunchedEffect(greskaSpremanja) {
        val poruka = greskaSpremanja ?: return@LaunchedEffect
        viewModel.ocistiRezultatSpremanja()
        snackbarHostState.showSnackbar(poruka)
    }

    // ViewModel nema zastavicu učitavanja jela: kostur stoji dok prva jela ne stignu (ili do roka).
    var jelaStigla by remember { mutableStateOf(false) }
    var istekaoRok by remember { mutableStateOf(false) }
    LaunchedEffect(jela) { if (jela.isNotEmpty()) jelaStigla = true }
    LaunchedEffect(Unit) {
        delay(ROK_ZA_JELA_MS)
        istekaoRok = true
    }
    val stanjeJela = when {
        greska != null -> StanjeJela.Greska
        !jelaStigla && !istekaoRok -> StanjeJela.Ucitavanje
        else -> StanjeJela.Sadrzaj(jela)
    }
    val stanjeMenija = when {
        greskaMenija != null -> StanjeMenija.Greska(greskaMenija.orEmpty())
        menijiUcitavanje || meniji.isEmpty() -> StanjeMenija.Ucitavanje
        else -> StanjeMenija.Sadrzaj(meniji)
    }

    val ui = UnosObrokaUi(
        tip = tip,
        uredivanje = uredivanje,
        ucitavanjeObroka = ucitavanjeObroka,
        mozeBrisati = uredivanje && uredivaniObrok != null,
        spremanjeUTijeku = spremanjeUTijeku,
        datum = datum,
        danas = danas,
        vrijeme = vrijeme,
        nakonObroka = sazetak.nakonObroka,
        ciljKalorije = sazetak.ciljKalorije,
        upit = search,
        kategorija = odabranaKategorija,
        kategorije = kategorije,
        makroFilter = makroFilter,
        jela = stanjeJela,
        cesto = cestoJedeno,
        meniji = stanjeMenija,
        tipMenija = tipMenija,
        samoVegetarijanski = samoVegetarijanski,
        kolicine = odabir.stavke.mapValues { it.value.kolicina }
    )

    val akcije = UnosObrokaAkcije(
        onNatrag = { if (imaPromjena) showOdbaciDialog = true else navController.popBackStack() },
        onTip = {
            tip = it
            tipRucno = true
        },
        onDatum = { showDatePicker = true },
        onVrijeme = { showTimePicker = true },
        onObrisi = { showBrisanjeDialog = true },
        onSlikaIliOpis = { navController.navigate(RUTA_SLIKA_ILI_OPIS) },
        onUpit = { viewModel.postaviUpit(it) },
        onKategorija = { viewModel.postaviKategoriju(it) },
        onMakroFilter = { viewModel.primijeniMakroFilter(it) },
        onFilteri = { showFilterSheet = true },
        onDodaj = { viewModel.povecaj(it) },
        onDetalj = { detaljJelo = it },
        onDodajMeni = { dodajMeni(it) },
        onTipMenija = { viewModel.postaviTipMenija(it) },
        onVegetarijanski = { viewModel.postaviSamoVegetarijanski(it) },
        onPonoviJela = {
            viewModel.ucitajKategorije()
            viewModel.ucitajJela()
        },
        onPonoviMenije = { viewModel.ucitajMenije() }
    )

    UnosObrokaSadrzaj(
        ui = ui,
        akcije = akcije,
        // Prostor za plutajuću navigaciju i sažetak obroka da zadnje jelo ostane vidljivo.
        donjiRazmak = screenBottomPadding() + visinaBara + MacroRazmaci.m,
        puniUlaz = puniUlaz
    ) {
        SazetakObrokaBar(
            sazetak = sazetak,
            stavke = odabir.stavke.values.toList(),
            spremanjeUTijeku = spremanjeUTijeku,
            onPlus = { viewModel.povecaj(it) },
            onMinus = { viewModel.smanji(it) },
            onUkloni = { viewModel.ukloni(it) },
            onSpremi = {
                if (uredivanje) {
                    viewModel.azurirajObrok(
                        vrijeme = vrijeme.format(FORMAT_VREMENA),
                        tipObroka = tip
                    )
                } else {
                    viewModel.spremiObrok(
                        vrijeme = vrijeme.format(FORMAT_VREMENA),
                        tipObroka = tip
                    )
                }
            },
            onVisina = { visinaBara = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = screenBottomPadding() - MacroRazmaci.m),
            tekstSpremi = stringResource(if (uredivanje) R.string.uo_spremi_promjene else R.string.uo_spremi)
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    start = MacroRazmaci.rubEkrana,
                    end = MacroRazmaci.rubEkrana,
                    bottom = screenBottomPadding() + visinaBara + MacroRazmaci.xs
                )
        )
    }

    if (showBrisanjeDialog) {
        uredivaniObrok?.let { obrok ->
            PotvrdaBrisanjaObrokaDialog(
                obrok = obrok,
                onPotvrdi = {
                    showBrisanjeDialog = false
                    viewModel.obrisiObrok()
                },
                onOdustani = { showBrisanjeDialog = false }
            )
        }
    }

    if (showOdbaciDialog) {
        AlertDialog(
            onDismissRequest = { showOdbaciDialog = false },
            title = { Text(stringResource(R.string.uo_odbaciti_naslov)) },
            text = { Text(stringResource(R.string.uo_odbaciti_tekst)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showOdbaciDialog = false
                        navController.popBackStack()
                    }
                ) {
                    Text(stringResource(R.string.uo_odbaci), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showOdbaciDialog = false }) {
                    Text(stringResource(R.string.uo_nastavi))
                }
            }
        )
    }

    if (showDatePicker) {
        val danasUtcMillis = danas.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis =
                datum.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis <= danasUtcMillis

                override fun isSelectableYear(year: Int): Boolean =
                    year <= danas.year
            }
        )

        val shema = MaterialTheme.colorScheme
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            viewModel.postaviDatum(
                                Instant.ofEpochMilli(millis)
                                    .atZone(ZoneOffset.UTC)
                                    .toLocalDate()
                            )
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.uo_u_redu))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.uo_odustani), color = shema.onSurfaceVariant)
                }
            }
        ) {
            // Odabrani dan je stanje, ne akcija: orah/krem umjesto paprike.
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = shema.secondary,
                    selectedDayContentColor = shema.onSecondary,
                    selectedYearContainerColor = shema.secondary,
                    selectedYearContentColor = shema.onSecondary,
                    todayDateBorderColor = shema.secondary,
                    todayContentColor = shema.secondary
                )
            )
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = vrijeme.hour,
            initialMinute = vrijeme.minute,
            is24Hour = true
        )

        val shema = MaterialTheme.colorScheme
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        vrijeme = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        if (!tipRucno) {
                            val noviTip = zadaniTipZaVrijeme(vrijeme)
                            // Vrsta se mijenja s vremenom dok je nisi sam odabrao; reci to, da ne promakne.
                            if (noviTip != tip) {
                                tip = noviTip
                                scope.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    snackbarHostState.showSnackbar(
                                        context.getString(R.string.uo_vrsta_promijenjena, nazivTipa(context, noviTip))
                                    )
                                }
                            }
                        }
                        showTimePicker = false
                    }
                ) {
                    Text(stringResource(R.string.uo_u_redu))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.uo_odustani), color = shema.onSurfaceVariant)
                }
            },
            text = {
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(
                        selectorColor = shema.secondary,
                        clockDialSelectedContentColor = shema.onSecondary,
                        timeSelectorSelectedContainerColor = shema.secondaryContainer,
                        timeSelectorSelectedContentColor = shema.onSecondaryContainer
                    )
                )
            }
        )
    }

    detaljJelo?.let { jelo ->
        JeloDetaljSheet(
            idJelo = jelo.id,
            onDodaj = { porcije ->
                viewModel.dodaj(jelo, porcije)
                detaljJelo = null
            },
            onOdakleSuKalorije = {
                detaljJelo = null
                navController.navigate("odakle_su_kalorije")
            },
            onDismiss = { detaljJelo = null }
        )
    }

    if (showFilterSheet) {
        FilterRasponaSheet(
            filter = makroFilter,
            onPrimijeni = {
                viewModel.primijeniMakroFilter(it)
                showFilterSheet = false
            },
            onResetiraj = { viewModel.resetirajMakroFilter() },
            onDismiss = { showFilterSheet = false }
        )
    }
}

/** Zadani tip prema vremenu: do 10:30 doručak, do 15:00 ručak, do 18:00 užina, inače večera. */
internal fun zadaniTipZaVrijeme(vrijeme: LocalTime): String = when {
    !vrijeme.isAfter(LocalTime.of(10, 30)) -> "DORUCAK"
    !vrijeme.isAfter(LocalTime.of(15, 0)) -> "RUCAK"
    !vrijeme.isAfter(LocalTime.of(18, 0)) -> "UZINA"
    else -> "VECERA"
}
