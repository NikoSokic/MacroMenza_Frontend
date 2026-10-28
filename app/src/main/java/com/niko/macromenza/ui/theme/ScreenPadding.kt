package com.niko.macromenza.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.niko.macromenza.ui.theme.MacroDimenzije

object ScreenSpacing {
    val TopExtra = 16.dp

    // prostor koji sadržaj treba ostaviti za floating bottom nav
    val FloatingBottomBar = MacroDimenzije.trakaNavVisina + MacroDimenzije.trakaNavOdmak * 2
}

@Composable
fun screenTopPadding(): Dp {
    return WindowInsets.statusBars
        .asPaddingValues()
        .calculateTopPadding() +
            ScreenSpacing.TopExtra
}

@Composable
fun screenBottomPadding(
    hasBottomNavigation: Boolean = true
): Dp {

    val systemNavigation =
        WindowInsets.navigationBars
            .asPaddingValues()
            .calculateBottomPadding()

    return if (hasBottomNavigation) {
        systemNavigation +
                ScreenSpacing.FloatingBottomBar
    } else {
        systemNavigation + 20.dp
    }
}