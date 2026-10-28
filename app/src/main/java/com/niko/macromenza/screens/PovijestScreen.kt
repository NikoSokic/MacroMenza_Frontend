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
import com.niko.macromenza.screens.povijest.DanStanje
import com.niko.macromenza.screens.povijest.PovijestAkcije
import com.niko.macromenza.screens.povijest.PovijestSadrzaj
import com.niko.macromenza.screens.povijest.PovijestTab
import com.niko.macromenza.screens.povijest.danStanje
import com.niko.macromenza.screens.povijest.tjedanStanje
import com.niko.macromenza.session.UserSessionManager
import com.niko.macromenza.ui.components.screenBottomPadding
import com.niko.macromenza.ui.theme.MacroRazmaci
import com.niko.macromenza.ui.theme.UlazUSesiji
import com.niko.macromenza.viewmodel.PovijestViewModel
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.ZoneId

private val ZAGREB = ZoneId.of("Europe/Zagreb")

/** ViewModel ne izlaže grešku dana; ako dan ne stigne u ovom roku, prikazuje se prazan dan. */
private const val ROK_ZA_DAN_MS = 6_000L

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
    val greskaBrisanja by viewModel.greskaBrisanja.collectAsState()

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

    // Optimistično brisanje (kao Home): obrok nestaje odmah, a vraća se ako backend javi grešku.
    var skriveni by remember { mutableStateOf(setOf<Long>()) }
    LaunchedEffect(greskaBrisanja) {
        if (greskaBrisanja != null) skriveni = emptySet()
    }
    LaunchedEffect(obrociDana) {
        skriveni = skriveni.filterTo(mutableSetOf()) { id -> obrociDana.any { it.id == id } }
    }

    val danas = LocalDate.now(ZAGREB)
    val stanje = remember(tjedan, greskaTjedna, danas) { tjedanStanje(tjedan, greskaTjedna, danas) }

    val odabraniDan = odabraniDatum?.let { LocalDate.parse(it) }
    var istekaoRok by remember(odabraniDatum) { mutableStateOf(false) }
    val stanjeDana = remember(odabraniDan, povijestDana, obrociDana, skriveni, tjedan, istekaoRok) {
        odabraniDan?.let { danStanje(it, povijestDana, obrociDana, skriveni, tjedan, istekaoRok) }
    }
    val danSeUcitava = stanjeDana is DanStanje.Ucitavanje
    LaunchedEffect(odabraniDatum, danSeUcitava) {
        if (danSeUcitava) {
            delay(ROK_ZA_DAN_MS)
            istekaoRok = true
        }
    }

    fun uKorisnika(radnja: (Long) -> Unit) {
        prijavljeniKorisnikId?.let(radnja)
    }

    /** Novi dan; kad prijeđe granicu učitanog tjedna, tjedan ga prati (cilj dana i kartica Tjedan ostaju usklađeni). */
    fun promijeniDan(pomakDana: Long) {
        val trenutni = odabraniDan ?: return
        val novi = trenutni.plusDays(pomakDana)
        odabraniDatum = novi.toString()
        val t = tjedan ?: return
        when {
            novi.isBefore(LocalDate.parse(t.pocetak)) -> uKorisnika { viewModel.prethodniTjedan(it) }
            novi.isAfter(LocalDate.parse(t.kraj)) -> uKorisnika { viewModel.sljedeciTjedan(it) }
        }
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
        onPrethodniDan = { promijeniDan(-1) },
        onSljedeciDan = { if (odabraniDan?.isBefore(danas) == true) promijeniDan(1) },
        onUredi = { obrok -> obrok.izvor?.let { navController.navigate(rutaUredivanjaObroka(it)) } },
        onObrisi = { obrok ->
            obrok.izvor?.let { izvor ->
                uKorisnika {
                    skriveni = skriveni + obrok.id
                    viewModel.obrisiObrok(it, izvor)
                }
            }
        }
    )

    PovijestSadrzaj(
        stanje = stanje,
        odabraniTab = odabraniTab,
        primjerUTijeku = primjerUTijeku,
        odabraniDan = odabraniDan,
        danStanje = stanjeDana ?: DanStanje.Ucitavanje,
        danas = danas,
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
