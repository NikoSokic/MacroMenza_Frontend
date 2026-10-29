package com.niko.macromenza.screens

import com.niko.macromenza.model.Tjedan
import com.niko.macromenza.screens.povijest.DanStanje
import com.niko.macromenza.screens.povijest.DanTjednaUi
import com.niko.macromenza.screens.povijest.DanUi
import com.niko.macromenza.screens.povijest.StanjeDana
import com.niko.macromenza.screens.povijest.TjedanStanje
import com.niko.macromenza.screens.povijest.ciljDana
import com.niko.macromenza.screens.povijest.danStanje
import com.niko.macromenza.screens.povijest.formatRasponTjedna
import com.niko.macromenza.screens.povijest.tjedanStanje
import com.niko.macromenza.screens.povijest.uUi
import com.niko.macromenza.screens.povijest.vrhLjestvice
import com.niko.macromenza.testutil.obrok
import com.niko.macromenza.testutil.povijestDana
import com.niko.macromenza.testutil.stavka
import com.niko.macromenza.testutil.tjedan
import com.niko.macromenza.testutil.tjedanDan
import com.niko.macromenza.testutil.vrijednosti
import com.niko.macromenza.ui.components.Makro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StanjeDanaTest {

    private fun dan(
        kcal: Double,
        cilj: Double? = 2000.0,
        imaUnos: Boolean = kcal > 0,
        uCilju: Boolean = false,
        buducnost: Boolean = false
    ) = DanTjednaUi(
        datum = LocalDate.of(2026, 10, 27), makro = Makro(kcal, 0.0, 0.0, 0.0), ciljKcal = cilj,
        imaUnos = imaUnos, uCilju = uCilju, uBuducnosti = buducnost, jeDanas = false
    )

    @Test
    fun buducnostImaPrednostPredSvimOstalim() {
        assertEquals(StanjeDana.BUDUCNOST, dan(1500.0, uCilju = true, buducnost = true).stanje)
    }

    @Test
    fun danBezUnosa() {
        assertEquals(StanjeDana.BEZ_UNOSA, dan(0.0).stanje)
    }

    @Test
    fun danSUnosomBezCilja() {
        assertEquals(StanjeDana.BEZ_CILJA, dan(1500.0, cilj = null).stanje)
        assertEquals(StanjeDana.BEZ_CILJA, dan(1500.0, cilj = 0.0).stanje)
    }

    @Test
    fun uCiljuDolaziSBackendaAIspodIPrekoSeRacunaju() {
        assertEquals("iznad cilja, ali backend kaže u cilju (±10 %)", StanjeDana.U_CILJU, dan(2150.0, uCilju = true).stanje)
        assertEquals(StanjeDana.PREKO_CILJA, dan(2500.0).stanje)
        assertEquals(StanjeDana.ISPOD_CILJA, dan(1200.0).stanje)
        assertEquals("točno na cilju bez oznake uCilju nije preko", StanjeDana.ISPOD_CILJA, dan(2000.0).stanje)
    }
}

class TjedanUiTest {

    private val danas = LocalDate.of(2026, 10, 29) // četvrtak

    @Test
    fun danaSaUnosomZnaPretvoritiUMakroIPravilnimRedomPoljaMasti() {
        val t = tjedan(
            "2026-10-26",
            dani = listOf(tjedanDan("2026-10-26", 1800.0, 2400.0, uCilju = false)) +
                (1..6).map { tjedanDan(LocalDate.of(2026, 10, 26).plusDays(it.toLong()).toString()) }
        ).uUi(danas)

        val d = t.dani[0].makro
        // tjedanDan(1800): proteini 90, ugljikohidrati 225, masti 50
        assertEquals(1800.0, d.kcal, 0.0)
        assertEquals(90.0, d.proteini, 0.0)
        assertEquals(50.0, d.masti, 0.0)
        assertEquals(225.0, d.ugljikohidrati, 0.0)
    }

    @Test
    fun daniBezUnosaImajuNultiMakro() {
        val t = tjedan("2026-10-26").uUi(danas)

        assertTrue(t.dani.all { it.makro == Makro.NULA && !it.imaUnos })
        assertTrue(t.jePrazan)
    }

    @Test
    fun oznacujeDanasIBuducnost() {
        val t = tjedan("2026-10-26").uUi(danas)

        assertEquals(
            listOf(false, false, false, true, false, false, false),
            t.dani.map { it.jeDanas }
        )
        assertEquals(
            listOf(false, false, false, false, true, true, true),
            t.dani.map { it.uBuducnosti }
        )
    }

    @Test
    fun mozePrimjerSamoAkoImaDanaUZadnjihSedamBezDanas() {
        fun moze(pocetak: String, d: LocalDate = danas) = tjedan(pocetak).uUi(d).mozePrimjer

        assertTrue("tekući tjedan", moze("2026-10-26"))
        assertTrue("prošli tjedan", moze("2026-10-19"))
        assertFalse("tjedan prije 8+ dana", moze("2026-10-12"))
        assertFalse("budući tjedan", moze("2026-11-02"))
        // Ponedjeljak: u tekućem tjednu je samo danas, a danas se ne popunjava.
        assertFalse(moze("2026-10-26", LocalDate.of(2026, 10, 26)))
    }

    @Test
    fun jeTekuciKadNemaSljedecegIRasponJeFormatiran() {
        val tekuci = tjedan("2026-10-26", imaSljedeci = false).uUi(danas)
        val prosli = tjedan("2026-10-19", imaSljedeci = true).uUi(danas)

        assertTrue(tekuci.jeTekuci)
        assertFalse(prosli.jeTekuci)
        assertEquals("26. 10. – 1. 11. 2026.", tekuci.raspon)
        assertEquals("19. – 25. 10. 2026.", prosli.raspon)
    }

    @Test
    fun prosjeciSePrenose() {
        val t = tjedan("2026-10-26", prosjek = vrijednosti(2000.0, 150.0, 250.0, 60.0)).uUi(danas)

        assertEquals(Makro(2000.0, 150.0, 60.0, 250.0), t.prosjek)
        assertEquals(Makro(2400.0, 160.0, 67.0, 290.0), t.prosjekCilj)
        assertNull(tjedan("2026-10-26", prosjek = null, prosjekCilj = null).uUi(danas).prosjek)
    }

    @Test
    fun rasponTjednaZaMjesecIGodinu() {
        assertEquals("20. – 26. 10. 2026.", formatRasponTjedna(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 26)))
        assertEquals("27. 10. – 2. 11. 2026.", formatRasponTjedna(LocalDate.of(2026, 10, 27), LocalDate.of(2026, 11, 2)))
        assertEquals("28. 12. 2026. – 3. 1. 2027.", formatRasponTjedna(LocalDate.of(2026, 12, 28), LocalDate.of(2027, 1, 3)))
    }

    @Test
    fun greskaImaPrednostPredStarimTjednom() {
        val t: Tjedan = tjedan("2026-10-26")

        assertEquals(TjedanStanje.Greska, tjedanStanje(t, greska = true, danas = danas))
        assertEquals(TjedanStanje.Greska, tjedanStanje(null, greska = true, danas = danas))
        assertEquals(TjedanStanje.Ucitavanje, tjedanStanje(null, greska = false, danas = danas))
        assertTrue(tjedanStanje(t, greska = false, danas = danas) is TjedanStanje.Sadrzaj)
    }
}

class CiljDanaTest {

    @Test
    fun tjedniProsjekSeSkaliraOmjeromKcalCilja() {
        val c = ciljDana(2000.0, Makro(2400.0, 160.0, 67.0, 290.0))!!

        assertEquals(2000.0, c.kcal, 0.0)
        assertEquals(160.0 * 2000 / 2400, c.proteini, 1e-9)
        assertEquals(67.0 * 2000 / 2400, c.masti, 1e-9)
        assertEquals(290.0 * 2000 / 2400, c.ugljikohidrati, 1e-9)
    }

    @Test
    fun bezKcalCiljaIliProsjekaNemaCilja() {
        val prosjek = Makro(2400.0, 160.0, 67.0, 290.0)

        assertNull(ciljDana(null, prosjek))
        assertNull(ciljDana(0.0, prosjek))
        assertNull(ciljDana(-5.0, prosjek))
        assertNull(ciljDana(2000.0, null))
        assertNull(ciljDana(2000.0, Makro.NULA))
    }
}

class DanStanjeTest {

    private val datum = LocalDate.of(2026, 10, 28)
    private val doruck = obrok(11, "2026-10-28", "08:00", "DORUCAK", stavke = listOf(stavka(kcal = 400.0)))
    private val rucak = obrok(12, "2026-10-28", "13:00", "RUCAK", stavke = listOf(stavka(kcal = 700.0)))
    private val tjedanSCiljem = tjedan(
        "2026-10-26",
        dani = (0..6).map { tjedanDan(LocalDate.of(2026, 10, 26).plusDays(it.toLong()).toString(), 1100.0, 2000.0) },
        prosjekCilj = vrijednosti(2400.0, 160.0, 290.0, 67.0)
    )

    private fun sadrzaj(s: DanStanje): DanUi = (s as DanStanje.Sadrzaj).dan

    @Test
    fun dokDanNijeStigaoPrikazujeUcitavanjeAPoIstekuRokaPrazanDan() {
        assertEquals(DanStanje.Ucitavanje, danStanje(datum, null, emptyList(), emptySet(), null, istekaoRok = false))

        val prazan = sadrzaj(danStanje(datum, null, emptyList(), emptySet(), null, istekaoRok = true))
        assertTrue(prazan.jePrazan)
        assertEquals(Makro.NULA, prazan.ukupno)
    }

    @Test
    fun povijestDrugogDanaSeNeUzima() {
        val s = danStanje(datum, povijestDana("2026-10-27"), listOf(doruck), emptySet(), null, istekaoRok = false)

        assertEquals(DanStanje.Ucitavanje, s)
    }

    @Test
    fun cekaObrokeKadPovijestImaKalorijeAObrociJosNisuStigli() {
        val s = danStanje(datum, povijestDana("2026-10-28", 1100.0), emptyList(), emptySet(), null, istekaoRok = false)
        assertEquals(DanStanje.Ucitavanje, s)

        val poIstekuRoka = danStanje(datum, povijestDana("2026-10-28", 1100.0), emptyList(), emptySet(), null, istekaoRok = true)
        assertTrue(poIstekuRoka is DanStanje.Sadrzaj)
    }

    @Test
    fun danBezKalorijaOdmahJePrazanDan() {
        val s = danStanje(datum, povijestDana("2026-10-28", 0.0), emptyList(), emptySet(), null, istekaoRok = false)

        assertTrue(sadrzaj(s).jePrazan)
    }

    @Test
    fun zbrajaObrokeSortiraPoVremenuIFiltriraPoDatumu() {
        val tudjiDan = obrok(13, "2026-10-27", "07:00", "DORUCAK")
        val s = sadrzaj(
            danStanje(datum, povijestDana("2026-10-28"), listOf(rucak, tudjiDan, doruck), emptySet(), tjedanSCiljem, false)
        )

        assertEquals(listOf(11L, 12L), s.obroci.map { it.id })
        assertEquals(1100.0, s.ukupno.kcal, 0.0)
        assertEquals(datum, s.datum)
    }

    @Test
    fun ciljDanaIzTjednaICiljMakroSkaliran() {
        val s = sadrzaj(danStanje(datum, povijestDana("2026-10-28"), listOf(doruck), emptySet(), tjedanSCiljem, false))

        assertEquals(2000.0, s.ciljKcal!!, 0.0)
        assertEquals(2000.0, s.ciljMakro!!.kcal, 0.0)
        assertEquals(160.0 * 2000 / 2400, s.ciljMakro!!.proteini, 1e-9)
    }

    @Test
    fun bezTjednaNemaCilja() {
        val s = sadrzaj(danStanje(datum, povijestDana("2026-10-28"), listOf(doruck), emptySet(), null, false))

        assertNull(s.ciljKcal)
        assertNull(s.ciljMakro)
    }

    @Test
    fun skriveniObrociNestajuIzZbrojaOdmah() {
        val s = sadrzaj(danStanje(datum, povijestDana("2026-10-28"), listOf(doruck, rucak), setOf(12L), null, false))

        assertEquals(listOf(11L), s.obroci.map { it.id })
        assertEquals(400.0, s.ukupno.kcal, 0.0)
    }

    @Test
    fun kadSuSviObrociSkriveniDanJePrazanAIneCeka() {
        val s = danStanje(datum, povijestDana("2026-10-28", 400.0), emptyList(), setOf(11L), null, istekaoRok = false)

        assertTrue(sadrzaj(s).jePrazan)
    }

    @Test
    fun prekoCiljaJeIznadDesetPostoCilja() {
        fun dan(ukupnoKcal: Double, cilj: Double?) =
            DanUi(datum, Makro(ukupnoKcal, 0.0, 0.0, 0.0), cilj, null, emptyList())

        assertFalse(dan(2200.0, 2000.0).prekoCilja)
        assertTrue(dan(2201.0, 2000.0).prekoCilja)
        assertFalse(dan(5000.0, null).prekoCilja)
        assertFalse(dan(5000.0, 0.0).prekoCilja)
    }
}

class VrhLjestviceTest {

    private fun dan(kcal: Double, cilj: Double?, imaUnos: Boolean = kcal > 0) =
        DanTjednaUi(LocalDate.of(2026, 10, 27), Makro(kcal, 0.0, 0.0, 0.0), cilj, imaUnos, false, false, false)

    @Test
    fun saCiljemJeStodvadesetPetPostoNajvecegCilja() {
        assertEquals(3000.0, vrhLjestvice(listOf(dan(1800.0, 2400.0), dan(0.0, 2400.0))), 1e-9)
        assertEquals(3000.0, vrhLjestvice(listOf(dan(1800.0, 2000.0), dan(1500.0, 2400.0))), 1e-9)
    }

    @Test
    fun stupacKojiPremasujeLjestvicuProsirujeJe() {
        assertEquals(3500.0, vrhLjestvice(listOf(dan(3500.0, 2400.0))), 1e-9)
    }

    @Test
    fun bezCiljaJeNajveciDanPutaJedanZarezJedan() {
        assertEquals(2200.0, vrhLjestvice(listOf(dan(2000.0, null), dan(1500.0, null))), 1e-9)
    }

    @Test
    fun daniBezUnosaSeIgnoriraju() {
        assertEquals(3000.0, vrhLjestvice(listOf(dan(9999.0, 2400.0, imaUnos = false), dan(1000.0, 2400.0))), 1e-9)
    }

    @Test
    fun prazanTjedanImaNajmanjeJedan() {
        assertEquals(1.0, vrhLjestvice(emptyList()), 0.0)
        assertEquals(1.0, vrhLjestvice(listOf(dan(0.0, null))), 0.0)
    }
}
