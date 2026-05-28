package com.niko.macromenza.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : BottomNavItem("home", "Početna", Icons.Default.Home)
    object Konzumacija : BottomNavItem("konzumacija", "Konzumacija", Icons.Default.Restaurant)
    object Povijest : BottomNavItem("povijest", "Povijest", Icons.Default.BarChart)
    object Profil : BottomNavItem("profil", "Profil", Icons.Default.Person)
}