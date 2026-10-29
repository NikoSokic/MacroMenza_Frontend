package com.niko.macromenza.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.niko.macromenza.screens.profil.ProfilAkcije
import com.niko.macromenza.screens.profil.ProfilSadrzaj
import com.niko.macromenza.screens.profil.profilStanje
import com.niko.macromenza.session.UserSessionManager
import com.niko.macromenza.ui.theme.UlazUSesiji
import com.niko.macromenza.util.uString
import com.niko.macromenza.viewmodel.AuthViewModel
import com.niko.macromenza.viewmodel.AuthViewModelFactory
import com.niko.macromenza.viewmodel.ProfilViewModel

/** Profil (pages/profil.md): spaja ProfilViewModel, sesiju, odjavu i navigaciju; UI je u screens/profil/. */
@Composable
fun ProfilScreen(
    navController: NavController,
    refreshKey: Int = 0,
    viewModel: ProfilViewModel = viewModel()
) {
    val korisnik by viewModel.korisnik.collectAsState()
    val zadnjeMjerenje by viewModel.zadnjeMjerenje.collectAsState()
    val zadnjaPreporuka by viewModel.zadnjaPreporuka.collectAsState()
    val poruka by viewModel.poruka.collectAsState()

    val context = LocalContext.current
    val sessionManager = remember { UserSessionManager(context) }
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(UserSessionManager(context))
    )
    val prijavljeniKorisnikId by sessionManager.korisnikId.collectAsState(initial = null)

    // Puni ulaz samo pri prvom otvaranju taba u ovoj sesiji.
    val puniUlaz = remember { UlazUSesiji.jePrvi(UlazUSesiji.PROFIL) }
    SideEffect { UlazUSesiji.oznaci(UlazUSesiji.PROFIL) }

    LaunchedEffect(refreshKey, prijavljeniKorisnikId) {
        prijavljeniKorisnikId?.let { id -> viewModel.ucitajProfil(id) }
    }

    val porukaTekst = poruka?.uString()
    val stanje = remember(korisnik, zadnjeMjerenje, zadnjaPreporuka, porukaTekst) {
        profilStanje(korisnik, zadnjeMjerenje, zadnjaPreporuka, porukaTekst)
    }

    ProfilSadrzaj(
        stanje = stanje,
        puniUlaz = puniUlaz,
        akcije = ProfilAkcije(
            onUrediProfil = { navController.navigate("uredi_profil") },
            onPostavkeCiljeva = { navController.navigate("postavke_ciljeva") },
            onMojeMjere = { navController.navigate("moje_mjere") },
            onOdakleSuKalorije = { navController.navigate("odakle_su_kalorije") },
            onPostavkeAplikacije = { navController.navigate("postavke_aplikacije") },
            onPonovi = { prijavljeniKorisnikId?.let { id -> viewModel.ucitajProfil(id) } },
            onOdjava = {
                authViewModel.odjava()
                navController.navigate("login") {
                    popUpTo(0)
                }
            }
        )
    )
}
