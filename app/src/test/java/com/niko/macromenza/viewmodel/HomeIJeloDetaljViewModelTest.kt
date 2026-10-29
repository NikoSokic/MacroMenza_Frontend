package com.niko.macromenza.viewmodel

import com.niko.macromenza.R
import com.niko.macromenza.model.JeloDetalj
import com.niko.macromenza.testutil.FakeApiRule
import com.niko.macromenza.testutil.MainDispatcherRule
import com.niko.macromenza.testutil.danPregled
import com.niko.macromenza.testutil.httpGreska
import com.niko.macromenza.util.UiTekst
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule val main = MainDispatcherRule()
    @get:Rule val fake = FakeApiRule()

    @Test
    fun prviPutPokazujeUcitavanjePaPodatke() = runTest {
        val pregled = danPregled()
        val odgovor = CompletableDeferred<com.niko.macromenza.model.DanPregled>()
        coEvery { fake.api.getDanPregled(any(), any()) } coAnswers { odgovor.await() }
        val vm = HomeViewModel()

        vm.ucitajHome(7)
        runCurrent()
        assertTrue(vm.ucitava.value)
        assertNull(vm.danPregled.value)

        odgovor.complete(pregled)
        advanceUntilIdle()
        assertFalse(vm.ucitava.value)
        assertEquals(pregled, vm.danPregled.value)
    }

    @Test
    fun saljeDanasnjiDatumUZagrebu() = runTest {
        coEvery { fake.api.getDanPregled(any(), any()) } returns danPregled()
        val vm = HomeViewModel()

        vm.ucitajHome(7)
        advanceUntilIdle()

        coVerify { fake.api.getDanPregled(7, LocalDate.now(ZoneId.of("Europe/Zagreb")).toString()) }
    }

    @Test
    fun kasnijeOsvjezavanjeIdeTihoUPozadini() = runTest {
        val prvi = danPregled()
        val drugi = danPregled(pojedeno = com.niko.macromenza.testutil.vrijednosti(1500.0))
        val vrata = CompletableDeferred<com.niko.macromenza.model.DanPregled>()
        var poziva = 0
        coEvery { fake.api.getDanPregled(any(), any()) } coAnswers { if (poziva++ == 0) prvi else vrata.await() }
        val vm = HomeViewModel()
        vm.ucitajHome(7)
        advanceUntilIdle()

        vm.ucitajHome(7)
        runCurrent()
        assertFalse("splash se ne vraća kad već ima podataka", vm.ucitava.value)
        assertEquals(prvi, vm.danPregled.value)

        vrata.complete(drugi)
        advanceUntilIdle()
        assertEquals(drugi, vm.danPregled.value)
    }

    @Test
    fun greskaOstavljaEkranPrazanBezSrusavanja() = runTest {
        coEvery { fake.api.getDanPregled(any(), any()) } throws IOException("nema mreže")
        val vm = HomeViewModel()

        vm.ucitajHome(7)
        advanceUntilIdle()

        assertNull(vm.danPregled.value)
        assertFalse(vm.ucitava.value)
    }

    @Test
    fun greskaNakonUspjehaZadrzavaStarePodatke() = runTest {
        val prvi = danPregled()
        coEvery { fake.api.getDanPregled(any(), any()) } returns prvi andThenThrows IOException()
        val vm = HomeViewModel()
        vm.ucitajHome(7)
        advanceUntilIdle()

        vm.ucitajHome(7)
        advanceUntilIdle()

        assertEquals(prvi, vm.danPregled.value)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class JeloDetaljViewModelTest {

    @get:Rule val main = MainDispatcherRule()
    @get:Rule val fake = FakeApiRule()

    private fun detalj(id: Long, naziv: String) =
        JeloDetalj(id, naziv, null, 320.0, 18.0, 40.0, 9.0, null, "Variva", null, null, 350.0, emptyList())

    @Test
    fun pocinjeUcitavanjem() {
        assertEquals(DetaljStanje.Ucitavanje, JeloDetaljViewModel().stanje.value)
    }

    @Test
    fun uspjehDajePodatke() = runTest {
        val d = detalj(5, "Grah varivo")
        coEvery { fake.api.getJeloDetalj(5) } returns d
        val vm = JeloDetaljViewModel()

        vm.ucitaj(5)
        assertEquals(DetaljStanje.Ucitavanje, vm.stanje.value)
        advanceUntilIdle()

        assertEquals(DetaljStanje.Podaci(d), vm.stanje.value)
    }

    @Test
    fun nepostojeceJeloDaje404Poruku() = runTest {
        coEvery { fake.api.getJeloDetalj(any()) } throws httpGreska(404)
        val vm = JeloDetaljViewModel()

        vm.ucitaj(999)
        advanceUntilIdle()

        assertEquals(DetaljStanje.Greska(UiTekst.Resurs(R.string.jd_greska_404)), vm.stanje.value)
    }

    @Test
    fun drugiHttpStatusPrikazujeKod() = runTest {
        coEvery { fake.api.getJeloDetalj(any()) } throws httpGreska(500)
        val vm = JeloDetaljViewModel()

        vm.ucitaj(5)
        advanceUntilIdle()

        assertEquals(DetaljStanje.Greska(UiTekst.Resurs(R.string.jd_greska_http, 500)), vm.stanje.value)
    }

    @Test
    fun greskaVezeNijeHttpGreska() = runTest {
        coEvery { fake.api.getJeloDetalj(any()) } throws IOException()
        val vm = JeloDetaljViewModel()

        vm.ucitaj(5)
        advanceUntilIdle()

        assertEquals(DetaljStanje.Greska(UiTekst.Resurs(R.string.jd_greska_veza)), vm.stanje.value)
    }

    @Test
    fun noviZahtjevOtkazujePrethodniPaSporijiOdgovorNePregazujeNoviji() = runTest {
        val sporo = CompletableDeferred<JeloDetalj>()
        val brzo = detalj(2, "Juha")
        coEvery { fake.api.getJeloDetalj(1) } coAnswers { sporo.await() }
        coEvery { fake.api.getJeloDetalj(2) } returns brzo
        val vm = JeloDetaljViewModel()

        vm.ucitaj(1)
        runCurrent()
        vm.ucitaj(2)
        advanceUntilIdle()
        sporo.complete(detalj(1, "Grah"))
        advanceUntilIdle()

        assertEquals(DetaljStanje.Podaci(brzo), vm.stanje.value)
    }

    @Test
    fun ponovnoUcitavanjeNakonGreskeVracaUcitavanje() = runTest {
        coEvery { fake.api.getJeloDetalj(5) } throws httpGreska(500) andThen detalj(5, "Grah")
        val vm = JeloDetaljViewModel()
        vm.ucitaj(5)
        advanceUntilIdle()

        vm.ucitaj(5)
        assertEquals(DetaljStanje.Ucitavanje, vm.stanje.value)
        advanceUntilIdle()

        assertTrue(vm.stanje.value is DetaljStanje.Podaci)
    }
}
