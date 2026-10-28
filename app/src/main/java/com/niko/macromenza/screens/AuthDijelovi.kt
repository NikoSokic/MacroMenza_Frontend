package com.niko.macromenza.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.niko.macromenza.ui.components.skalaPritiska
import com.niko.macromenza.ui.theme.Kasnjenje
import com.niko.macromenza.ui.theme.MacroDimenzije
import com.niko.macromenza.ui.theme.MacroRazmaci
import com.niko.macromenza.ui.theme.POMAK_ULAZA_DP
import com.niko.macromenza.ui.theme.Trajanje
import com.niko.macromenza.ui.theme.Ublazavanje
import com.niko.macromenza.ui.theme.UlazUSesiji
import com.niko.macromenza.ui.theme.rememberSmanjeniPokret
import kotlinx.coroutines.delay

/**
 * Prvi prikaz u sesiji: izblijeđivanje + pomak nakon prijelaza ekrana, stupnjevano po [indeks]
 * (samo graphicsLayer). Kad [aktivno] nije, element je odmah u završnom stanju.
 */
internal fun Modifier.ulazAuth(indeks: Int, aktivno: Boolean): Modifier = composed {
    val napredak = remember { Animatable(if (aktivno) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (napredak.value < 1f) {
            delay((Kasnjenje.nakonPrijelaza + indeks * Kasnjenje.korak).toLong())
            napredak.animateTo(1f, tween(Trajanje.srednje, easing = Ublazavanje.usporavanje))
        }
    }
    graphicsLayer {
        alpha = napredak.value
        translationY = (1f - napredak.value) * POMAK_ULAZA_DP.dp.toPx()
    }
}

/**
 * Tihi red ispod gumba: greška (ErrorOutline, error) ili uspjeh (Check, onSurface), bez podloge.
 * Boja nikad ne nosi značenje sama. Mijenja se izblijeđivanjem.
 */
@Composable
internal fun TihaPoruka(tekst: String?, uspjeh: Boolean, modifier: Modifier = Modifier) {
    val shema = MaterialTheme.colorScheme
    val smanjeno = rememberSmanjeniPokret()
    Crossfade(
        targetState = tekst,
        animationSpec = if (smanjeno) snap() else tween(Trajanje.kratko),
        modifier = modifier,
        label = "porukaAuth"
    ) { t ->
        if (t != null) {
            val boja = if (uspjeh) shema.onSurface else shema.error
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MacroRazmaci.m)
                    .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = if (uspjeh) Icons.Rounded.Check else Icons.Rounded.ErrorOutline,
                    contentDescription = null,
                    tint = boja,
                    modifier = Modifier.size(MacroDimenzije.ikonaMala)
                )
                Spacer(Modifier.width(MacroRazmaci.s))
                Text(text = t, style = MaterialTheme.typography.bodyMedium, color = boja)
            }
        }
    }
}

/** „Nemaš račun? Registriraj se": pitanje prigušeno, radnja u primary i podcrtana; jedan cilj ≥ 48 dp. */
@Composable
internal fun LinkAuth(pitanje: String, radnja: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shema = MaterialTheme.colorScheme
    val izvor = remember { MutableInteractionSource() }
    TextButton(
        onClick = onClick,
        interactionSource = izvor,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MacroDimenzije.dodirniCilj)
            .skalaPritiska(izvor)
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = shema.onSurfaceVariant)) { append(pitanje) }
                append(" ")
                withStyle(SpanStyle(color = shema.primary, textDecoration = TextDecoration.Underline)) { append(radnja) }
            },
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center
        )
    }
}

/** True kad ekran u ovoj sesiji još nije imao puni ulazni slijed (i pokret nije isključen). */
@Composable
internal fun rememberPrviUlaz(kljuc: String): Boolean {
    val smanjeno = rememberSmanjeniPokret()
    val prvi = remember { UlazUSesiji.jePrvi(kljuc) && !smanjeno }
    LaunchedEffect(Unit) { UlazUSesiji.oznaci(kljuc) }
    return prvi
}
