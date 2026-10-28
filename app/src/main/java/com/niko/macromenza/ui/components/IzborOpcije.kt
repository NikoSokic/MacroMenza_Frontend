package com.niko.macromenza.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import com.niko.macromenza.ui.theme.MacroDimenzije
import com.niko.macromenza.ui.theme.MacroRazmaci
import com.niko.macromenza.ui.theme.Trajanje
import com.niko.macromenza.ui.theme.rememberSmanjeniPokret

/** Jedna opcija izbora; [opis] je kratak prigušen tekst ispod naslova (npr. „×1,375 · 1–3 treninga tjedno"). */
@Immutable
data class Opcija<T>(val vrijednost: T, val naslov: String, val opis: String? = null)

/**
 * Izbor jedne opcije (pages/profil.md § Zajednički elementi): redovi s radio oznakom, odvojeni dividerom,
 * bez kartica i bez obojene podloge. Odabir je stanje, pa je oznaka u onSurface, nikad paprika.
 */
@Composable
fun <T> IzborOpcije(
    opcije: List<Opcija<T>>,
    odabrana: T,
    onOdabir: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().selectableGroup()) {
        opcije.forEachIndexed { indeks, opcija ->
            if (indeks > 0) {
                HorizontalDivider(thickness = MacroDimenzije.linija, color = MaterialTheme.colorScheme.outlineVariant)
            }
            RedIzbora(
                naslov = opcija.naslov,
                opis = opcija.opis,
                odabran = opcija.vrijednost == odabrana,
                onClick = { onOdabir(opcija.vrijednost) }
            )
        }
    }
}

/** Jedan red izbora: dodir na cijelom redu (≥ 48 dp), semantika radio gumba. */
@Composable
fun RedIzbora(
    naslov: String,
    odabran: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    opis: String? = null
) {
    val shema = MaterialTheme.colorScheme
    val izvor = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MacroDimenzije.dodirniCilj)
            .skalaPritiska(izvor)
            .selectable(
                selected = odabran,
                interactionSource = izvor,
                indication = null,
                role = Role.RadioButton,
                onClick = onClick
            )
            .padding(vertical = MacroRazmaci.m),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioOznaka(odabran)
        Spacer(Modifier.width(MacroRazmaci.l))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = naslov,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (odabran) FontWeight.SemiBold else FontWeight.Normal,
                color = shema.onSurface
            )
            opis?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall, color = shema.onSurfaceVariant)
            }
        }
    }
}

/** Prsten i točka; točka raste od nule, a boja prelazi u Trajanje.kratko (točka samo kroz graphicsLayer). */
@Composable
private fun RadioOznaka(odabran: Boolean) {
    val shema = MaterialTheme.colorScheme
    val smanjeno = rememberSmanjeniPokret()
    val boja by animateColorAsState(
        targetValue = if (odabran) shema.onSurface else shema.onSurfaceVariant,
        animationSpec = if (smanjeno) snap() else tween(Trajanje.kratko),
        label = "bojaRadija"
    )
    val skala by animateFloatAsState(
        targetValue = if (odabran) 1f else 0f,
        animationSpec = if (smanjeno) snap() else tween(Trajanje.kratko),
        label = "tockaRadija"
    )
    Box(modifier = Modifier.size(MacroDimenzije.radioOznaka), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(MacroDimenzije.radioOznaka)) {
            val obrub = MacroDimenzije.radioObrub.toPx()
            drawCircle(color = boja, radius = (size.minDimension - obrub) / 2, style = Stroke(width = obrub))
        }
        Canvas(
            Modifier
                .size(MacroDimenzije.radioTocka)
                .graphicsLayer {
                    scaleX = skala
                    scaleY = skala
                }
        ) {
            drawCircle(color = boja)
        }
    }
}
