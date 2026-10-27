package com.niko.macromenza.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import com.niko.macromenza.ui.theme.Trajanje
import com.niko.macromenza.ui.theme.SKALA_EKRANA
import com.niko.macromenza.ui.theme.Ublazavanje

/*
 * Prijelaz "fade-through" između kartice Unos i unosa obroka: odlazni ekran se brzo izblijedi,
 * tek onda dolazni izblijedi uz blagi rast. Trajanja su iz Motion.kt (izlaz kraći od ulaza).
 * Zasad samo za taj par ekrana; ostali prijelazi ostaju zadani.
 */

private const val RUTA_UNOSA = "unos_obroka"

fun jeRutaUnosa(ruta: String?): Boolean = ruta?.startsWith(RUTA_UNOSA) == true

fun jeRutaKarticeUnos(ruta: String?): Boolean = ruta == BottomNavItem.Konzumacija.route

fun prijelazIzlaz(): ExitTransition =
    fadeOut(tween(Trajanje.izlaz, easing = Ublazavanje.ubrzavanje))

fun prijelazUlaz(): EnterTransition =
    fadeIn(tween(Trajanje.srednje, delayMillis = Trajanje.izlaz, easing = Ublazavanje.usporavanje)) +
        scaleIn(
            initialScale = SKALA_EKRANA,
            animationSpec = tween(Trajanje.srednje, delayMillis = Trajanje.izlaz, easing = Ublazavanje.usporavanje)
        )
