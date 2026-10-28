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
 * Puni fade-through samo pri prvom ulazu u unos u ovoj sesiji (UlazUSesiji); poslije, i pri povratku,
 * kratko izblijeđivanje od Trajanje.ponovniUlaz. Zasad samo za taj par ekrana; ostali prijelazi ostaju zadani.
 */

/** Zadani prijelazi cijele aplikacije: fade-through od ukupno 250 ms (Motion.kt), umjesto zadanih 700 ms NavHosta. */
fun globalniIzlaz(): ExitTransition =
    fadeOut(tween(Trajanje.prijelazIzlaz, easing = Ublazavanje.ubrzavanje))

fun globalniUlaz(): EnterTransition =
    fadeIn(tween(Trajanje.prijelazUlaz, delayMillis = Trajanje.prijelazIzlaz, easing = Ublazavanje.usporavanje)) +
        scaleIn(
            initialScale = SKALA_EKRANA,
            animationSpec = tween(Trajanje.prijelazUlaz, delayMillis = Trajanje.prijelazIzlaz, easing = Ublazavanje.usporavanje)
        )

private const val RUTA_UNOSA = "unos_obroka"

fun jeRutaUnosa(ruta: String?): Boolean = ruta?.startsWith(RUTA_UNOSA) == true

fun jeRutaKarticeUnos(ruta: String?): Boolean = ruta == BottomNavItem.Konzumacija.route

/** [puni] = prvi ulaz u unos obroka u ovoj sesiji (fade-through); inače kratko izblijeđivanje. */
fun prijelazIzlaz(puni: Boolean): ExitTransition =
    if (puni) fadeOut(tween(Trajanje.izlaz, easing = Ublazavanje.ubrzavanje))
    else fadeOut(tween(Trajanje.ponovniUlaz, easing = Ublazavanje.ubrzavanje))

fun prijelazUlaz(puni: Boolean): EnterTransition =
    if (puni) {
        fadeIn(tween(Trajanje.srednje, delayMillis = Trajanje.izlaz, easing = Ublazavanje.usporavanje)) +
            scaleIn(
                initialScale = SKALA_EKRANA,
                animationSpec = tween(Trajanje.srednje, delayMillis = Trajanje.izlaz, easing = Ublazavanje.usporavanje)
            )
    } else {
        fadeIn(tween(Trajanje.ponovniUlaz, easing = Ublazavanje.usporavanje))
    }
