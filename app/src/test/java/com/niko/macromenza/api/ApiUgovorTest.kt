package com.niko.macromenza.api

import com.niko.macromenza.model.BrzoMjerenjeRequest
import com.niko.macromenza.model.IzracunPreporukeRequest
import com.niko.macromenza.model.KategorijaJela
import com.niko.macromenza.model.Korisnik
import com.niko.macromenza.model.Mjerenje
import com.niko.macromenza.model.ObrokRequest
import com.niko.macromenza.model.ProfilPodaci
import com.niko.macromenza.model.StavkaObrokaRequest
import com.niko.macromenza.testutil.NullProvjera
import com.niko.macromenza.testutil.odgovor
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Ugovor s backendom: ApiService (isti Retrofit + Gson kao RetrofitInstance) čita stvarne odgovore backenda
 * snimljene u src/test/resources/odgovori (SnimiOdgovoreIT u backendu) i šalje točno onakve zahtjeve
 * (putanja, upit, nazivi polja u tijelu) kakve backend prima. Gson ne poštuje Kotlin null-safety, pa se
 * dodatno provjerava da nijedno ne-nullable polje nije ostalo null.
 */
class ApiUgovorTest {

    private lateinit var server: MockWebServer
    private lateinit var api: ApiService

    @Before
    fun pokreni() {
        server = MockWebServer().also { it.start() }
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    @After
    fun zaustavi() {
        server.shutdown()
    }

    private fun odgovara(ime: String, kod: Int = 200) {
        server.enqueue(
            MockResponse().setResponseCode(kod).setHeader("Content-Type", "application/json").setBody(odgovor(ime))
        )
    }

    private fun zahtjev(): RecordedRequest = server.takeRequest(5, TimeUnit.SECONDS)!!

    private fun tijelo(r: RecordedRequest) = JSONObject(r.body.readUtf8())

    // ---------- jela i meniji ----------

    @Test
    fun pretragaJela() = runBlocking {
        odgovara("jela_pretraga")

        val jela = api.getJela(search = "grah", minProteini = 20.0)

        val r = zahtjev()
        assertEquals("GET", r.method)
        assertEquals("/api/jela?search=grah&minProteini=20.0", r.path)
        assertTrue(jela.isNotEmpty())
        assertTrue(jela.all { it.kalorije > 0 && it.naziv.isNotBlank() })
        NullProvjera.provjeri(jela, odgovor("jela_pretraga"))
    }

    @Test
    fun kategorije() = runBlocking {
        odgovara("jela_kategorije")

        val k = api.getKategorijeJela()

        assertEquals("/api/jela/kategorije", zahtjev().path)
        assertTrue(k.isNotEmpty())
        assertTrue(k.all { it.brojJela > 0 })
        NullProvjera.provjeri(k, odgovor("jela_kategorije"))
    }

    @Test
    fun detaljJelaSZbrojemSastojaka() = runBlocking {
        odgovara("jelo_detalj")

        val d = api.getJeloDetalj(174)

        assertEquals("/api/jela/174/detalj", zahtjev().path)
        assertTrue(d.sastojci.isNotEmpty())
        assertEquals(d.kalorije, d.sastojci.sumOf { it.kalorije }, 0.05 * d.sastojci.size)
        assertNotNull(d.masaG)
        NullProvjera.provjeri(d, odgovor("jelo_detalj"))
    }

    @Test
    fun cestoJedeno() = runBlocking {
        odgovara("jela_cesto")

        val c = api.getCestoJedeno(7)

        assertEquals("/api/jela/cesto?idKorisnik=7&limit=10", zahtjev().path)
        assertTrue(c.first().brojPuta >= c.last().brojPuta)
        assertEquals("2026-10-29".length, c.first().zadnjiPut.length)
        NullProvjera.provjeri(c, odgovor("jela_cesto"))
    }

    @Test
    fun meniji() = runBlocking {
        odgovara("meniji")

        val m = api.getMeniji(tip = "RUCAK")

        assertEquals("/api/meniji?tip=RUCAK", zahtjev().path)
        assertEquals(3, m.size)
        assertTrue(m.all { it.tip == "RUCAK" && it.stavke.isNotEmpty() })
        // Total menija je zbroj stavki (zaokruženo)
        m.forEach { assertEquals(it.kalorije, it.stavke.sumOf { s -> s.kalorije }, 0.05 * it.stavke.size) }
        NullProvjera.provjeri(m, odgovor("meniji"))
    }

    // ---------- obroci ----------

    @Test
    fun spremanjeObrokaSaljeTocnaImenaPolja() = runBlocking {
        odgovara("obrok_spremljen")

        val o = api.spremiObrok(
            ObrokRequest(7, "2026-10-29", "08:00", "DORUCAK", "MENI", listOf(StavkaObrokaRequest(174, 1.5)))
        )

        val r = zahtjev()
        assertEquals("POST", r.method)
        assertEquals("/api/obroci", r.path)
        assertTrue(r.getHeader("Content-Type")!!.startsWith("application/json"))
        val t = tijelo(r)
        assertEquals(setOf("idKorisnik", "datum", "vrijeme", "tipObroka", "izvor", "stavke"), t.keys().asSequence().toSet())
        assertEquals(7, t.getInt("idKorisnik"))
        assertEquals("08:00", t.getString("vrijeme"))
        assertEquals("MENI", t.getString("izvor"))
        val s = t.getJSONArray("stavke").getJSONObject(0)
        assertEquals(setOf("idJelo", "kolicina"), s.keys().asSequence().toSet())
        assertEquals(1.5, s.getDouble("kolicina"), 0.0)

        assertEquals("DORUCAK", o.tipObroka)
        assertEquals("08:00", o.vrijeme)
        assertEquals(o.stavke.sumOf { it.kalorije }, o.ukupnoKalorije, 0.02)
        NullProvjera.provjeri(o, odgovor("obrok_spremljen"))
    }

    @Test
    fun obrociDanaPregledDanaITjedan() = runBlocking {
        odgovara("obroci_dan")
        odgovara("dan_pregled")
        odgovara("tjedan")
        odgovara("tjedan_prosli")

        val obroci = api.getObroci(7, "2026-10-29")
        val pregled = api.getDanPregled(7, "2026-10-29")
        val tekuci = api.getTjedan(7)
        val prosli = api.getTjedan(7, "2026-10-19")

        assertEquals("/api/obroci?idKorisnik=7&datum=2026-10-29", zahtjev().path)
        assertEquals("/api/obroci/dan-pregled?idKorisnik=7&datum=2026-10-29", zahtjev().path)
        assertEquals("/api/obroci/tjedan?idKorisnik=7", zahtjev().path)
        assertEquals("/api/obroci/tjedan?idKorisnik=7&pocetak=2026-10-19", zahtjev().path)

        assertEquals(2, obroci.size)
        assertEquals(listOf("DORUCAK", "RUCAK", "UZINA", "VECERA"), pregled.poTipu.map { it.tipObroka })
        assertEquals(2798.0, pregled.cilj!!.kalorije, 0.0)
        assertEquals(pregled.pojedeno.kalorije, pregled.poTipu.sumOf { it.kalorije }, 0.0)
        assertTrue(pregled.savjet.naslov.isNotBlank())

        assertEquals(7, tekuci.dani.size)
        assertFalse(tekuci.imaSljedeci)
        assertTrue(prosli.imaSljedeci)
        assertEquals(tekuci.dani.count { it.imaUnos }, tekuci.daniSUnosom)
        assertEquals(0, prosli.daniSUnosom)
        assertTrue(tekuci.dani.first { it.imaUnos }.kalorije > 0)
        NullProvjera.provjeri(obroci, odgovor("obroci_dan"))
        NullProvjera.provjeri(pregled, odgovor("dan_pregled"))
        NullProvjera.provjeri(tekuci, odgovor("tjedan"))
        NullProvjera.provjeri(prosli, odgovor("tjedan_prosli"))
    }

    @Test
    fun povijestDanaGrupiraStavkePoTipuObroka() = runBlocking {
        odgovara("povijest_dana")

        val p = api.dohvatiPovijestZaDan(7, "2026-10-29")

        assertEquals("/api/konzumacije/povijest/7?datum=2026-10-29", zahtjev().path)
        assertEquals(setOf("RUCAK", "DORUCAK"), p.obroci.keys)
        assertTrue(p.obroci.getValue("RUCAK").all { it.tipObroka == "RUCAK" })
        NullProvjera.provjeri(p, odgovor("povijest_dana"))
    }

    @Test
    fun pogreskeBackendaImajuPorukuKojuAplikacijaPrikazuje() = runBlocking {
        odgovara("greska_400_kolicina", 400)
        odgovara("greska_404_korisnik", 404)

        val krivaKolicina = assertThrows(HttpException::class.java) {
            runBlocking { api.spremiObrok(ObrokRequest(7, "2026-10-29", "12:30", "RUCAK", "RUCNO", listOf(StavkaObrokaRequest(1, 0.3)))) }
        }
        val nepostojeci = assertThrows(HttpException::class.java) {
            runBlocking { api.getDanPregled(-1) }
        }

        assertEquals(400, krivaKolicina.code())
        assertEquals("Količina mora biti višekratnik od 0,5.", JSONObject(krivaKolicina.response()!!.errorBody()!!.string()).getString("poruka"))
        assertEquals(404, nepostojeci.code())
        assertEquals("Korisnik ne postoji.", JSONObject(nepostojeci.response()!!.errorBody()!!.string()).getString("poruka"))
    }

    // ---------- korisnik, mjerenja, preporuke ----------

    @Test
    fun korisnikIProfil() = runBlocking {
        odgovara("korisnik")
        odgovara("profil")
        odgovara("onboarding_status")
        odgovara("korisnik")
        odgovara("profil")

        val k = api.dohvatiKorisnika(94)
        val p = api.dohvatiProfilPoKorisniku(94)
        val status = api.dohvatiOnboardingStatus(94)
        api.azurirajKorisnika(94, Korisnik(94, null, "Ana", "Anić", "ana@mail.hr"))
        api.azurirajProfil(3, ProfilPodaci(3, 94, 165, "Z", 30))

        assertEquals("/korisnici/94", zahtjev().path)
        assertEquals("/profil/korisnik/94", zahtjev().path)
        assertEquals("/korisnici/94/onboarding-status", zahtjev().path)
        val putKorisnik = zahtjev()
        assertEquals("PUT /korisnici/94", "${putKorisnik.method} ${putKorisnik.path}")
        assertEquals(setOf("id", "ime", "prezime", "email"), tijelo(putKorisnik).keys().asSequence().toSet())
        val putProfil = zahtjev()
        assertEquals("PUT /profil/3", "${putProfil.method} ${putProfil.path}")
        assertEquals(setOf("id", "idKorisnik", "visina", "spol", "dob"), tijelo(putProfil).keys().asSequence().toSet())

        assertTrue(status.onboardingZavrsen)
        assertEquals("Test", k.ime)
        NullProvjera.provjeri(k, odgovor("korisnik"))
        NullProvjera.provjeri(p, odgovor("profil"))
        NullProvjera.provjeri(status, odgovor("onboarding_status"))
    }

    @Test
    fun mjerenjaSaSnakeCaseNazivimaPolja() = runBlocking {
        odgovara("mjerenja_korisnik")
        // POST /mjerenja vraća jedno spremljeno mjerenje, u istom obliku kao element popisa.
        server.enqueue(
            MockResponse().setHeader("Content-Type", "application/json")
                .setBody(JSONArray(odgovor("mjerenja_korisnik")).getJSONObject(0).toString())
        )

        val m = api.dohvatiMjerenjaZaKorisnika(94)
        api.dodajMjerenje(Mjerenje(null, 94, 78.5, "visoka", "dobivanje_mase", "2026-10-29"))

        assertEquals("/mjerenja/korisnik/94", zahtjev().path)
        val post = zahtjev()
        assertEquals("/mjerenja", post.path)
        assertEquals(
            setOf("idKorisnik", "masa", "razina_aktivnosti", "tip_cilja", "datum"),
            tijelo(post).keys().asSequence().toSet()
        )
        assertEquals("umjerena", m.first().razinaAktivnosti)
        assertEquals("odrzavanje", m.first().tipCilja)
        NullProvjera.provjeri(m, odgovor("mjerenja_korisnik"))
    }

    @Test
    fun brzoMjerenjeIGrafMase() = runBlocking {
        odgovara("brzo_mjerenje")
        odgovara("mase")

        val b = api.brzoMjerenje(BrzoMjerenjeRequest(94, 79.9))
        val mase = api.getMase(94)

        val r = zahtjev()
        assertEquals("/api/mjerenja/brzo", r.path)
        assertEquals(setOf("idKorisnik", "masa"), tijelo(r).keys().asSequence().toSet())
        assertEquals("/api/mjerenja?idKorisnik=94", zahtjev().path)
        assertEquals(79.9, b.mjerenje.masa, 0.0)
        assertEquals(2796.0, b.preporuka.kalorije, 0.0)
        assertEquals(1, mase.size)
        NullProvjera.provjeri(b, odgovor("brzo_mjerenje"))
        NullProvjera.provjeri(mase, odgovor("mase"))
    }

    @Test
    fun preporukeIPregledIzracuna() = runBlocking {
        odgovara("preporuke_korisnik")
        odgovara("izracun_pregled")
        odgovara("izvori_podataka")

        val preporuke = api.dohvatiPreporukeZaKorisnika(94)
        val izracun = api.pregledPreporuke(IzracunPreporukeRequest("M", 25, 180, 80.0, "umjerena", "odrzavanje"))
        val izvori = api.getIzvoriPodataka()

        assertEquals("/preporuke/korisnik/94", zahtjev().path)
        val post = zahtjev()
        assertEquals("/api/preporuke/pregled", post.path)
        assertEquals(
            setOf("spol", "dob", "visina", "masa", "razinaAktivnosti", "tipCilja"),
            tijelo(post).keys().asSequence().toSet()
        )
        assertEquals("/api/izvori-podataka", zahtjev().path)

        assertEquals(2798.0, preporuke.single().kalorije, 0.0)
        assertEquals(2798, izracun.ciljKalorije)
        assertEquals("10 × 80 + 6,25 × 180 − 5 × 25 + 5", izracun.bmrFormula)
        assertEquals("NN 120/2013", izvori.izvorNormativa)
        assertTrue(izvori.brojJela >= 311)
        NullProvjera.provjeri(preporuke, odgovor("preporuke_korisnik"))
        NullProvjera.provjeri(izracun, odgovor("izracun_pregled"))
        NullProvjera.provjeri(izvori, odgovor("izvori_podataka"))
    }

    // ---------- prepoznavanje ----------

    @Test
    fun prepoznavanjeSaljeMultipartSTrimaDijelovimaIVracaPrijedlog() = runBlocking {
        odgovara("prijedlog_obroka")
        val slika = MultipartBody.Part.createFormData("slika", "slika.jpg", byteArrayOf(1, 2, 3).toRequestBody("image/jpeg".toMediaType()))

        val p = api.prepoznaj(
            slika = slika,
            opis = "grah".toRequestBody("text/plain; charset=utf-8".toMediaType()),
            idKorisnik = "94".toRequestBody("text/plain".toMediaType())
        )

        val r = zahtjev()
        assertEquals("/api/prepoznavanje", r.path)
        assertTrue(r.getHeader("Content-Type")!!.startsWith("multipart/form-data"))
        val tekst = r.body.readUtf8()
        listOf("name=\"slika\"; filename=\"slika.jpg\"", "name=\"opis\"", "name=\"idKorisnik\"").forEach {
            assertTrue("nema dijela $it", tekst.contains(it))
        }

        assertEquals(2, p.stavke.size)
        assertEquals(listOf("Nepoznata pita"), p.neprepoznato)
        assertEquals("VISOKA", p.stavke[0].sigurnost)
        assertNotNull(p.model)
        NullProvjera.provjeri(p, odgovor("prijedlog_obroka"))
    }

    @Test
    fun bezSlikeSeDioSlikaIzostavlja() = runBlocking {
        odgovara("prijedlog_obroka")

        api.prepoznaj(null, "grah".toRequestBody("text/plain".toMediaType()), "94".toRequestBody("text/plain".toMediaType()))

        val tekst = zahtjev().body.readUtf8()
        assertFalse(tekst.contains("name=\"slika\""))
        assertTrue(tekst.contains("name=\"opis\""))
    }

    // ---------- brisanje ----------

    @Test
    fun brisanjaKoristeDeleteSaSvimParametrima() = runBlocking {
        repeat(3) { server.enqueue(MockResponse().setResponseCode(204)) }
        server.enqueue(MockResponse().setResponseCode(200).setHeader("Content-Type", "application/json").setBody("""{"obrok":2}"""))

        assertTrue(api.obrisiObrok(100, 7).isSuccessful)
        assertTrue(api.ukloniPrimjerTjedna(7).isSuccessful)
        assertTrue(api.obrisiSvePodatke(7, "OBRISI").isSuccessful)
        assertTrue(api.ucitajPrimjerTjedna(7).isSuccessful)

        assertEquals("DELETE /api/obroci/100?idKorisnik=7", zahtjev().let { "${it.method} ${it.path}" })
        assertEquals("DELETE /api/obroci/primjer-tjedna?idKorisnik=7", zahtjev().let { "${it.method} ${it.path}" })
        assertEquals("DELETE /api/korisnici/7/podaci?potvrda=OBRISI", zahtjev().let { "${it.method} ${it.path}" })
        assertEquals("POST /api/obroci/primjer-tjedna?idKorisnik=7", zahtjev().let { "${it.method} ${it.path}" })
    }

    // ---------- provjera same provjere ----------

    @Test
    fun provjeraNullaHvataPoljeKojeFaliUOdgovoru() {
        val gson = com.google.gson.Gson()
        val bezNaziva = gson.fromJson("""{"brojJela": 3}""", KategorijaJela::class.java)

        val e = assertThrows(AssertionError::class.java) { NullProvjera.provjeri(bezNaziva) }

        assertTrue(e.message, e.message!!.contains("naziv"))
    }

    @Test
    fun provjeraNullaDopustaNullablePolja() {
        val gson = com.google.gson.Gson()
        val jelo = gson.fromJson(
            """{"id":1,"naziv":"x","kalorije":1,"proteini":1,"ugljikohidrati":1,"masti":1}""",
            com.niko.macromenza.model.Jelo::class.java
        )

        NullProvjera.provjeri(jelo) // opis, sifra, kategorija, cijenaEur i napomena smiju biti null
        assertNull(jelo.opis)
    }
}

class NullProvjeraPrimitivaTest {

    @Test
    fun preimenovanoPrimitivnoPoljeSeUocavaIako_GsonGaTihoPostaviNaNulu() {
        val gson = com.google.gson.Gson()
        val json = """{"naziv":"Juhe","brojJelaa":12}""" // krivi naziv polja
        val k = gson.fromJson(json, KategorijaJela::class.java)

        assertEquals("Gson ne javlja grešku", 0, k.brojJela)
        val e = assertThrows(AssertionError::class.java) { NullProvjera.provjeri(k, json) }
        assertTrue(e.message, e.message!!.contains("brojJela"))
    }

    @Test
    fun ispravanOdgovorProlazi() {
        val json = """{"naziv":"Juhe","brojJela":12}"""
        NullProvjera.provjeri(com.google.gson.Gson().fromJson(json, KategorijaJela::class.java), json)
    }
}
