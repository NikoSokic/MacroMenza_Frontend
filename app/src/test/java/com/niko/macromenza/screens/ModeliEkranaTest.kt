package com.niko.macromenza.screens

import com.niko.macromenza.model.CestoJedenoJelo
import com.niko.macromenza.model.KategorijaJela
import com.niko.macromenza.model.PrijedlogObroka
import com.niko.macromenza.model.PrijedlogStavka
import com.niko.macromenza.screens.dnevnipregled.TIPOVI_UTORA
import com.niko.macromenza.screens.dnevnipregled.spojiDnevniPregled
import com.niko.macromenza.screens.slikailiopis.trebaProvjeriti
import com.niko.macromenza.screens.slikailiopis.ukupno
import com.niko.macromenza.screens.unosobroka.SkupinaJela
import com.niko.macromenza.screens.unosobroka.brojAktivnihFiltera
import com.niko.macromenza.screens.unosobroka.grupirajPoKategoriji
import com.niko.macromenza.screens.unosobroka.pogodakUpita
import com.niko.macromenza.screens.unosobroka.rangPogotka
import com.niko.macromenza.screens.unosobroka.rangirajPoUpitu
import com.niko.macromenza.screens.unosobroka.uMakro
import com.niko.macromenza.testutil.danPregled
import com.niko.macromenza.testutil.jelo
import com.niko.macromenza.testutil.obrok
import com.niko.macromenza.testutil.stavka
import com.niko.macromenza.testutil.vrijednosti
import com.niko.macromenza.ui.components.Makro
import com.niko.macromenza.ui.components.uMakro
import com.niko.macromenza.ui.components.uUi
import com.niko.macromenza.viewmodel.MakroFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MakroTest {

    @Test
    fun zbrajanjeIdePoPoljima() {
        assertEquals(Makro(300.0, 30.0, 12.0, 33.0), Makro(100.0, 10.0, 4.0, 11.0) + Makro(200.0, 20.0, 8.0, 22.0))
        assertEquals(Makro(5.0, 1.0, 2.0, 3.0), Makro.NULA + Makro(5.0, 1.0, 2.0, 3.0))
    }

    @Test
    fun udjeliSuUdjeliKalorijaPaMastiBrojeSDevet() {
        val (p, m, u) = Makro(0.0, proteini = 25.0, masti = 10.0, ugljikohidrati = 50.0).udjeli()

        // 100 + 90 + 200 = 390 kcal
        assertEquals(100f / 390f, p, 1e-6f)
        assertEquals(90f / 390f, m, 1e-6f)
        assertEquals(200f / 390f, u, 1e-6f)
        assertEquals(1f, p + m + u, 1e-6f)
    }

    @Test
    fun bezMakronutrijenataSviUdjeliSuNula() {
        assertEquals(Triple(0f, 0f, 0f), Makro.NULA.udjeli())
    }

    @Test
    fun nutritivneVrijednostiSeMapirajuPoImenuPoljaNePoRedu() {
        // API ih šalje redom kcal, P, UH, M; Makro ih drži redom kcal, P, M, UH.
        assertEquals(Makro(kcal = 100.0, proteini = 10.0, masti = 4.0, ugljikohidrati = 20.0), vrijednosti(100.0, p = 10.0, u = 20.0, m = 4.0).uMakro())
    }

    @Test
    fun jeloSeMapiraUMakroPoImenu() {
        assertEquals(Makro(320.0, 18.0, 9.0, 40.0), jelo(kcal = 320.0, proteini = 18.0, ugljikohidrati = 40.0, masti = 9.0).uMakro())
    }

    @Test
    fun obrokUUiSkracujeVrijemeIVelikaSlovaTipa() {
        val o = obrok(
            id = 5, vrijeme = "12:30:00", tip = "rucak",
            stavke = listOf(stavka(1, "Grah varivo", 1.5, 480.0, 27.0, 60.0, 13.5), stavka(2, "Kruh", 2.0, 140.0, 4.0, 28.0, 1.0))
        ).uUi()

        assertEquals("12:30", o.vrijeme)
        assertEquals("RUCAK", o.tip)
        assertEquals(Makro(620.0, 31.0, 14.5, 88.0), o.makro)
        assertEquals(listOf("Grah varivo", "Kruh"), o.jela.map { it.naziv })
        assertEquals(1.5, o.jela[0].kolicina, 0.0)
        assertEquals(Makro(480.0, 27.0, 13.5, 60.0), o.jela[0].makro)
    }
}

class DnevniPregledModelTest {

    private val datum = LocalDate.of(2026, 10, 28)
    private val doruck = obrok(1, "2026-10-28", "08:00", "DORUCAK", stavke = listOf(stavka(kcal = 400.0)))
    private val doruck2 = obrok(2, "2026-10-28", "09:15", "DORUCAK", stavke = listOf(stavka(kcal = 100.0)))
    private val rucak = obrok(3, "2026-10-28", "13:00", "RUCAK", stavke = listOf(stavka(kcal = 700.0)))
    private val uzina = obrok(4, "2026-10-28", "16:00", "UZINA", stavke = listOf(stavka(kcal = 150.0)))
    private val vecera = obrok(5, "2026-10-28", "19:30", "VECERA", stavke = listOf(stavka(kcal = 600.0)))

    private fun spoji(obroci: List<com.niko.macromenza.model.Obrok>, skriveni: Set<Long> = emptySet(), koristiObroke: Boolean = true) =
        spojiDnevniPregled(datum, danPregled(), obroci, skriveni, koristiObroke)

    @Test
    fun utoriSuDorucakRucakVecera() {
        assertEquals(listOf("DORUCAK", "RUCAK", "VECERA"), TIPOVI_UTORA)
    }

    @Test
    fun pladanjImaTriUtoraCakIKadJePrazan() {
        val p = spoji(emptyList())

        assertEquals(listOf("DORUCAK", "RUCAK", "VECERA"), p.tanjuri.map { it.tip })
        assertTrue(p.tanjuri.all { it.makro == null })
        assertEquals(0, p.popunjeniUtori)
        assertTrue(p.jePrazan)
    }

    @Test
    fun uzinaDobivaTanjuricIzmeduRuckaIVecere() {
        val p = spoji(listOf(vecera, uzina, doruck, rucak))

        assertEquals(listOf("DORUCAK", "RUCAK", "UZINA", "VECERA"), p.tanjuri.map { it.tip })
        assertEquals(listOf(false, false, true, false), p.tanjuri.map { it.jeUzina })
        assertEquals("utori se broje bez užine", 3, p.popunjeniUtori)
    }

    @Test
    fun viseObrokaIstogTipaSeZbrajajuAVrijemeJePrvog() {
        val p = spoji(listOf(doruck2, doruck))

        val tanjur = p.tanjuri.first { it.tip == "DORUCAK" }
        assertEquals(500.0, tanjur.makro!!.kcal, 0.0)
        assertEquals("08:00", tanjur.vrijeme)
        assertEquals(1, p.popunjeniUtori)
    }

    @Test
    fun obrociSuPoredaniPoVremenuPaPoTipu() {
        val istoVrijemeA = obrok(6, "2026-10-28", "13:00", "VECERA")
        val p = spoji(listOf(vecera, istoVrijemeA, doruck, rucak, uzina))

        assertEquals(listOf(1L, 3L, 6L, 4L, 5L), p.obroci.map { it.id })
    }

    @Test
    fun pojedenoSeRacunaIzObrokaAKadIhJosNemaIzDanPregleda() {
        // Obroci još nisu stigli i nije rečeno da ih treba čekati: zbroj iz dan-pregleda (1000 kcal).
        assertEquals(1000.0, spoji(emptyList(), koristiObroke = false).pojedeno.kcal, 0.0)
        // Obroci su stigli (čak i prazni): vrijedi zbroj iz obroka.
        assertEquals(0.0, spoji(emptyList(), koristiObroke = true).pojedeno.kcal, 0.0)
        // Obroci postoje: uvijek se računa iz njih.
        assertEquals(1100.0, spoji(listOf(doruck, rucak), koristiObroke = false).pojedeno.kcal, 0.0)
    }

    @Test
    fun skriveniObrociNestajuIzPladnjaIZbroja() {
        val p = spoji(listOf(doruck, rucak), skriveni = setOf(3L))

        assertEquals(listOf(1L), p.obroci.map { it.id })
        assertEquals(400.0, p.pojedeno.kcal, 0.0)
        assertNull(p.tanjuri.first { it.tip == "RUCAK" }.makro)
    }

    @Test
    fun kadSuSviObrociSkriveniPojedenoJeNulaAneZbrojIzDanPregleda() {
        val p = spoji(listOf(doruck), skriveni = setOf(1L), koristiObroke = false)

        assertEquals(0.0, p.pojedeno.kcal, 0.0)
    }

    @Test
    fun ciljISavjetDolazeIzDanPregleda() {
        val p = spoji(emptyList())

        assertEquals(2400.0, p.cilj!!.kcal, 0.0)
        assertEquals("Savjet za menzu", p.savjet!!.naslov)
        assertNull(spojiDnevniPregled(datum, danPregled(cilj = null), emptyList(), emptySet(), true).cilj)
    }
}

class UnosObrokaModelTest {

    private val kategorije = listOf(KategorijaJela("Juhe", 12), KategorijaJela("Variva", 20), KategorijaJela("Salate", 8))

    @Test
    fun jelaSeGrupirajuRedomKategorijaSBackendaNepoznateIdemNaKrajABezKategorijeZadnja() {
        val jela = listOf(
            jelo(1, "Grah", kategorija = "Variva"),
            jelo(2, "Čudo", kategorija = null),
            jelo(3, "Juha od graška", kategorija = "Juhe"),
            jelo(4, "Zagonetka", kategorija = "Zeta"),
            jelo(5, "Alfa", kategorija = "Alfa"),
            jelo(6, "Gulaš", kategorija = "Variva")
        )

        val skupine = grupirajPoKategoriji(jela, kategorije)

        assertEquals(listOf("Juhe", "Variva", "Alfa", "Zeta", null), skupine.map { it.kategorija })
        assertEquals(listOf(1L, 6L), skupine[1].jela.map { it.id })
        assertEquals(SkupinaJela(null, listOf(jela[1])), skupine.last())
    }

    @Test
    fun bezJelaNemaSkupina() {
        assertTrue(grupirajPoKategoriji(emptyList(), kategorije).isEmpty())
    }

    @Test
    fun pogodakUpitaNeovisiOVelikimSlovimaIDijakritici() {
        assertEquals(0..4, pogodakUpita("Ćevapi", "cevap"))
        assertEquals(0..4, pogodakUpita("Ćevapi", "ĆEVAP"))
        assertEquals(0..2, pogodakUpita("Đuveč", "duv"))
        assertEquals(7..10, pogodakUpita("Pileći file", "file"))
    }

    @Test
    fun pogodakUpitaPrednostDajePocetkuRijeciInaceBiloGdje() {
        // "ana" u "Banana" nije početak riječi, ali u "Sok od ananasa" jest
        assertEquals(7..9, pogodakUpita("Sok od ananasa", "ana"))
        assertEquals(1..3, pogodakUpita("Banana", "ana"))
        assertEquals(0..2, pogodakUpita("Ananas", "ana"))
    }

    @Test
    fun pogodakUpitaBezPogotkaIliPrazanUpit() {
        assertNull(pogodakUpita("Grah varivo", "riba"))
        assertNull(pogodakUpita("Grah varivo", ""))
        assertNull(pogodakUpita("Grah varivo", "   "))
    }

    @Test
    fun rangPogotka() {
        assertEquals(0, rangPogotka("Ananas", "ana"))
        assertEquals(1, rangPogotka("Sok od ananasa", "ana"))
        assertEquals(2, rangPogotka("Banana", "ana"))
        assertEquals(3, rangPogotka("Grah", "ana"))
        assertEquals(3, rangPogotka("Grah", ""))
        assertEquals("dijakritika se zanemaruje", 0, rangPogotka("Šnicla", "snic"))
    }

    @Test
    fun rezultatiPretrageSlazuSePoRelevantnostiPaPoCestoti() {
        val banana = jelo(1, "Banana")
        val sok = jelo(2, "Sok od ananasa")
        val ananasA = jelo(3, "Ananas A")
        val ananasB = jelo(4, "Ananas B")
        val grah = jelo(5, "Grah varivo")
        val ananasC = jelo(6, "Ananas C")
        val cesto = listOf(
            CestoJedenoJelo(ananasB, 5, "2026-10-27"),
            CestoJedenoJelo(ananasA, 2, "2026-10-27")
        )

        val poredano = rangirajPoUpitu(listOf(banana, sok, ananasA, ananasB, grah, ananasC), "ana", cesto)

        // početak naziva (češće jedeno prvo, inače redoslijed s backenda), početak riječi, bilo gdje, bez pogotka
        assertEquals(listOf(4L, 3L, 6L, 2L, 1L, 5L), poredano.map { it.id })
    }

    @Test
    fun aktivniFilteriBrojeSeKaoParoviMinMax() {
        assertEquals(0, brojAktivnihFiltera(MakroFilter()))
        assertEquals(1, brojAktivnihFiltera(MakroFilter(minKalorije = 100.0, maxKalorije = 500.0)))
        assertEquals(1, brojAktivnihFiltera(MakroFilter(maxMasti = 15.0)))
        assertEquals(
            4,
            brojAktivnihFiltera(MakroFilter(minKalorije = 1.0, maxProteini = 2.0, minUgljikohidrati = 3.0, maxMasti = 4.0))
        )
    }
}

class PrijedlogObrokaModelTest {

    private val prijedlog = PrijedlogObroka(
        stavke = listOf(
            PrijedlogStavka(1, "Grah varivo", 1.5, "VISOKA", 480.0, 27.0, 60.0, 13.5),
            PrijedlogStavka(2, "Kruh", 2.0, "NISKA", 140.0, 4.0, 28.0, 1.0)
        )
    )

    @Test
    fun ukupnoJeZbrojStavkiBezPonovnogMnozenja() {
        // Backend je vrijednosti već pomnožio količinom.
        assertEquals(Makro(620.0, 31.0, 14.5, 88.0), prijedlog.ukupno())
        assertEquals(Makro.NULA, PrijedlogObroka().ukupno())
    }

    @Test
    fun provjeraTrebaSveOsimVisokeSigurnosti() {
        assertFalse(prijedlog.stavke[0].trebaProvjeriti())
        assertTrue(prijedlog.stavke[1].trebaProvjeriti())
        assertTrue(prijedlog.stavke[0].copy(sigurnost = "SREDNJA").trebaProvjeriti())
    }
}
