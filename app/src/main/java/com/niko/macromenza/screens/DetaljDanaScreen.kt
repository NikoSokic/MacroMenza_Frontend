package com.niko.macromenza.screens

import com.niko.macromenza.util.formatKcal
import com.niko.macromenza.util.formatKolicina
import com.niko.macromenza.util.formatMakro
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.niko.macromenza.model.Obrok
import com.niko.macromenza.session.UserSessionManager
import com.niko.macromenza.ui.components.screenBottomPadding
import com.niko.macromenza.ui.theme.MacroGreen
import com.niko.macromenza.ui.theme.MacroLightGreen
import com.niko.macromenza.viewmodel.PovijestViewModel
import com.google.gson.Gson
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private val DetailProteinBlue = Color(0xFF4F8EF7)
private val DetailCarbsOrange = Color(0xFFF4A340)
private val DetailFatPurple = Color(0xFF9B6DE3)

@Composable
fun DetaljDanaScreen(
    datum: String,
    navController: NavController,
    viewModel: PovijestViewModel = viewModel()
) {
    val povijestDana by viewModel.povijestDana.collectAsState()
    val obroci by viewModel.obrociDana.collectAsState()

    val context = LocalContext.current
    val sessionManager = remember {
        UserSessionManager(context)
    }

    val prijavljeniKorisnikId by
    sessionManager.korisnikId.collectAsState(initial = null)

    LaunchedEffect(datum, prijavljeniKorisnikId) {
        prijavljeniKorisnikId?.let { id ->
            viewModel.ucitajDan(id, datum)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    BrisanjeObrokaEfekti(
        viewModel = viewModel,
        navController = navController,
        snackbarHostState = snackbarHostState,
        idKorisnik = prijavljeniKorisnikId
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        Box(
            modifier = Modifier
                .size(230.dp)
                .offset(x = 170.dp, y = (-100).dp)
                .clip(CircleShape)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.09f)
                )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 22.dp,
                end = 22.dp,
                top = 28.dp,
                bottom = 110.dp
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {

            item {
                Column {
                    Text(
                        text = "Detalj dana",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = datum,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            povijestDana?.let { dan ->

                val imaObroka =
                    dan.obroci.any { (_, stavke) ->
                        stavke.isNotEmpty()
                    }

                if (!imaObroka) {
                    item {
                        EmptyDetailDay()
                    }
                } else {

                    item {
                        DetailSummaryCard(
                            kalorije = dan.ukupnoKalorije,
                            proteini = dan.ukupnoProteini,
                            uh = dan.ukupnoUgljikohidrati,
                            masti = dan.ukupnoMasti
                        )
                    }

                    item {
                        Text(
                            text = "Obroci",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    item {
                        ObrociDana(
                            obroci = obroci,
                            onUredi = { navController.navigate(rutaUredivanjaObroka(it)) },
                            onObrisi = { obrok ->
                                prijavljeniKorisnikId?.let { viewModel.obrisiObrok(it, obrok) }
                            }
                        )
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
private fun DetailSummaryCard(
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
                color = Color.White.copy(alpha = 0.78f),
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = "${formatKcal(kalorije)} kcal",
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            HorizontalDivider(
                color = Color.White.copy(alpha = 0.18f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                DetailSummaryMacro(
                    title = "Proteini",
                    value = proteini
                )

                DetailSummaryMacro(
                    title = "UH",
                    value = uh
                )

                DetailSummaryMacro(
                    title = "Masti",
                    value = masti
                )
            }
        }
    }
}

@Composable
private fun DetailSummaryMacro(
    title: String,
    value: Double
) {
    Column {
        Text(
            text = "${formatMakro(value)} g",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp
        )

        Text(
            text = title,
            color = Color.White.copy(alpha = 0.70f),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

/** Ruta za uređivanje spremljenog obroka (isti ekran kao za unos). */
fun rutaUredivanjaObroka(obrok: Obrok): String =
    "unos_obroka/${obrok.tipObroka}?idObrok=${obrok.id}"

/** Obroci dana sortirani po vremenu; zajedničko za Detalj dana i karticu "Dan" na Povijesti. */
@Composable
fun ObrociDana(
    obroci: List<Obrok>,
    onUredi: (Obrok) -> Unit,
    onObrisi: (Obrok) -> Unit
) {
    // Čuva se id: nakon brisanja obrok nestaje iz liste pa dijalog nestaje s njim.
    var zaBrisanjeId by rememberSaveable { mutableStateOf<Long?>(null) }
    val zaBrisanje = obroci.firstOrNull { it.id == zaBrisanjeId }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        obroci.forEach { obrok ->
            ObrokDanaKartica(
                obrok = obrok,
                onUredi = { onUredi(obrok) },
                onObrisi = { zaBrisanjeId = obrok.id }
            )
        }
    }

    zaBrisanje?.let { obrok ->
        PotvrdaBrisanjaObrokaDialog(
            obrok = obrok,
            onPotvrdi = {
                zaBrisanjeId = null
                onObrisi(obrok)
            },
            onOdustani = { zaBrisanjeId = null }
        )
    }
}

@Composable
private fun ObrokDanaKartica(
    obrok: Obrok,
    onUredi: () -> Unit,
    onObrisi: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = MacroGreen,
                        modifier = Modifier.size(23.dp)
                    )
                }

                Spacer(modifier = Modifier.width(13.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = formatDetaljTipObroka(obrok.tipObroka),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = "${obrok.vrijeme} · ${formatIzvor(obrok.izvor)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = formatKcal(obrok.ukupnoKalorije),
                        color = MacroGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    )

                    Text(
                        text = "kcal",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            obrok.stavke.forEach { stavka ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${formatKolicina(stavka.kolicina)}× ${stavka.naziv}",
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "${formatKcal(stavka.kalorije)} kcal",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.10f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                DetaljMacroValue(
                    label = "Proteini",
                    value = obrok.ukupnoProteini,
                    color = DetailProteinBlue
                )

                DetaljMacroValue(
                    label = "UH",
                    value = obrok.ukupnoUgljikohidrati,
                    color = DetailCarbsOrange
                )

                DetaljMacroValue(
                    label = "Masti",
                    value = obrok.ukupnoMasti,
                    color = DetailFatPurple
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {

                TextButton(onClick = onObrisi) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "Obriši",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                TextButton(onClick = onUredi) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MacroGreen,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "Uredi",
                        color = MacroGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/** Ključ u savedStateHandle ekrana na koji se vraća uređivanje: JSON obroka obrisanog iz uređivanja. */
const val KLJUC_OBRISANI_OBROK = "obrisaniObrok"

/** "1 stavka", "3 stavke", "5 stavki", "12 stavki". */
private fun formatBrojStavki(broj: Int): String {
    val oblik = when {
        broj % 100 in 11..14 -> "stavki"
        broj % 10 == 1 -> "stavka"
        broj % 10 in 2..4 -> "stavke"
        else -> "stavki"
    }
    return "$broj $oblik"
}

/** Potvrda brisanja cijelog obroka; zajednička za karticu obroka i uređivanje. */
@Composable
fun PotvrdaBrisanjaObrokaDialog(
    obrok: Obrok,
    onPotvrdi: () -> Unit,
    onOdustani: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onOdustani,
        title = {
            Text("Obrisati ${formatDetaljTipObroka(obrok.tipObroka)} u ${obrok.vrijeme}?")
        },
        text = {
            Text(
                "${formatBrojStavki(obrok.stavke.size)}, ${formatKcal(obrok.ukupnoKalorije)} kcal. " +
                    "Ovo briše cijeli obrok."
            )
        },
        confirmButton = {
            TextButton(onClick = onPotvrdi) {
                Text("Obriši", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onOdustani) {
                Text("Odustani", color = MacroGreen)
            }
        }
    )
}

/**
 * Ponašanje nakon brisanja za ekrane s obrocima dana: Snackbar "Obrok obrisan / Poništi" (i kad je obrok
 * obrisan u uređivanju, pa stigao preko savedStateHandle) te poruke grešaka.
 */
@Composable
fun BrisanjeObrokaEfekti(
    viewModel: PovijestViewModel,
    navController: NavController,
    snackbarHostState: SnackbarHostState,
    idKorisnik: Long?
) {
    val obrisaniObrok by viewModel.obrisaniObrok.collectAsState()
    val greskaBrisanja by viewModel.greskaBrisanja.collectAsState()
    val scope = rememberCoroutineScope()

    fun ponudiPonistavanje(obrok: Obrok) {
        scope.launch {
            if (snackbarHostState.ponudiPonistavanjeBrisanja()) {
                idKorisnik?.let { viewModel.vratiObrok(it, obrok) }
            }
        }
    }

    LaunchedEffect(obrisaniObrok) {
        val obrok = obrisaniObrok ?: return@LaunchedEffect
        viewModel.ocistiRezultatBrisanja()
        ponudiPonistavanje(obrok)
    }

    LaunchedEffect(greskaBrisanja) {
        val poruka = greskaBrisanja ?: return@LaunchedEffect
        viewModel.ocistiRezultatBrisanja()
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(poruka)
        }
    }

    // Obrok obrisan iz uređivanja: ekran uređivanja je ostavio JSON obroka prije povratka.
    LaunchedEffect(idKorisnik) {
        if (idKorisnik == null) return@LaunchedEffect
        val json = navController.currentBackStackEntry?.savedStateHandle
            ?.remove<String>(KLJUC_OBRISANI_OBROK) ?: return@LaunchedEffect
        val obrok = try {
            Gson().fromJson(json, Obrok::class.java)
        } catch (e: Exception) {
            null
        } ?: return@LaunchedEffect
        ponudiPonistavanje(obrok)
    }
}

/** Snackbar "Obrok obrisan" s akcijom "Poništi" (oko 5 s). Vraća true ako je korisnik kliknuo "Poništi". */
suspend fun SnackbarHostState.ponudiPonistavanjeBrisanja(): Boolean {
    currentSnackbarData?.dismiss()

    val rezultat = withTimeoutOrNull(5_000L) {
        showSnackbar(
            message = "Obrok obrisan",
            actionLabel = "Poništi",
            duration = SnackbarDuration.Indefinite
        )
    }

    return rezultat == SnackbarResult.ActionPerformed
}

@Composable
private fun DetaljMacroValue(
    label: String,
    value: Double,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "${formatMakro(value)} g",
            fontWeight = FontWeight.SemiBold,
            color = color,
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
private fun EmptyDetailDay() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    ) {
        Column(
            modifier = Modifier.padding(22.dp)
        ) {
            Text(
                text = "Nema evidentiranih obroka",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Za ovaj datum nije evidentirano nijedno jelo.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun formatDetaljTipObroka(
    tip: String
): String {
    return when (tip.uppercase()) {
        "DORUCAK" -> "Doručak"
        "RUCAK" -> "Ručak"
        "VECERA" -> "Večera"
        "UZINA" -> "Užina"
        else -> tip
    }
}

private fun formatIzvor(
    izvor: String
): String {
    return when (izvor.uppercase()) {
        "RUCNO" -> "Ručno"
        "MENI" -> "Meni"
        "SLIKA" -> "Slika"
        "OPIS" -> "Opis"
        else -> izvor
    }
}