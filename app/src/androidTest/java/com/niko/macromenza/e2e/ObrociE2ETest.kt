package com.niko.macromenza.e2e

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.performTextInput
import com.niko.macromenza.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Uređivanje, brisanje i poništavanje obroka na Početnoj; obrok se stvara izravno preko API-ja. */
class ObrociE2ETest : E2EBaza() {

    private val grah by lazy { korisnik.jelo("Grah varivo") }

    private fun obrokiDanas() = korisnik.obrociDana(danas.toString())

    private fun otvoriRedak(vrijeme: String) {
        cekajPocetnu()
        compose.kliknuOpisGore("u $vrijeme", podniz = true)
    }

    @Test
    fun uredivanjeMijenjaKolicinuNaBackendu() {
        korisnik.spremiObrok(danas.toString(), "13:00", "RUCAK", listOf(grah.first to 1.0))
        otvoriRedak("13:00")

        compose.kliknuTekst("Uredi")
        compose.cekaTekst("UREĐIVANJE OBROKA")
        // Pretraga stavlja jelo pri vrh liste; u retku "Često jedeš" "+" je dok se popis ne učita ispod plutajuće trake.
        compose.onNode(hasSetTextAction()).performTextInput(grah.third)
        compose.kliknuOpis("${grah.third}: 1 porcija u obroku. Dodaj još pola porcije")
        compose.cekaTekst(tekst(R.string.uo_spremi_promjene))
        compose.kliknuTekst(tekst(R.string.uo_spremi_promjene))
        cekajPocetnu()

        val o = obrokiDanas().single()
        assertEquals(1.5, o.getJSONArray("stavke").getJSONObject(0).getDouble("kolicina"), 0.0)
        assertEquals(grah.second * 1.5, o.getDouble("ukupnoKalorije"), 0.02)
        compose.cekaOpis("Ručak u 13:00, ${Math.round(grah.second * 1.5)} kcal", podniz = true)
    }

    @Test
    fun uredivanjeBezPromjenaVracaNaPocetnuBezDijaloga() {
        korisnik.spremiObrok(danas.toString(), "13:00", "RUCAK", listOf(grah.first to 1.0))
        otvoriRedak("13:00")

        compose.kliknuTekst("Uredi")
        compose.cekaTekst("UREĐIVANJE OBROKA")
        compose.kliknuOpis("Natrag")

        cekajPocetnu()
        assertEquals(1, obrokiDanas().size)
    }

    @Test
    fun brisanjeIzRetkaSPonistavanjemVracaIstiObrokKaoNovi() {
        val stariId = korisnik.spremiObrok(danas.toString(), "08:00", "DORUCAK", listOf(grah.first to 1.5))
        otvoriRedak("08:00")

        compose.kliknuTekst("Obriši")
        compose.cekaTekst("Obrok obrisan")
        assertTrue("obrok mora nestati s backenda", obrokiDanasUskoro { it.isEmpty() })

        compose.kliknuTekst("Poništi")
        compose.ceka("obrok vraćen") { obrokiDanas().size == 1 }

        val vracen = obrokiDanas().single()
        assertNotEquals("poništavanje stvara novi obrok", stariId, vracen.getLong("id"))
        assertEquals("DORUCAK", vracen.getString("tipObroka"))
        assertEquals("08:00", vracen.getString("vrijeme"))
        assertEquals(1.5, vracen.getJSONArray("stavke").getJSONObject(0).getDouble("kolicina"), 0.0)
    }

    @Test
    fun brisanjeBezPonistavanjaOstavljaDanPrazan() {
        korisnik.spremiObrok(danas.toString(), "08:00", "DORUCAK", listOf(grah.first to 1.0))
        otvoriRedak("08:00")

        compose.kliknuTekst("Obriši")
        compose.cekaTekst("Obrok obrisan")

        assertTrue(obrokiDanasUskoro { it.isEmpty() })
        compose.cekaTekst("Što si danas jeo u menzi?")
    }

    /** Backend potvrdi brisanje tek nakon odgovora; kratko se čeka da popis postane prazan. */
    private fun obrokiDanasUskoro(uvjet: (List<org.json.JSONObject>) -> Boolean): Boolean {
        val kraj = System.currentTimeMillis() + ROK_MS
        while (System.currentTimeMillis() < kraj) {
            if (uvjet(obrokiDanas())) return true
            Thread.sleep(250)
        }
        return false
    }
}
