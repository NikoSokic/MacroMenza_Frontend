package com.niko.macromenza.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.niko.macromenza.R
import com.niko.macromenza.model.PrijedlogObroka
import com.niko.macromenza.model.PrijedlogStavka
import com.niko.macromenza.testutil.FakeApiRule
import com.niko.macromenza.testutil.MainDispatcherRule
import com.niko.macromenza.testutil.httpGreska
import com.niko.macromenza.util.UiTekst
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.slot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import org.robolectric.Shadows.shadowOf
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.SocketTimeoutException

@RunWith(RobolectricTestRunner::class)
// NATIVE: pravo dekodiranje slike (u zadanom načinu "nije slika" bi se dekodiralo kao prazna bitmapa).
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalCoroutinesApi::class)
class SlikaIliOpisViewModelTest {

    @get:Rule val main = MainDispatcherRule()
    @get:Rule val fake = FakeApiRule()

    private val app: Application = ApplicationProvider.getApplicationContext()

    private val prijedlog = PrijedlogObroka(
        stavke = listOf(PrijedlogStavka(1, "Grah varivo", 1.5, "VISOKA", 480.0, 27.0, 60.0, 13.5)),
        neprepoznato = listOf("Nepoznata pita"),
        napomena = "Okvirna procjena.",
        model = "test-model"
    )

    private fun vm() = SlikaIliOpisViewModel(app)

    /** Slika se priprema na Dispatchers.IO (prava dretva), pa se uz virtualno vrijeme čeka i stvarno. */
    private fun TestScope.cekaDok(uvjet: () -> Boolean) {
        val kraj = System.nanoTime() + 15_000_000_000L
        while (!uvjet()) {
            check(System.nanoTime() < kraj) { "Istek čekanja" }
            Thread.sleep(5)
            advanceUntilIdle()
        }
    }

    private fun registrirajSliku(bajtovi: ByteArray, adresa: String = "content://test/slika/1"): Uri {
        val uri = Uri.parse(adresa)
        shadowOf(app.contentResolver).registerInputStreamSupplier(uri) { ByteArrayInputStream(bajtovi) }
        return uri
    }

    private fun png(sirina: Int = 40, visina: Int = 30): ByteArray {
        val izlaz = ByteArrayOutputStream()
        Bitmap.createBitmap(sirina, visina, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG, 100, izlaz)
        return izlaz.toByteArray()
    }

    private fun RequestBody?.tekst(): String {
        val b = Buffer()
        this!!.writeTo(b)
        return b.readUtf8()
    }

    // ---------- opis ----------

    @Test
    fun opisSeSkracujeNaTisucuZnakova() {
        val vm = vm()

        vm.postaviOpis("a".repeat(1500))
        assertEquals(NAJVISE_ZNAKOVA_OPISA, vm.opis.value.length)
        assertEquals(1000, NAJVISE_ZNAKOVA_OPISA)

        vm.postaviOpis("kratki opis")
        assertEquals("kratki opis", vm.opis.value)
    }

    @Test
    fun bezSlikeIBezOpisaNistaSeNeSalje() = runTest {
        val vm = vm()
        vm.postaviOpis("   \n ")

        vm.prepoznaj(7)
        advanceUntilIdle()

        coVerify(exactly = 0) { fake.api.prepoznaj(any(), any(), any()) }
        assertFalse(vm.ucitava.value)
        assertNull(vm.rezultat.value)
    }

    @Test
    fun samoOpisSaljeObrezanTekstIKorisnikaBezSlike() = runTest {
        val opis = slot<RequestBody>()
        val korisnik = slot<RequestBody>()
        coEvery { fake.api.prepoznaj(null, capture(opis), capture(korisnik)) } returns prijedlog
        val vm = vm()
        vm.postaviOpis("  grah i juha \n")

        vm.prepoznaj(7)
        advanceUntilIdle()

        assertEquals("grah i juha", opis.captured.tekst())
        assertEquals("7", korisnik.captured.tekst())
        assertEquals(prijedlog, vm.rezultat.value)
        assertEquals("OPIS", vm.izvor.value)
        assertNull(vm.greska.value)
        assertFalse(vm.ucitava.value)
    }

    @Test
    fun dokTraziPrepoznavanjeUcitavaIBrisePrethodniRezultat() = runTest {
        val vrata = CompletableDeferred<PrijedlogObroka>()
        var poziva = 0
        coEvery { fake.api.prepoznaj(any(), any(), any()) } coAnswers { if (poziva++ == 0) prijedlog else vrata.await() }
        val vm = vm()
        vm.postaviOpis("grah")
        vm.prepoznaj(7)
        advanceUntilIdle()
        assertEquals(prijedlog, vm.rezultat.value)

        vm.prepoznaj(7)
        runCurrent()

        assertTrue(vm.ucitava.value)
        assertNull("stari rezultat ne smije ostati dok traje novi zahtjev", vm.rezultat.value)

        vm.prepoznaj(7) // treći dodir dok traje: ignorira se
        runCurrent()
        vrata.complete(prijedlog)
        advanceUntilIdle()

        coVerify(exactly = 2) { fake.api.prepoznaj(any(), any(), any()) }
    }

    // ---------- slika ----------

    @Test
    fun slikaSeSmanjujeSaljeKaoJpegIPrivremenaDatotekaSeBrise() = runTest {
        val dio = slot<MultipartBody.Part>()
        coEvery { fake.api.prepoznaj(capture(dio), any(), any()) } returns prijedlog
        val vm = vm()
        vm.postaviSliku(registrirajSliku(png()))

        vm.prepoznaj(7)
        cekaDok { !vm.ucitava.value && vm.rezultat.value != null }

        assertEquals("SLIKA", vm.izvor.value)
        assertEquals("image/jpeg", dio.captured.body.contentType().toString())
        val zaglavlje = dio.captured.headers!!["Content-Disposition"]!!
        assertTrue(zaglavlje, zaglavlje.contains("name=\"slika\"") && zaglavlje.contains("filename=\"slika.jpg\""))
        val ostalo = File(app.cacheDir, "prepoznavanje").listFiles().orEmpty()
        assertTrue("privremena slika mora biti obrisana: ${ostalo.toList()}", ostalo.isEmpty())
    }

    @Test
    fun slikaIOpisSeSaljuZajednoAIzvorJeSlika() = runTest {
        val opis = slot<RequestBody>()
        coEvery { fake.api.prepoznaj(any(), capture(opis), any()) } returns prijedlog
        val vm = vm()
        vm.postaviSliku(registrirajSliku(png()))
        vm.postaviOpis("pola porcije")

        vm.prepoznaj(7)
        cekaDok { !vm.ucitava.value && vm.rezultat.value != null }

        assertEquals("pola porcije", opis.captured.tekst())
        assertEquals("SLIKA", vm.izvor.value)
    }

    @Test
    fun datotekaKojaNijeSlikaDajePorukuIBezZahtjeva() = runTest {
        val vm = vm()
        vm.postaviSliku(registrirajSliku("ovo nije slika".toByteArray()))

        vm.prepoznaj(7)
        cekaDok { !vm.ucitava.value && vm.greska.value != null }

        assertEquals(UiTekst.Dinamicki("Odabrana datoteka nije slika."), vm.greska.value)
        coVerify(exactly = 0) { fake.api.prepoznaj(any(), any(), any()) }
        assertNull(vm.rezultat.value)
    }

    @Test
    fun slikaBezPravaCitanjaDajeSvojuPoruku() = runTest {
        val uri = Uri.parse("content://tudja/slika/9")
        shadowOf(app.contentResolver).registerInputStreamSupplier(uri) { throw SecurityException("nema prava") }
        val vm = vm()
        vm.postaviSliku(uri)

        vm.prepoznaj(7)
        cekaDok { !vm.ucitava.value && vm.greska.value != null }

        assertEquals(UiTekst.Resurs(R.string.sio_greska_slika_pristup), vm.greska.value)
        coVerify(exactly = 0) { fake.api.prepoznaj(any(), any(), any()) }
    }

    @Test
    fun uklanjanjeSlikeIPostavljanjeNoveBrisuGresku() = runTest {
        val vm = vm()
        vm.postaviSliku(registrirajSliku("x".toByteArray()))
        vm.prepoznaj(7)
        cekaDok { !vm.ucitava.value && vm.greska.value != null }

        vm.postaviSliku(registrirajSliku(png(), "content://test/slika/2"))
        assertNull(vm.greska.value)
        assertNotNull(vm.slika.value)

        vm.ukloniSliku()
        assertNull(vm.slika.value)
    }

    // ---------- greške zahtjeva ----------

    @Test
    fun greskeZahtjeva() = runTest {
        val vm = vm()
        vm.postaviOpis("grah")

        coEvery { fake.api.prepoznaj(any(), any(), any()) } throws httpGreska(503, "Prepoznavanje nije podešeno")
        vm.prepoznaj(7)
        advanceUntilIdle()
        assertEquals(UiTekst.Dinamicki("Prepoznavanje nije podešeno"), vm.greska.value)

        coEvery { fake.api.prepoznaj(any(), any(), any()) } throws httpGreska(502)
        vm.prepoznaj(7)
        advanceUntilIdle()
        assertEquals(UiTekst.Resurs(R.string.sio_greska_http, 502), vm.greska.value)

        coEvery { fake.api.prepoznaj(any(), any(), any()) } throws SocketTimeoutException()
        vm.prepoznaj(7)
        advanceUntilIdle()
        assertEquals(UiTekst.Resurs(R.string.sio_greska_timeout), vm.greska.value)

        coEvery { fake.api.prepoznaj(any(), any(), any()) } throws IOException()
        vm.prepoznaj(7)
        advanceUntilIdle()
        assertEquals(UiTekst.Resurs(R.string.sio_greska_veza), vm.greska.value)

        coEvery { fake.api.prepoznaj(any(), any(), any()) } throws IllegalStateException("neočekivano")
        vm.prepoznaj(7)
        advanceUntilIdle()
        assertEquals(UiTekst.Resurs(R.string.sio_greska_opca), vm.greska.value)

        assertFalse(vm.ucitava.value)
        assertNull(vm.rezultat.value)
    }

    @Test
    fun uspjehNakonGreskeBriseGresku() = runTest {
        coEvery { fake.api.prepoznaj(any(), any(), any()) } throws IOException() andThen prijedlog
        val vm = vm()
        vm.postaviOpis("grah")
        vm.prepoznaj(7)
        advanceUntilIdle()
        assertNotNull(vm.greska.value)

        vm.prepoznaj(7)
        advanceUntilIdle()

        assertNull(vm.greska.value)
        assertEquals(prijedlog, vm.rezultat.value)
    }

    // ---------- kamera ----------
    // pripremiKameru() koristi FileProvider, a on se u Robolectricu ne uspijeva povezati s privremenim cacheDir-om;
    // taj dio provjerava se ručno na uređaju (lista ručnih provjera).

    @Test
    fun uspjesnaFotografijaPostajeSlika() {
        val vm = vm()
        val uri = Uri.parse("content://com.niko.macromenza.fileprovider/kamera/foto.jpg")

        vm.kameraGotova(true, uri)

        assertEquals(uri, vm.slika.value)
        assertNull(vm.greska.value)
    }

    @Test
    fun otkazanaFotografijaNeMijenjaOdabranuSliku() {
        val vm = vm()
        val odabrana = Uri.parse("content://test/slika/1")
        vm.postaviSliku(odabrana)

        vm.kameraGotova(false, null)
        vm.kameraGotova(true, null)

        assertEquals(odabrana, vm.slika.value)
    }
}
