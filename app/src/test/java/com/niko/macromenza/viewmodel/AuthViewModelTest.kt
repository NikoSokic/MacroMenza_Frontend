package com.niko.macromenza.viewmodel

import com.niko.macromenza.R
import com.niko.macromenza.api.SupabaseAuthApi
import com.niko.macromenza.api.SupabaseAuthInstance
import com.niko.macromenza.model.Korisnik
import com.niko.macromenza.model.OnboardingStatusResponse
import com.niko.macromenza.model.SupabaseAuthRequest
import com.niko.macromenza.model.SupabaseAuthResponse
import com.niko.macromenza.model.SupabaseUser
import com.niko.macromenza.session.UserSessionManager
import com.niko.macromenza.testutil.FakeApiRule
import com.niko.macromenza.testutil.MainDispatcherRule
import com.niko.macromenza.testutil.httpGreska
import com.niko.macromenza.util.UiTekst
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** Prijava i registracija; Supabase i backend su lažni (nikad se ne zove pravi Supabase). Robolectric zbog Patterns. */
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @get:Rule val main = MainDispatcherRule()
    @get:Rule val fake = FakeApiRule()

    private val supabase: SupabaseAuthApi = mockk()
    private val sesija: UserSessionManager = mockk(relaxed = true)
    private val sesijaUid = MutableStateFlow<String?>(null)
    private val sesijaId = MutableStateFlow<Long?>(null)

    private val odgovor = SupabaseAuthResponse("tok", "bearer", 3600, "ref", SupabaseUser("uid-1", "ana@mail.hr"))

    @Before
    fun postavi() {
        mockkObject(SupabaseAuthInstance)
        every { SupabaseAuthInstance.api } returns supabase
        every { sesija.supabaseUid } returns sesijaUid
        every { sesija.korisnikId } returns sesijaId
    }

    @After
    fun pocisti() {
        unmockkObject(SupabaseAuthInstance)
    }

    private fun vm() = AuthViewModel(sesija)

    // ---------- sesija ----------

    @Test
    fun spremljenaSesijaSeProcitaOdmah() = runTest {
        sesijaUid.value = "uid-9"
        sesijaId.value = 42
        val vm = vm()

        advanceUntilIdle()

        assertEquals("uid-9", vm.supabaseUid.value)
        assertEquals(42L, vm.korisnikId.value)
    }

    @Test
    fun odjavaBriseSesijuIPokazujePoruku() = runTest {
        sesijaUid.value = "uid-9"
        sesijaId.value = 42
        val vm = vm()
        advanceUntilIdle()

        vm.odjava()
        advanceUntilIdle()

        coVerify(exactly = 1) { sesija.obrisiSesiju() }
        assertNull(vm.supabaseUid.value)
        assertNull(vm.korisnikId.value)
        assertNull(vm.onboardingZavrsen.value)
        assertEquals(UiTekst.Resurs(R.string.auth_odjavljen), vm.poruka.value)
        vm.ocistiPoruku()
        assertNull(vm.poruka.value)
    }

    // ---------- registracija ----------

    @Test
    fun registracijaProvjeravaUnosPrijeMreze() = runTest {
        val vm = vm()

        vm.registracija("", "lozinka1")
        advanceUntilIdle()
        assertEquals(UiTekst.Resurs(R.string.auth_unesi_email), vm.poruka.value)

        vm.registracija("nije-email", "lozinka1")
        advanceUntilIdle()
        assertEquals(UiTekst.Resurs(R.string.auth_reg_email_neispravan), vm.poruka.value)

        vm.registracija("ana@mail.hr", "   ")
        advanceUntilIdle()
        assertEquals(UiTekst.Resurs(R.string.auth_unesi_lozinku), vm.poruka.value)

        vm.registracija("ana@mail.hr", "12345")
        advanceUntilIdle()
        assertEquals(UiTekst.Resurs(R.string.auth_reg_lozinka_kratka), vm.poruka.value)

        assertFalse(vm.ucitavanje.value)
        coVerify(exactly = 0) { supabase.registracija(any(), any(), any()) }
    }

    @Test
    fun uspjesnaRegistracijaSaljeObrezaneUnoseIJavljaDaJeEmailPoslan() = runTest {
        coEvery { supabase.registracija(any(), any(), any()) } returns odgovor
        val vm = vm()

        vm.registracija("  ana@mail.hr ", " lozinka1 ")
        advanceUntilIdle()

        coVerify { supabase.registracija(SupabaseAuthInstance.SUPABASE_ANON_KEY, "application/json", SupabaseAuthRequest("ana@mail.hr", "lozinka1")) }
        assertEquals(UiTekst.Resurs(R.string.auth_reg_email_poslan), vm.poruka.value)
        assertFalse(vm.ucitavanje.value)
    }

    @Test
    fun greskeRegistracije() = runTest {
        val vm = vm()
        val ocekivano = mapOf(
            400 to UiTekst.Resurs(R.string.auth_reg_greska_400),
            401 to UiTekst.Resurs(R.string.auth_reg_greska_401),
            409 to UiTekst.Resurs(R.string.auth_reg_greska_409),
            422 to UiTekst.Resurs(R.string.auth_reg_greska_422),
            429 to UiTekst.Resurs(R.string.auth_reg_greska_429),
            500 to UiTekst.Resurs(R.string.auth_greska_http, 500)
        )
        ocekivano.forEach { (kod, poruka) ->
            coEvery { supabase.registracija(any(), any(), any()) } throws httpGreska(kod)
            vm.registracija("ana@mail.hr", "lozinka1")
            advanceUntilIdle()
            assertEquals("HTTP $kod", poruka, vm.poruka.value)
        }

        coEvery { supabase.registracija(any(), any(), any()) } throws UnknownHostException()
        vm.registracija("ana@mail.hr", "lozinka1")
        advanceUntilIdle()
        assertEquals(UiTekst.Resurs(R.string.auth_reg_nema_interneta), vm.poruka.value)

        coEvery { supabase.registracija(any(), any(), any()) } throws SocketTimeoutException()
        vm.registracija("ana@mail.hr", "lozinka1")
        advanceUntilIdle()
        assertEquals(UiTekst.Resurs(R.string.auth_reg_timeout), vm.poruka.value)

        coEvery { supabase.registracija(any(), any(), any()) } throws IllegalStateException()
        vm.registracija("ana@mail.hr", "lozinka1")
        advanceUntilIdle()
        assertEquals(UiTekst.Resurs(R.string.auth_reg_greska), vm.poruka.value)
        assertFalse(vm.ucitavanje.value)
    }

    // ---------- prijava ----------

    @Test
    fun prijavaProvjeravaUnosPrijeMreze() = runTest {
        val vm = vm()

        vm.prijava(" ", "x")
        advanceUntilIdle()
        assertEquals(UiTekst.Resurs(R.string.auth_unesi_email), vm.poruka.value)

        vm.prijava("nije-email", "x")
        advanceUntilIdle()
        assertEquals(UiTekst.Resurs(R.string.auth_prij_email_neispravan), vm.poruka.value)

        vm.prijava("ana@mail.hr", "")
        advanceUntilIdle()
        assertEquals(UiTekst.Resurs(R.string.auth_unesi_lozinku), vm.poruka.value)

        coVerify(exactly = 0) { supabase.prijava(any(), any(), any(), any()) }
        assertFalse(vm.ucitavanje.value)
    }

    private fun pripremiUspjesnuPrijavu(onboardingZavrsen: Boolean) {
        coEvery { supabase.prijava(any(), any(), any(), any()) } returns odgovor
        coEvery { fake.api.pronadiIliDodajKorisnikaPrekoSupabase(any()) } returns
            Korisnik(7, "uid-1", "Ana", "Anić", "ana@mail.hr")
        coEvery { fake.api.dohvatiOnboardingStatus(7) } returns OnboardingStatusResponse(onboardingZavrsen)
    }

    @Test
    fun prijavaKorisnikaSZavrsenimOnboardingomSpremaSesiju() = runTest {
        pripremiUspjesnuPrijavu(onboardingZavrsen = true)
        val vm = vm()

        vm.prijava("ana@mail.hr", "lozinka1")
        advanceUntilIdle()

        coVerify { supabase.prijava(SupabaseAuthInstance.SUPABASE_ANON_KEY, any(), "password", SupabaseAuthRequest("ana@mail.hr", "lozinka1")) }
        coVerify { fake.api.pronadiIliDodajKorisnikaPrekoSupabase(Korisnik(null, "uid-1", "", "", "ana@mail.hr")) }
        coVerify(exactly = 1) { sesija.spremiSesiju(7, "uid-1") }
        assertEquals("uid-1", vm.supabaseUid.value)
        assertEquals(7L, vm.korisnikId.value)
        assertEquals(true, vm.onboardingZavrsen.value)
        assertFalse(vm.ucitavanje.value)
    }

    @Test
    fun prijavaBezZavrsenogOnboardingaNeSpremaSesijuAliJavljaStatus() = runTest {
        pripremiUspjesnuPrijavu(onboardingZavrsen = false)
        val vm = vm()

        vm.prijava("ana@mail.hr", "lozinka1")
        advanceUntilIdle()

        coVerify(exactly = 0) { sesija.spremiSesiju(any(), any()) }
        assertEquals(false, vm.onboardingZavrsen.value)
        assertEquals(7L, vm.korisnikId.value)
    }

    @Test
    fun odgovorBezKorisnikaJeNeuspjelaPrijava() = runTest {
        coEvery { supabase.prijava(any(), any(), any(), any()) } returns odgovor.copy(user = null)
        val vm = vm()

        vm.prijava("ana@mail.hr", "lozinka1")
        advanceUntilIdle()

        assertEquals(UiTekst.Resurs(R.string.auth_prij_neuspjela), vm.poruka.value)
        assertFalse(vm.ucitavanje.value)
        coVerify(exactly = 0) { fake.api.pronadiIliDodajKorisnikaPrekoSupabase(any()) }
    }

    @Test
    fun greskePrijave() = runTest {
        val vm = vm()
        val ocekivano = mapOf(
            400 to UiTekst.Resurs(R.string.auth_prij_greska_400),
            401 to UiTekst.Resurs(R.string.auth_prij_pogresni_podaci),
            403 to UiTekst.Resurs(R.string.auth_prij_greska_403),
            422 to UiTekst.Resurs(R.string.auth_prij_greska_422),
            429 to UiTekst.Resurs(R.string.auth_prij_greska_429),
            503 to UiTekst.Resurs(R.string.auth_greska_http, 503)
        )
        ocekivano.forEach { (kod, poruka) ->
            coEvery { supabase.prijava(any(), any(), any(), any()) } throws httpGreska(kod)
            vm.prijava("ana@mail.hr", "lozinka1")
            advanceUntilIdle()
            assertEquals("HTTP $kod", poruka, vm.poruka.value)
        }

        coEvery { supabase.prijava(any(), any(), any(), any()) } throws UnknownHostException()
        vm.prijava("ana@mail.hr", "lozinka1")
        advanceUntilIdle()
        assertEquals(UiTekst.Resurs(R.string.auth_prij_nema_interneta), vm.poruka.value)

        coEvery { supabase.prijava(any(), any(), any(), any()) } throws SocketTimeoutException()
        vm.prijava("ana@mail.hr", "lozinka1")
        advanceUntilIdle()
        assertEquals(UiTekst.Resurs(R.string.auth_prij_timeout), vm.poruka.value)
        assertFalse(vm.ucitavanje.value)
    }

    @Test
    fun ostaleGreskePrijavePrepoznajuSeIzPoruke() = runTest {
        val vm = vm()
        val slucajevi = listOf(
            "Email not confirmed" to UiTekst.Resurs(R.string.auth_prij_nije_potvrden),
            "Invalid login credentials" to UiTekst.Resurs(R.string.auth_prij_pogresni_podaci),
            "nešto treće" to UiTekst.Resurs(R.string.auth_prij_neuspjela)
        )
        slucajevi.forEach { (poruka, ocekivano) ->
            coEvery { supabase.prijava(any(), any(), any(), any()) } throws IllegalStateException(poruka)
            vm.prijava("ana@mail.hr", "lozinka1")
            advanceUntilIdle()
            assertEquals(poruka, ocekivano, vm.poruka.value)
        }

        coEvery { supabase.prijava(any(), any(), any(), any()) } throws IllegalStateException()
        vm.prijava("ana@mail.hr", "lozinka1")
        advanceUntilIdle()
        assertEquals(UiTekst.Resurs(R.string.auth_prij_neuspjela), vm.poruka.value)
    }

    @Test
    fun greskaBackendaPoslijeUspjesnePrijaveNeSpremaSesiju() = runTest {
        coEvery { supabase.prijava(any(), any(), any(), any()) } returns odgovor
        coEvery { fake.api.pronadiIliDodajKorisnikaPrekoSupabase(any()) } throws java.io.IOException("backend nedostupan")
        val vm = vm()

        vm.prijava("ana@mail.hr", "lozinka1")
        advanceUntilIdle()

        coVerify(exactly = 0) { sesija.spremiSesiju(any(), any()) }
        assertEquals(UiTekst.Resurs(R.string.auth_prij_neuspjela), vm.poruka.value)
        assertFalse(vm.ucitavanje.value)
    }
}
