package com.niko.macromenza.e2e

import com.niko.macromenza.R
import com.niko.macromenza.screens.povijest.formatRasponTjedna
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters

class PovijestE2ETest : E2EBaza() {

    private val grah by lazy { korisnik.jelo("Grah varivo") }

    private val ponedjeljak get() = danas.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    @Test
    fun tjedniGrafPokazujeDaneSUnosomITekuciTjedanBezSljedeceg() {
        korisnik.spremiObrok(danas.toString(), "13:00", "RUCAK", listOf(grah.first to 1.0))
        cekajPocetnu()

        idiNa(R.string.nav_povijest)
        compose.cekaTekst("Ovaj tjedan")
        compose.cekaOpis("${Math.round(grah.second)} od 2046 kcal", podniz = true)
    }

    @Test
    fun listanjeNaProsliTjedanIPovratak() {
        val prosliPonedjeljak = ponedjeljak.minusWeeks(1)
        korisnik.spremiObrok(prosliPonedjeljak.plusDays(4).toString(), "19:00", "VECERA", listOf(grah.first to 1.0))
        cekajPocetnu()

        idiNa(R.string.nav_povijest)
        compose.cekaTekst(formatRasponTjedna(ponedjeljak, ponedjeljak.plusDays(6)))
        compose.kliknuOpis("Prethodni tjedan")
        compose.cekaTekst(formatRasponTjedna(prosliPonedjeljak, prosliPonedjeljak.plusDays(6)))
        compose.cekaOpis("Prosječno ${Math.round(grah.second)} kcal dnevno", podniz = true)

        // Na prošlom tjednu "Ovaj tjedan" je gumb za povratak, a "Sljedeći tjedan" vraća za jedan tjedan.
        compose.kliknuOpis("Sljedeći tjedan")
        compose.cekaTekst(formatRasponTjedna(ponedjeljak, ponedjeljak.plusDays(6)))
    }

    @Test
    fun danKarticaPokazujeObrokeIZbrojDana() {
        korisnik.spremiObrok(danas.toString(), "08:00", "DORUCAK", listOf(grah.first to 1.0))
        korisnik.spremiObrok(danas.toString(), "13:00", "RUCAK", listOf(grah.first to 1.5))
        cekajPocetnu()

        idiNa(R.string.nav_povijest)
        compose.cekaTekst("Ovaj tjedan")
        compose.kliknuTekst("Dan")
        val ukupno = Math.round(grah.second * 2.5)
        compose.cekaOpis("Ukupno $ukupno od 2046 kcal")
        compose.cekaOpis("Doručak u 08:00", podniz = true)
        compose.cekaOpis("Ručak u 13:00", podniz = true)
    }

    @Test
    fun primjerTjednaSeUcitavaIUklanjaSamoZaPrazneDane() {
        // Ponedjeljkom tekući tjedan nema prošlih dana, pa primjer nije ponuđen.
        org.junit.Assume.assumeTrue(danas.dayOfWeek != DayOfWeek.MONDAY)
        cekajPocetnu()
        idiNa(R.string.nav_povijest)
        compose.cekaTekst("Nema unosa u ovom tjednu")

        compose.kliknuTekst("Učitaj primjer tjedna")
        compose.ceka("primjer učitan") { compose.vidi("Prikazan je primjer tjedna") }

        // Svaki prošli dan tekućeg tjedna dobiva doručak, ručak i večeru izvora PRIMJER.
        val prosliDani = (0 until java.time.temporal.ChronoUnit.DAYS.between(ponedjeljak, danas).toInt()).map { ponedjeljak.plusDays(it.toLong()) }
        prosliDani.forEach { dan ->
            val obroci = korisnik.obrociDana(dan.toString())
            assertEquals("dan $dan", 3, obroci.size)
            assertTrue(obroci.all { it.getString("izvor") == "PRIMJER" })
        }
        assertTrue("danas se ne popunjava", korisnik.obrociDana(danas.toString()).isEmpty())

        compose.kliknuTekst("Ukloni primjer")
        compose.ceka("primjer uklonjen") { !compose.vidi("Prikazan je primjer tjedna") }
        prosliDani.forEach { assertTrue(korisnik.obrociDana(it.toString()).isEmpty()) }
    }
}

class MjereIProfilE2ETest : E2EBaza() {

    @Test
    fun brzoMjerenjeSpremaMasuIPreracunavaCilj() {
        cekajPocetnu()
        idiNa(R.string.nav_profil)
        compose.cekaTekst("kcal dnevno")
        compose.kliknuTekst("Moje mjere")
        compose.cekaTekst("Brzo mjerenje")
        compose.cekaOpis("Današnja masa 60,0 kg")

        compose.kliknuOpis("Povećaj za 0,1 kg")
        compose.cekaOpis("Današnja masa 60,1 kg")
        compose.kliknuTekst("Spremi")
        compose.ceka("spremljeno") { compose.vidi("Danas već upisano: 60,1 kg") }

        val mase = JSONArray(korisnik.poziv("GET", "/api/mjerenja?idKorisnik=${korisnik.id}"))
        assertEquals(1, mase.length()) // brzo mjerenje ažurira današnje mjerenje, ne dodaje novo
        assertEquals(60.1, mase.getJSONObject(0).getDouble("masa"), 0.0)
    }

    @Test
    fun odakleSuKalorijePokazujeBrojeveIzBaze() {
        cekajPocetnu()
        idiNa(R.string.nav_profil)
        compose.cekaTekst("kcal dnevno")
        compose.kliknuTekst("Odakle su kalorije")
        compose.cekaTekst("Vrijednosti su okvirne")
        compose.ceka("brojevi") { !compose.vidi("Učitavanje brojeva") }

        val izvori = org.json.JSONObject(korisnik.poziv("GET", "/api/izvori-podataka"))
        compose.cekaOpis("${izvori.getInt("brojJela")} jela, ${izvori.getInt("brojNamirnica")} namirnica, ${izvori.getInt("brojMenija")} menija")
    }
}
