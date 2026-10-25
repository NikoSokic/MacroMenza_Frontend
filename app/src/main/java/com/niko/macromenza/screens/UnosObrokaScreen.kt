package com.niko.macromenza.screens

import com.niko.macromenza.util.formatKcal
import com.niko.macromenza.util.formatKolicina
import com.niko.macromenza.util.formatMakro
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.gson.Gson
import com.niko.macromenza.model.CestoJedenoJelo
import com.niko.macromenza.model.Jelo
import com.niko.macromenza.model.MeniDTO
import com.niko.macromenza.model.StavkaObrokaRequest
import com.niko.macromenza.session.UserSessionManager
import com.niko.macromenza.ui.components.screenBottomPadding
import com.niko.macromenza.ui.components.screenTopPadding
import com.niko.macromenza.ui.theme.MacroGreen
import com.niko.macromenza.viewmodel.JelaViewModel
import com.niko.macromenza.viewmodel.KarticaUnosa
import com.niko.macromenza.viewmodel.MakroFilter
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val ZAGREB = ZoneId.of("Europe/Zagreb")
private val FORMAT_DATUMA = DateTimeFormatter.ofPattern("d.M.yyyy.")
private val FORMAT_VREMENA = DateTimeFormatter.ofPattern("HH:mm")
private val TIPOVI_OBROKA = listOf("DORUCAK", "RUCAK", "UZINA", "VECERA")

private val ProteinBlue = Color(0xFF4F8EF7)
private val CarbsOrange = Color(0xFFF4A340)
private val FatPurple = Color(0xFF9B6DE3)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnosObrokaScreen(
    tipObroka: String,
    navController: NavController,
    // Zadan kod uređivanja spremljenog obroka; null je novi unos.
    idObrok: Long? = null,
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
    val kartica by viewModel.kartica.collectAsState()
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
    var vrijeme by remember {
        mutableStateOf(LocalTime.now(ZAGREB).truncatedTo(ChronoUnit.MINUTES))
    }
    var tip by remember { mutableStateOf(tipObroka.uppercase()) }
    var tipRucno by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    var showFilterSheet by remember { mutableStateOf(false) }

    var minKalorije by remember { mutableStateOf("") }
    var maxKalorije by remember { mutableStateOf("") }
    var minProteini by remember { mutableStateOf("") }
    var maxProteini by remember { mutableStateOf("") }

    var minUgljikohidrati by remember { mutableStateOf("") }
    var maxUgljikohidrati by remember { mutableStateOf("") }
    var minMasti by remember { mutableStateOf("") }
    var maxMasti by remember { mutableStateOf("") }

    var prikaziOstalo by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // Količina odabrana za jelo; odabir živi u ViewModelu pa ostaje i kad pretraga makne jelo s liste.
    fun kolicinaJela(idJelo: Long): Double = odabir.stavke[idJelo]?.kolicina ?: 0.0

    var detaljJelo by remember { mutableStateOf<Jelo?>(null) }

    // Svaka stavka ide u odabir s količinom iz menija (zbraja se s već odabranom);
    // "Poništi" vraća odabir na stanje prije dodavanja.
    fun dodajMeni(meni: MeniDTO) {
        val staro = viewModel.dodajMeni(meni)

        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val rezultat = snackbarHostState.showSnackbar(
                message = "Dodan meni ${nazivMenija(meni)}",
                actionLabel = "Poništi",
                duration = SnackbarDuration.Short
            )
            if (rezultat == SnackbarResult.ActionPerformed) {
                viewModel.vratiOdabir(staro)
            }
        }
    }

    val context = LocalContext.current

    val sessionManager = remember {
        UserSessionManager(context)
    }

    val prijavljeniKorisnikId by
    sessionManager.korisnikId.collectAsState(initial = null)

    LaunchedEffect(Unit) {
        viewModel.ucitajKategorije()
        viewModel.ucitajJela()
    }

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
    var izbornikOtvoren by remember { mutableStateOf(false) }
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

    // Tip obroka RUCAK/VECERA određuje tip menija; za doručak i užinu bira se chipom.
    val tipFilterMenija = if (tip == "RUCAK" || tip == "VECERA") tip else tipMenija
    val prikazaniMeniji = remember(meniji, tipFilterMenija, samoVegetarijanski) {
        meniji.filter {
            (tipFilterMenija == null || it.tip == tipFilterMenija) &&
                (!samoVegetarijanski || it.vegetarijanski)
        }
    }

    // "Često jedeš" vidljivo je samo bez upita i kategorije; makro filter vrijedi i za njega.
    val prikaziCesto = search.isBlank() && odabranaKategorija == null
    val cestoZaPrikaz = if (prikaziCesto) {
        cestoJedeno.filter { makroFilter.propusta(it.jelo) }
    } else {
        emptyList()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        Box(
            modifier = Modifier
                .size(220.dp)
                .offset(x = 150.dp, y = (-110).dp)
                .clip(CircleShape)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                top = screenTopPadding(),
                end = 20.dp,
                // Prostor za plutajuću navigaciju i sažetak obroka da zadnje jelo ostane vidljivo.
                bottom = screenBottomPadding() + visinaBara + 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (uredivanje) "Uredi obrok" else formatTipObroka(tip),
                        modifier = Modifier.weight(1f),
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (uredivanje && uredivaniObrok != null) {
                        Box {
                            IconButton(
                                onClick = { izbornikOtvoren = true },
                                enabled = !spremanjeUTijeku
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Više opcija"
                                )
                            }

                            DropdownMenu(
                                expanded = izbornikOtvoren,
                                onDismissRequest = { izbornikOtvoren = false }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "Obriši obrok",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        izbornikOtvoren = false
                                        showBrisanjeDialog = true
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (uredivanje) {
                        "Promijeni datum, vrijeme, tip ili jela."
                    } else {
                        "Odaberi jela i količinu za svoj obrok."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (ucitavanjeObroka) {
                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MacroGreen
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DatumVrijemeGumb(
                        ikona = Icons.Default.CalendarToday,
                        tekst = if (datum == danas) "Danas" else datum.format(FORMAT_DATUMA),
                        opis = "Datum",
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f)
                    )

                    DatumVrijemeGumb(
                        ikona = Icons.Default.Schedule,
                        tekst = vrijeme.format(FORMAT_VREMENA),
                        opis = "Vrijeme",
                        onClick = { showTimePicker = true },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TIPOVI_OBROKA.forEach { t ->
                        FilterChip(
                            selected = tip == t,
                            onClick = {
                                tip = t
                                tipRucno = true
                            },
                            label = {
                                Text(
                                    text = formatTipObroka(t),
                                    maxLines = 1,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MacroGreen.copy(alpha = 0.22f),
                                selectedLabelColor = MacroGreen
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val kartice = listOf(KarticaUnosa.JELA to "Jela", KarticaUnosa.MENIJI to "Meniji")
                    kartice.forEachIndexed { index, (k, naziv) ->
                        SegmentedButton(
                            selected = kartica == k,
                            onClick = { viewModel.postaviKarticu(k) },
                            shape = SegmentedButtonDefaults.itemShape(index, kartice.size),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = MacroGreen.copy(alpha = 0.22f),
                                activeContentColor = MacroGreen
                            )
                        ) {
                            Text(naziv)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            if (kartica == KarticaUnosa.JELA) item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    OutlinedTextField(
                        value = search,
                        onValueChange = { viewModel.postaviUpit(it) },
                        placeholder = {
                            Text("Pretraži jelo")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MacroGreen,
                            focusedLabelColor = MacroGreen,
                            cursorColor = MacroGreen
                        )
                    )

                    Surface(
                        onClick = {
                            showFilterSheet = true
                        },
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Filter",
                                tint = MacroGreen,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                if (kategorije.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            KategorijaChip(
                                naziv = "Sve",
                                selected = odabranaKategorija == null,
                                onClick = { viewModel.postaviKategoriju(null) }
                            )
                        }

                        items(kategorije, key = { it.naziv }) { kategorija ->
                            KategorijaChip(
                                naziv = kategorija.naziv,
                                selected = odabranaKategorija == kategorija.naziv,
                                onClick = { viewModel.postaviKategoriju(kategorija.naziv) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
            }

            if (kartica == KarticaUnosa.JELA && greska != null) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
                    ) {
                        Text(
                            text = greska ?: "Došlo je do greške.",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                }
            }

            if (kartica == KarticaUnosa.JELA && cestoZaPrikaz.isNotEmpty()) {
                item {
                    Text(
                        text = "Često jedeš",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Jela iz tvojih obroka u zadnjih 60 dana.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(cestoZaPrikaz, key = { it.jelo.id }) { cesto ->
                            CestoJedenoKartica(
                                cesto = cesto,
                                kolicina = kolicinaJela(cesto.jelo.id),
                                onPlus = { viewModel.povecaj(cesto.jelo) },
                                onMinus = { viewModel.smanji(cesto.jelo.id) },
                                onDetalj = { detaljJelo = cesto.jelo }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))
                }
            }

            if (kartica == KarticaUnosa.JELA) item {
                Text(
                    text = "Jela",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Odaberi količinu za svako jelo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))
            }

            if (kartica == KarticaUnosa.MENIJI) {
                item {
                    MeniFilteri(
                        prikaziTipove = tip != "RUCAK" && tip != "VECERA",
                        tipMenija = tipMenija,
                        samoVegetarijanski = samoVegetarijanski,
                        onTip = { viewModel.postaviTipMenija(it) },
                        onVegetarijanski = { viewModel.postaviSamoVegetarijanski(it) }
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                }

                when {
                    menijiUcitavanje -> item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MacroGreen)
                        }
                    }

                    greskaMenija != null -> item {
                        MeniGreskaKartica(
                            poruka = greskaMenija ?: "",
                            onPonovno = { viewModel.ucitajMenije() }
                        )
                    }

                    prikazaniMeniji.isEmpty() -> item {
                        PraznaKartica(
                            naslov = "Nema menija za odabrane filtere",
                            opis = "Pokušaj promijeniti tip ili ukloniti filter."
                        )
                    }

                    else -> items(prikazaniMeniji, key = { it.id }) { meni ->
                        MeniKartica(
                            meni = meni,
                            onDodaj = { dodajMeni(meni) }
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            } else if (jela.isEmpty()) {
                item {
                    EmptyFoodCard()
                }
            } else {
                items(jela) { jelo ->

                    JeloKolicinaItem(
                        jelo = jelo,
                        kolicina = kolicinaJela(jelo.id),
                        onPlus = { viewModel.povecaj(jelo) },
                        onMinus = { viewModel.smanji(jelo.id) },
                        onDetalj = { detaljJelo = jelo }
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

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
                .padding(bottom = screenBottomPadding() - 12.dp),
            tekstSpremi = if (uredivanje) "Spremi promjene" else "Spremi"
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = screenBottomPadding() + visinaBara + 4.dp
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
            title = { Text("Odbaciti promjene?") },
            text = { Text("Promjene ovog obroka neće biti spremljene.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showOdbaciDialog = false
                        navController.popBackStack()
                    }
                ) {
                    Text("Odbaci", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showOdbaciDialog = false }) {
                    Text("Nastavi uređivati", color = MacroGreen)
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
                    Text("U redu", color = MacroGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Odustani")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = MacroGreen,
                    todayDateBorderColor = MacroGreen,
                    todayContentColor = MacroGreen
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

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        vrijeme = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        if (!tipRucno) {
                            tip = zadaniTipZaVrijeme(vrijeme)
                        }
                        showTimePicker = false
                    }
                ) {
                    Text("U redu", color = MacroGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Odustani")
                }
            },
            text = {
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(
                        selectorColor = MacroGreen,
                        timeSelectorSelectedContainerColor = MacroGreen.copy(alpha = 0.25f)
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
            onDismiss = { detaljJelo = null }
        )
    }

    if (showFilterSheet) {
        FilterBottomSheet(
            minKalorije = minKalorije,
            maxKalorije = maxKalorije,
            minProteini = minProteini,
            maxProteini = maxProteini,
            minUgljikohidrati = minUgljikohidrati,
            maxUgljikohidrati = maxUgljikohidrati,
            minMasti = minMasti,
            maxMasti = maxMasti,
            prikaziOstalo = prikaziOstalo,

            onMinKalorijeChange = { minKalorije = it },
            onMaxKalorijeChange = { maxKalorije = it },
            onMinProteiniChange = { minProteini = it },
            onMaxProteiniChange = { maxProteini = it },

            onMinUgljikohidratiChange = { minUgljikohidrati = it },
            onMaxUgljikohidratiChange = { maxUgljikohidrati = it },
            onMinMastiChange = { minMasti = it },
            onMaxMastiChange = { maxMasti = it },

            onToggleOstalo = {
                prikaziOstalo = !prikaziOstalo
            },

            onApply = {
                viewModel.primijeniMakroFilter(MakroFilter(
                    minKalorije = minKalorije.replace(',', '.').toDoubleOrNull(),
                    maxKalorije = maxKalorije.replace(',', '.').toDoubleOrNull(),
                    minProteini = minProteini.replace(',', '.').toDoubleOrNull(),
                    maxProteini = maxProteini.replace(',', '.').toDoubleOrNull(),
                    minUgljikohidrati = minUgljikohidrati.replace(',', '.').toDoubleOrNull(),
                    maxUgljikohidrati = maxUgljikohidrati.replace(',', '.').toDoubleOrNull(),
                    minMasti = minMasti.replace(',', '.').toDoubleOrNull(),
                    maxMasti = maxMasti.replace(',', '.').toDoubleOrNull()
                ))

                showFilterSheet = false
            },

            onReset = {
                minKalorije = ""
                maxKalorije = ""
                minProteini = ""
                maxProteini = ""
                minUgljikohidrati = ""
                maxUgljikohidrati = ""
                minMasti = ""
                maxMasti = ""

                prikaziOstalo = false

                viewModel.resetirajMakroFilter()
            },

            onDismiss = {
                showFilterSheet = false
            }
        )
    }}

@Composable
fun JeloKolicinaItem(
    jelo: Jelo,
    kolicina: Double,
    onPlus: () -> Unit,
    onMinus: () -> Unit,
    onDetalj: () -> Unit
) {
    val selected = kolicina > 0.0

    // Tap dodaje porciju, dugi pritisak otvara detalj jela.
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .combinedClickable(onClick = onPlus, onLongClick = onDetalj),
        shape = RoundedCornerShape(22.dp),
        color =
            if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        border =
            if (selected) {
                androidx.compose.foundation.BorderStroke(
                    width = 1.5.dp,
                    color = MacroGreen.copy(alpha = 0.65f)
                )
            } else {
                null
            },
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = jelo.naziv,
                            modifier = Modifier.weight(1f, fill = false),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp
                        )

                        IconButton(
                            onClick = onDetalj,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = "Detalj jela",
                                tint = MacroGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${formatKcal(jelo.kalorije)} kcal",
                        color = MacroGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                QuantitySelector(
                    quantity = kolicina,
                    onPlus = onPlus,
                    onMinus = onMinus
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            HorizontalDivider(
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = 0.09f
                    )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {
                FoodMacro(
                    label = "Proteini",
                    value = jelo.proteini,
                    color = ProteinBlue
                )

                FoodMacro(
                    label = "UH",
                    value = jelo.ugljikohidrati,
                    color = CarbsOrange
                )

                FoodMacro(
                    label = "Masti",
                    value = jelo.masti,
                    color = FatPurple
                )
            }
        }
    }
}

@Composable
private fun QuantitySelector(
    quantity: Double,
    onPlus: () -> Unit,
    onMinus: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color =
            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    ) {
        Row(
            verticalAlignment =
                Alignment.CenterVertically,
            modifier = Modifier.padding(4.dp)
        ) {

            IconButton(
                onClick = onMinus,
                enabled = quantity > 0.0,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Smanji",
                    tint =
                        if (quantity > 0.0) {
                            MacroGreen
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                                .copy(alpha = 0.30f)
                        }
                )
            }

            Text(
                text = formatKolicina(quantity),
                modifier = Modifier.width(36.dp),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                textAlign =
                    androidx.compose.ui.text.style.TextAlign.Center
            )

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MacroGreen),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onPlus,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Povećaj",
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FoodMacro(
    label: String,
    value: Double,
    color: Color
) {
    Column(
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {
        Text(
            text = "${formatMakro(value)} g",
            color = color,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun KategorijaChip(
    naziv: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = naziv,
                maxLines = 1
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MacroGreen.copy(alpha = 0.22f),
            selectedLabelColor = MacroGreen
        )
    )
}

@Composable
private fun CestoJedenoKartica(
    cesto: CestoJedenoJelo,
    kolicina: Double,
    onPlus: () -> Unit,
    onMinus: () -> Unit,
    onDetalj: () -> Unit
) {
    val selected = kolicina > 0.0

    // Tap i dalje dodaje porciju, dugi pritisak otvara detalj jela.
    Surface(
        modifier = Modifier
            .width(190.dp)
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(onClick = onPlus, onLongClick = onDetalj),
        shape = RoundedCornerShape(20.dp),
        color =
            if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        border =
            if (selected) {
                androidx.compose.foundation.BorderStroke(
                    width = 1.5.dp,
                    color = MacroGreen.copy(alpha = 0.65f)
                )
            } else {
                null
            },
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = cesto.jelo.naziv,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 2,
                minLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${formatKcal(cesto.jelo.kalorije)} kcal",
                color = MacroGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${cesto.brojPuta}× u 60 dana",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )

                IconButton(
                    onClick = onDetalj,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Detalj jela",
                        tint = MacroGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (selected) {
                    IconButton(
                        onClick = onMinus,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Smanji",
                            tint = MacroGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = formatKolicina(kolicina),
                        color = MacroGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

private fun nazivMenija(meni: MeniDTO): String =
    listOfNotNull(formatTipObroka(meni.tip), meni.redniBroj?.toString()).joinToString(" ")

@Composable
private fun MeniFilteri(
    prikaziTipove: Boolean,
    tipMenija: String?,
    samoVegetarijanski: Boolean,
    onTip: (String?) -> Unit,
    onVegetarijanski: (Boolean) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (prikaziTipove) {
            KategorijaChip(
                naziv = "Ručak",
                selected = tipMenija == "RUCAK",
                onClick = { onTip(if (tipMenija == "RUCAK") null else "RUCAK") }
            )

            KategorijaChip(
                naziv = "Večera",
                selected = tipMenija == "VECERA",
                onClick = { onTip(if (tipMenija == "VECERA") null else "VECERA") }
            )
        }

        KategorijaChip(
            naziv = "Vegetarijanski",
            selected = samoVegetarijanski,
            onClick = { onVegetarijanski(!samoVegetarijanski) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MeniKartica(
    meni: MeniDTO,
    onDodaj: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = nazivMenija(meni),
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp
                )

                if (meni.vegetarijanski) {
                    Text(
                        text = "Vegetarijanski",
                        color = MacroGreen,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            meni.stavke.forEach { stavka ->
                val tekst = buildString {
                    if (stavka.priblizno) append("≈ ")
                    if (stavka.kolicina != 1.0) append("${formatKolicina(stavka.kolicina)}× ")
                    append(stavka.oznaka)
                }

                if (stavka.priblizno) {
                    // Dugi pritisak otvara objašnjenje.
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = {
                            PlainTooltip {
                                Text("Približna vrijednost: jelo nema vlastitu recepturu, uzeto je slično jelo.")
                            }
                        },
                        state = rememberTooltipState()
                    ) {
                        Text(
                            text = tekst,
                            modifier = Modifier.padding(vertical = 2.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    Text(
                        text = tekst,
                        modifier = Modifier.padding(vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.09f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${formatKcal(meni.kalorije)} kcal",
                    color = MacroGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )

                FoodMacro(label = "Proteini", value = meni.proteini, color = ProteinBlue)
                FoodMacro(label = "UH", value = meni.ugljikohidrati, color = CarbsOrange)
                FoodMacro(label = "Masti", value = meni.masti, color = FatPurple)
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onDodaj,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MacroGreen)
            ) {
                Text(text = "Dodaj meni", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun MeniGreskaKartica(
    poruka: String,
    onPonovno: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = poruka, color = MaterialTheme.colorScheme.error)

            TextButton(onClick = onPonovno) {
                Text("Pokušaj ponovno", color = MacroGreen)
            }
        }
    }
}

@Composable
private fun PraznaKartica(
    naslov: String,
    opis: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Text(
                text = naslov,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = opis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun EmptyFoodCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(22.dp)
        ) {
            Text(
                text = "Nema jela za odabrane filtere",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Pokušaj promijeniti pretragu, kategoriju ili filter.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBottomSheet(
    minKalorije: String,
    maxKalorije: String,
    minProteini: String,
    maxProteini: String,
    minUgljikohidrati: String,
    maxUgljikohidrati: String,
    minMasti: String,
    maxMasti: String,
    prikaziOstalo: Boolean,

    onMinKalorijeChange: (String) -> Unit,
    onMaxKalorijeChange: (String) -> Unit,
    onMinProteiniChange: (String) -> Unit,
    onMaxProteiniChange: (String) -> Unit,

    onMinUgljikohidratiChange: (String) -> Unit,
    onMaxUgljikohidratiChange: (String) -> Unit,
    onMinMastiChange: (String) -> Unit,
    onMaxMastiChange: (String) -> Unit,

    onToggleOstalo: () -> Unit,
    onApply: () -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(
            topStart = 28.dp,
            topEnd = 28.dp
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 30.dp)
        ) {

            Text(
                text = "Filtriraj jela",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Postavi raspon koji ti odgovara.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            FilterSectionTitle("Kalorije")

            Spacer(modifier = Modifier.height(8.dp))

            FilterRangeRow(
                minValue = minKalorije,
                maxValue = maxKalorije,
                onMinChange = onMinKalorijeChange,
                onMaxChange = onMaxKalorijeChange
            )

            Spacer(modifier = Modifier.height(20.dp))

            FilterSectionTitle("Proteini")

            Spacer(modifier = Modifier.height(8.dp))

            FilterRangeRow(
                minValue = minProteini,
                maxValue = maxProteini,
                onMinChange = onMinProteiniChange,
                onMaxChange = onMaxProteiniChange
            )

            Spacer(modifier = Modifier.height(10.dp))

            TextButton(
                onClick = onToggleOstalo,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text =
                        if (prikaziOstalo)
                            "Sakrij dodatne filtere"
                        else
                            "Još filtera",
                    color = MacroGreen,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (prikaziOstalo) {

                Spacer(modifier = Modifier.height(10.dp))

                FilterSectionTitle("Ugljikohidrati")

                Spacer(modifier = Modifier.height(8.dp))

                FilterRangeRow(
                    minValue = minUgljikohidrati,
                    maxValue = maxUgljikohidrati,
                    onMinChange = onMinUgljikohidratiChange,
                    onMaxChange = onMaxUgljikohidratiChange
                )

                Spacer(modifier = Modifier.height(20.dp))

                FilterSectionTitle("Masti")

                Spacer(modifier = Modifier.height(8.dp))

                FilterRangeRow(
                    minValue = minMasti,
                    maxValue = maxMasti,
                    onMinChange = onMinMastiChange,
                    onMaxChange = onMaxMastiChange
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            Button(
                onClick = onApply,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MacroGreen
                )
            ) {
                Text(
                    text = "Primijeni filtere",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onReset,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Resetiraj filtere",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FilterNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = { noviUnos ->
            if (jeDecimalniUnos(noviUnos)) {
                onValueChange(noviUnos)
            }
        },
        label = {
            Text(label)
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MacroGreen,
            focusedLabelColor = MacroGreen,
            cursorColor = MacroGreen
        )
    )
}

/** Zadani tip prema vremenu: do 10:30 doručak, do 15:00 ručak, do 18:00 užina, inače večera. */
private fun zadaniTipZaVrijeme(vrijeme: LocalTime): String = when {
    !vrijeme.isAfter(LocalTime.of(10, 30)) -> "DORUCAK"
    !vrijeme.isAfter(LocalTime.of(15, 0)) -> "RUCAK"
    !vrijeme.isAfter(LocalTime.of(18, 0)) -> "UZINA"
    else -> "VECERA"
}

@Composable
private fun DatumVrijemeGumb(
    ikona: androidx.compose.ui.graphics.vector.ImageVector,
    tekst: String,
    opis: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = ikona,
                contentDescription = opis,
                tint = MacroGreen,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = opis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = tekst,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

private fun formatTipObroka(tip: String): String {
    return when (tip.uppercase()) {
        "DORUCAK" -> "Doručak"
        "RUCAK" -> "Ručak"
        "VECERA" -> "Večera"
        "UZINA" -> "Užina"
        else -> tip
    }
}

@Composable
private fun FilterSectionTitle(
    title: String
) {
    Text(
        text = title,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp
    )
}

@Composable
private fun FilterRangeRow(
    minValue: String,
    maxValue: String,
    onMinChange: (String) -> Unit,
    onMaxChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        FilterCompactField(
            value = minValue,
            onValueChange = onMinChange,
            label = "Min",
            modifier = Modifier.weight(1f)
        )

        FilterCompactField(
            value = maxValue,
            onValueChange = onMaxChange,
            label = "Max",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FilterCompactField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { noviUnos ->
            if (jeDecimalniUnos(noviUnos)) {
                onValueChange(noviUnos)
            }
        },
        label = {
            Text(label)
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal
        ),
        shape = RoundedCornerShape(15.dp),
        modifier = modifier,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MacroGreen,
            focusedLabelColor = MacroGreen,
            cursorColor = MacroGreen
        )
    )
}

private fun jeDecimalniUnos(unos: String): Boolean =
    unos.count { it == ',' || it == '.' } <= 1 &&
            unos.all { it.isDigit() || it == ',' || it == '.' }
