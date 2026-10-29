package com.niko.macromenza.viewmodel

import com.niko.macromenza.R
import com.niko.macromenza.model.BrzoMjerenjeOdgovor
import com.niko.macromenza.model.BrzoMjerenjeRequest
import com.niko.macromenza.model.MjerenjeMase
import com.niko.macromenza.testutil.FakeApiRule
import com.niko.macromenza.testutil.MainDispatcherRule
import com.niko.macromenza.testutil.httpGreska
import com.niko.macromenza.testutil.mjerenje
import com.niko.macromenza.testutil.preporuka
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class MjereViewModelTest {

    @get:Rule val main = MainDispatcherRule()
    @get:Rule val fake = FakeApiRule()

    private val mase = listOf(MjerenjeMase(1, "2026-10-27", 80.2), MjerenjeMase(2, "2026-10-28", 80.0))

    @Before
    fun osnovniOdgovori() {
        coEvery { fake.api.dohvatiMjerenjaZaKorisnika(7) } returns listOf(mjerenje(1), mjerenje(2))
        coEvery { fake.api.getMase(7, any(), any()) } returns mase
        coEvery { fake.api.dohvatiPreporukeZaKorisnika(7) } returns listOf(preporuka(1))
    }

    @Test
    fun ucitavaMjerenjaGrafIPreporuke() = runTest {
        val vm = MjereViewModel()

        vm.ucitajMjerenja(7)
        advanceUntilIdle()

        assertEquals(2, vm.mjerenja.value.size)
        assertEquals(mase, vm.mase.value)
        assertEquals(1, vm.preporuke.value.size)
        assertNull(vm.greska.value)
    }

    @Test
    fun greskaUcitavanjaPostajePoruka() = runTest {
        coEvery { fake.api.getMase(any(), any(), any()) } throws IOException("Nema veze")
        val vm = MjereViewModel()

        vm.ucitajMjerenja(7)
        advanceUntilIdle()

        assertEquals("Nema veze", vm.greska.value)
    }

    @Test
    fun brzoMjerenjeSaljeMasuIzDesetinki() = runTest {
        coEvery { fake.api.brzoMjerenje(any()) } returns BrzoMjerenjeOdgovor(mase.last(), preporuka(kcal = 2796.0))
        val vm = MjereViewModel()

        vm.spremiBrzo(7, 724)
        advanceUntilIdle()

        // 724 / 10.0 je točno 72.4 (72.3 + 0.1 bi dalo 72.39999999999999)
        coVerify(exactly = 1) { fake.api.brzoMjerenje(BrzoMjerenjeRequest(7, 72.4)) }
    }

    @Test
    fun uspjehPokazujeNoviCiljIOsvjezavaGraf() = runTest {
        coEvery { fake.api.brzoMjerenje(any()) } returns BrzoMjerenjeOdgovor(mase.last(), preporuka(kcal = 2796.0))
        val vm = MjereViewModel()

        vm.spremiBrzo(7, 800)
        advanceUntilIdle()

        assertEquals(PorukaMjerenja(UiTekst.Resurs(R.string.mj_brzo_spremljeno, "2796"), uspjeh = true), vm.poruka.value)
        assertFalse(vm.sprema.value)
        assertEquals(mase, vm.mase.value)
        coVerify(exactly = 1) { fake.api.getMase(7, any(), any()) }
    }

    @Test
    fun drugiDodirDokSpremaSeIgnorira() = runTest {
        val vrata = CompletableDeferred<BrzoMjerenjeOdgovor>()
        coEvery { fake.api.brzoMjerenje(any()) } coAnswers { vrata.await() }
        val vm = MjereViewModel()

        vm.spremiBrzo(7, 800)
        runCurrent()
        assertTrue(vm.sprema.value)
        vm.spremiBrzo(7, 801)
        runCurrent()
        vrata.complete(BrzoMjerenjeOdgovor(mase.last(), preporuka()))
        advanceUntilIdle()

        coVerify(exactly = 1) { fake.api.brzoMjerenje(any()) }
        coVerify(exactly = 0) { fake.api.brzoMjerenje(BrzoMjerenjeRequest(7, 80.1)) }
        assertFalse(vm.sprema.value)
    }

    @Test
    fun porukaBackendaSePrikazujeKakvaJe() = runTest {
        coEvery { fake.api.brzoMjerenje(any()) } throws httpGreska(400, "Masa mora biti između 20 i 400 kg.")
        val vm = MjereViewModel()

        vm.spremiBrzo(7, 190)
        advanceUntilIdle()

        assertEquals(
            PorukaMjerenja(UiTekst.Resurs(R.string.mj_brzo_greska_poruka, "Masa mora biti između 20 i 400 kg."), false),
            vm.poruka.value
        )
        assertFalse(vm.sprema.value)
    }

    @Test
    fun httpGreskaBezPorukePokazujeKod() = runTest {
        coEvery { fake.api.brzoMjerenje(any()) } throws httpGreska(500)
        val vm = MjereViewModel()

        vm.spremiBrzo(7, 800)
        advanceUntilIdle()

        assertEquals(PorukaMjerenja(UiTekst.Resurs(R.string.mj_brzo_greska_http, 500), false), vm.poruka.value)
    }

    @Test
    fun greskaVezeImaSvojuPoruku() = runTest {
        coEvery { fake.api.brzoMjerenje(any()) } throws IOException()
        val vm = MjereViewModel()

        vm.spremiBrzo(7, 800)
        advanceUntilIdle()

        assertEquals(PorukaMjerenja(UiTekst.Resurs(R.string.kom_spremanje_greska_veza), false), vm.poruka.value)
        assertFalse(vm.sprema.value)
    }

    @Test
    fun novoSpremanjeBrisePrethodnuPoruku() = runTest {
        val vrata = CompletableDeferred<BrzoMjerenjeOdgovor>()
        var poziva = 0
        coEvery { fake.api.brzoMjerenje(any()) } coAnswers { if (poziva++ == 0) throw IOException() else vrata.await() }
        val vm = MjereViewModel()
        vm.spremiBrzo(7, 800)
        advanceUntilIdle()
        assertFalse(vm.poruka.value!!.uspjeh)

        vm.spremiBrzo(7, 800)
        runCurrent()
        assertNull("greška ne smije ostati dok traje novi pokušaj", vm.poruka.value)
        vrata.complete(BrzoMjerenjeOdgovor(mase.last(), preporuka()))
        advanceUntilIdle()

        assertTrue(vm.poruka.value!!.uspjeh)
    }
}
