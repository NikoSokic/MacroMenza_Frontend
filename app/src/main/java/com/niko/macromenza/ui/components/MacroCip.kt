package com.niko.macromenza.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalInspectionMode
import com.niko.macromenza.ui.theme.MacroDimenzije
import com.niko.macromenza.ui.theme.MacroOblici
import com.niko.macromenza.ui.theme.Trajanje
import com.niko.macromenza.ui.theme.Ublazavanje
import com.niko.macromenza.ui.theme.rememberSmanjeniPokret

/**
 * Čip filtera: odabir animira boju spremnika, teksta i ruba (Trajanje.kratko) i uvodi kvačicu koja se
 * povećava od nule. Čipovi s vlastitom ikonom ([ikona]) nemaju kvačicu, samo promjenu boje.
 * Kvačica proširuje čip pa se susjedi pomaknu odmah (bez animiranja širine); sama kvačica skalira graphicsLayerom.
 */
@Composable
fun MacroCip(
    oznaka: String,
    odabran: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    ikona: (@Composable () -> Unit)? = null
) {
    val shema = MaterialTheme.colorScheme
    val smanjeno = rememberSmanjeniPokret()
    val prijelaz = if (smanjeno) snap<Color>() else tween(Trajanje.kratko, easing = Ublazavanje.usporavanje)

    val spremnik by animateColorAsState(if (odabran) shema.secondaryContainer else shema.surface, prijelaz, label = "cipSpremnik")
    val tekst by animateColorAsState(if (odabran) shema.onSecondaryContainer else shema.onSurface, prijelaz, label = "cipTekst")
    val rub by animateColorAsState(if (odabran) shema.secondaryContainer else shema.outlineVariant, prijelaz, label = "cipRub")

    FilterChip(
        selected = odabran,
        onClick = onClick,
        modifier = modifier,
        label = { Text(oznaka, style = MaterialTheme.typography.labelLarge, maxLines = 1) },
        leadingIcon = ikona ?: if (odabran) {
            { Kvacica(tekst) }
        } else null,
        shape = MacroOblici.cip,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = spremnik,
            labelColor = tekst,
            iconColor = tekst,
            selectedContainerColor = spremnik,
            selectedLabelColor = tekst,
            selectedLeadingIconColor = tekst
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = odabran,
            borderColor = rub,
            selectedBorderColor = rub,
            borderWidth = MacroDimenzije.linija
        )
        // FilterChip sam proširuje dodirni cilj na 48 dp (minimumInteractiveComponentSize).
    )
}

/** Kvačica odabranog čipa: raste od nule pri prvom prikazu (Trajanje.kratko); previewi i smanjeni pokret je odmah pokazuju. */
@Composable
private fun Kvacica(boja: Color) {
    val bezPokreta = rememberSmanjeniPokret() || LocalInspectionMode.current
    val skala = remember { Animatable(if (bezPokreta) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (skala.value < 1f) skala.animateTo(1f, tween(Trajanje.kratko, easing = Ublazavanje.usporavanje))
    }
    Icon(
        imageVector = Icons.Rounded.Check,
        contentDescription = null,
        tint = boja,
        modifier = Modifier
            .size(MacroDimenzije.ikonaSitna)
            .graphicsLayer {
                scaleX = skala.value
                scaleY = skala.value
            }
    )
}
