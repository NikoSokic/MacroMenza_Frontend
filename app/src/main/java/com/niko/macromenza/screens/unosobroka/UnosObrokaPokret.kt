package com.niko.macromenza.screens.unosobroka

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.niko.macromenza.ui.theme.Kasnjenje
import com.niko.macromenza.ui.theme.MAKS_ANIMIRANIH_REDOVA
import com.niko.macromenza.ui.theme.Opruge
import com.niko.macromenza.ui.theme.POMAK_ULAZA_DP
import com.niko.macromenza.ui.theme.Trajanje
import com.niko.macromenza.ui.theme.Ublazavanje
import kotlinx.coroutines.delay

/**
 * Pokret ekrana unosa. [aktivan] je istina dok traje ulaz (prvi prikaz liste): redovi koji se u tom
 * razdoblju prvi put pojave izblijeđuju stupnjevano, a svi kasniji (pretraga, filteri, listanje) samo
 * kroz animateItem. [smanjeno] znači sve odmah na završnim vrijednostima.
 */
@Immutable
data class PokretUnosa(
    val aktivan: Boolean,
    val smanjeno: Boolean,
    /** SystemClock.uptimeMillis() pri prvom prikazu ekrana: raspored ulaza računa se od tog trenutka. */
    val pocetak: Long = 0L
) {
    val ulazAktivan: Boolean get() = aktivan && !smanjeno

    companion object {
        val Bez = PokretUnosa(aktivan = false, smanjeno = true)
    }
}

/**
 * Ulaz elementa pri prvom prikazu: izblijeđivanje, a za [pomak] i uzlazni pomak od 12 dp, stupnjevano po [indeks].
 * Elementi s indeksom od [MAKS_ANIMIRANIH_REDOVA] naviše su odmah tu. Odluka se donosi pri prvom prikazu
 * elementa, pa se red koji se vrati u prikaz listanjem ne animira ponovno. Samo graphicsLayer, bez promjene layouta.
 */
fun Modifier.ulazUnosa(
    indeks: Int,
    pokret: PokretUnosa,
    pomak: Boolean = true
): Modifier = if (indeks >= MAKS_ANIMIRANIH_REDOVA) this else composed {
    val napredak = remember { Animatable(if (pokret.ulazAktivan) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (napredak.value < 1f) {
            // Raspored se računa od prvog prikaza ekrana, pa redovi koji stignu kasnije ne čekaju dvaput.
            val cilj = Kasnjenje.nakonPrijelaza + Kasnjenje.unosRedovi + indeks * Kasnjenje.redovi
            delay((cilj - (SystemClock.uptimeMillis() - pokret.pocetak)).coerceAtLeast(0L))
            napredak.animateTo(1f, tween(Trajanje.srednje, easing = Ublazavanje.usporavanje))
        }
    }
    graphicsLayer {
        alpha = napredak.value
        if (pomak) translationY = (1f - napredak.value) * POMAK_ULAZA_DP.dp.toPx()
    }
}

/**
 * Pretraga i traka filtera izblijeđuju prije redova (bez pomaka): kreću čim prijelaz ekrana završi.
 * Ovisi samo o prvom prikazu ekrana, a ne o dolasku podataka.
 */
fun Modifier.ulazZaglavlja(indeks: Int, smanjeno: Boolean): Modifier = composed {
    val napredak = remember { Animatable(if (smanjeno) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (napredak.value < 1f) {
            delay((Kasnjenje.nakonPrijelaza + indeks * Kasnjenje.korak).toLong())
            napredak.animateTo(1f, tween(Trajanje.srednje, easing = Ublazavanje.usporavanje))
        }
    }
    graphicsLayer { alpha = napredak.value }
}

/**
 * Redovi liste: novi redovi izblijeđuju (rezultati pretrage se preklapaju s odlaznima), preostali klize
 * na mjesto uz Opruge.slijeganje. Dok traje ulaz, fade-in preuzima [ulazUnosa] pa se ne udvostručuje.
 * [naslov] = ljepljivi naslov: samo izblijeđivanje, bez animiranja pomaka, da se pri prianjanju ne trzne.
 */
@OptIn(ExperimentalFoundationApi::class)
fun LazyItemScope.pomakReda(pokret: PokretUnosa, naslov: Boolean = false): Modifier =
    if (pokret.smanjeno) Modifier else Modifier.animateItem(
        fadeInSpec = if (pokret.aktivan) null else tween(Trajanje.srednje, easing = Ublazavanje.usporavanje),
        placementSpec = if (naslov) null else Opruge.slijeganje<IntOffset>(),
        fadeOutSpec = tween(Trajanje.kratko, easing = Ublazavanje.ubrzavanje)
    )
