package com.niko.macromenza.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Jezik pladnja i tanjura: mekani pravokutnici za pladanj i kartice, krugovi za tanjure (CircleShape). */
object MacroOblici {
    val pladanj = RoundedCornerShape(24.dp)
    val utor = RoundedCornerShape(16.dp)
    val kartica = RoundedCornerShape(18.dp)
    val gumb = RoundedCornerShape(18.dp)
    val cip = RoundedCornerShape(12.dp)
    val traka = RoundedCornerShape(3.dp)
}

val MacroShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = MacroOblici.cip,
    medium = MacroOblici.utor,
    large = MacroOblici.kartica,
    extraLarge = MacroOblici.pladanj
)
