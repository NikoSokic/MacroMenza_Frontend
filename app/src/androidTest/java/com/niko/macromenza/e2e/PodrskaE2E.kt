package com.niko.macromenza.e2e

import android.content.Context
import androidx.annotation.StringRes
import android.util.Log
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.printToLog
import androidx.compose.ui.test.onRoot
import androidx.test.platform.app.InstrumentationRegistry
import com.niko.macromenza.BuildConfig
import com.niko.macromenza.MainActivity
import com.niko.macromenza.api.RetrofitInstance
import com.niko.macromenza.model.Korisnik
import com.niko.macromenza.model.RegistracijaProfilRequest
import com.niko.macromenza.session.UserSessionManager
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Request
import org.json.JSONObject
import org.junit.rules.ExternalResource
import java.util.UUID

/** Dulje od ovoga se na emulatoru + lokalnom backendu + Supabase bazi ne čeka na jedan ekran. */
const val ROK_MS = 20_000L

val kontekst: Context get() = InstrumentationRegistry.getInstrumentation().targetContext

fun tekst(@StringRes id: Int, vararg argumenti: Any): String = kontekst.getString(id, *argumenti)

typealias E2ERule = AndroidComposeTestRule<androidx.test.ext.junit.rules.ActivityScenarioRule<MainActivity>, MainActivity>

/**
 * Testni korisnik na pravom (lokalnom) backendu, jedan po testu. Račun se stvara istim endpointom kao registracija
 * u aplikaciji, a sesija se upisuje izravno u DataStore prije pokretanja aktivnosti, pa se Supabase prijava ne dodiruje.
 * Nakon testa briše se sve što je korisnik napravio, zatim profil i sam račun; ako išta ostane, test pada.
 */
class TestniKorisnik : ExternalResource() {

    lateinit var korisnik: Korisnik
        private set
    val id: Long get() = korisnik.id!!

    // Primjer tjedna i brisanje na udaljenoj bazi traju dulje od zadanih 10 s.
    private val klijent = OkHttpClient.Builder().callTimeout(120, java.util.concurrent.TimeUnit.SECONDS).readTimeout(120, java.util.concurrent.TimeUnit.SECONDS).build()
    private val baza = BuildConfig.BASE_URL.trimEnd('/')

    override fun before() = runBlocking {
        val uid = UUID.randomUUID().toString()
        korisnik = RetrofitInstance.api.registracijaProfil(
            RegistracijaProfilRequest(
                supabaseUid = uid, email = "e2e-$uid@macromenza.test", ime = "Ana", prezime = "Testić",
                spol = "Z", visina = 165, dob = 30, masa = 60.0,
                razinaAktivnosti = "umjerena", tipCilja = "odrzavanje"
            )
        )
        UserSessionManager(kontekst).spremiSesiju(id, uid)
    }

    override fun after() = runBlocking {
        UserSessionManager(kontekst).obrisiSesiju()
        // Aplikacija je mogla ostaviti zahtjev koji backend još obrađuje (npr. primjer tjedna), pa se čisti dok račun ne nestane.
        repeat(5) { pokusaj ->
            poziv("DELETE", "/api/korisnici/$id/podaci?potvrda=OBRISI")
            val profil = poziv("GET", "/profil/korisnik/$id")
            if (profil.isNotBlank()) poziv("DELETE", "/profil/${JSONObject(profil).getLong("id")}")
            poziv("DELETE", "/korisnici/$id")
            if (poziv("GET", "/korisnici/$id").isBlank()) return@runBlocking
            Thread.sleep(1500L * (pokusaj + 1))
        }
        error("Testni korisnik $id nije obrisan")
    }

    /** Odgovor backenda za jedan obrok dana, kao što ga vidi aplikacija. */
    fun obrociDana(datum: String): List<JSONObject> {
        val tijelo = org.json.JSONArray(poziv("GET", "/api/obroci?idKorisnik=$id&datum=$datum"))
        return (0 until tijelo.length()).map { tijelo.getJSONObject(it) }
    }

    /** Prvo jelo čiji naziv sadrži [trazi]: (id, kalorije po porciji, naziv). */
    fun jelo(trazi: String): Triple<Long, Double, String> {
        val o = org.json.JSONArray(poziv("GET", "/api/jela?search=$trazi")).getJSONObject(0)
        return Triple(o.getLong("id"), o.getDouble("kalorije"), o.getString("naziv"))
    }

    /** Sprema obrok izravno preko API-ja (brže od klikanja); vraća id obroka. */
    fun spremiObrok(datum: String, vrijeme: String, tip: String, stavke: List<Pair<Long, Double>>, izvor: String = "RUCNO"): Long {
        val niz = org.json.JSONArray()
        stavke.forEach { niz.put(JSONObject().put("idJelo", it.first).put("kolicina", it.second)) }
        val tijelo = JSONObject().put("idKorisnik", id).put("datum", datum).put("vrijeme", vrijeme)
            .put("tipObroka", tip).put("izvor", izvor).put("stavke", niz).toString()
        return JSONObject(poziv("POST", "/api/obroci", json = tijelo)).getLong("id")
    }

    fun poziv(metoda: String, put: String, json: String? = null): String {
        val tijelo = when {
            json != null -> json.toRequestBody("application/json".toMediaType())
            metoda == "GET" -> null
            else -> ByteArray(0).toRequestBody(null)
        }
        val zahtjev = Request.Builder().url(baza + put).method(metoda, tijelo).build()
        klijent.newCall(zahtjev).execute().use { return it.body?.string().orEmpty() }
    }
}

// ---------- pomoćne funkcije za Compose ----------

fun E2ERule.cekaTekst(t: String, podniz: Boolean = false) =
    ceka("tekst $t") { onAllNodesWithText(t, substring = podniz, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }

fun E2ERule.cekaOpis(t: String, podniz: Boolean = false) =
    ceka("opis $t") { onAllNodesWithContentDescription(t, substring = podniz, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }

fun E2ERule.cekaNestanak(t: String) =
    ceka("nestanak $t") { onAllNodesWithText(t, useUnmergedTree = true).fetchSemanticsNodes().isEmpty() }

fun E2ERule.tekstNode(t: String, podniz: Boolean = false): SemanticsNodeInteraction =
    onAllNodesWithText(t, substring = podniz, useUnmergedTree = true).onFirst()

fun E2ERule.opisNode(t: String, podniz: Boolean = false): SemanticsNodeInteraction =
    onAllNodesWithContentDescription(t, substring = podniz, useUnmergedTree = true).onFirst()

fun E2ERule.kliknuTekst(t: String, podniz: Boolean = false) {
    cekaTekst(t, podniz)
    tekstNode(t, podniz).performClick()
}

fun E2ERule.kliknuOpis(t: String, podniz: Boolean = false) {
    cekaOpis(t, podniz)
    opisNode(t, podniz).performClick()
}

fun E2ERule.imaTeksta(t: String, podniz: Boolean = false) =
    onAllNodesWithText(t, substring = podniz, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

/** Za dijagnostiku: cijelo stablo semantike u logcat (oznaka E2E). */
fun E2ERule.ispisiStablo() = onRoot(useUnmergedTree = true).printToLog("E2E")

/** Sažeti popis vidljivih tekstova i opisa ([K] = može se kliknuti) u logcat pod oznakom E2E. */
fun E2ERule.oznake(naslov: String) {
    val izlaz = mutableListOf<String>()
    fun hodaj(n: SemanticsNode) {
        val c = n.config
        val t = c.getOrNull(SemanticsProperties.Text)?.joinToString("|") { it.text }
        val d = c.getOrNull(SemanticsProperties.ContentDescription)?.joinToString("|")
        val klik = c.contains(SemanticsActions.OnClick)
        if ((t != null && t.length > 1) || d != null) {
            izlaz += (if (klik) "[K] " else "    ") + (t ?: "") + (d?.let { " {$it}" } ?: "")
        }
        n.children.forEach(::hodaj)
    }
    onAllNodes(androidx.compose.ui.test.isRoot(), useUnmergedTree = true).fetchSemanticsNodes().forEach(::hodaj)
    Log.i("E2E", "=== $naslov\n" + izlaz.joinToString("\n"))
}

/** Klik na element čiji je tekst ILI opis jednak zadanom (ikone imaju samo opis). */
fun E2ERule.kliknuBilo(t: String) {
    ceka("klik $t") { imaTeksta(t) || onAllNodesWithContentDescription(t, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
    if (imaTeksta(t)) tekstNode(t).performClick() else opisNode(t).performClick()
}

/** Čeka uvjet; pri isteku ispisuje što se vidi na ekranu (oznaka E2E), pa je jasno gdje je test stao. */
fun E2ERule.ceka(opis: String, uvjet: () -> Boolean) {
    try {
        waitUntil(ROK_MS, uvjet)
    } catch (e: androidx.compose.ui.test.ComposeTimeoutException) {
        oznake("ISTEKLO: $opis")
        throw e
    }
}

/**
 * Plutajući gumbi ("Slika ili opis", "Dodaj obrok") i donja traka leže preko dna liste, pa klik u sredinu elementa
 * pri dnu ekrana pogodi njih. Zato se element skrola u vidno polje i klikne mu se gornji rub.
 */
fun E2ERule.kliknuOpisGore(t: String, podniz: Boolean = false) {
    skrolajDo(androidx.compose.ui.test.hasContentDescription(t, substring = podniz))
    cekaOpis(t, podniz)
    val n = opisNode(t, podniz)
    n.performScrollTo()
    n.performTouchInput { click(androidx.compose.ui.geometry.Offset(centerX, height * 0.12f)) }
}

fun E2ERule.kliknuTekstGore(t: String) {
    skrolajDo(androidx.compose.ui.test.hasText(t))
    cekaTekst(t)
    val n = tekstNode(t)
    n.performScrollTo()
    n.performTouchInput { click(androidx.compose.ui.geometry.Offset(centerX, height * 0.12f)) }
}

/** Elementi izvan vidnog polja lijene liste nisu komponirani, pa se lista skrola do njih (ako se uopće skrola). */
fun E2ERule.skrolajDo(trazeno: androidx.compose.ui.test.SemanticsMatcher) {
    val skrolabilni = onAllNodes(okomitaLista)
    if (skrolabilni.fetchSemanticsNodes().isEmpty()) return
    runCatching { skrolabilni.onFirst().performScrollToNode(trazeno) }
}
