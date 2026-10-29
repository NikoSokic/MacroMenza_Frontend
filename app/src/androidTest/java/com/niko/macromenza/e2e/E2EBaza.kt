package com.niko.macromenza.e2e

import androidx.annotation.StringRes
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.niko.macromenza.MainActivity
import com.niko.macromenza.R
import org.junit.Rule
import org.junit.rules.RuleChain
import java.time.LocalDate
import java.time.ZoneId

/**
 * Zajednička osnova: testni korisnik (s upisanom sesijom) postoji prije nego što se aktivnost pokrene, a nakon testa
 * se sve što je napravio briše. Aplikacija radi protiv pravog lokalnog backenda i baze (adb reverse tcp:8080).
 */
abstract class E2EBaza {

    protected val korisnik = TestniKorisnik()
    protected val compose = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val lanac: RuleChain = RuleChain.outerRule(korisnik).around(compose)

    protected val danas: LocalDate get() = LocalDate.now(ZoneId.of("Europe/Zagreb"))

    protected fun cekajPocetnu() = compose.cekaTekst(tekst(R.string.dp_dodaj_obrok))

    protected fun idiNa(@StringRes stavka: Int) = compose.kliknuBilo(tekst(stavka))
}
