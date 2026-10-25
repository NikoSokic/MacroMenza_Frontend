package com.niko.macromenza.screens

import com.niko.macromenza.util.formatGrami
import com.niko.macromenza.util.formatKcal
import com.niko.macromenza.util.formatKcalTisuce
import com.niko.macromenza.util.formatMakro
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.niko.macromenza.model.Tjedan
import com.niko.macromenza.model.TjedanDan
import com.niko.macromenza.session.UserSessionManager
import com.niko.macromenza.ui.components.screenBottomPadding
import com.niko.macromenza.ui.components.screenTopPadding
import com.niko.macromenza.ui.theme.MacroGreen
import com.niko.macromenza.viewmodel.PovijestViewModel
import java.time.LocalDate
import java.time.ZoneId

private val ZAGREB = ZoneId.of("Europe/Zagreb")
private val OZNAKE_DANA = listOf("P", "U", "S", "Č", "P", "S", "N")
private val NAZIVI_DANA = listOf("Pon", "Uto", "Sri", "Čet", "Pet", "Sub", "Ned")

private val TjedanProteinBlue = Color(0xFF4F8EF7)
private val TjedanCarbsOrange = Color(0xFFF4A340)
private val TjedanFatPurple = Color(0xFF9B6DE3)

enum class PovijestTab {
    TJEDNI,
    DNEVNI
}

@Composable
fun PovijestScreen(
    navController: NavController,
    viewModel: PovijestViewModel = viewModel()
) {
    val tjedan by viewModel.tjedan.collectAsState()
    val greskaTjedna by viewModel.greskaTjedna.collectAsState()
    val povijestDana by viewModel.povijestDana.collectAsState()
    val obrociDana by viewModel.obrociDana.collectAsState()
    val primjerUTijeku by viewModel.primjerUTijeku.collectAsState()

    // Saveable: nakon povratka iz uređivanja obroka otvara se isti dan.
    var odabraniTab by rememberSaveable {
        mutableStateOf(PovijestTab.TJEDNI)
    }

    var odabraniDatum by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    val context = LocalContext.current

    val sessionManager = remember {
        UserSessionManager(context)
    }

    val prijavljeniKorisnikId by
    sessionManager.korisnikId.collectAsState(initial = null)

    // Pokreće se i pri povratku s uređivanja, pa graf i dan pokazuju svježe podatke.
    LaunchedEffect(prijavljeniKorisnikId) {
        prijavljeniKorisnikId?.let { id ->
            viewModel.ucitajTjedan(id)
        }
    }

    // Učitavanje ili uklanjanje primjera mijenja i odabrani dan.
    LaunchedEffect(odabraniDatum, prijavljeniKorisnikId, tjedan?.imaPrimjer) {
        odabraniDatum?.let { datum ->
            prijavljeniKorisnikId?.let { id ->
                viewModel.ucitajDan(id, datum)
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    BrisanjeObrokaEfekti(
        viewModel = viewModel,
        navController = navController,
        snackbarHostState = snackbarHostState,
        idKorisnik = prijavljeniKorisnikId
    )

    val pragPovlacenja = with(LocalDensity.current) { 80.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        // Dekorativni krug
        Box(
            modifier = Modifier
                .size(240.dp)
                .offset(
                    x = 190.dp,
                    y = (-100).dp
                )
                .clip(CircleShape)
                .background(
                    MaterialTheme.colorScheme.primary.copy(
                        alpha = 0.10f
                    )
                )
        )

        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                // Povlačenje lijevo/desno na tabu "Tjedan" mijenja tjedan (desno = prethodni).
                .pointerInput(odabraniTab, prijavljeniKorisnikId) {
                    val id = prijavljeniKorisnikId
                    if (odabraniTab != PovijestTab.TJEDNI || id == null) return@pointerInput

                    var pomak = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { pomak = 0f },
                        onDragEnd = {
                            when {
                                pomak > pragPovlacenja -> viewModel.prethodniTjedan(id)
                                pomak < -pragPovlacenja -> viewModel.sljedeciTjedan(id)
                            }
                        }
                    ) { _, dragAmount ->
                        pomak += dragAmount
                    }
                },
            contentPadding = PaddingValues(
                start = 22.dp,
                end = 22.dp,
                top = screenTopPadding(),
                bottom = screenBottomPadding()
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {

            // HEADER
            item {
                Column {
                    Text(
                        text = "Povijest",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Tvoj napredak kroz vrijeme",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // TAB SELECTOR
            item {
                PovijestTabSelector(
                    selected = odabraniTab,
                    onSelected = { tab ->

                        if (tab == PovijestTab.DNEVNI) {
                            // Zadnji dan odabranog tjedna koji nije u budućnosti.
                            val danas = LocalDate.now(ZAGREB)
                            odabraniDatum =
                                tjedan
                                    ?.dani
                                    ?.lastOrNull { !LocalDate.parse(it.datum).isAfter(danas) }
                                    ?.datum
                                    ?: danas.toString()
                        }

                        odabraniTab = tab
                    }
                )
            }

            if (odabraniTab == PovijestTab.TJEDNI) {

                val trenutni = tjedan

                if (trenutni == null) {

                    item {
                        if (greskaTjedna) {
                            PorukaTjednaCard(
                                naslov = "Povijest nije dostupna",
                                tekst = "Provjeri vezu s poslužiteljem."
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MacroGreen)
                            }
                        }
                    }

                } else {

                    item {
                        TjedanZaglavlje(
                            tjedan = trenutni,
                            onPrethodni = { prijavljeniKorisnikId?.let { viewModel.prethodniTjedan(it) } },
                            onSljedeci = { prijavljeniKorisnikId?.let { viewModel.sljedeciTjedan(it) } },
                            onOvajTjedan = { prijavljeniKorisnikId?.let { viewModel.ovajTjedan(it) } }
                        )
                    }

                    if (trenutni.imaPrimjer) {

                        item {
                            PrimjerTraka(
                                uTijeku = primjerUTijeku,
                                onUkloni = { prijavljeniKorisnikId?.let { viewModel.ukloniPrimjerTjedna(it) } }
                            )
                        }
                    }

                    if (trenutni.daniSUnosom == 0) {

                        item {
                            PorukaTjednaCard(
                                naslov = "Nema unosa u ovom tjednu",
                                tekst = "Dodaj obroke ili prelistaj na drugi tjedan."
                            )
                        }

                        // Primjer se stvara za zadnjih 7 dana (bez danas), pa tjedan mora imati barem jedan takav dan.
                        val danas = LocalDate.now(ZAGREB)
                        val pocetakTjedna = LocalDate.parse(trenutni.pocetak)
                        val krajTjedna = LocalDate.parse(trenutni.kraj)
                        val mozePrimjer = !pocetakTjedna.isAfter(danas.minusDays(1)) &&
                            !krajTjedna.isBefore(danas.minusDays(7))

                        if (mozePrimjer) {
                            item {
                                OutlinedButton(
                                    onClick = { prijavljeniKorisnikId?.let { viewModel.ucitajPrimjerTjedna(it) } },
                                    enabled = !primjerUTijeku,
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                ) {
                                    Text(
                                        text = "Učitaj primjer tjedna",
                                        color = MacroGreen,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                    } else {

                        item {
                            TjedniGrafCard(
                                tjedan = trenutni,
                                onOtvoriDan = { datum ->
                                    odabraniDatum = datum
                                    odabraniTab = PovijestTab.DNEVNI
                                }
                            )
                        }

                        item {
                            TjednaStatistikaCard(tjedan = trenutni)
                        }
                    }
                }

            } else {

                item {
                    DaySelector(
                        datum = odabraniDatum,
                        onPrevious = {

                            odabraniDatum?.let {
                                odabraniDatum =
                                    LocalDate
                                        .parse(it)
                                        .minusDays(1)
                                        .toString()
                            }
                        },
                        onNext = {

                            odabraniDatum?.let {
                                odabraniDatum =
                                    LocalDate
                                        .parse(it)
                                        .plusDays(1)
                                        .toString()
                            }
                        }
                    )
                }

                povijestDana?.let { dan ->

                    val imaObroka =
                        dan.obroci.any { (_, stavke) ->
                            stavke.isNotEmpty()
                        }

                    if (!imaObroka) {

                        item {
                            EmptyDayCard()
                        }

                    } else {

                        item {
                            DailySummaryCard(
                                kalorije =
                                    dan.ukupnoKalorije,
                                proteini =
                                    dan.ukupnoProteini,
                                uh =
                                    dan.ukupnoUgljikohidrati,
                                masti =
                                    dan.ukupnoMasti
                            )
                        }

                        item {
                            ObrociDana(
                                obroci = obrociDana,
                                onUredi = { navController.navigate(rutaUredivanjaObroka(it)) },
                                onObrisi = { obrok ->
                                    prijavljeniKorisnikId?.let { viewModel.obrisiObrok(it, obrok) }
                                }
                            )
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = screenBottomPadding())
        )
    }
}

@Composable
private fun PovijestTabSelector(
    selected: PovijestTab,
    onSelected: (PovijestTab) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(18.dp),
        color =
            MaterialTheme.colorScheme.primary.copy(
                alpha = 0.12f
            )
    ) {

        Row(
            modifier = Modifier.padding(5.dp)
        ) {

            HistoryTab(
                text = "Tjedan",
                selected =
                    selected == PovijestTab.TJEDNI,
                onClick = {
                    onSelected(PovijestTab.TJEDNI)
                },
                modifier = Modifier.weight(1f)
            )

            HistoryTab(
                text = "Dan",
                selected =
                    selected == PovijestTab.DNEVNI,
                onClick = {
                    onSelected(PovijestTab.DNEVNI)
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun HistoryTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(14.dp),
        color =
            if (selected) {
                MacroGreen
            } else {
                Color.Transparent
            }
    ) {

        Box(
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontWeight = FontWeight.SemiBold,
                color =
                    if (selected) {
                        Color.White
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
            )
        }
    }
}

@Composable
private fun DaySelector(
    datum: String?,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {

        IconButton(
            onClick = onPrevious
        ) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                text = datum ?: "Odaberi datum",
                style =
                    MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color =
                    MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Dnevni pregled",
                style =
                    MaterialTheme.typography.bodySmall,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(
            onClick = onNext
        ) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun DailySummaryCard(
    kalorije: Double,
    proteini: Double,
    uh: Double,
    masti: Double
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MacroGreen
    ) {

        Column(
            modifier = Modifier.padding(22.dp)
        ) {

            Text(
                text = "Ukupni dnevni unos",
                color =
                    Color.White.copy(alpha = 0.8f)
            )

            Text(
                text = "${formatKcal(kalorije)} kcal",
                style =
                    MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {
                DailyMacro(
                    title = "Proteini",
                    value = proteini
                )

                DailyMacro(
                    title = "UH",
                    value = uh
                )

                DailyMacro(
                    title = "Masti",
                    value = masti
                )
            }
        }
    }
}

@Composable
private fun DailyMacro(
    title: String,
    value: Double
) {
    Column {

        Text(
            text = "${formatMakro(value)} g",
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = title,
            color =
                Color.White.copy(alpha = 0.7f),
            style =
                MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun EmptyDayCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color =
            MaterialTheme.colorScheme.primary.copy(
                alpha = 0.12f
            )
    ) {

        Column(
            modifier = Modifier.padding(20.dp)
        ) {

            Text(
                text = "Nema unosa za ovaj dan",
                fontWeight = FontWeight.Bold,
                color =
                    MaterialTheme.colorScheme.onSurface
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text =
                    "Za odabrani datum nije evidentiran nijedan obrok.",
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** "20. – 26. 10. 2026.", preko mjeseca "27. 10. – 2. 11. 2026.", preko godine s obje godine. */
private fun formatRasponTjedna(pocetak: String, kraj: String): String {
    val od = LocalDate.parse(pocetak)
    val doDana = LocalDate.parse(kraj)

    return when {
        od.year != doDana.year ->
            "${od.dayOfMonth}. ${od.monthValue}. ${od.year}. – ${doDana.dayOfMonth}. ${doDana.monthValue}. ${doDana.year}."
        od.monthValue != doDana.monthValue ->
            "${od.dayOfMonth}. ${od.monthValue}. – ${doDana.dayOfMonth}. ${doDana.monthValue}. ${doDana.year}."
        else ->
            "${od.dayOfMonth}. – ${doDana.dayOfMonth}. ${doDana.monthValue}. ${doDana.year}."
    }
}

@Composable
private fun TjedanZaglavlje(
    tjedan: Tjedan,
    onPrethodni: () -> Unit,
    onSljedeci: () -> Unit,
    onOvajTjedan: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        StrelicaTjedna(
            ikona = Icons.Default.ChevronLeft,
            opis = "Prethodni tjedan",
            omoguceno = tjedan.imaPrethodni,
            onClick = onPrethodni
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = formatRasponTjedna(tjedan.pocetak, tjedan.kraj),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            // Na tekućem tjednu nema sljedećeg, pa je gumb tada samo oznaka.
            if (tjedan.imaSljedeci) {
                TextButton(
                    onClick = onOvajTjedan,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text(
                        text = "Ovaj tjedan",
                        color = MacroGreen,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                Text(
                    text = "Ovaj tjedan",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
        }

        StrelicaTjedna(
            ikona = Icons.Default.ChevronRight,
            opis = "Sljedeći tjedan",
            omoguceno = tjedan.imaSljedeci,
            onClick = onSljedeci
        )
    }
}

@Composable
private fun StrelicaTjedna(
    ikona: androidx.compose.ui.graphics.vector.ImageVector,
    opis: String,
    omoguceno: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary.copy(
            alpha = if (omoguceno) 0.18f else 0.06f
        )
    ) {
        IconButton(
            onClick = onClick,
            enabled = omoguceno
        ) {
            Icon(
                imageVector = ikona,
                contentDescription = opis,
                tint = MaterialTheme.colorScheme.primary.copy(
                    alpha = if (omoguceno) 1f else 0.3f
                )
            )
        }
    }
}

/**
 * Stupčasti graf kalorija po danu s isprekidanom linijom cilja. Prvi dodir na stupac pokazuje kcal,
 * drugi dodir na isti stupac (ili "Otvori dan") otvara karticu "Dan". Dani u budućnosti se ne mogu odabrati.
 */
@Composable
private fun TjedniGrafCard(
    tjedan: Tjedan,
    onOtvoriDan: (String) -> Unit
) {
    val dani = tjedan.dani
    val danas = LocalDate.now(ZAGREB)

    var odabraniIndeks by rememberSaveable(tjedan.pocetak) {
        mutableStateOf<Int?>(null)
    }

    val bojaUCilju = MacroGreen
    val bojaIzvanCilja = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
    val bojaObrisa = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    val bojaCilja = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
    val bojaOdabira = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    val bojaOznake = MaterialTheme.colorScheme.onSurface
    val bojaPodloge = MaterialTheme.colorScheme.surface

    val textMeasurer = rememberTextMeasurer()
    val stilOznake = MaterialTheme.typography.labelMedium.copy(
        color = bojaOznake,
        fontWeight = FontWeight.Bold
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface
    ) {

        Column(
            modifier = Modifier.padding(20.dp)
        ) {

            Text(
                text = "Kalorije po danima",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
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

                            if (LocalDate.parse(dan.datum).isAfter(danas)) return@detectTapGestures

                            if (odabraniIndeks == indeks) {
                                onOtvoriDan(dan.datum)
                            } else {
                                odabraniIndeks = indeks
                            }
                        }
                    }
            ) {
                val maxVrijednost = maxOf(
                    dani.maxOfOrNull { it.kalorije } ?: 0.0,
                    dani.maxOfOrNull { it.ciljKalorije ?: 0.0 } ?: 0.0,
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
                        val visina = maxOf(size.height - y(dan.kalorije), 4.dp.toPx())

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
                    dan.ciljKalorije?.let { cilj ->
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
                    val tekst = if (dan.imaUnos) formatKcalTisuce(dan.kalorije) else "0"
                    val izmjereno = textMeasurer.measure(tekst, stilOznake)
                    val ruba = 4.dp.toPx()
                    val sirina = izmjereno.size.width + 2 * ruba
                    val visina = izmjereno.size.height.toFloat()
                    val vrhStupca = if (dan.imaUnos) {
                        size.height - maxOf(size.height - y(dan.kalorije), 4.dp.toPx())
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
                    val jeDanas = dan.datum == danas.toString()

                    Text(
                        text = OZNAKE_DANA.getOrElse(indeks) { "" },
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (jeDanas || indeks == odabraniIndeks) FontWeight.Bold else FontWeight.Normal,
                        color = if (jeDanas) MacroGreen else MaterialTheme.colorScheme.onSurfaceVariant
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

            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.10f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            val odabrani = odabraniIndeks?.let { dani.getOrNull(it) }

            if (odabrani == null) {
                Text(
                    text = "Dodirni stupac za kalorije, ponovno za obroke tog dana.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 10.dp)
                )
            } else {
                OdabraniDanRed(
                    dan = odabrani,
                    naziv = NAZIVI_DANA.getOrElse(odabraniIndeks ?: 0) { "" },
                    onOtvori = { onOtvoriDan(odabrani.datum) }
                )
            }
        }
    }
}

@Composable
private fun OdabraniDanRed(
    dan: TjedanDan,
    naziv: String,
    onOtvori: () -> Unit
) {
    val datum = LocalDate.parse(dan.datum)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "$naziv, ${datum.dayOfMonth}. ${datum.monthValue}.",
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            val cilj = dan.ciljKalorije?.let { " · cilj ${formatKcalTisuce(it)}" } ?: ""

            Text(
                text = if (dan.imaUnos) "${formatKcalTisuce(dan.kalorije)} kcal$cilj" else "Nema unosa$cilj",
                style = MaterialTheme.typography.bodySmall,
                color = if (dan.uCilju) MacroGreen else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        TextButton(onClick = onOtvori) {
            Text(
                text = "Otvori dan",
                color = MacroGreen,
                fontWeight = FontWeight.SemiBold
            )

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MacroGreen,
                modifier = Modifier.size(18.dp)
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
        StavkaLegende(tekst = "U cilju") {
            Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(bojaUCilju))
        }
        StavkaLegende(tekst = "Izvan cilja") {
            Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(bojaIzvanCilja))
        }
        StavkaLegende(tekst = "Bez unosa") {
            Canvas(Modifier.size(10.dp)) {
                drawRoundRect(
                    color = bojaObrisa,
                    cornerRadius = CornerRadius(3.dp.toPx()),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }
        StavkaLegende(tekst = "Cilj") {
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
private fun TjednaStatistikaCard(tjedan: Tjedan) {
    val prosjek = tjedan.prosjek ?: return
    val cilj = tjedan.prosjekCilj

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MacroGreen
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
                        text = "U cilju",
                        color = Color.White.copy(alpha = 0.78f),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = "${tjedan.daniUCilju} / ${tjedan.daniSUnosom} dana",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Prosječno dnevno",
                        color = Color.White.copy(alpha = 0.78f),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = "${formatKcalTisuce(prosjek.kalorije)} kcal",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = cilj?.let { "cilj ${formatKcalTisuce(it.kalorije)} kcal" } ?: "bez cilja",
                        color = Color.White.copy(alpha = 0.78f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Text(
                text = "±10 % od cilja, samo dani s unosom",
                color = Color.White.copy(alpha = 0.65f),
                style = MaterialTheme.typography.labelSmall
            )

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(color = Color.White.copy(alpha = 0.18f))

            Spacer(modifier = Modifier.height(14.dp))

            MakroTraka(
                naziv = "Proteini",
                vrijednost = prosjek.proteini,
                cilj = cilj?.proteini,
                boja = TjedanProteinBlue
            )

            Spacer(modifier = Modifier.height(12.dp))

            MakroTraka(
                naziv = "Ugljikohidrati",
                vrijednost = prosjek.ugljikohidrati,
                cilj = cilj?.ugljikohidrati,
                boja = TjedanCarbsOrange
            )

            Spacer(modifier = Modifier.height(12.dp))

            MakroTraka(
                naziv = "Masti",
                vrijednost = prosjek.masti,
                cilj = cilj?.masti,
                boja = TjedanFatPurple
            )

            if (cilj == null) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Za dane s unosom nema postavljenog cilja.",
                    color = Color.White.copy(alpha = 0.78f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun MakroTraka(
    naziv: String,
    vrijednost: Double,
    cilj: Double?,
    boja: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = naziv,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = if (cilj != null) "${formatMakro(vrijednost)} / ${formatGrami(cilj)} g"
                else "${formatMakro(vrijednost)} g",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (cilj != null && cilj > 0) {
            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.25f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((vrijednost / cilj).toFloat().coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(boja)
                )
            }
        }
    }
}

@Composable
private fun PrimjerTraka(
    uTijeku: Boolean,
    onUkloni: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MacroGreen.copy(alpha = 0.16f)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Prikazan je primjer tjedna",
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )

            TextButton(
                onClick = onUkloni,
                enabled = !uTijeku
            ) {
                Text("Ukloni primjer", color = MacroGreen, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun PorukaTjednaCard(
    naslov: String,
    tekst: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = naslov,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = tekst,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
