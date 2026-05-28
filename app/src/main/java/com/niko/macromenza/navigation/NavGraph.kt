package com.niko.macromenza.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import com.niko.macromenza.screens.*



@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Konzumacija,
        BottomNavItem.Povijest,
        BottomNavItem.Profil
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val currentRoute =
                    navController.currentBackStackEntryAsState().value?.destination?.route

                items.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
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
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Home.route) {
                HomeScreen(navController)
            }
            composable(BottomNavItem.Konzumacija.route) {
                KonzumacijaScreen(navController)
            }
            composable(BottomNavItem.Povijest.route) {
                PovijestScreen(navController)
            }
            composable(BottomNavItem.Profil.route) {
                ProfilScreen(navController)
            }
            composable("uredi_profil") {
                UrediProfilScreen(navController)
            }
            composable("postavke_ciljeva") {
                PostavkeCiljevaScreen(navController)
            }

            composable("unos_obroka/{tipObroka}") { backStackEntry ->
                val tipObroka = backStackEntry.arguments?.getString("tipObroka") ?: "RUCAK"
                    UnosObrokaScreen(
                    tipObroka = tipObroka,
                    navController = navController
                )

            }
            composable("obrok_spremljen/{tipObroka}") { backStackEntry ->
                val tipObroka = backStackEntry.arguments?.getString("tipObroka") ?: "RUCAK"
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