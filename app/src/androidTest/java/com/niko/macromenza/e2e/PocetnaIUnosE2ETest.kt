package com.niko.macromenza.e2e

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.performTextInput
import com.niko.macromenza.R
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PocetnaE2ETest : E2EBaza() {

    @Test
    fun noviKorisnikVidiCiljIzracunatNaBackenduIPrazanPladanj() {
        cekajPocetnu()

        // Ž, 30 god., 165 cm, 60 kg, umjerena, održavanje: BMR 1320,25 × 1,55 = 2046 kcal
        compose.cekaOpis("Ostaje 2046 od 2046 kcal. Uneseno 0 kcal.")
        compose.cekaOpis("Pladanj. Doručak: prazno, Ručak: prazno, Večera: prazno")
        compose.cekaOpis("Proteini: 0 od 120 grama")
        compose.cekaOpis("Masti: 0 od 57 grama")
        compose.cekaOpis("Ugljikohidrati: 0 od 264 grama")
    }

    @Test
    fun donjaTrakaVodiNaSveCetiriEkranaINatrag() {
        cekajPocetnu()

        idiNa(R.string.nav_unos)
        compose.cekaTekst("Što jedeš?")

        idiNa(R.string.nav_povijest)
        compose.cekaTekst("Ovaj tjedan")

        idiNa(R.string.nav_profil)
        compose.cekaTekst("Ana Testić")
        compose.cekaTekst("Postavke ciljeva")

        idiNa(R.string.nav_pocetna)
        cekajPocetnu()
    }
}

class UnosObrokaE2ETest : E2EBaza() {

    private fun obrokiDanas() = korisnik.obrociDana(danas.toString())

    @Test
    fun rucniUnosSpremaObrokNaBackendIOsvjezavaPocetnu() {
        val grah = korisnik.jelo("Grah varivo")
        cekajPocetnu()

        compose.kliknuOpisGore("Dodaj obrok: Ručak")
        compose.cekaTekst("Pretraži jela iz menze")
        compose.onNode(hasSetTextAction()).performTextInput("grah")
        // Prvi dodir daje 1 porciju, drugi 1,5
        compose.kliknuOpis("Dodaj ${grah.third}")
        compose.cekaOpis("Danas s ovim obrokom ${Math.round(grah.second)} od 2046 kcal")
        compose.kliknuOpis("${grah.third}: 1 porcija u obroku. Dodaj još pola porcije")
        compose.cekaOpis("Danas s ovim obrokom ${Math.round(grah.second * 1.5)} od 2046 kcal")

        compose.kliknuTekst(tekst(R.string.uo_spremi))
        compose.cekaTekst("Obrok spremljen")
        compose.cekaTekst("Ručak · 12:30 · danas")

        val o = obrokiDanas().single()
        assertEquals("RUCAK", o.getString("tipObroka"))
        assertEquals("RUCNO", o.getString("izvor"))
        assertEquals("12:30", o.getString("vrijeme"))
        val stavka = o.getJSONArray("stavke").getJSONObject(0)
        assertEquals(grah.first, stavka.getLong("idJelo"))
        assertEquals(1.5, stavka.getDouble("kolicina"), 0.0)
        assertEquals(grah.second * 1.5, o.getDouble("ukupnoKalorije"), 0.02)

        compose.kliknuTekst("Natrag na početnu")
        compose.cekaOpis("Ručak: ${Math.round(grah.second * 1.5)} kcal")
    }

    @Test
    fun meniSeDodajeKaoCijelaJelaIzMenijaSIzvoromMeni() {
        val prvi = JSONArray(korisnik.poziv("GET", "/api/meniji?tip=RUCAK")).getJSONObject(0)
        val brojStavki = prvi.getJSONArray("stavke").length()
        cekajPocetnu()

        compose.kliknuOpisGore("Dodaj obrok: Ručak")
        compose.cekaTekst("Pretraži jela iz menze")
        compose.kliknuOpis("Dodaj meni Ručak ${prvi.getInt("redniBroj")}")
        compose.cekaOpis("Danas s ovim obrokom ${Math.round(prvi.getDouble("kalorije"))} od 2046 kcal")
        compose.kliknuTekst(tekst(R.string.uo_spremi))
        compose.cekaTekst("Obrok spremljen")

        val o = obrokiDanas().single()
        assertEquals("MENI", o.getString("izvor"))
        assertEquals(brojStavki, o.getJSONArray("stavke").length())
        assertEquals(prvi.getDouble("kalorije"), o.getDouble("ukupnoKalorije"), 0.05 * brojStavki)
    }

    @Test
    fun tipObrokaSeMozePromijenitiPrijeSpremanja() {
        val grah = korisnik.jelo("Grah varivo")
        cekajPocetnu()

        compose.kliknuOpisGore("Dodaj obrok: Ručak")
        compose.cekaTekst("Pretraži jela iz menze")
        compose.kliknuOpis("Vrsta obroka: Ručak. Promijeni")
        compose.kliknuTekst("Večera")
        compose.cekaOpis("Vrsta obroka: Večera. Promijeni")
        compose.onNode(hasSetTextAction()).performTextInput("grah")
        compose.kliknuOpis("Dodaj ${grah.third}")
        compose.kliknuTekst(tekst(R.string.uo_spremi))
        compose.cekaTekst("Obrok spremljen")

        assertEquals("VECERA", obrokiDanas().single().getString("tipObroka"))
    }

    @Test
    fun bezOdabranogJelaNemaSpremljenogObroka() {
        cekajPocetnu()
        compose.kliknuOpisGore("Dodaj obrok: Večera")
        compose.cekaTekst("Pretraži jela iz menze")
        compose.cekaTekst("Dodaj jelo ili meni")

        assertTrue(obrokiDanas().isEmpty())
    }
}
