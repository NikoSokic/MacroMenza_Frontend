package com.niko.macromenza.viewmodel

import com.niko.macromenza.R
import com.niko.macromenza.model.CestoJedenoJelo
import com.niko.macromenza.model.Jelo
import com.niko.macromenza.model.KategorijaJela
import com.niko.macromenza.model.Obrok
import com.niko.macromenza.model.ObrokRequest
import com.niko.macromenza.model.PovijestDana
import com.niko.macromenza.model.PrijedlogObroka
import com.niko.macromenza.model.PrijedlogStavka
import com.niko.macromenza.model.StavkaObrokaRequest
import com.niko.macromenza.testutil.FakeApiRule
import com.niko.macromenza.testutil.MainDispatcherRule
import com.niko.macromenza.testutil.httpGreska
import com.niko.macromenza.testutil.jelo
import com.niko.macromenza.testutil.meni
import com.niko.macromenza.testutil.meniStavka
import com.niko.macromenza.testutil.neuspjeh
import com.niko.macromenza.testutil.obrok
import com.niko.macromenza.testutil.povijestDana
import com.niko.macromenza.testutil.preporuka
import com.niko.macromenza.testutil.stavka
import com.niko.macromenza.testutil.uspjeh
import com.niko.macromenza.util.UiTekst
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.slot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Response
import java.io.IOException
import java.time.LocalDate

class MakroFilterTest {

    private val grah = jelo(kcal = 320.0, proteini = 18.0, ugljikohidrati = 40.0, masti = 9.0)

    @Test
    fun prazanFilterNijeAktivanIPropustaSve() {
        assertFalse(MakroFilter().aktivan)
        assertTrue(MakroFilter().propusta(grah))
    }

    @Test
    fun svakoPoljeAktiviraFilter() {
        listOf(
            MakroFilter(minKalorije = 1.0), MakroFilter(maxKalorije = 1.0),
            MakroFilter(minProteini = 1.0), MakroFilter(maxProteini = 1.0),
            MakroFilter(minUgljikohidrati = 1.0), MakroFilter(maxUgljikohidrati = 1.0),
            MakroFilter(minMasti = 1.0), MakroFilter(maxMasti = 1.0)
        ).forEach { assertTrue(it.toString(), it.aktivan) }
    }

    @Test
    fun graniceSuUkljucive() {
        assertTrue(MakroFilter(minKalorije = 320.0, maxKalorije = 320.0).propusta(grah))
        assertFalse(MakroFilter(minKalorije = 320.1).propusta(grah))
        assertFalse(MakroFilter(maxKalorije = 319.9).propusta(grah))
        assertTrue(MakroFilter(minProteini = 18.0).propusta(grah))
        assertFalse(MakroFilter(maxProteini = 17.9).propusta(grah))
        assertTrue(MakroFilter(maxUgljikohidrati = 40.0).propusta(grah))
        assertFalse(MakroFilter(minUgljikohidrati = 41.0).propusta(grah))
        assertTrue(MakroFilter(minMasti = 9.0, maxMasti = 9.0).propusta(grah))
        assertFalse(MakroFilter(minMasti = 10.0).propusta(grah))
    }

    @Test
    fun sviUvjetiMorajuVrijeditiIstodobno() {
        val f = MakroFilter(maxKalorije = 400.0, minProteini = 20.0)
        assertFalse("kcal je u rasponu, ali proteini nisu", f.propusta(grah))
        assertTrue(f.propusta(grah.copy(proteini = 20.0)))
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class JelaViewModelTest {

    @get:Rule val main = MainDispatcherRule()
    @get:Rule val fake = FakeApiRule()

    private val grah = jelo(1, "Grah varivo", 320.0, 18.0, 40.0, 9.0)
    private val juha = jelo(2, "Juha od gljiva", 80.0, 4.0, 10.0, 2.0)

    @Before
    fun osnovniOdgovori() {
        coEvery { fake.api.getCestoJedeno(7, any()) } returns emptyList()
        coEvery { fake.api.dohvatiPreporukeZaKorisnika(7) } returns listOf(preporuka(1, 2400.0))
        coEvery { fake.api.dohvatiPovijestZaDan(7, any()) } returns povijestDana(kcal = 1000.0)
    }

    private fun vm(sKorisnikom: Boolean = true) = JelaViewModel().also { if (sKorisnikom) it.postaviKorisnika(7) }

    private fun stubJela(odgovor: suspend () -> List<Jelo>) {
        coEvery {
            fake.api.getJela(
                search = any(), kategorija = any(),
                minKalorije = any(), maxKalorije = any(), minProteini = any(), maxProteini = any(),
                minUgljikohidrati = any(), maxUgljikohidrati = any(), minMasti = any(), maxMasti = any()
            )
        } coAnswers { odgovor() }
    }

    // ---------- količine ----------

    @Test
    fun prviPlusDajeJednuPorcijuPaKoraciPoPola() {
        val vm = vm(false)

        vm.povecaj(grah)
        assertEquals(1.0, vm.odabir.value.stavke.getValue(1).kolicina, 0.0)
        vm.povecaj(grah)
        assertEquals(1.5, vm.odabir.value.stavke.getValue(1).kolicina, 0.0)
        vm.povecaj(grah)
        assertEquals(2.0, vm.odabir.value.stavke.getValue(1).kolicina, 0.0)
    }

    @Test
    fun minusSmanjujePoPolaIPosljednjaPolaMakneJelo() {
        val vm = vm(false)
        vm.dodaj(grah, 1.5)

        vm.smanji(1)
        assertEquals(1.0, vm.odabir.value.stavke.getValue(1).kolicina, 0.0)
        vm.smanji(1)
        assertEquals(0.5, vm.odabir.value.stavke.getValue(1).kolicina, 0.0)
        vm.smanji(1)
        assertTrue(vm.odabir.value.stavke.isEmpty())
    }

    @Test
    fun smanjivanjeJelaKojeNijeUObrokuNistaNeRadi() {
        val vm = vm(false)
        vm.smanji(99)
        assertTrue(vm.odabir.value.stavke.isEmpty())
    }

    @Test
    fun dodavanjeIzDetaljaZbrajaSeNaPostojecuKolicinu() {
        val vm = vm(false)
        vm.povecaj(grah)

        vm.dodaj(grah, 2.0)

        assertEquals(3.0, vm.odabir.value.stavke.getValue(1).kolicina, 0.0)
    }

    @Test
    fun redoslijedJelaJeRedoslijedDodavanja() {
        val vm = vm(false)
        vm.povecaj(juha)
        vm.povecaj(grah)
        vm.povecaj(juha)

        assertEquals(listOf(2L, 1L), vm.odabir.value.stavke.keys.toList())
    }

    @Test
    fun uklanjanjeMakneJeloIOznakuMenija() {
        val vm = vm(false)
        vm.dodajMeni(meni(1, "RUCAK", false, meniStavka(10, "Juha", 1.0, 90.0)))
        assertEquals(setOf(10L), vm.odabir.value.jelaIzMenija)

        vm.ukloni(10)

        assertTrue(vm.odabir.value.stavke.isEmpty())
        assertTrue(vm.odabir.value.jelaIzMenija.isEmpty())
    }

    // ---------- meniji i prijedlozi u odabiru ----------

    @Test
    fun meniDodajeJelaSVrijednostimaPoPorciji() {
        val vm = vm(false)
        val m = meni(
            1, "RUCAK", false,
            meniStavka(10, "Juha od gljiva", 1.0, 90.0, 3.0, 10.0, 4.0),
            meniStavka(11, "Kruh", 3.0, 210.0, 6.0, 42.0, 1.5)
        )

        val staro = vm.dodajMeni(m)

        assertTrue(staro.stavke.isEmpty())
        val kruh = vm.odabir.value.stavke.getValue(11)
        assertEquals(3.0, kruh.kolicina, 0.0)
        assertEquals(70.0, kruh.jelo.kalorije, 0.0) // 210 kcal za 3 šnite = 70 po porciji
        assertEquals(14.0, kruh.jelo.ugljikohidrati, 0.0)
        assertEquals(setOf(10L, 11L), vm.odabir.value.jelaIzMenija)
    }

    @Test
    fun istiMeniDvaputaZbrajaKolicineIPonistavaSeNaStaro() {
        val vm = vm(false)
        val m = meni(1, "RUCAK", false, meniStavka(11, "Kruh", 3.0, 210.0))
        vm.dodajMeni(m)

        val prijeDrugog = vm.dodajMeni(m)

        assertEquals(6.0, vm.odabir.value.stavke.getValue(11).kolicina, 0.0)
        assertEquals(70.0, vm.odabir.value.stavke.getValue(11).jelo.kalorije, 0.0)
        vm.vratiOdabir(prijeDrugog)
        assertEquals(3.0, vm.odabir.value.stavke.getValue(11).kolicina, 0.0)
    }

    @Test
    fun meniNaVecOdabranoJeloKoristiNjegovoJelo() {
        val vm = vm(false)
        vm.povecaj(juha) // 1 porcija, 80 kcal

        vm.dodajMeni(meni(1, "RUCAK", false, meniStavka(2, "Juha od gljiva", 1.0, 95.0)))

        val s = vm.odabir.value.stavke.getValue(2)
        assertEquals(2.0, s.kolicina, 0.0)
        assertEquals(80.0, s.jelo.kalorije, 0.0)
    }

    @Test
    fun prijedlogIzPrepoznavanjaPreskaceNulteKolicine() {
        val vm = vm(false)
        val prijedlog = PrijedlogObroka(
            stavke = listOf(
                PrijedlogStavka(1, "Grah varivo", 1.5, "VISOKA", 480.0, 27.0, 60.0, 13.5),
                PrijedlogStavka(2, "Nešto", 0.0, "NISKA", 0.0, 0.0, 0.0, 0.0)
            )
        )

        vm.primijeniPrijedlog(prijedlog, "SLIKA")

        assertEquals(listOf(1L), vm.odabir.value.stavke.keys.toList())
        val s = vm.odabir.value.stavke.getValue(1)
        assertEquals(1.5, s.kolicina, 0.0)
        assertEquals(320.0, s.jelo.kalorije, 0.0) // 480 / 1,5
        assertEquals("SLIKA", vm.odabir.value.izvorPrijedloga)
    }

    // ---------- sažetak ----------

    @Test
    fun sazetakZbrajaOdabirIPrikazujeDanasnjePojedenoICilj() = runTest {
        val vm = vm()
        vm.povecaj(grah); vm.povecaj(grah) // 1,5 porcija
        vm.povecaj(juha)
        advanceUntilIdle()

        val s = vm.sazetak.value
        assertEquals(2, s.brojStavki)
        assertEquals(560.0, s.kalorije, 1e-9)       // 320 · 1,5 + 80
        assertEquals(31.0, s.proteini, 1e-9)
        assertEquals(70.0, s.ugljikohidrati, 1e-9)
        assertEquals(15.5, s.masti, 1e-9)
        assertEquals(1000.0, s.pojedenoKalorije!!, 0.0)
        assertEquals(2400.0, s.ciljKalorije!!, 0.0)
        assertEquals(1560.0, s.nakonObroka!!, 1e-9)
    }

    @Test
    fun sazetakBezPojedenogIBezCilja() = runTest {
        coEvery { fake.api.dohvatiPovijestZaDan(7, any()) } throws IOException()
        coEvery { fake.api.dohvatiPreporukeZaKorisnika(7) } returns emptyList()
        val vm = vm()
        vm.povecaj(grah)
        advanceUntilIdle()

        val s = vm.sazetak.value
        assertNull(s.pojedenoKalorije)
        assertNull(s.ciljKalorije)
        assertNull(s.nakonObroka)
        assertEquals(320.0, s.kalorije, 0.0)
    }

    @Test
    fun ciljJeZadnjaPreporukaPoIdu() = runTest {
        coEvery { fake.api.dohvatiPreporukeZaKorisnika(7) } returns listOf(preporuka(9, 2100.0), preporuka(4, 1800.0))
        val vm = vm()
        advanceUntilIdle()

        assertEquals(2100.0, vm.sazetak.value.ciljKalorije!!, 0.0)
    }

    // ---------- datum ----------

    @Test
    fun promjenaDatumaUcitavaPojedenoZaNoviDatumAIstiDatumNeUcitava() = runTest {
        val vm = vm()
        advanceUntilIdle()
        val danas = vm.datum.value
        coVerify(exactly = 1) { fake.api.dohvatiPovijestZaDan(7, danas.toString()) }

        vm.postaviDatum(danas)
        advanceUntilIdle()
        coVerify(exactly = 1) { fake.api.dohvatiPovijestZaDan(7, danas.toString()) }

        vm.postaviDatum(danas.minusDays(1))
        advanceUntilIdle()
        assertEquals(danas.minusDays(1), vm.datum.value)
        coVerify(exactly = 1) { fake.api.dohvatiPovijestZaDan(7, danas.minusDays(1).toString()) }
    }

    @Test
    fun sporijiOdgovorZaStariDatumNePregazujeNoviji() = runTest {
        val vm = vm()
        advanceUntilIdle()
        val d1 = vm.datum.value.minusDays(1)
        val d2 = vm.datum.value.minusDays(2)
        val sporo = CompletableDeferred<PovijestDana>()
        coEvery { fake.api.dohvatiPovijestZaDan(7, d1.toString()) } coAnswers { sporo.await() }
        coEvery { fake.api.dohvatiPovijestZaDan(7, d2.toString()) } returns povijestDana(d2.toString(), 222.0)

        vm.postaviDatum(d1)
        runCurrent()
        vm.postaviDatum(d2)
        advanceUntilIdle()
        sporo.complete(povijestDana(d1.toString(), 111.0))
        advanceUntilIdle()

        assertEquals(222.0, vm.sazetak.value.pojedenoKalorije!!, 0.0)
    }

    @Test
    fun bezKorisnikaDatumNistaNeUcitava() = runTest {
        val vm = vm(false)
        vm.postaviDatum(vm.datum.value.minusDays(1))
        advanceUntilIdle()

        coVerify(exactly = 0) { fake.api.dohvatiPovijestZaDan(any(), any()) }
    }

    // ---------- spremanje ----------

    @Test
    fun spremaCijeliObrokJednimPozivomPaCistiOdabir() = runTest {
        val zahtjev = slot<ObrokRequest>()
        coEvery { fake.api.spremiObrok(capture(zahtjev)) } returns obrok(id = 200)
        val vm = vm()
        advanceUntilIdle()
        vm.povecaj(grah); vm.povecaj(grah)
        vm.povecaj(juha)

        vm.spremiObrok("12:30", "RUCAK")
        advanceUntilIdle()

        assertEquals(
            ObrokRequest(
                idKorisnik = 7, datum = vm.datum.value.toString(), vrijeme = "12:30", tipObroka = "RUCAK",
                izvor = "RUCNO",
                stavke = listOf(StavkaObrokaRequest(1, 1.5), StavkaObrokaRequest(2, 1.0))
            ),
            zahtjev.captured
        )
        assertEquals(200L, vm.spremljeniObrok.value!!.id)
        assertTrue("prazan odabir sprječava dvostruko spremanje", vm.odabir.value.stavke.isEmpty())
        assertFalse(vm.spremanjeUTijeku.value)
        // Nakon spremanja osvježavaju se pojedeno i "Često jedeš".
        coVerify(exactly = 2) { fake.api.dohvatiPovijestZaDan(7, any()) }
        coVerify(exactly = 2) { fake.api.getCestoJedeno(7, any()) }
    }

    @Test
    fun izvorObrokaJeMeniDokGodImaJelaIzMenija() = runTest {
        val zahtjev = slot<ObrokRequest>()
        coEvery { fake.api.spremiObrok(capture(zahtjev)) } returns obrok()
        val vm = vm()
        vm.dodajMeni(meni(1, "RUCAK", false, meniStavka(10, "Juha", 1.0, 90.0)))
        vm.povecaj(grah)

        vm.spremiObrok("13:00", "RUCAK")
        advanceUntilIdle()
        assertEquals("MENI", zahtjev.captured.izvor)

        // Kad se jelo iz menija ukloni, izvor se vraća na RUCNO.
        vm.dodajMeni(meni(1, "RUCAK", false, meniStavka(10, "Juha", 1.0, 90.0)))
        vm.povecaj(grah)
        vm.ukloni(10)
        vm.spremiObrok("13:00", "RUCAK")
        advanceUntilIdle()
        assertEquals("RUCNO", zahtjev.captured.izvor)
    }

    @Test
    fun izvorObrokaIzPrijedlogaJeSlikaIliOpis() = runTest {
        val zahtjev = slot<ObrokRequest>()
        coEvery { fake.api.spremiObrok(capture(zahtjev)) } returns obrok()
        val vm = vm()
        vm.primijeniPrijedlog(
            PrijedlogObroka(listOf(PrijedlogStavka(1, "Grah", 1.0, "VISOKA", 320.0, 18.0, 40.0, 9.0))), "OPIS"
        )

        vm.spremiObrok("13:00", "RUCAK")
        advanceUntilIdle()

        assertEquals("OPIS", zahtjev.captured.izvor)
    }

    @Test
    fun prazanObrokIliNepoznatKorisnikSeNeSpremaju() = runTest {
        val bezKorisnika = vm(false)
        bezKorisnika.povecaj(grah)
        bezKorisnika.spremiObrok("12:30", "RUCAK")
        val prazan = vm()
        prazan.spremiObrok("12:30", "RUCAK")
        advanceUntilIdle()

        coVerify(exactly = 0) { fake.api.spremiObrok(any()) }
    }

    @Test
    fun dvostrukiDodirNaSpremiSaljeJedanZahtjev() = runTest {
        val vrata = CompletableDeferred<Obrok>()
        coEvery { fake.api.spremiObrok(any()) } coAnswers { vrata.await() }
        val vm = vm()
        vm.povecaj(grah)

        vm.spremiObrok("12:30", "RUCAK")
        runCurrent()
        assertTrue(vm.spremanjeUTijeku.value)
        vm.spremiObrok("12:30", "RUCAK")
        runCurrent()
        vrata.complete(obrok())
        advanceUntilIdle()

        coVerify(exactly = 1) { fake.api.spremiObrok(any()) }
    }

    @Test
    fun greskaBackendaZadrzavaOdabirIPokazujePoruku() = runTest {
        coEvery { fake.api.spremiObrok(any()) } throws httpGreska(400, "Datum obroka ne može biti u budućnosti.")
        val vm = vm()
        vm.povecaj(grah)

        vm.spremiObrok("12:30", "RUCAK")
        advanceUntilIdle()

        assertEquals(UiTekst.Dinamicki("Datum obroka ne može biti u budućnosti."), vm.greskaSpremanja.value)
        assertEquals(1, vm.odabir.value.stavke.size)
        assertNull(vm.spremljeniObrok.value)
        assertFalse(vm.spremanjeUTijeku.value)
    }

    @Test
    fun httpGreskaBezPorukeIGreskaVeze() = runTest {
        val vm = vm()
        vm.povecaj(grah)

        coEvery { fake.api.spremiObrok(any()) } throws httpGreska(500)
        vm.spremiObrok("12:30", "RUCAK")
        advanceUntilIdle()
        assertEquals(UiTekst.Resurs(R.string.uo_spremanje_greska_http, 500), vm.greskaSpremanja.value)

        coEvery { fake.api.spremiObrok(any()) } throws IOException()
        vm.spremiObrok("12:30", "RUCAK")
        advanceUntilIdle()
        assertEquals(UiTekst.Resurs(R.string.kom_spremanje_greska_veza), vm.greskaSpremanja.value)
        assertFalse(vm.spremanjeUTijeku.value)
    }

    @Test
    fun ocistiRezultatBrisePoruke() = runTest {
        coEvery { fake.api.spremiObrok(any()) } returns obrok()
        val vm = vm()
        vm.povecaj(grah)
        vm.spremiObrok("12:30", "RUCAK")
        advanceUntilIdle()
        assertTrue(vm.spremljeniObrok.value != null)

        vm.ocistiRezultatSpremanja()

        assertNull(vm.spremljeniObrok.value)
        assertNull(vm.azuriraniObrok.value)
        assertNull(vm.obrisaniObrok.value)
        assertNull(vm.greskaSpremanja.value)
    }

    // ---------- uređivanje ----------

    private val spremljen = obrok(
        id = 100, datum = vmDatum(), vrijeme = "12:30", tip = "RUCAK", izvor = "OPIS",
        stavke = listOf(stavka(1, "Grah varivo", 1.5, 480.0, 27.0, 60.0, 13.5))
    )

    private fun vmDatum(): String = LocalDate.now(java.time.ZoneId.of("Europe/Zagreb")).toString()

    private fun TestScope.uVrstiUredivanje(): JelaViewModel {
        coEvery { fake.api.getObrok(100, 7) } returns spremljen
        val vm = vm()
        vm.ucitajObrok(100)
        advanceUntilIdle()
        return vm
    }

    @Test
    fun ucitavanjeObrokaPunioOdabirSVrijednostimaPoPorciji() = runTest {
        val vm = uVrstiUredivanje()

        assertEquals(spremljen, vm.uredivaniObrok.value)
        val s = vm.odabir.value.stavke.getValue(1)
        assertEquals(1.5, s.kolicina, 0.0)
        assertEquals(320.0, s.jelo.kalorije, 1e-9) // 480 / 1,5
        assertEquals(40.0, s.jelo.ugljikohidrati, 1e-9)
        assertFalse(vm.ucitavanjeObroka.value)
    }

    @Test
    fun urediviObrokSeNeBrojiDvaputUPojedenom() = runTest {
        val vm = uVrstiUredivanje()

        // Pojedeno danas je 1000, a od toga je 480 ovaj obrok.
        assertEquals(520.0, vm.sazetak.value.pojedenoKalorije!!, 1e-9)
        assertEquals(480.0, vm.sazetak.value.kalorije, 1e-9)
        assertEquals(1000.0, vm.sazetak.value.nakonObroka!!, 1e-9)
    }

    @Test
    fun obrokIzDrugogDanaPrebacujeDatum() = runTest {
        val jucer = LocalDate.now(java.time.ZoneId.of("Europe/Zagreb")).minusDays(1).toString()
        coEvery { fake.api.getObrok(100, 7) } returns spremljen.copy(datum = jucer)
        val vm = vm()

        vm.ucitajObrok(100)
        advanceUntilIdle()

        assertEquals(jucer, vm.datum.value.toString())
        coVerify { fake.api.dohvatiPovijestZaDan(7, jucer) }
    }

    @Test
    fun istiObrokSeNeUcitavaDvaput() = runTest {
        val vm = uVrstiUredivanje()

        vm.ucitajObrok(100)
        advanceUntilIdle()

        coVerify(exactly = 1) { fake.api.getObrok(100, 7) }
    }

    @Test
    fun greskeUcitavanjaObroka() = runTest {
        val vm = vm()

        coEvery { fake.api.getObrok(any(), any()) } throws httpGreska(404)
        vm.ucitajObrok(1)
        advanceUntilIdle()
        assertEquals(UiTekst.Resurs(R.string.uo_obrok_ne_postoji), vm.greskaUcitavanja.value)

        coEvery { fake.api.getObrok(any(), any()) } throws httpGreska(500)
        vm.ucitajObrok(2)
        advanceUntilIdle()
        assertEquals(UiTekst.Resurs(R.string.uo_obrok_greska_http, 500), vm.greskaUcitavanja.value)

        coEvery { fake.api.getObrok(any(), any()) } throws IOException()
        vm.ucitajObrok(3)
        advanceUntilIdle()
        assertEquals(UiTekst.Resurs(R.string.uo_obrok_greska_veza), vm.greskaUcitavanja.value)
        assertFalse(vm.ucitavanjeObroka.value)

        vm.ocistiGreskuUcitavanja()
        assertNull(vm.greskaUcitavanja.value)
    }

    @Test
    fun imaPromjenaPrepoznajeSvakuIzmjenu() = runTest {
        val vm = uVrstiUredivanje()

        assertFalse(vm.imaPromjena("12:30", "RUCAK"))
        assertTrue("vrijeme", vm.imaPromjena("13:00", "RUCAK"))
        assertTrue("tip", vm.imaPromjena("12:30", "VECERA"))

        vm.povecaj(grah)
        assertTrue("količina", vm.imaPromjena("12:30", "RUCAK"))
        vm.smanji(1)
        assertFalse("vraćeno na staro", vm.imaPromjena("12:30", "RUCAK"))

        vm.povecaj(juha)
        assertTrue("novo jelo", vm.imaPromjena("12:30", "RUCAK"))
        vm.ukloni(2)

        vm.postaviDatum(vm.datum.value.minusDays(1))
        assertTrue("datum", vm.imaPromjena("12:30", "RUCAK"))
    }

    @Test
    fun noviUnosNikadNemaPromjena() {
        val vm = vm(false)
        vm.povecaj(grah)
        assertFalse(vm.imaPromjena("12:30", "RUCAK"))
    }

    @Test
    fun azuriranjeSaljeIzmijenjenObrokSIzvornimIzvorom() = runTest {
        val vm = uVrstiUredivanje()
        val zahtjev = slot<ObrokRequest>()
        coEvery { fake.api.azurirajObrok(100, 7, capture(zahtjev)) } returns spremljen.copy(vrijeme = "13:00")
        vm.povecaj(grah) // 2,0

        vm.azurirajObrok("13:00", "VECERA")
        advanceUntilIdle()

        assertEquals(
            ObrokRequest(7, vm.datum.value.toString(), "13:00", "VECERA", "OPIS", listOf(StavkaObrokaRequest(1, 2.0))),
            zahtjev.captured
        )
        assertEquals("13:00", vm.azuriraniObrok.value!!.vrijeme)
        assertFalse(vm.spremanjeUTijeku.value)
    }

    @Test
    fun azuriranjeSMenijemMijenjaIzvorUMeni() = runTest {
        val vm = uVrstiUredivanje()
        val zahtjev = slot<ObrokRequest>()
        coEvery { fake.api.azurirajObrok(any(), any(), capture(zahtjev)) } returns spremljen
        vm.dodajMeni(meni(1, "RUCAK", false, meniStavka(10, "Juha", 1.0, 90.0)))

        vm.azurirajObrok("12:30", "RUCAK")
        advanceUntilIdle()

        assertEquals("MENI", zahtjev.captured.izvor)
    }

    @Test
    fun azuriranjeBezUcitanogObrokaNistaNeSalje() = runTest {
        val vm = vm()
        vm.povecaj(grah)

        vm.azurirajObrok("12:30", "RUCAK")
        advanceUntilIdle()

        coVerify(exactly = 0) { fake.api.azurirajObrok(any(), any(), any()) }
    }

    @Test
    fun greskaAzuriranja() = runTest {
        val vm = uVrstiUredivanje()
        coEvery { fake.api.azurirajObrok(any(), any(), any()) } throws httpGreska(404, "Obrok ne postoji.")

        vm.azurirajObrok("12:30", "RUCAK")
        advanceUntilIdle()

        assertEquals(UiTekst.Dinamicki("Obrok ne postoji."), vm.greskaSpremanja.value)
        assertNull(vm.azuriraniObrok.value)
    }

    @Test
    fun brisanjeUredivanogObrokaVracaOriginalniObrok() = runTest {
        val vm = uVrstiUredivanje()
        coEvery { fake.api.obrisiObrok(100, 7) } returns uspjeh()
        vm.povecaj(grah) // nespremljena izmjena ne smije utjecati na ono što se vraća za "Poništi"

        vm.obrisiObrok()
        advanceUntilIdle()

        assertEquals(spremljen, vm.obrisaniObrok.value)
        assertFalse(vm.spremanjeUTijeku.value)
    }

    @Test
    fun greskeBrisanjaUredivanogObroka() = runTest {
        val vm = uVrstiUredivanje()

        coEvery { fake.api.obrisiObrok(any(), any()) } returns neuspjeh(500)
        vm.obrisiObrok()
        advanceUntilIdle()
        assertEquals(UiTekst.Resurs(R.string.kom_brisanje_greska_http, 500), vm.greskaSpremanja.value)

        coEvery { fake.api.obrisiObrok(any(), any()) } returns neuspjeh(404, "Obrok ne postoji.")
        vm.obrisiObrok()
        advanceUntilIdle()
        assertEquals(UiTekst.Dinamicki("Obrok ne postoji."), vm.greskaSpremanja.value)

        coEvery { fake.api.obrisiObrok(any(), any()) } throws IOException()
        vm.obrisiObrok()
        advanceUntilIdle()
        assertEquals(UiTekst.Resurs(R.string.kom_brisanje_greska_veza), vm.greskaSpremanja.value)
        assertNull(vm.obrisaniObrok.value)
    }

    // ---------- pretraga i filteri ----------

    @Test
    fun pretragaCekaMirovanjeOdTristoMsISaljeSamoZadnjiUpit() = runTest {
        stubJela { listOf(grah) }
        val vm = vm(false)

        vm.postaviUpit("g")
        advanceTimeBy(100)
        vm.postaviUpit("gr")
        advanceTimeBy(100)
        vm.postaviUpit("gra")
        advanceTimeBy(299)
        runCurrent()
        coVerify(exactly = 0) { fake.api.getJela(search = any(), kategorija = any()) }

        advanceTimeBy(2)
        runCurrent()

        coVerify(exactly = 1) { fake.api.getJela(search = "gra") }
        coVerify(exactly = 1) { fake.api.getJela(search = any(), kategorija = any()) }
        assertEquals(listOf(grah), vm.jela.value)
        assertEquals("gra", vm.upit.value)
    }

    @Test
    fun prazanUpitSeSaljeKaoNull() = runTest {
        stubJela { emptyList() }
        val vm = vm(false)

        vm.postaviUpit("   ")
        advanceUntilIdle()

        coVerify(exactly = 1) { fake.api.getJela(search = null) }
    }

    @Test
    fun kategorijaIMakroFilterSePrenoseNaBackend() = runTest {
        stubJela { listOf(juha) }
        val vm = vm(false)

        vm.postaviKategoriju("Juhe")
        advanceUntilIdle()
        coVerify(exactly = 1) { fake.api.getJela(kategorija = "Juhe") }

        vm.postaviKategoriju("Juhe") // ista kategorija: bez novog zahtjeva
        advanceUntilIdle()
        coVerify(exactly = 1) { fake.api.getJela(kategorija = any(), search = any()) }

        vm.primijeniMakroFilter(MakroFilter(minProteini = 25.0, maxKalorije = 500.0))
        advanceUntilIdle()
        coVerify(exactly = 1) { fake.api.getJela(kategorija = "Juhe", minProteini = 25.0, maxKalorije = 500.0) }
        assertTrue(vm.makroFilter.value.aktivan)

        vm.resetirajMakroFilter()
        advanceUntilIdle()
        coVerify(exactly = 2) { fake.api.getJela(kategorija = "Juhe") }
        assertFalse(vm.makroFilter.value.aktivan)
    }

    @Test
    fun kategorijaNaNullUklanjaFilter() = runTest {
        stubJela { listOf(grah) }
        val vm = vm(false)
        vm.postaviKategoriju("Juhe")
        advanceUntilIdle()

        vm.postaviKategoriju(null)
        advanceUntilIdle()

        coVerify(exactly = 1) { fake.api.getJela(kategorija = null) }
        assertNull(vm.kategorija.value)
    }

    @Test
    fun noviZahtjevOtkazujeStariPaSporijiOdgovorNePregazujeNoviji() = runTest {
        val sporo = CompletableDeferred<List<Jelo>>()
        var poziva = 0
        stubJela { if (poziva++ == 0) sporo.await() else listOf(juha) }
        val vm = vm(false)

        vm.primijeniMakroFilter(MakroFilter(maxKalorije = 500.0)) // vise
        runCurrent()
        vm.resetirajMakroFilter() // brzi
        advanceUntilIdle()
        sporo.complete(listOf(grah))
        advanceUntilIdle()

        assertEquals(listOf(juha), vm.jela.value)
    }

    @Test
    fun greskaPretrageSeBrisePriSljedecemUspjehu() = runTest {
        var poziva = 0
        stubJela { if (poziva++ == 0) throw IOException("Nema veze") else listOf(grah) }
        val vm = vm(false)

        vm.ucitajJela()
        advanceUntilIdle()
        assertEquals("Nema veze", vm.greska.value)

        vm.ucitajJela()
        advanceUntilIdle()
        assertNull(vm.greska.value)
        assertEquals(listOf(grah), vm.jela.value)
    }

    // ---------- kategorije, često jedeno, meniji ----------

    @Test
    fun kategorijeSeUcitavaju() = runTest {
        val kategorije = listOf(KategorijaJela("Juhe", 12), KategorijaJela("Variva", 20))
        coEvery { fake.api.getKategorijeJela() } returns kategorije
        val vm = vm(false)

        vm.ucitajKategorije()
        advanceUntilIdle()

        assertEquals(kategorije, vm.kategorije.value)
    }

    @Test
    fun greskaKategorijaPostajePoruka() = runTest {
        coEvery { fake.api.getKategorijeJela() } throws IOException("Nema veze")
        val vm = vm(false)

        vm.ucitajKategorije()
        advanceUntilIdle()

        assertEquals("Nema veze", vm.greska.value)
    }

    @Test
    fun cestoJedenoSeUcitavaAGreskaDajePrazanPopis() = runTest {
        val cesto = listOf(CestoJedenoJelo(grah, 5, "2026-10-27"))
        coEvery { fake.api.getCestoJedeno(7, any()) } returns cesto andThenThrows IOException()
        val vm = vm()
        advanceUntilIdle()
        assertEquals(cesto, vm.cestoJedeno.value)

        vm.ucitajCestoJedeno(7)
        advanceUntilIdle()

        assertTrue("dodatak koji ne uspije samo nestane", vm.cestoJedeno.value.isEmpty())
    }

    @Test
    fun meniUcitavaSeJednomKadSeOtvoriKartica() = runTest {
        val meniji = listOf(meni(1, "RUCAK", false, meniStavka(10, "Juha", 1.0, 90.0)))
        coEvery { fake.api.getMeniji(any(), any()) } returns meniji
        val vm = vm(false)

        vm.postaviKarticu(KarticaUnosa.MENIJI)
        advanceUntilIdle()
        vm.postaviKarticu(KarticaUnosa.JELA)
        vm.postaviKarticu(KarticaUnosa.MENIJI)
        advanceUntilIdle()

        assertEquals(meniji, vm.meniji.value)
        assertEquals(KarticaUnosa.MENIJI, vm.kartica.value)
        assertFalse(vm.menijiUcitavanje.value)
        coVerify(exactly = 1) { fake.api.getMeniji(any(), any()) }
    }

    @Test
    fun greskaMenijaDopustaPonovniPokusaj() = runTest {
        val meniji = listOf(meni(1, "RUCAK", false, meniStavka(10, "Juha", 1.0, 90.0)))
        coEvery { fake.api.getMeniji(any(), any()) } throws IOException() andThen meniji
        val vm = vm(false)

        vm.postaviKarticu(KarticaUnosa.MENIJI)
        advanceUntilIdle()
        assertEquals(UiTekst.Resurs(R.string.uo_greska_menija), vm.greskaMenija.value)

        vm.ucitajMenije()
        advanceUntilIdle()

        assertNull(vm.greskaMenija.value)
        assertEquals(meniji, vm.meniji.value)
    }

    @Test
    fun filteriMenijaSuStanjeViewModela() {
        val vm = vm(false)

        vm.postaviTipMenija("VECERA")
        vm.postaviSamoVegetarijanski(true)

        assertEquals("VECERA", vm.tipMenija.value)
        assertTrue(vm.samoVegetarijanski.value)
    }
}
