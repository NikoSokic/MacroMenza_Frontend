package com.niko.macromenza.screens.unosobroka

import androidx.compose.animation.core.animate
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Velocity
import com.niko.macromenza.ui.theme.Opruge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Traka filtera koja se skuplja pri listanju prema dolje i vraća pri listanju prema gore (quick return).
 * Pretraga ostaje prikvačena; ljepljivi naslovi kategorija prianjaju uz vrh liste, koja se pritom povećava.
 * Kad se prst digne, traka se priljepi na skupljeno ili prošireno stanje uz Opruge.slijeganje
 * (kod smanjenog pokreta odmah). [mozeListatiDalje] sprječava skupljanje kad se lista ionako ne može pomaknuti.
 */
@Stable
class SkupljanjeTrake(
    private val scope: CoroutineScope,
    private val smanjeno: Boolean,
    private val mozeListatiDalje: () -> Boolean
) {
    /** 0 = potpuno vidljiva, -[visina] = potpuno skupljena. Čita se u fazi layouta, pa pomicanje ne rekomponira. */
    var odmak by mutableFloatStateOf(0f)
        private set

    /** Izmjerena visina trake u pikselima; postavlja je layout modifikator. */
    var visina = 0f
        internal set

    val skupljena: Boolean get() = visina > 0f && odmak <= -visina + 1f

    private var animacija: Job? = null

    val veza = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (visina <= 0f || available.y == 0f) return Offset.Zero
            // Skupljanje samo dok se lista stvarno može dalje listati; vraćanje uvijek.
            if (available.y < 0f && !mozeListatiDalje()) return Offset.Zero
            animacija?.cancel()
            val novi = (odmak + available.y).coerceIn(-visina, 0f)
            val potroseno = novi - odmak
            odmak = novi
            return Offset(0f, potroseno)
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            priljepi()
            return Velocity.Zero
        }
    }

    /** Priljepljuje traku na bliže krajnje stanje. */
    private fun priljepi() {
        if (visina <= 0f) return
        pomakniNa(if (odmak < -visina / 2f) -visina else 0f)
    }

    /** Vraća traku (npr. nakon nove pretrage ili filtera). */
    fun prikazi() = pomakniNa(0f)

    private fun pomakniNa(cilj: Float) {
        if (odmak == cilj) return
        animacija?.cancel()
        animacija = scope.launch {
            if (smanjeno) {
                odmak = cilj
            } else {
                animate(initialValue = odmak, targetValue = cilj, animationSpec = Opruge.slijeganje()) { v, _ ->
                    odmak = v.coerceIn(-visina, 0f)
                }
            }
        }
    }
}

/**
 * Visina trake prati [SkupljanjeTrake.odmak]: sadržaj se pomiče prema gore i blijedi, a prostor koji
 * ostavlja preuzima lista ispod. Čita se samo u fazi layouta (bez rekompozicije po kadru).
 */
fun Modifier.skupljajuca(trake: SkupljanjeTrake): Modifier = this
    .layout { izmjerivo, ogranicenja ->
        val sadrzaj = izmjerivo.measure(ogranicenja)
        trake.visina = sadrzaj.height.toFloat()
        val odmak = if (sadrzaj.height > 0) trake.odmak.coerceIn(-sadrzaj.height.toFloat(), 0f) else 0f
        val visina = (sadrzaj.height + odmak).roundToInt()
        val vidljivost = if (sadrzaj.height > 0) (1f + odmak / sadrzaj.height).coerceIn(0f, 1f) else 1f
        layout(sadrzaj.width, visina) {
            sadrzaj.placeRelativeWithLayer(0, odmak.roundToInt()) { alpha = vidljivost }
        }
    }
    .clipToBounds()
