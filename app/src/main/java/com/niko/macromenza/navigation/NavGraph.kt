package com.niko.macromenza.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import com.niko.macromenza.screens.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.niko.macromenza.session.UserSessionManager
import com.niko.macromenza.viewmodel.AuthViewModel
import com.niko.macromenza.viewmodel.AuthViewModelFactory


@Composable
fun AppNavigation() {
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

    val showBottomBar =
        currentRoute != "welcome" &&
                currentRoute != "login" &&
                currentRoute != "register" &&
                currentRoute?.startsWith("onboarding") != true

    if (isLoggedIn == null) {
        SplashScreen()
        return
    }


    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    items.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
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
                            icon = {
                                Icon(item.icon, contentDescription = item.title)
                            },
                            label = {
                                Text(item.title)
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = if (isLoggedIn == true) {
                BottomNavItem.Home.route
            } else {
                "welcome"
            },
            modifier = Modifier.padding(innerPadding)
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
                )            }

            composable(BottomNavItem.Home.route) {
                HomeScreen(
                    navController = navController,
                    refreshKey = homeRefreshKey
                )
            }

            composable(BottomNavItem.Konzumacija.route) {
                KonzumacijaScreen(navController)
            }

            composable(BottomNavItem.Povijest.route) {
                PovijestScreen()
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

            composable("moje_mjere") {
                MojeMjereScreen(navController)
            }

            composable("postavke_aplikacije") {
                PostavkeAplikacijeScreen(navController)
            }

            composable("unos_obroka/{tipObroka}") { backStackEntry ->
                val tipObroka =
                    backStackEntry.arguments?.getString("tipObroka") ?: "RUCAK"

                UnosObrokaScreen(
                    tipObroka = tipObroka,
                    navController = navController
                )
            }

            composable("obrok_spremljen/{tipObroka}") { backStackEntry ->
                val tipObroka =
                    backStackEntry.arguments?.getString("tipObroka") ?: "RUCAK"

                ObrokSpremljenScreen(
                    tipObroka = tipObroka,
                    navController = navController
                )
            }

            composable("detalj_dana/{datum}") { backStackEntry ->
                val datum =
                    backStackEntry.arguments?.getString("datum") ?: ""

                DetaljDanaScreen(datum = datum)
            }
        }
    }
}