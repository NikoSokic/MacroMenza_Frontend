package com.niko.macromenza.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.ui.graphics.vector.ImageVector
import com.niko.macromenza.R

sealed class BottomNavItem(
    val route: String,
    @StringRes val titleRes: Int,
    val icon: ImageVector
) {
    object Home : BottomNavItem("home", R.string.nav_pocetna, Icons.Rounded.Home)
    object Konzumacija : BottomNavItem("konzumacija", R.string.nav_unos, Icons.Rounded.Restaurant)
    object Povijest : BottomNavItem("povijest", R.string.nav_povijest, Icons.Rounded.BarChart)
    object Profil : BottomNavItem("profil", R.string.nav_profil, Icons.Rounded.Person)
}
