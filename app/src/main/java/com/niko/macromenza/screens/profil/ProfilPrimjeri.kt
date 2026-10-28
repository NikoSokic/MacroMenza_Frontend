package com.niko.macromenza.screens.profil

import com.niko.macromenza.ui.components.Makro

// Lažni profili samo za previewe ProfilSadrzaj.kt.

/** Održavanje, 2400 kcal (P 2 g/kg za 78,4 kg, M 25 % kcal, U ostatak). */
internal val PRIMJER_PROFIL_NORMALNO = ProfilUi(
    ime = "Niko",
    prezime = "Sokić",
    tipCilja = "odrzavanje",
    preporuka = Makro(kcal = 2400.0, proteini = 157.0, masti = 67.0, ugljikohidrati = 290.0),
    masa = 78.4
)

/** Mjerenje postoji, ali dnevna preporuka još nije izračunata. */
internal val PRIMJER_PROFIL_BEZ_CILJA = ProfilUi(
    ime = "Ana",
    prezime = "Marić",
    tipCilja = "mrsavljenje",
    preporuka = null,
    masa = 61.5
)

internal val PRIMJER_PROFIL_BEZ_IMENA = PRIMJER_PROFIL_NORMALNO.copy(ime = "", prezime = "")

internal val PRIMJER_PROFIL_DUGO_IME = ProfilUi(
    ime = "Marija Magdalena",
    prezime = "Kovačević-Horvatinović",
    tipCilja = "dobivanje_mase",
    preporuka = Makro(kcal = 2890.0, proteini = 145.0, masti = 80.0, ugljikohidrati = 397.0),
    masa = 72.6
)
