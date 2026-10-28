package com.niko.macromenza.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode

/*
 * Jedini izvor pokreta. Karakter: pladanj se spušta na stol, teško i mekano, bez odskoka.
 * Odskok (razigrana) smije se koristiti samo za proslavu dosegnutog cilja.
 */

object Opruge {
    /** Prijelazi vrijednosti: luk, brojevi, trake, rubovi tanjura. */
    fun <T> blaga(): SpringSpec<T> = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 200f)

    /** Tanjur sjeda na pladanj, red se vraća nakon povlačenja: malo težak, bez odskoka. */
    fun <T> slijeganje(): SpringSpec<T> = spring(dampingRatio = 0.9f, stiffness = 260f)

    /** Pritisak: brz i bez odskoka. */
    fun <T> brza(): SpringSpec<T> = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 1400f)

    /** Samo proslava cilja. */
    fun <T> razigrana(): SpringSpec<T> = spring(dampingRatio = 0.5f, stiffness = 300f)
}

object Trajanje {
    /** Luk i brojevi pri prvom učitavanju; završavaju zajedno. */
    const val ulaz = 650

    /** Glif porcije ("nalijevanje juhe"), izblijeđivanje. */
    const val kratko = 200

    /** Trake i redovi pri ulazu. */
    const val srednje = 320

    /** Disanje praznih utora (jedan smjer). */
    const val disanje = 1600

    /** Iskre proslave. */
    const val proslava = 900

    /** Odlazni ekran u prijelazu "fade-through": brži od dolaznog (~60 % njegova trajanja). */
    const val izlaz = 140

    /**
     * Ponovni ulaz u ekran u istoj sesiji: jedno kratko izblijeđivanje, bez stupnjevanja (≤ 300 ms).
     * Puni slijed (fade-through + stupnjevani redovi, < 900 ms) igra samo pri prvom ulazu, vidi [UlazUSesiji].
     */
    const val ponovniUlaz = 250

    /** Globalni fade-through između ekrana: odlazni 90 ms, dolazni 160 ms nakon njega (ukupno 250 ms). */
    const val prijelazIzlaz = 90
    const val prijelazUlaz = 160

    /** Forma nakon uspješnog spremanja: snackbar je vidljiv ovoliko prije povratka na prethodni ekran. */
    const val porukaPrijePovratka = 1200
}

object Kasnjenje {
    /** Odmak između elemenata u nizu. */
    const val korak = 50
    const val tanjuri = 40
    const val redovi = 40

    /** Trake počinju kad je luk na oko 60 %. */
    const val trake = 400

    /** Redovi obroka nakon traka. */
    const val redoviPocetak = 450

    /** Dolazni ekran kreće tek kad odlazni nestane (fade-through). */
    const val nakonPrijelaza = Trajanje.izlaz

    /** Redovi unosa kreću malo nakon pretrage i prvog naslova. */
    const val unosRedovi = 100
}

object Ublazavanje {
    /** Usporavanje pri dolasku (ease-out-cubic). */
    val usporavanje: Easing = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)

    /** Ubrzavanje pri odlasku. */
    val ubrzavanje: Easing = CubicBezierEasing(0.32f, 0f, 0.67f, 0f)

    /** Nalijevanje: brz početak, mekan kraj. */
    val nalijevanje: Easing = CubicBezierEasing(0.3f, 0.7f, 0.4f, 1f)
}

/** Skala pritiska za sve dodirne elemente. */
const val SKALA_PRITISKA = 0.96f

/** Pomak pri ulazu reda (u dp, primjenjuje se kroz graphicsLayer). */
const val POMAK_ULAZA_DP = 12f

/** Pomak sadržaja tjedna pri listanju (u dp, kroz graphicsLayer). */
const val POMAK_TJEDNA_DP = 48f

/** Visina s koje tanjur "pada" na pladanj (u dp) i početna skala. */
const val POMAK_TANJURA_DP = 18f
const val SKALA_TANJURA = 1.07f

/** Najviše toliko redova liste animira ulaz (ostali su odmah tu). */
const val MAKS_ANIMIRANIH_REDOVA = 8

/** Početna skala dolaznog ekrana u prijelazu fade-through. */
const val SKALA_EKRANA = 0.96f

/** Početna skala kontrole za dodavanje koja se pretvara u glif porcije. */
const val SKALA_GUMBA = 0.8f

/** Tween za ulaz s usporavanjem; kod smanjenog pokreta postaje skok na kraj. */
fun <T> ulazniTween(trajanje: Int, kasnjenje: Int = 0, smanjeno: Boolean): FiniteAnimationSpec<T> =
    if (smanjeno) snap() else tween(durationMillis = trajanje, delayMillis = kasnjenje, easing = Ublazavanje.usporavanje)

/** Opruga ili skok, ovisno o smanjenom pokretu. */
fun <T> opruga(spec: SpringSpec<T>, smanjeno: Boolean): FiniteAnimationSpec<T> =
    if (smanjeno) snap() else spec

fun <T> nalijevanjeTween(smanjeno: Boolean): FiniteAnimationSpec<T> =
    if (smanjeno) snap() else TweenSpec(durationMillis = Trajanje.kratko, easing = Ublazavanje.nalijevanje)

/**
 * True kad je u postavkama za razvojne programere "Skala trajanja animatora" 0 (ili je uključeno
 * uklanjanje animacija u pristupačnosti, što postavlja istu vrijednost). Tada se sve postavlja na
 * završne vrijednosti i preskaču se ukrasni pokreti.
 */
@Composable
fun rememberSmanjeniPokret(): Boolean {
    val context = LocalContext.current
    val pregled = LocalInspectionMode.current
    return remember(context) {
        if (pregled) false
        else Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/**
 * Pamti koji su ekrani u ovoj sesiji aplikacije (dok proces živi) već imali puni ulazni slijed.
 * Prvi ulaz igra puni koreografirani slijed, svaki sljedeći samo [Trajanje.ponovniUlaz].
 */
object UlazUSesiji {
    const val KARTICA_UNOS = "kartica_unos"
    const val UNOS_OBROKA = "unos_obroka"
    const val SLIKA_ILI_OPIS = "slika_ili_opis"
    const val POVIJEST = "povijest"
    const val PROFIL = "profil"
    const val MOJE_MJERE = "moje_mjere"

    private val vidjeni = mutableSetOf<String>()

    fun jePrvi(kljuc: String): Boolean = kljuc !in vidjeni

    fun oznaci(kljuc: String) {
        vidjeni += kljuc
    }
}
