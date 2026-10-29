package com.niko.macromenza.e2e

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.swipeUp

/**
 * Klik u sredinu elementa nakon što se skrola do njega i makne ispod donje trake: ona (kao i plutajući gumbi)
 * leži preko dna liste, pa bi klik pri samom dnu pogodio njih.
 */
fun E2ERule.kliknuTekstSkrol(t: String, podniz: Boolean = false) {
    skrolajDo(hasText(t, substring = podniz))
    cekaTekst(t, podniz)
    izNadDonjeTrake(tekstNode(t, podniz))
    tekstNode(t, podniz).performClick()
}

fun E2ERule.kliknuOpisSkrol(t: String, podniz: Boolean = false) {
    skrolajDo(hasContentDescription(t, substring = podniz))
    cekaOpis(t, podniz)
    izNadDonjeTrake(opisNode(t, podniz))
    opisNode(t, podniz).performClick()
}

private fun E2ERule.izNadDonjeTrake(cvor: SemanticsNodeInteraction) {
    val visina = onAllNodes(isRoot(), useUnmergedTree = true).fetchSemanticsNodes().first().size.height
    val dno = cvor.fetchSemanticsNode().boundsInRoot.bottom
    if (dno > visina - DONJA_TRAKA_PX) {
        onAllNodes(okomitaLista).onFirst().performTouchInput { swipeUp(startY = height * 0.75f, endY = height * 0.45f) }
        waitForIdle()
    }
}

/** Donja traka i plutajući gumbi zauzimaju otprilike donjih 320 px na emulatoru 1080 × 2220. */
private const val DONJA_TRAKA_PX = 320f

/** Postoji li (u bilo kojem prozoru) čvor s tim tekstom ili opisom. */
fun E2ERule.vidi(t: String, podniz: Boolean = false): Boolean =
    imaTeksta(t, podniz) ||
        onAllNodesWithContentDescription(t, substring = podniz, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

/** Okomito listanje (LazyColumn); vodoravni redovi (npr. kategorije) se ne smiju miješati s njim. */
val okomitaLista: androidx.compose.ui.test.SemanticsMatcher =
    androidx.compose.ui.test.hasScrollAction() and
        androidx.compose.ui.test.SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange)

/** Klik na desni kraj teksta: poveznica ("Nemaš račun? Registriraj se") je dio jednog Text čvora, na njegovu kraju. */
fun E2ERule.kliknuKrajTeksta(t: String) {
    cekaTekst(t, podniz = true)
    tekstNode(t, podniz = true).performTouchInput { click(androidx.compose.ui.geometry.Offset(width * 0.9f, height / 2f)) }
}
