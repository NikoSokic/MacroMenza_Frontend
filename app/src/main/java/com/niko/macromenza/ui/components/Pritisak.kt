package com.niko.macromenza.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import com.niko.macromenza.ui.theme.Opruge
import com.niko.macromenza.ui.theme.SKALA_PRITISKA

/**
 * Blago smanjenje pri pritisku (Opruge.brza). Skala se čita u graphicsLayer lambdi, pa pritisak
 * ne rekomponira sadržaj. Isti [interactionSource] mora dobiti i clickable/Button.
 */
fun Modifier.skalaPritiska(interactionSource: InteractionSource): Modifier = composed {
    val pritisnuto by interactionSource.collectIsPressedAsState()
    val skala by animateFloatAsState(
        targetValue = if (pritisnuto) SKALA_PRITISKA else 1f,
        animationSpec = Opruge.brza(),
        label = "skalaPritiska"
    )
    graphicsLayer {
        scaleX = skala
        scaleY = skala
    }
}
