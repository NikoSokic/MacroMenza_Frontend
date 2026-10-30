package com.niko.macromenza.viewmodel

import com.niko.macromenza.R
import com.niko.macromenza.model.IzracunPreporuke
import com.niko.macromenza.model.IzracunPreporukeRequest
import com.niko.macromenza.model.Korisnik
import com.niko.macromenza.model.Mjerenje
import com.niko.macromenza.model.ProfilPodaci
import com.niko.macromenza.testutil.FakeApiRule
import com.niko.macromenza.testutil.MainDispatcherRule
import com.niko.macromenza.testutil.mjerenje
import com.niko.macromenza.testutil.preporuka
import com.niko.macromenza.testutil.profil
import com.niko.macromenza.util.UiTekst
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.LocalDate

private val IZRACUN = IzracunPreporuke(1805.0, "10 × 80 + 6,25 × 180 − 5 × 25 + 5", 1.55, 2797.8, 0.0, 2798, 160, 78, 365)

@OptIn(ExperimentalCoroutinesApi::class)
class CiljeviViewModelTest {

    @get:Rule val main = MainDispatcherRule()
    @get:Rule val fake = FakeApiRule()

    private fun osnovno() {
        coEvery { fake.api.dohvatiProfilPoKorisniku(7) } returns profil(id = 5, spol = "Z", visina = 165, dob = 30)
        coEvery { fake.api.dohvatiMjerenjaZaKorisnika(7) } returns listOf(mjerenje(3, 70.0), mjerenje(9, 62.5, "lagana", "mrsavljenje"), mjerenje(4, 71.0))
        coEvery { fake.api.pregledPreporuke(any()) } returns IZRACUN
    }

    @Test
    fun objasnjenjeKoristiProfilIZadnjeMjerenjePoIdu() = runTest {
        osnovno()
        val vm = CiljeviViewModel()

        vm.ucitajObjasnjenje(7)
        advanceUntilIdle()

        assertEquals(CiljeviViewModel.Objasnjenje(IZRACUN, 62.5), vm.objasnjenje.value)
        coVerify {
            fake.api.pregledPreporuke(IzracunPreporukeRequest("Z", 30, 165, 62.5, "lagana", "mrsavljenje"))
        }
    }

    @Test
    fun bezMjerenjaNemaObjasnjenja() = runTest {
        osnovno()
        coEvery { fake.api.dohvatiMjerenjaZaKorisnika(7) } returns emptyList()
        val vm = CiljeviViewModel()

        vm.ucitajObjasnjenje(7)
        advanceUntilIdle()

        assertNull(vm.objasnjenje.value)
        coVerify(exactly = 0) { fake.api.pregledPreporuke(any()) }
    }

    @Test
    fun greskaMakneObjasnjenje() = runTest {
        osnovno()
        val vm = CiljeviViewModel()
        vm.ucitajObjasnjenje(7)
        advanceUntilIdle()
        assertNotNull(vm.objasnjenje.value)

        coEvery { fake.api.pregledPreporuke(any()) } throws IOException()
        vm.ucitajObjasnjenje(7)
        advanceUntilIdle()

        assertNull(vm.objasnjenje.value)
    }

    @Test
    fun spremanjeCiljaMijenjaProfilDodajeMjerenjeIRacunaPreporuku() = runTest {
        osnovno()
        val profilZahtjev = slot<ProfilPodaci>()
        val mjerenjeZahtjev = slot<Mjerenje>()
        coEvery { fake.api.azurirajProfil(5, capture(profilZahtjev)) } returns profil()
        coEvery { fake.api.dodajMjerenje(capture(mjerenjeZahtjev)) } returns mjerenje()
        coEvery { fake.api.izracunajPreporuku(7) } returns preporuka()
        val vm = CiljeviViewModel()

        vm.spremiCilj(7, visina = 166, dob = 31, masa = 61.5, razinaAktivnosti = "umjerena", tipCilja = "odrzavanje")
        advanceUntilIdle()

        // Spol se ne mijenja u ciljevima, ostaje iz profila.
        assertEquals(ProfilPodaci(id = 5, idKorisnik = 7, visina = 166, spol = "Z", dob = 31), profilZahtjev.captured)
        with(mjerenjeZahtjev.captured) {
            assertNull(id)
            assertEquals(7L, idKorisnik)
            assertEquals(61.5, masa, 0.0)
            assertEquals("umjerena", razinaAktivnosti)
            assertEquals("odrzavanje", tipCilja)
            LocalDate.parse(datum) // ISO datum
        }
        coVerifyOrder {
            fake.api.azurirajProfil(5, any())
            fake.api.dodajMjerenje(any())
            fake.api.izracunajPreporuku(7)
        }
        assertEquals(UiTekst.Resurs(R.string.prof_cilj_spremljen), vm.poruka.value)
        assertNotNull("nakon spremanja kartica objašnjenja se osvježi", vm.objasnjenje.value)
    }

    @Test
    fun profilBezIdaSePreskace() = runTest {
        osnovno()
        coEvery { fake.api.dohvatiProfilPoKorisniku(7) } returns profil(id = null)
        coEvery { fake.api.dodajMjerenje(any()) } returns mjerenje()
        coEvery { fake.api.izracunajPreporuku(7) } returns preporuka()
        val vm = CiljeviViewModel()

        vm.spremiCilj(7, 166, 31, 61.5, "umjerena", "odrzavanje")
        advanceUntilIdle()

        coVerify(exactly = 0) { fake.api.azurirajProfil(any(), any()) }
        coVerify(exactly = 1) { fake.api.dodajMjerenje(any()) }
    }

    @Test
    fun greskaSpremanjaPrikazujePorukuIznimke() = runTest {
        osnovno()
        coEvery { fake.api.azurirajProfil(any(), any()) } throws IOException("Timeout")
        val vm = CiljeviViewModel()

        vm.spremiCilj(7, 166, 31, 61.5, "umjerena", "odrzavanje")
        advanceUntilIdle()

        assertEquals(UiTekst.Dinamicki("Timeout"), vm.poruka.value)
        coVerify(exactly = 0) { fake.api.dodajMjerenje(any()) } // pola promjene se ne nastavlja
    }

    @Test
    fun greskaBezPorukeNeDajeNistaZaPrikaz() = runTest {
        osnovno()
        coEvery { fake.api.azurirajProfil(any(), any()) } throws IOException()
        val vm = CiljeviViewModel()

        vm.spremiCilj(7, 166, 31, 61.5, "umjerena", "odrzavanje")
        advanceUntilIdle()

        assertNull(vm.poruka.value)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ProfilViewModelTest {

    @get:Rule val main = MainDispatcherRule()
    @get:Rule val fake = FakeApiRule()

    private val korisnik = Korisnik(7, "uid-1", "Ana", "Anić", "ana@mail.hr")

    private fun osnovno() {
        coEvery { fake.api.dohvatiKorisnika(7) } returns korisnik
        coEvery { fake.api.dohvatiProfilPoKorisniku(7) } returns profil()
        coEvery { fake.api.dohvatiMjerenjaZaKorisnika(7) } returns listOf(mjerenje(2, 80.0), mjerenje(8, 79.0), mjerenje(5, 81.0))
        coEvery { fake.api.dohvatiPreporukeZaKorisnika(7) } returns listOf(preporuka(3, 2000.0), preporuka(12, 2300.0))
    }

    @Test
    fun ucitavaSveIBiraNajnovijeMjerenjeIPreporukuPoIdu() = runTest {
        osnovno()
        val vm = ProfilViewModel()

        vm.ucitajProfil(7)
        advanceUntilIdle()

        assertEquals(korisnik, vm.korisnik.value)
        assertEquals(profil(), vm.profil.value)
        assertEquals(79.0, vm.zadnjeMjerenje.value!!.masa, 0.0)
        assertEquals(2300.0, vm.zadnjaPreporuka.value!!.kalorije, 0.0)
        assertFalse(vm.ucitava.value)
        assertNull(vm.poruka.value)
    }

    @Test
    fun nedostajuciProfilMjerenjeIPreporukaNeSrusavajuEkran() = runTest {
        osnovno()
        coEvery { fake.api.dohvatiProfilPoKorisniku(7) } throws IOException()
        coEvery { fake.api.dohvatiMjerenjaZaKorisnika(7) } throws IOException()
        coEvery { fake.api.dohvatiPreporukeZaKorisnika(7) } throws IOException()
        val vm = ProfilViewModel()

        vm.ucitajProfil(7)
        advanceUntilIdle()

        assertEquals(korisnik, vm.korisnik.value)
        assertNull(vm.profil.value)
        assertNull(vm.zadnjeMjerenje.value)
        assertNull(vm.zadnjaPreporuka.value)
        assertNull(vm.poruka.value)
        assertFalse(vm.ucitava.value)
    }

    @Test
    fun greskaDohvataKorisnikaPostajePoruka() = runTest {
        osnovno()
        coEvery { fake.api.dohvatiKorisnika(7) } throws IOException("Nema veze")
        val vm = ProfilViewModel()

        vm.ucitajProfil(7)
        advanceUntilIdle()

        assertEquals(UiTekst.Dinamicki("Nema veze"), vm.poruka.value)
        assertNull(vm.korisnik.value)
        assertFalse(vm.ucitava.value)
    }

    @Test
    fun spremanjeProfilaSaljeKorisnikaIProfilSNovimSpolom() = runTest {
        osnovno()
        val korisnikZahtjev = slot<Korisnik>()
        val profilZahtjev = slot<ProfilPodaci>()
        coEvery { fake.api.azurirajKorisnika(7, capture(korisnikZahtjev)) } returns korisnik
        coEvery { fake.api.azurirajProfil(5, capture(profilZahtjev)) } returns profil()
        val vm = ProfilViewModel()
        vm.ucitajProfil(7)
        advanceUntilIdle()

        vm.spremiProfil(7, "Ivana", "Ivić", "ivana@mail.hr", "Z")
        advanceUntilIdle()

        assertEquals(Korisnik(7, null, "Ivana", "Ivić", "ivana@mail.hr"), korisnikZahtjev.captured)
        // Visina i dob ostaju iz postojećeg profila; mijenja se samo spol.
        assertEquals(ProfilPodaci(5, 7, 180, "Z", 25), profilZahtjev.captured)
        // Uspjeh se ne vidi u poruci: ponovno učitavanje profila odmah briše poruku, a UrediProfilScreen
        // uspjeh prepoznaje po tom ciklusu (ucitava true → false uz poruku null).
        assertNull(vm.poruka.value)
        assertFalse(vm.ucitava.value)
        coVerify(exactly = 2) { fake.api.dohvatiKorisnika(7) }
    }

    @Test
    fun spremanjeBezUcitanogProfilaMijenjaSamoKorisnika() = runTest {
        coEvery { fake.api.azurirajKorisnika(any(), any()) } returns korisnik
        coEvery { fake.api.dohvatiKorisnika(7) } returns korisnik
        coEvery { fake.api.dohvatiProfilPoKorisniku(7) } throws IOException()
        coEvery { fake.api.dohvatiMjerenjaZaKorisnika(7) } returns emptyList()
        coEvery { fake.api.dohvatiPreporukeZaKorisnika(7) } returns emptyList()
        val vm = ProfilViewModel()

        vm.spremiProfil(7, "Ana", "Anić", "ana@mail.hr", "Z")
        advanceUntilIdle()

        coVerify(exactly = 0) { fake.api.azurirajProfil(any(), any()) }
        coVerify(exactly = 1) { fake.api.azurirajKorisnika(7, any()) }
        assertNull(vm.poruka.value)
    }

    @Test
    fun greskaSpremanjaPostajePoruka() = runTest {
        osnovno()
        coEvery { fake.api.azurirajKorisnika(any(), any()) } throws IOException("Email je zauzet")
        val vm = ProfilViewModel()

        vm.spremiProfil(7, "Ana", "Anić", "ana@mail.hr", "Z")
        advanceUntilIdle()

        assertEquals(UiTekst.Dinamicki("Email je zauzet"), vm.poruka.value)
        coVerify(exactly = 0) { fake.api.azurirajProfil(any(), any()) }
    }
}
