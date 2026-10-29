package com.niko.macromenza.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.niko.macromenza.R
import com.niko.macromenza.model.ObrokRequest
import com.niko.macromenza.model.StavkaObrokaRequest
import com.niko.macromenza.model.Tjedan
import com.niko.macromenza.testutil.FakeApiRule
import com.niko.macromenza.testutil.MainDispatcherRule
import com.niko.macromenza.testutil.httpGreska
import com.niko.macromenza.testutil.neuspjeh
import com.niko.macromenza.testutil.obrok
import com.niko.macromenza.testutil.povijestDana
import com.niko.macromenza.testutil.stavka
import com.niko.macromenza.testutil.tjedan
import com.niko.macromenza.testutil.uspjeh
import com.niko.macromenza.util.UiTekst
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.slot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
class PovijestViewModelTest {

    @get:Rule val main = MainDispatcherRule()
    @get:Rule val fake = FakeApiRule()

    private val rucak = obrok(id = 100, datum = "2026-10-28", vrijeme = "13:00", tip = "RUCAK", izvor = "MENI",
        stavke = listOf(stavka(3, "Pileći file", 1.5, 380.0), stavka(4, "Rižoto", 1.0, 290.0)))
    private val dorucak = obrok(id = 101, datum = "2026-10-28", vrijeme = "08:00", tip = "DORUCAK")

    private val trenutni = tjedan("2026-10-26", imaPrethodni = true, imaSljedeci = false)

    @Before
    fun osnovniOdgovori() {
        coEvery { fake.api.getTjedan(7, any()) } returns trenutni
        coEvery { fake.api.getObroci(7, any()) } returns listOf(rucak, dorucak)
        coEvery { fake.api.dohvatiPovijestZaDan(7, any()) } returns povijestDana()
    }

    private fun vm(handle: SavedStateHandle = SavedStateHandle()) = PovijestViewModel(handle)

    // ---------- tjedan ----------

    @Test
    fun ucitavaTrenutniTjedan() = runTest {
        val vm = vm()

        vm.ucitajTjedan(7)
        advanceUntilIdle()

        assertEquals(trenutni, vm.tjedan.value)
        assertFalse(vm.greskaTjedna.value)
        coVerify { fake.api.getTjedan(7, null) }
    }

    @Test
    fun greskaTjednaSeBriseKadSljedeciPokusajUspije() = runTest {
        coEvery { fake.api.getTjedan(7, any()) } throws IOException() andThen trenutni
        val vm = vm()

        vm.ucitajTjedan(7)
        advanceUntilIdle()
        assertTrue(vm.greskaTjedna.value)
        assertNull(vm.tjedan.value)

        vm.ucitajTjedan(7)
        advanceUntilIdle()
        assertFalse(vm.greskaTjedna.value)
        assertEquals(trenutni, vm.tjedan.value)
    }

    @Test
    fun prethodniTjedanPomicePocetakZaTjedanDana() = runTest {
        val prosli = tjedan("2026-10-19", imaPrethodni = true, imaSljedeci = true)
        coEvery { fake.api.getTjedan(7, "2026-10-19") } returns prosli
        val handle = SavedStateHandle()
        val vm = vm(handle)
        vm.ucitajTjedan(7)
        advanceUntilIdle()

        vm.prethodniTjedan(7)
        advanceUntilIdle()

        assertEquals(prosli, vm.tjedan.value)
        assertEquals("odabir preživi gašenje procesa", "2026-10-19", handle.get<String>("pocetakTjedna"))
    }

    @Test
    fun sljedeciTjedanSeSmijeSamoAkoGaImaIPomiceUNaprijed() = runTest {
        val vm = vm()
        vm.ucitajTjedan(7)
        advanceUntilIdle()
        vm.sljedeciTjedan(7) // tekući tjedan nema sljedećeg
        advanceUntilIdle()
        coVerify(exactly = 1) { fake.api.getTjedan(7, any()) }

        val prosli = tjedan("2026-10-19", imaPrethodni = true, imaSljedeci = true)
        val sljedeci = tjedan("2026-10-26", imaSljedeci = false)
        coEvery { fake.api.getTjedan(7, null) } returns prosli
        coEvery { fake.api.getTjedan(7, "2026-10-26") } returns sljedeci
        vm.ovajTjedan(7)
        advanceUntilIdle()
        vm.sljedeciTjedan(7)
        advanceUntilIdle()

        assertEquals(sljedeci, vm.tjedan.value)
    }

    @Test
    fun prethodniTjedanBezPrethodnogIliBezUcitanogTjednaNistaNeRadi() = runTest {
        coEvery { fake.api.getTjedan(7, any()) } returns tjedan("2026-10-26", imaPrethodni = false)
        val vm = vm()

        vm.prethodniTjedan(7) // još ništa nije učitano
        advanceUntilIdle()
        coVerify(exactly = 0) { fake.api.getTjedan(any(), any()) }

        vm.ucitajTjedan(7)
        advanceUntilIdle()
        vm.prethodniTjedan(7) // backend kaže da nema prethodnog
        advanceUntilIdle()
        coVerify(exactly = 1) { fake.api.getTjedan(any(), any()) }
    }

    @Test
    fun ovajTjedanVracaNaTekuci() = runTest {
        val handle = SavedStateHandle(mapOf("pocetakTjedna" to "2026-09-14"))
        val vm = vm(handle)

        vm.ucitajTjedan(7)
        advanceUntilIdle()
        coVerify { fake.api.getTjedan(7, "2026-09-14") } // obnovljeno nakon gašenja procesa

        vm.ovajTjedan(7)
        advanceUntilIdle()

        coVerify { fake.api.getTjedan(7, null) }
        assertNull(handle.get<String>("pocetakTjedna"))
    }

    @Test
    fun brzoListanjeStariOdgovorNePregazujeNoviji() = runTest {
        val sporo = CompletableDeferred<Tjedan>()
        val prosli = tjedan("2026-10-19", imaPrethodni = true, imaSljedeci = true)
        coEvery { fake.api.getTjedan(7, null) } returns trenutni
        coEvery { fake.api.getTjedan(7, "2026-10-19") } coAnswers { sporo.await() }
        val vm = vm()
        vm.ucitajTjedan(7)
        advanceUntilIdle()

        vm.prethodniTjedan(7) // zahtjev za 19.10. visi
        runCurrent()
        vm.ovajTjedan(7) // korisnik se odmah vraća na tekući
        advanceUntilIdle()
        sporo.complete(prosli)
        advanceUntilIdle()

        assertEquals(trenutni, vm.tjedan.value)
    }

    // ---------- dan ----------

    @Test
    fun danSortiraObrokePoVremenu() = runTest {
        val vm = vm()

        vm.ucitajDan(7, "2026-10-28")
        advanceUntilIdle()

        assertEquals(listOf(101L, 100L), vm.obrociDana.value.map { it.id })
        assertEquals(povijestDana(), vm.povijestDana.value)
    }

    @Test
    fun greskaObrokaDanaDajePrazanPopis() = runTest {
        coEvery { fake.api.getObroci(7, any()) } throws IOException()
        val vm = vm()

        vm.ucitajDan(7, "2026-10-28")
        advanceUntilIdle()

        assertTrue(vm.obrociDana.value.isEmpty())
        assertEquals(povijestDana(), vm.povijestDana.value) // zbroj dana ne ovisi o popisu obroka
    }

    // ---------- brisanje i poništavanje ----------

    @Test
    fun uspjesnoBrisanjeMakneObrokOsvjezavaIOmogucujePonistavanje() = runTest {
        coEvery { fake.api.obrisiObrok(100, 7) } returns uspjeh()
        coEvery { fake.api.getObroci(7, any()) } returns listOf(rucak, dorucak) andThen listOf(dorucak)
        val vm = vm()
        vm.ucitajDan(7, "2026-10-28")
        advanceUntilIdle()

        vm.obrisiObrok(7, rucak)
        advanceUntilIdle()

        assertEquals(listOf(101L), vm.obrociDana.value.map { it.id })
        assertEquals(rucak, vm.obrisaniObrok.value)
        assertNull(vm.greskaBrisanja.value)
        coVerify(exactly = 2) { fake.api.getObroci(7, "2026-10-28") }
        coVerify(exactly = 1) { fake.api.getTjedan(7, any()) }
    }

    @Test
    fun brisanjeVecObrisanogObrokaJavljaPoruku() = runTest {
        coEvery { fake.api.obrisiObrok(any(), any()) } returns neuspjeh(404)
        val vm = vm()

        vm.obrisiObrok(7, rucak)
        advanceUntilIdle()

        assertEquals(UiTekst.Resurs(R.string.pov_obrok_vec_obrisan), vm.greskaBrisanja.value)
        assertNull(vm.obrisaniObrok.value)
        coVerify(exactly = 1) { fake.api.getTjedan(7, any()) } // popis se ipak osvježi
    }

    @Test
    fun drugaGreskaBrisanjaOstavljaObrokNaMjestu() = runTest {
        coEvery { fake.api.obrisiObrok(any(), any()) } returns neuspjeh(500)
        val vm = vm()
        vm.ucitajDan(7, "2026-10-28")
        advanceUntilIdle()

        vm.obrisiObrok(7, rucak)
        advanceUntilIdle()

        assertEquals(UiTekst.Resurs(R.string.kom_brisanje_greska_http, 500), vm.greskaBrisanja.value)
        assertEquals(2, vm.obrociDana.value.size)
        assertNull(vm.obrisaniObrok.value)
    }

    @Test
    fun prekidVezePriBrisanjuJeGreskaVeze() = runTest {
        coEvery { fake.api.obrisiObrok(any(), any()) } throws IOException()
        val vm = vm()

        vm.obrisiObrok(7, rucak)
        advanceUntilIdle()

        assertEquals(UiTekst.Resurs(R.string.kom_brisanje_greska_veza), vm.greskaBrisanja.value)
    }

    @Test
    fun dvostrukiDodirNaBrisanjeSaljejedanZahtjev() = runTest {
        val vrata = CompletableDeferred<Response<Unit>>()
        coEvery { fake.api.obrisiObrok(any(), any()) } coAnswers { vrata.await() }
        val vm = vm()

        vm.obrisiObrok(7, rucak)
        runCurrent()
        vm.obrisiObrok(7, rucak)
        runCurrent()
        vrata.complete(uspjeh())
        advanceUntilIdle()

        coVerify(exactly = 1) { fake.api.obrisiObrok(any(), any()) }
    }

    @Test
    fun ponistavanjeSpremaIstiObrokKaoNovi() = runTest {
        val zahtjev = slot<ObrokRequest>()
        coEvery { fake.api.spremiObrok(capture(zahtjev)) } returns rucak.copy(id = 555)
        val vm = vm()

        vm.vratiObrok(7, rucak)
        advanceUntilIdle()

        assertEquals(
            ObrokRequest(
                idKorisnik = 7, datum = "2026-10-28", vrijeme = "13:00", tipObroka = "RUCAK", izvor = "MENI",
                stavke = listOf(StavkaObrokaRequest(3, 1.5), StavkaObrokaRequest(4, 1.0))
            ),
            zahtjev.captured
        )
        coVerify(exactly = 1) { fake.api.getTjedan(7, any()) } // osvježeno nakon vraćanja
    }

    @Test
    fun greskePoništavanja() = runTest {
        val vm = vm()

        coEvery { fake.api.spremiObrok(any()) } throws httpGreska(400, "Datum obroka ne može biti u budućnosti.")
        vm.vratiObrok(7, rucak)
        advanceUntilIdle()
        assertEquals(
            UiTekst.Resurs(R.string.pov_vracanje_greska_poruka, "Datum obroka ne može biti u budućnosti."),
            vm.greskaBrisanja.value
        )

        coEvery { fake.api.spremiObrok(any()) } throws httpGreska(502)
        vm.vratiObrok(7, rucak)
        advanceUntilIdle()
        assertEquals(UiTekst.Resurs(R.string.pov_vracanje_greska_http, 502), vm.greskaBrisanja.value)

        coEvery { fake.api.spremiObrok(any()) } throws IOException()
        vm.vratiObrok(7, rucak)
        advanceUntilIdle()
        assertEquals(UiTekst.Resurs(R.string.pov_vracanje_greska_veza), vm.greskaBrisanja.value)
    }

    @Test
    fun ocistiRezultatBrisanjaBriseObrokIGresku() = runTest {
        coEvery { fake.api.obrisiObrok(any(), any()) } returns uspjeh()
        val vm = vm()
        vm.obrisiObrok(7, rucak)
        advanceUntilIdle()
        assertEquals(rucak, vm.obrisaniObrok.value)

        vm.ocistiRezultatBrisanja()

        assertNull(vm.obrisaniObrok.value)
        assertNull(vm.greskaBrisanja.value)
    }

    // ---------- primjer tjedna ----------

    @Test
    fun ucitavanjePrimjeraOsvjezavaTjedanITrajeSamoDokTraje() = runTest {
        val vrata = CompletableDeferred<Response<Unit>>()
        coEvery { fake.api.ucitajPrimjerTjedna(7) } coAnswers { vrata.await() }
        val vm = vm()

        vm.ucitajPrimjerTjedna(7)
        runCurrent()
        assertTrue(vm.primjerUTijeku.value)
        vm.ucitajPrimjerTjedna(7) // drugi dodir se ignorira
        runCurrent()
        vrata.complete(uspjeh())
        advanceUntilIdle()

        assertFalse(vm.primjerUTijeku.value)
        assertNull(vm.greskaBrisanja.value)
        coVerify(exactly = 1) { fake.api.ucitajPrimjerTjedna(7) }
        coVerify(exactly = 1) { fake.api.getTjedan(7, any()) }
    }

    @Test
    fun uklanjanjePrimjeraPozivaDeleteEndpoint() = runTest {
        coEvery { fake.api.ukloniPrimjerTjedna(7) } returns uspjeh()
        val vm = vm()

        vm.ukloniPrimjerTjedna(7)
        advanceUntilIdle()

        coVerify(exactly = 1) { fake.api.ukloniPrimjerTjedna(7) }
        coVerify(exactly = 0) { fake.api.ucitajPrimjerTjedna(any()) }
    }

    @Test
    fun greskePrimjeraTjedna() = runTest {
        val vm = vm()

        coEvery { fake.api.ucitajPrimjerTjedna(7) } returns neuspjeh(400, "Korisnik nema dnevni cilj (preporuku).")
        vm.ucitajPrimjerTjedna(7)
        advanceUntilIdle()
        assertEquals(
            UiTekst.Resurs(R.string.pov_primjer_ucitavanje_poruka, "Korisnik nema dnevni cilj (preporuku)."),
            vm.greskaBrisanja.value
        )

        coEvery { fake.api.ucitajPrimjerTjedna(7) } returns neuspjeh(503)
        vm.ucitajPrimjerTjedna(7)
        advanceUntilIdle()
        assertEquals(UiTekst.Resurs(R.string.pov_primjer_ucitavanje_http, 503), vm.greskaBrisanja.value)

        coEvery { fake.api.ukloniPrimjerTjedna(7) } throws IOException()
        vm.ukloniPrimjerTjedna(7)
        advanceUntilIdle()
        assertEquals(UiTekst.Resurs(R.string.pov_primjer_uklanjanje_veza), vm.greskaBrisanja.value)
        assertFalse(vm.primjerUTijeku.value)
    }
}
