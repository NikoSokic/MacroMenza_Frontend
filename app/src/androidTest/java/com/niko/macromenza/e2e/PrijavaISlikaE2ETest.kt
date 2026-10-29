package com.niko.macromenza.e2e

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performTextInput
import com.niko.macromenza.MainActivity
import com.niko.macromenza.R
import com.niko.macromenza.session.UserSessionManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.Rule
import org.junit.Test

/**
 * Neprijavljeni korisnik: provjere unosa rade se u aplikaciji prije ikakvog poziva Supabasea, pa se ovdje nikad
 * ne šalje prava prijava ni registracija (nema testnog Supabase računa).
 */
class PrijavaE2ETest {

    private val bezSesije = object : ExternalResource() {
        override fun before() = runBlocking { UserSessionManager(kontekst).obrisiSesiju() }
    }
    private val compose = createAndroidComposeRule<MainActivity>()

    // Sesija se briše prije pokretanja aktivnosti, da aplikacija krene s ekranom dobrodošlice.
    @get:Rule
    val lanac: RuleChain = RuleChain.outerRule(bezSesije).around(compose)

    private fun polje(indeks: Int) = compose.onAllNodes(hasSetTextAction()).get(indeks)

    @Test
    fun dobrodoslicaVodiNaPrijavuIRegistraciju() {
        compose.cekaTekst(tekst(R.string.auth_welcome_naslov))
        compose.kliknuTekst(tekst(R.string.auth_prijava_gumb))
        compose.cekaTekst(tekst(R.string.auth_prijava_naslov))
        // "Nemaš račun? Registriraj se" je jedan tekst; poveznica je na njegovu kraju.
        compose.kliknuKrajTeksta("Registriraj se")
        compose.cekaTekst(tekst(R.string.auth_registracija_naslov))
    }

    @Test
    fun prijavaJeOnemogucenaDokPoljaNisuPopunjenaINeprihvacaNeispravanEmail() {
        compose.cekaTekst(tekst(R.string.auth_welcome_naslov))
        compose.kliknuTekst(tekst(R.string.auth_prijava_gumb))
        compose.cekaTekst(tekst(R.string.auth_prijava_naslov))
        val gumb = compose.onNode(hasText(tekst(R.string.auth_prijava_gumb)) and hasClickAction())

        gumb.assertIsNotEnabled()

        polje(0).performTextInput("nije-email")
        polje(1).performTextInput("lozinka1")
        gumb.assertIsEnabled()
        gumb.performClick()
        compose.cekaTekst(tekst(R.string.auth_prij_email_neispravan))
    }

    @Test
    fun registracijaOdbijaKratkuLozinku() {
        compose.cekaTekst(tekst(R.string.auth_welcome_naslov))
        compose.kliknuTekst(tekst(R.string.auth_registracija_gumb))
        compose.cekaTekst(tekst(R.string.auth_registracija_naslov))

        polje(0).performTextInput("ana@mail.hr")
        polje(1).performTextInput("12345")
        polje(2).performTextInput("12345")
        compose.kliknuTekst(tekst(R.string.auth_registracija_gumb))
        compose.cekaTekst(tekst(R.string.auth_reg_lozinka_kratka))
    }
}

/** Jedan stvarni poziv Geminija preko lokalnog backenda; bez ključa ili kvote test se preskače, ne pada. */
class SlikaIliOpisE2ETest : E2EBaza() {

    @Test
    fun opisObrokaDajePrijedlogKojiSeSpremaKaoObrokSIzvoromOpis() {
        cekajPocetnu()
        compose.kliknuOpis("Slika ili opis")
        compose.cekaTekst("Opiši obrok")
        compose.onAllNodes(hasSetTextAction()).onFirst().performTextInput("jedna porcija graha")
        compose.kliknuTekst("Prepoznaj")

        // Čeka prijedlog ili poruku o grešci (Gemini ima vlastiti timeout od 60 s).
        val kraj = System.currentTimeMillis() + 75_000
        while (System.currentTimeMillis() < kraj && !compose.vidi("Prijedlog obroka") && !compose.vidi("Prepoznavanje nije", true) &&
            !compose.vidi("Nema veze", true) && !compose.vidi("nije odgovorio", true)
        ) Thread.sleep(500)
        assumeTrue("Gemini nije dostupan (ključ, kvota ili mreža): nije moguće provjeriti prepoznavanje", compose.vidi("Prijedlog obroka"))

        compose.kliknuTekstSkrol("Koristi prijedlog")
        compose.cekaTekst("Pretraži jela iz menze")
        compose.kliknuTekst(tekst(R.string.uo_spremi))
        compose.cekaTekst("Obrok spremljen")

        val obroci = korisnik.obrociDana(danas.toString())
        assertEquals(1, obroci.size)
        assertEquals("OPIS", obroci.single().getString("izvor"))
        assertTrue(obroci.single().getJSONArray("stavke").length() >= 1)
    }

    @Test
    fun prazanOpisNeSaljeZahtjev() {
        cekajPocetnu()
        compose.kliknuOpis("Slika ili opis")
        compose.cekaTekst("Opiši obrok")
        // Bez slike i opisa gumb je onemogućen: nema zahtjeva, prijedloga ni greške.
        compose.onNode(hasText("Prepoznaj") and hasClickAction()).assertIsNotEnabled()
        assertTrue(!compose.vidi("Prijedlog obroka"))
    }
}
