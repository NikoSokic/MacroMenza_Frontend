package com.niko.macromenza.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.gson.Gson
import com.niko.macromenza.model.Obrok
import com.niko.macromenza.screens.povijest.DanStanje
import com.niko.macromenza.screens.povijest.DetaljDanaSadrzaj
import com.niko.macromenza.screens.povijest.danStanje
import com.niko.macromenza.session.UserSessionManager
import com.niko.macromenza.ui.components.PotvrdaBrisanjaObrokaDialog
import com.niko.macromenza.ui.components.screenBottomPadding
import com.niko.macromenza.ui.theme.MacroRazmaci
import com.niko.macromenza.viewmodel.PovijestViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import java.time.ZoneId

private val ZAGREB_DETALJ = ZoneId.of("Europe/Zagreb")

/** ViewModel ne izlaže grešku dana; ako dan ne stigne u ovom roku, prikazuje se prazan dan. */
private const val ROK_ZA_DETALJ_MS = 6_000L

/**
 * Detalj dana (otvara se s Home ‹): isti sadržaj kao kartica Dan u Povijesti (pages/povijest.md, "Detalj dana"),
 * ali brisanje traži potvrdu. Cilj dana dolazi iz tekućeg tjedna, pa ga dan iz drugog tjedna nema.
 */
@Composable
fun DetaljDanaScreen(
    datum: String,
    navController: NavController,
    viewModel: PovijestViewModel = viewModel()
) {
    val povijestDana by viewModel.povijestDana.collectAsState()
    val obroci by viewModel.obrociDana.collectAsState()
    val tjedan by viewModel.tjedan.collectAsState()

    val context = LocalContext.current
    val sessionManager = remember { UserSessionManager(context) }
    val prijavljeniKorisnikId by sessionManager.korisnikId.collectAsState(initial = null)

    LaunchedEffect(datum, prijavljeniKorisnikId) {
        prijavljeniKorisnikId?.let { id ->
            viewModel.ucitajDan(id, datum)
            viewModel.ucitajTjedan(id)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    BrisanjeObrokaEfekti(
        viewModel = viewModel,
        navController = navController,
        snackbarHostState = snackbarHostState,
        idKorisnik = prijavljeniKorisnikId
    )

    val dan = remember(datum) { LocalDate.parse(datum) }
    var istekaoRok by remember { mutableStateOf(false) }
    val stanje = remember(dan, povijestDana, obroci, tjedan, istekaoRok) {
        danStanje(dan, povijestDana, obroci, emptySet(), tjedan, istekaoRok)
    }
    val ucitava = stanje is DanStanje.Ucitavanje
    LaunchedEffect(ucitava) {
        if (ucitava) {
            delay(ROK_ZA_DETALJ_MS)
            istekaoRok = true
        }
    }

    // Čuva se id: nakon brisanja obrok nestaje iz liste pa dijalog nestaje s njim.
    var zaBrisanjeId by rememberSaveable { mutableStateOf<Long?>(null) }
    val zaBrisanje = obroci.firstOrNull { it.id == zaBrisanjeId }

    DetaljDanaSadrzaj(
        datum = dan,
        danas = LocalDate.now(ZAGREB_DETALJ),
        stanje = stanje,
        onNatrag = { navController.popBackStack() },
        onUredi = { obrok -> obrok.izvor?.let { navController.navigate(rutaUredivanjaObroka(it)) } },
        onObrisi = { obrok -> zaBrisanjeId = obrok.id },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = MacroRazmaci.rubEkrana, end = MacroRazmaci.rubEkrana, bottom = screenBottomPadding())
            )
        }
    )

    zaBrisanje?.let { obrok ->
        PotvrdaBrisanjaObrokaDialog(
            obrok = obrok,
            onPotvrdi = {
                zaBrisanjeId = null
                prijavljeniKorisnikId?.let { viewModel.obrisiObrok(it, obrok) }
            },
            onOdustani = { zaBrisanjeId = null }
        )
    }
}

/** Ruta za uređivanje spremljenog obroka (isti ekran kao za unos). */
fun rutaUredivanjaObroka(obrok: Obrok): String =
    "unos_obroka/${obrok.tipObroka}?idObrok=${obrok.id}"

/** Ključ u savedStateHandle ekrana na koji se vraća uređivanje: JSON obroka obrisanog iz uređivanja. */
const val KLJUC_OBRISANI_OBROK = "obrisaniObrok"

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
