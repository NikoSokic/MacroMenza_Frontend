package com.niko.macromenza.e2e

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.niko.macromenza.R
import com.niko.macromenza.session.ThemeManager
import com.niko.macromenza.ui.theme.AppTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfilIPostavkeE2ETest : E2EBaza() {

    private val grah by lazy { korisnik.jelo("Grah varivo") }

    private fun otvoriProfil() {
        cekajPocetnu()
        idiNa(R.string.nav_profil)
        compose.cekaTekst("kcal dnevno")
    }

    @Test
    fun profilPokazujeImeCiljIPreporukuIzBaze() {
        otvoriProfil()

        compose.cekaTekst("Ana Testić")
        compose.cekaTekst("Održavanje težine")
        compose.cekaOpis("Dnevni cilj 2046 kcal")
        compose.cekaOpis("Zadnja masa 60,0 kg")
        compose.cekaTekst("Preporuka: 120P 57M 264U")
    }

    @Test
    fun promjenaCiljaIAktivnostiPreracunavaPreporukuNaBackendu() {
        otvoriProfil()
        compose.kliknuTekst("Postavke ciljeva")
        compose.cekaTekst("Spremi cilj")

        // Obrazac se ne popunjava iz profila, pa se unosi sve. Ž, 30 god., 165 cm, 60 kg:
        // BMR 1320,25 × 1,725 (visoka) − 500 (mršavljenje) = 1777 kcal
        val polja = compose.onAllNodes(hasSetTextAction())
        polja[0].performTextInput("165")
        polja[1].performTextInput("30")
        polja[2].performTextInput("60")
        compose.kliknuTekstSkrol("Visoka")
        compose.kliknuTekstSkrol("Mršavljenje")
        compose.kliknuTekstSkrol("Spremi cilj")
        compose.ceka("preporuka preračunata") {
            JSONObject(korisnik.poziv("GET", "/api/preporuke/${korisnik.id}")).getInt("kalorije") == 1777
        }

        val mjerenja = org.json.JSONArray(korisnik.poziv("GET", "/mjerenja/korisnik/${korisnik.id}"))
        val zadnje = (0 until mjerenja.length()).map { mjerenja.getJSONObject(it) }.maxByOrNull { it.getLong("id") }!!
        assertEquals("visoka", zadnje.getString("razina_aktivnosti"))
        assertEquals("mrsavljenje", zadnje.getString("tip_cilja"))
    }

    @Test
    fun odabranaTemaSePamtiUPostavkama() {
        otvoriProfil()
        compose.kliknuTekst("Postavke aplikacije")
        compose.cekaTekst("Tema aplikacije")

        compose.kliknuTekst("Tamna")
        compose.ceka("tema spremljena") { runBlocking { ThemeManager(kontekst).theme.first() } == AppTheme.DARK }
        compose.kliknuTekst("Svijetla")
        compose.ceka("tema vraćena") { runBlocking { ThemeManager(kontekst).theme.first() } == AppTheme.LIGHT }
        compose.kliknuTekst("Sustav")
        compose.ceka("tema sustava") { runBlocking { ThemeManager(kontekst).theme.first() } == AppTheme.SYSTEM }
    }

    @Test
    fun statusSustavaJavljaDaSveRadi() {
        otvoriProfil()
        compose.kliknuTekst("Postavke aplikacije")

        compose.cekaOpis("Backend: Radi")
        compose.cekaOpis("Baza podataka: Radi")
        compose.cekaOpis("Autentifikacija: Radi")
    }

    @Test
    fun brisanjeSvihPodatakaOstavljaRacunAliBriseObrokeIMjerenja() {
        korisnik.spremiObrok(danas.toString(), "13:00", "RUCAK", listOf(grah.first to 1.0))
        otvoriProfil()
        compose.kliknuTekst("Postavke aplikacije")
        compose.cekaTekst("Obriši sve moje podatke")

        compose.kliknuTekstSkrol("Obriši sve moje podatke")
        // Potvrda traži da se upiše OBRIŠI; do tada je "Obriši" onemogućen i ništa se ne briše.
        compose.cekaTekst("Obrisati sve podatke?")
        val potvrdi = compose.onNode(hasText("Obriši") and hasClickAction())
        potvrdi.assertIsNotEnabled()
        compose.kliknuTekst("Odustani")
        compose.cekaNestanak("Obrisati sve podatke?")
        assertEquals(1, korisnik.obrociDana(danas.toString()).size)

        compose.kliknuTekstSkrol("Obriši sve moje podatke")
        compose.cekaTekst("Obrisati sve podatke?")
        compose.onNode(hasSetTextAction()).performTextInput("OBRIŠI")
        potvrdi.assertIsEnabled()
        potvrdi.performClick()
        compose.ceka("podaci obrisani") { korisnik.obrociDana(danas.toString()).isEmpty() }

        assertEquals("račun ostaje", "Ana", JSONObject(korisnik.poziv("GET", "/korisnici/${korisnik.id}")).getString("ime"))
        assertTrue("profil ostaje", korisnik.poziv("GET", "/profil/korisnik/${korisnik.id}").isNotBlank())
        assertEquals("mjerenja su obrisana", "[]", korisnik.poziv("GET", "/mjerenja/korisnik/${korisnik.id}").trim())
    }
}
