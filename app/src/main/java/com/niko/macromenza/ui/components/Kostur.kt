package com.niko.macromenza.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalInspectionMode
import com.niko.macromenza.ui.theme.MacroOblici
import com.niko.macromenza.ui.theme.Trajanje
import com.niko.macromenza.ui.theme.macroBoje
import com.niko.macromenza.ui.theme.rememberSmanjeniPokret

/**
 * Jedan element kostura učitavanja (traka ili točka): `macroBoje.kostur`, pulsira alfom 0,55 ↔ 1
 * (`Trajanje.disanje`). Veličinu daje [modifier]. Uz smanjeni pokret i u previewima ne pulsira.
 */
@Composable
fun KosturOblik(modifier: Modifier = Modifier, oblik: Shape = MacroOblici.traka) {
    val bezPokreta = rememberSmanjeniPokret() || LocalInspectionMode.current
    val puls = if (bezPokreta) null else rememberInfiniteTransition(label = "kostur").animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(Trajanje.disanje / 2), RepeatMode.Reverse),
        label = "puls"
    )
    Box(
        modifier
            .graphicsLayer { alpha = puls?.value ?: 1f }
            .background(MaterialTheme.macroBoje.kostur, oblik)
    )
}
