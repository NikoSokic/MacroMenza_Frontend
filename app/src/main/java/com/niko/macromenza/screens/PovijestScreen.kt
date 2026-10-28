package com.niko.macromenza.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.niko.macromenza.screens.povijest.PovijestAkcije
import com.niko.macromenza.screens.povijest.PovijestSadrzaj
import com.niko.macromenza.screens.povijest.PovijestTab
import com.niko.macromenza.screens.povijest.tjedanStanje
import com.niko.macromenza.session.UserSessionManager
import com.niko.macromenza.ui.components.screenBottomPadding
import com.niko.macromenza.ui.theme.MacroRazmaci
import com.niko.macromenza.ui.theme.UlazUSesiji
import com.niko.macromenza.viewmodel.PovijestViewModel
import java.time.LocalDate
import java.time.ZoneId

private val ZAGREB = ZoneId.of("Europe/Zagreb")

/** Povijest (pages/povijest.md): spaja PovijestViewModel, sesiju i navigaciju; UI je u screens/povijest/. */
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
    var odabraniTab by rememberSaveable { mutableStateOf(PovijestTab.TJEDNI) }
    var odabraniDatum by rememberSaveable { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val sessionManager = remember { UserSessionManager(context) }
    val prijavljeniKorisnikId by sessionManager.korisnikId.collectAsState(initial = null)

    // Puni ulaz samo pri prvom otvaranju taba u ovoj sesiji.
    val puniUlaz = remember { UlazUSesiji.jePrvi(UlazUSesiji.POVIJEST) }
    SideEffect { UlazUSesiji.oznaci(UlazUSesiji.POVIJEST) }

    // Pokreće se i pri povratku s uređivanja, pa tjedan i dan pokazuju svježe podatke.
    LaunchedEffect(prijavljeniKorisnikId) {
        prijavljeniKorisnikId?.let { id -> viewModel.ucitajTjedan(id) }
    }

    // Učitavanje ili uklanjanje primjera mijenja i odabrani dan.
    LaunchedEffect(odabraniDatum, prijavljeniKorisnikId, tjedan?.imaPrimjer) {
        odabraniDatum?.let { datum ->
            prijavljeniKorisnikId?.let { id -> viewModel.ucitajDan(id, datum) }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    BrisanjeObrokaEfekti(
        viewModel = viewModel,
        navController = navController,
        snackbarHostState = snackbarHostState,
        idKorisnik = prijavljeniKorisnikId
    )

    val danas = LocalDate.now(ZAGREB)
    val stanje = remember(tjedan, greskaTjedna, danas) { tjedanStanje(tjedan, greskaTjedna, danas) }

    fun uKorisnika(radnja: (Long) -> Unit) {
        prijavljeniKorisnikId?.let(radnja)
    }

    val akcije = PovijestAkcije(
        onTab = { tab ->
            if (tab == PovijestTab.DNEVNI) {
                // Zadnji dan odabranog tjedna koji nije u budućnosti.
                odabraniDatum = tjedan
                    ?.dani
                    ?.lastOrNull { !LocalDate.parse(it.datum).isAfter(danas) }
                    ?.datum
                    ?: danas.toString()
            }
            odabraniTab = tab
        },
        onPrethodniTjedan = { uKorisnika { viewModel.prethodniTjedan(it) } },
        onSljedeciTjedan = { uKorisnika { viewModel.sljedeciTjedan(it) } },
        onOvajTjedan = { uKorisnika { viewModel.ovajTjedan(it) } },
        onPonovi = { uKorisnika { viewModel.ucitajTjedan(it) } },
        onUcitajPrimjer = { uKorisnika { viewModel.ucitajPrimjerTjedna(it) } },
        onUkloniPrimjer = { uKorisnika { viewModel.ukloniPrimjerTjedna(it) } },
        onOtvoriDan = { datum ->
            odabraniDatum = datum
            odabraniTab = PovijestTab.DNEVNI
        },
        onPrethodniDan = {
            odabraniDatum?.let { odabraniDatum = LocalDate.parse(it).minusDays(1).toString() }
        },
        onSljedeciDan = {
            odabraniDatum?.let { odabraniDatum = LocalDate.parse(it).plusDays(1).toString() }
        },
        onUredi = { navController.navigate(rutaUredivanjaObroka(it)) },
        onObrisi = { obrok -> uKorisnika { viewModel.obrisiObrok(it, obrok) } }
    )

    PovijestSadrzaj(
        stanje = stanje,
        odabraniTab = odabraniTab,
        primjerUTijeku = primjerUTijeku,
        odabraniDatum = odabraniDatum,
        povijestDana = povijestDana,
        obrociDana = obrociDana,
        akcije = akcije,
        puniUlaz = puniUlaz,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = MacroRazmaci.rubEkrana, end = MacroRazmaci.rubEkrana, bottom = screenBottomPadding())
            )
        }
    )
}
