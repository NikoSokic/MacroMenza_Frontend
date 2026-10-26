package com.niko.macromenza.screens

import android.content.Context
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.niko.macromenza.navigation.BottomNavItem
import com.niko.macromenza.screens.dnevnipregled.DnevniPregledSadrzaj
import com.niko.macromenza.screens.dnevnipregled.DnevniPregledStanje
import com.niko.macromenza.screens.dnevnipregled.jeCiljDosegnut
import com.niko.macromenza.screens.dnevnipregled.spojiDnevniPregled
import com.niko.macromenza.session.UserSessionManager
import com.niko.macromenza.ui.components.screenBottomPadding
import com.niko.macromenza.ui.theme.MacroDimenzije
import com.niko.macromenza.ui.theme.MacroRazmaci
import com.niko.macromenza.ui.theme.rememberSmanjeniPokret
import com.niko.macromenza.viewmodel.HomeViewModel
import com.niko.macromenza.viewmodel.PovijestViewModel
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

private val ZAGREB = ZoneId.of("Europe/Zagreb")

/** Ako obroci ne stignu u ovom roku (a dan-pregled kaže da ih ima), prikazuje se greška s "Pokušaj ponovno". */
private const val ROK_ZA_OBROKE_MS = 6_000L

private const val POSTAVKE_PREGLEDA = "dnevni_pregled"
private const val KLJUC_PROSLAVA = "zadnja_proslava"

/**
 * Dnevni pregled (smjer A "Pladanj"). Postojeći ViewModeli se ne mijenjaju:
 * HomeViewModel daje cilj i savjet (dan-pregled), a PovijestViewModel obroke dana s vremenom i id-jem,
 * brisanje i poništavanje (isto kao Detalj dana).
 */
@Composable
fun HomeScreen(
    navController: NavController,
    refreshKey: Int = 0,
    viewModel: HomeViewModel = viewModel(),
    obrociViewModel: PovijestViewModel = viewModel()
) {
    val danPregled by viewModel.danPregled.collectAsState()
    val ucitava by viewModel.ucitava.collectAsState()
    val obroci by obrociViewModel.obrociDana.collectAsState()
    val greskaBrisanja by obrociViewModel.greskaBrisanja.collectAsState()

    val context = LocalContext.current
    val sessionManager = remember { UserSessionManager(context) }
    val prijavljeniKorisnikId by sessionManager.korisnikId.collectAsState(initial = null)

    val danas = remember { LocalDate.now(ZAGREB) }
    var pokusano by remember { mutableStateOf(false) }
    var obrociStigli by remember { mutableStateOf(false) }
    var istekaoRok by remember { mutableStateOf(false) }

    fun ucitaj(id: Long) {
        pokusano = true
        istekaoRok = false
        viewModel.ucitajHome(id)
        obrociViewModel.ucitajDan(id, danas.toString())
    }

    // Osvježava se pri svakom povratku na Home (nakon spremanja, uređivanja ili brisanja obroka).
    LifecycleResumeEffect(refreshKey, prijavljeniKorisnikId) {
        prijavljeniKorisnikId?.let { ucitaj(it) }
        onPauseOrDispose { }
    }

    // Optimistično brisanje: obrok nestaje odmah, a vraća se ako backend javi grešku.
    var skriveni by remember { mutableStateOf(setOf<Long>()) }
    LaunchedEffect(greskaBrisanja) {
        if (greskaBrisanja != null) skriveni = emptySet()
    }
    LaunchedEffect(obroci) {
        // Nakon prvog dolaska svaka promjena obroka (brisanje, poništavanje) osvježava i dan-pregled (savjet).
        if (obrociStigli) prijavljeniKorisnikId?.let { viewModel.ucitajHome(it) }
        if (obroci.isNotEmpty()) obrociStigli = true
        skriveni = skriveni.filterTo(mutableSetOf()) { id -> obroci.any { it.id == id } }
    }

    val pregled = danPregled
    val cekaObroke = pregled != null && obroci.isEmpty() && !obrociStigli && pregled.pojedeno.kalorije > 0
    LaunchedEffect(cekaObroke) {
        if (cekaObroke) {
            delay(ROK_ZA_OBROKE_MS)
            istekaoRok = true
        }
    }

    val stanje = when {
        pregled == null && (ucitava || !pokusano) -> DnevniPregledStanje.Ucitavanje
        pregled == null -> DnevniPregledStanje.Greska
        cekaObroke && !istekaoRok -> DnevniPregledStanje.Ucitavanje
        cekaObroke -> DnevniPregledStanje.Greska
        else -> DnevniPregledStanje.Sadrzaj(spojiDnevniPregled(danas, pregled, obroci, skriveni, koristiObroke = obrociStigli))
    }

    // Proslava cilja: jednom dnevno, pamti se datum zadnje proslave.
    val smanjeno = rememberSmanjeniPokret()
    val postavke = remember { context.getSharedPreferences(POSTAVKE_PREGLEDA, Context.MODE_PRIVATE) }
    var proslava by remember { mutableStateOf(false) }
    val dosegnut = (stanje as? DnevniPregledStanje.Sadrzaj)?.let { jeCiljDosegnut(it.pregled) } == true
    LaunchedEffect(dosegnut) {
        if (dosegnut && !smanjeno && postavke.getString(KLJUC_PROSLAVA, null) != danas.toString()) {
            postavke.edit().putString(KLJUC_PROSLAVA, danas.toString()).apply()
            proslava = true
        }
    }

    var detaljJeloId by remember { mutableStateOf<Long?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    BrisanjeObrokaEfekti(
        viewModel = obrociViewModel,
        navController = navController,
        snackbarHostState = snackbarHostState,
        idKorisnik = prijavljeniKorisnikId
    )

    DnevniPregledSadrzaj(
        stanje = stanje,
        datum = danas,
        proslava = proslava,
        onProslavaGotova = { proslava = false },
        onPrethodniDan = { navController.navigate("detalj_dana/${danas.minusDays(1)}") },
        onProfil = {
            navController.navigate(BottomNavItem.Profil.route) {
                popUpTo(BottomNavItem.Home.route) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        },
        onPostaviCilj = { navController.navigate("postavke_ciljeva") },
        onDodajObrok = { tip ->
            navController.navigate("unos_obroka/${tip ?: zadaniTipZaVrijeme(LocalTime.now(ZAGREB))}")
        },
        onSlikaIliOpis = { navController.navigate(RUTA_SLIKA_ILI_OPIS) },
        onUredi = { obrok -> obrok.izvor?.let { navController.navigate(rutaUredivanjaObroka(it)) } },
        onObrisi = { obrok ->
            val izvor = obrok.izvor ?: return@DnevniPregledSadrzaj
            val id = prijavljeniKorisnikId ?: return@DnevniPregledSadrzaj
            skriveni = skriveni + obrok.id
            obrociViewModel.obrisiObrok(id, izvor)
        },
        onJeloSavjeta = { detaljJeloId = it },
        onOdakle = { navController.navigate("odakle_su_kalorije") },
        onPonovi = { prijavljeniKorisnikId?.let { ucitaj(it) } },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = MacroRazmaci.rubEkrana,
                        end = MacroRazmaci.rubEkrana,
                        bottom = screenBottomPadding() + MacroDimenzije.fabVisina + MacroRazmaci.m
                    )
            )
        }
    )

    // Detalj jela iz savjeta dana: samo pregled, bez dodavanja u obrok
    detaljJeloId?.let { id ->
        JeloDetaljSheet(
            idJelo = id,
            onOdakleSuKalorije = {
                detaljJeloId = null
                navController.navigate("odakle_su_kalorije")
            },
            onDismiss = { detaljJeloId = null }
        )
    }
}
