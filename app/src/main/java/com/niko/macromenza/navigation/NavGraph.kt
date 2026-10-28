package com.niko.macromenza.navigation

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.niko.macromenza.screens.*
import com.niko.macromenza.ui.components.Makro
import java.time.LocalDate
import com.niko.macromenza.ui.theme.UlazUSesiji
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.niko.macromenza.session.UserSessionManager
import com.niko.macromenza.viewmodel.AuthViewModel
import com.niko.macromenza.viewmodel.AuthViewModelFactory
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment

private const val KLJUC_DIJELJENA_SLIKA = "dijeljena_slika"

@Composable
fun AppNavigation(
    // Slika podijeljena iz druge aplikacije; obrađuje se čim je korisnik prijavljen.
    dijeljenaSlika: Uri? = null,
    onDijeljenaObradena: () -> Unit = {}
) {
    val navController = rememberNavController()



    val context = LocalContext.current

    val sessionManager = remember {
        UserSessionManager(context)
    }

    val isLoggedIn by sessionManager.isLoggedIn.collectAsState(initial = null)


    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(sessionManager)
    )

    var homeRefreshKey by remember { mutableIntStateOf(0) }
    var profileRefreshKey by remember { mutableIntStateOf(0) }

    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Konzumacija,
        BottomNavItem.Povijest,
        BottomNavItem.Profil
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Kod uređivanja obroka izlaz ide samo preko "natrag", da dijalog za odbacivanje promjena ne bi bio zaobiđen.
    val uredivanjeObroka =
        currentRoute?.startsWith("unos_obroka") == true &&
                (navBackStackEntry?.arguments?.getLong("idObrok", -1L) ?: -1L) > 0

    val showBottomBar =
        currentRoute != "welcome" &&
                currentRoute != "login" &&
                currentRoute != "register" &&
                currentRoute?.startsWith("onboarding") != true &&
                !uredivanjeObroka

    // Neprijavljeni korisnik prvo prolazi prijavu (i onboarding), a slika čeka u MainActivityju.
    LaunchedEffect(isLoggedIn, dijeljenaSlika, currentRoute) {
        val uri = dijeljenaSlika ?: return@LaunchedEffect
        val ruta = currentRoute ?: return@LaunchedEffect
        if (isLoggedIn != true ||
            ruta == "welcome" || ruta == "login" || ruta == "register" ||
            ruta.startsWith("onboarding")
        ) return@LaunchedEffect

        navController.navigate(RUTA_SLIKA_ILI_OPIS) { launchSingleTop = true }
        navController.getBackStackEntry(RUTA_SLIKA_ILI_OPIS).savedStateHandle[KLJUC_DIJELJENA_SLIKA] = uri
        onDijeljenaObradena()
    }

    if (isLoggedIn == null) {
        SplashScreen()
        return
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        NavHost(
            navController = navController,
            startDestination = if (isLoggedIn == true) {
                BottomNavItem.Home.route
            } else {
                "welcome"
            },
            modifier = Modifier.fillMaxSize(),
            // Zadani prijelazi cijele aplikacije; ekrani s vlastitim prijelazima (Unos ↔ unos obroka) ih nadjačavaju.
            enterTransition = { globalniUlaz() },
            exitTransition = { globalniIzlaz() },
            popEnterTransition = { globalniUlaz() },
            popExitTransition = { globalniIzlaz() }
        ) {

            composable("welcome") {
                WelcomeScreen(navController = navController)
            }

            composable("login") {
                LoginScreen(
                    navController = navController,
                    viewModel = authViewModel
                )
            }

            composable("onboarding/{korisnikId}") { backStackEntry ->
                val korisnikId = backStackEntry.arguments
                    ?.getString("korisnikId")
                    ?.toLongOrNull() ?: 0L

                OnboardingScreen(
                    navController = navController,
                    korisnikId = korisnikId
                )
            }

            composable("register") {
                RegisterScreen(
                    navController = navController,
                    viewModel = authViewModel
                )
            }

            composable(BottomNavItem.Home.route) {
                HomeScreen(
                    navController = navController,
                    refreshKey = homeRefreshKey
                )
            }

            // Fade-through prema unosu obroka i natrag; za sve ostalo vrijede zadani prijelazi.
            composable(
                route = BottomNavItem.Konzumacija.route,
                exitTransition = {
                    if (jeRutaUnosa(targetState.destination.route)) prijelazIzlaz(UlazUSesiji.jePrvi(UlazUSesiji.UNOS_OBROKA)) else null
                },
                // Povratak je uvijek ponovni ulaz: kratko izblijeđivanje.
                popEnterTransition = { if (jeRutaUnosa(initialState.destination.route)) prijelazUlaz(puni = false) else null }
            ) {
                KonzumacijaScreen(navController)
            }

            composable(BottomNavItem.Povijest.route) {
                PovijestScreen(navController)
            }

            composable(BottomNavItem.Profil.route) {
                ProfilScreen(
                    navController = navController,
                    refreshKey = profileRefreshKey
                )
            }

            composable("uredi_profil") {
                UrediProfilScreen(navController)
            }

            composable("postavke_ciljeva") {
                PostavkeCiljevaScreen(navController)
            }

            composable("odakle_su_kalorije") {
                OdakleSuKalorijeScreen(navController)
            }

            composable("moje_mjere") {
                MojeMjereScreen(navController)
            }

            composable("postavke_aplikacije") {
                PostavkeAplikacijeScreen(navController)
            }

            // Bez idObrok je novi unos; s njim se uređuje spremljeni obrok. dodajJelo: jelo iz prečaca
            // "Često jedeš" na kartici Unos, koje se odmah dodaje u obrok.
            composable(
                route = "unos_obroka/{tipObroka}?idObrok={idObrok}&dodajJelo={dodajJelo}",
                arguments = listOf(
                    navArgument("idObrok") {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                    navArgument("dodajJelo") {
                        type = NavType.LongType
                        defaultValue = -1L
                    }
                ),
                enterTransition = {
                    if (jeRutaKarticeUnos(initialState.destination.route)) prijelazUlaz(UlazUSesiji.jePrvi(UlazUSesiji.UNOS_OBROKA)) else null
                },
                popExitTransition = { if (jeRutaKarticeUnos(targetState.destination.route)) prijelazIzlaz(puni = false) else null }
            ) { backStackEntry ->
                val tipObroka =
                    backStackEntry.arguments?.getString("tipObroka")
                        ?: "RUCAK"

                val idObrok = backStackEntry.arguments
                    ?.getLong("idObrok", -1L)
                    ?.takeIf { it > 0 }

                val dodajJelo = backStackEntry.arguments
                    ?.getLong("dodajJelo", -1L)
                    ?.takeIf { it >= 0 }

                UnosObrokaScreen(
                    tipObroka = tipObroka,
                    navController = navController,
                    idObrok = idObrok,
                    dodajJeloId = dodajJelo
                )
            }

            composable(RUTA_SLIKA_ILI_OPIS) { backStackEntry ->
                val slika by backStackEntry.savedStateHandle
                    .getStateFlow<Uri?>(KLJUC_DIJELJENA_SLIKA, null)
                    .collectAsState()

                SlikaIliOpisScreen(
                    navController = navController,
                    dijeljenaSlika = slika,
                    onDijeljenaPrimljena = {
                        backStackEntry.savedStateHandle[KLJUC_DIJELJENA_SLIKA] = null
                    }
                )
            }

            // Datum, vrijeme i zbroj spremljenog obroka su neobavezni; bez njih ekran pokazuje samo vrstu.
            composable(
                route = RUTA_OBROK_SPREMLJEN,
                arguments = listOf(
                    navArgument("datum") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("vrijeme") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("kcal") { type = NavType.FloatType; defaultValue = -1f },
                    navArgument("p") { type = NavType.FloatType; defaultValue = -1f },
                    navArgument("m") { type = NavType.FloatType; defaultValue = -1f },
                    navArgument("u") { type = NavType.FloatType; defaultValue = -1f }
                )
            ) { backStackEntry ->
                val argumenti = backStackEntry.arguments
                val tipObroka =
                    argumenti?.getString("tipObroka")
                        ?: "RUCAK"

                val datum = argumenti?.getString("datum")
                    ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                val kcal = argumenti?.getFloat("kcal", -1f) ?: -1f
                val makro = if (kcal < 0f) null else Makro(
                    kcal = kcal.toDouble(),
                    proteini = (argumenti?.getFloat("p", 0f) ?: 0f).coerceAtLeast(0f).toDouble(),
                    masti = (argumenti?.getFloat("m", 0f) ?: 0f).coerceAtLeast(0f).toDouble(),
                    ugljikohidrati = (argumenti?.getFloat("u", 0f) ?: 0f).coerceAtLeast(0f).toDouble()
                )

                ObrokSpremljenScreen(
                    tipObroka = tipObroka,
                    navController = navController,
                    datum = datum,
                    vrijeme = argumenti?.getString("vrijeme"),
                    makro = makro
                )
            }

            composable("detalj_dana/{datum}") { backStackEntry ->
                val datum =
                    backStackEntry.arguments?.getString("datum") ?: ""

                DetaljDanaScreen(
                    datum = datum,
                    navController = navController
                )
            }
        }

        // FLOATING NAVIGATION BAR PREKO SADRŽAJA
        if (showBottomBar) {
            MacroBottomBar(
                items = items,
                currentRoute = currentRoute,
                onItemClick = { item ->

                    if (item == BottomNavItem.Home) {
                        homeRefreshKey++
                    }

                    if (item == BottomNavItem.Profil) {
                        profileRefreshKey++
                    }

                    navController.navigate(item.route) {
                        popUpTo(BottomNavItem.Home.route) {
                            saveState = false
                        }

                        launchSingleTop = true
                        restoreState = false
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}