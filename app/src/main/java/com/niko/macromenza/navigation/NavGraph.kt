package com.niko.macromenza.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import com.niko.macromenza.screens.*

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

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

    val showBottomBar = currentRoute != "login" && currentRoute != "register"

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
            startDestination = "login",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("login") {
                LoginScreen(navController)
            }

            composable("register") {
                RegisterScreen(navController)
            }

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