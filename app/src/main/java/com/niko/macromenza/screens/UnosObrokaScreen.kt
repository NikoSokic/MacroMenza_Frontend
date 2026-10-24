package com.niko.macromenza.screens

import com.niko.macromenza.util.formatKcal
import com.niko.macromenza.util.formatKolicina
import com.niko.macromenza.util.formatMakro
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
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
import com.niko.macromenza.model.Jelo
import com.niko.macromenza.model.StavkaObrokaRequest
import com.niko.macromenza.session.UserSessionManager
import com.niko.macromenza.ui.components.screenBottomPadding
import com.niko.macromenza.ui.components.screenTopPadding
import com.niko.macromenza.ui.theme.MacroGreen
import com.niko.macromenza.viewmodel.JelaViewModel
import kotlinx.coroutines.delay
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
    viewModel: JelaViewModel = viewModel()
) {
    val jela by viewModel.jela.collectAsState()
    val greska by viewModel.greska.collectAsState()
    val spremanjeUTijeku by viewModel.spremanjeUTijeku.collectAsState()
    val spremljeniObrok by viewModel.spremljeniObrok.collectAsState()
    val greskaSpremanja by viewModel.greskaSpremanja.collectAsState()

    val danas = remember { LocalDate.now(ZAGREB) }
    var datum by remember { mutableStateOf(danas) }
    var vrijeme by remember {
        mutableStateOf(LocalTime.now(ZAGREB).truncatedTo(ChronoUnit.MINUTES))
    }
    var tip by remember { mutableStateOf(tipObroka.uppercase()) }
    var tipRucno by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    var search by remember { mutableStateOf("") }
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

    val kolicine = remember {
        mutableStateMapOf<Long, Double>()
    }

    val context = LocalContext.current

    val sessionManager = remember {
        UserSessionManager(context)
    }

    val prijavljeniKorisnikId by
    sessionManager.korisnikId.collectAsState(initial = null)

    LaunchedEffect(search) {
        delay(300)
        viewModel.ucitajJela(
            search = search.ifBlank { null }
        )
    }

    LaunchedEffect(spremljeniObrok) {
        val obrok = spremljeniObrok ?: return@LaunchedEffect
        viewModel.ocistiRezultatSpremanja()
        navController.navigate("obrok_spremljen/${obrok.tipObroka}")
    }

    LaunchedEffect(greskaSpremanja) {
        val poruka = greskaSpremanja ?: return@LaunchedEffect
        viewModel.ocistiRezultatSpremanja()
        snackbarHostState.showSnackbar(poruka)
    }

    val odabranaJela = jela.filter {
        (kolicine[it.id] ?: 0.0) > 0.0
    }

    val ukupnoKalorija = odabranaJela.sumOf {
        it.kalorije * (kolicine[it.id] ?: 0.0)
    }

    val ukupnoProteina = odabranaJela.sumOf {
        it.proteini * (kolicine[it.id] ?: 0.0)
    }

    val ukupnoUh = odabranaJela.sumOf {
        it.ugljikohidrati * (kolicine[it.id] ?: 0.0)
    }

    val ukupnoMasti = odabranaJela.sumOf {
        it.masti * (kolicine[it.id] ?: 0.0)
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
                bottom = screenBottomPadding()
            ),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {

            item {
                Text(
                    text = formatTipObroka(tip),
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Odaberi jela i količinu za svoj obrok.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

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

                Spacer(modifier = Modifier.height(20.dp))
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
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

                Spacer(modifier = Modifier.height(18.dp))
            }

            item {
                MealSummaryCard(
                    ukupnoKalorija = ukupnoKalorija,
                    ukupnoProteina = ukupnoProteina,
                    ukupnoUh = ukupnoUh,
                    ukupnoMasti = ukupnoMasti,
                    enabled = odabranaJela.isNotEmpty() && !spremanjeUTijeku,
                    spremanjeUTijeku = spremanjeUTijeku,
                    onSave = {
                        val idKorisnik =
                            prijavljeniKorisnikId ?: return@MealSummaryCard

                        viewModel.spremiObrok(
                            idKorisnik = idKorisnik,
                            datum = datum.toString(),
                            vrijeme = vrijeme.format(FORMAT_VREMENA),
                            tipObroka = tip,
                            stavke = odabranaJela.map { jelo ->
                                StavkaObrokaRequest(
                                    idJelo = jelo.id,
                                    kolicina = kolicine[jelo.id] ?: 0.0
                                )
                            }
                        )
                    }
                )

                Spacer(modifier = Modifier.height(22.dp))
            }

            if (greska != null) {
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

            item {
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

            if (jela.isEmpty()) {
                item {
                    EmptyFoodCard()
                }
            } else {
                items(jela) { jelo ->

                    val kolicina = kolicine[jelo.id] ?: 0.0

                    // Prvi + daje 1 porciju, dalje koraci po 0,5; − s 0,5 miče jelo iz obroka.
                    JeloKolicinaItem(
                        jelo = jelo,
                        kolicina = kolicina,
                        onPlus = {
                            kolicine[jelo.id] = if (kolicina == 0.0) 1.0 else kolicina + 0.5
                        },
                        onMinus = {
                            if (kolicina > 0.0) {
                                kolicine[jelo.id] = if (kolicina <= 0.5) 0.0 else kolicina - 0.5
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = screenBottomPadding()
                )
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
                            datum = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
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
                viewModel.filtrirajJela(
                    minKalorije = minKalorije.replace(',', '.').toDoubleOrNull(),
                    maxKalorije = maxKalorije.replace(',', '.').toDoubleOrNull(),
                    minProteini = minProteini.replace(',', '.').toDoubleOrNull(),
                    maxProteini = maxProteini.replace(',', '.').toDoubleOrNull(),
                    minUgljikohidrati = minUgljikohidrati.replace(',', '.').toDoubleOrNull(),
                    maxUgljikohidrati = maxUgljikohidrati.replace(',', '.').toDoubleOrNull(),
                    minMasti = minMasti.replace(',', '.').toDoubleOrNull(),
                    maxMasti = maxMasti.replace(',', '.').toDoubleOrNull()
                )

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

                viewModel.ucitajJela(
                    search = search.ifBlank { null }
                )
            },

            onDismiss = {
                showFilterSheet = false
            }
        )
    }}

@Composable
private fun MealSummaryCard(
    ukupnoKalorija: Double,
    ukupnoProteina: Double,
    ukupnoUh: Double,
    ukupnoMasti: Double,
    enabled: Boolean,
    spremanjeUTijeku: Boolean,
    onSave: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MacroGreen
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = "Tvoj obrok",
                color = Color.White.copy(alpha = 0.75f),
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = "${formatKcal(ukupnoKalorija)} kcal",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {
                SummaryMacro(
                    label = "Proteini",
                    value = ukupnoProteina,
                    color = Color.White
                )

                SummaryMacro(
                    label = "UH",
                    value = ukupnoUh,
                    color = Color.White
                )

                SummaryMacro(
                    label = "Masti",
                    value = ukupnoMasti,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onSave,
                enabled = enabled,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = MacroGreen,
                    disabledContainerColor =
                        Color.White.copy(alpha = 0.25f),
                    disabledContentColor =
                        Color.White.copy(alpha = 0.65f)
                )
            ) {
                Text(
                    text = if (spremanjeUTijeku) "Spremam..." else "Spremi obrok",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun SummaryMacro(
    label: String,
    value: Double,
    color: Color
) {
    Column {
        Text(
            text = "${formatMakro(value)} g",
            color = color,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp
        )

        Text(
            text = label,
            color = color.copy(alpha = 0.70f),
            fontSize = 11.sp
        )
    }
}

@Composable
fun JeloKolicinaItem(
    jelo: Jelo,
    kolicina: Double,
    onPlus: () -> Unit,
    onMinus: () -> Unit
) {
    val selected = kolicina > 0.0

    Surface(
        modifier = Modifier.fillMaxWidth(),
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
                    Text(
                        text = jelo.naziv,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp
                    )

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
                text = "Nema pronađenih jela",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Pokušaj promijeniti pretragu ili filter.",
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
