package com.niko.macromenza.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.niko.macromenza.ui.theme.MacroDimenzije
import com.niko.macromenza.ui.theme.nalijevanjeTween

/** Širina glifa: tanjur + polumjesec iza njega za porcije iznad 1. */
private const val OMJER_SIRINE = 1.4f

/** Polumjesec za 0,5 viška je prozirniji od punog drugog tanjura. */
private const val ALFA_POLA = 0.45f
private const val ALFA_PUNO = 0.9f

/**
 * Glif porcije (iz smjera C): 0,5 = pola ispunjen krug, 1 = pun krug, 1,5 = pun krug i polumjesec iza, 2 = dva puna.
 * Pri promjeni količine (i pri prvom prikazu) puni se odozdo kao juha koja se nalijeva (~200 ms).
 */
@Composable
fun PorcijaGlif(
    kolicina: Double,
    smanjeno: Boolean,
    modifier: Modifier = Modifier,
    /** Boja ispune; zadano orah/krem, u gumbu za dodavanje paprika. */
    ispuna: Color = MaterialTheme.colorScheme.secondary
) {
    val shema = MaterialTheme.colorScheme
    val opis = tekstPorcije(kolicina)

    val prednji = kolicina.coerceIn(0.0, 1.0).toFloat()
    val straznji = (kolicina - 1.0).coerceIn(0.0, 1.0).toFloat()
    // U previewu nema animacije, pa glif odmah prikazuje završno stanje.
    val pregled = LocalInspectionMode.current
    val razina = remember { Animatable(if (pregled) prednji else 0f) }
    val visak = remember { Animatable(if (pregled) straznji else 0f) }
    LaunchedEffect(prednji, straznji) {
        razina.animateTo(prednji, nalijevanjeTween(smanjeno))
        visak.animateTo(straznji, nalijevanjeTween(smanjeno))
    }

    Canvas(
        modifier = modifier
            .size(MacroDimenzije.glifPorcije * OMJER_SIRINE, MacroDimenzije.glifPorcije)
            .semantics { contentDescription = opis }
    ) {
        val obrub = MacroDimenzije.obrubIsprekidan.toPx()
        val r = size.height / 2 - obrub
        val prednjeSrediste = Offset(size.height / 2, size.height / 2)
        val straznjeSrediste = Offset(size.width - size.height / 2, size.height / 2)

        val v = visak.value
        if (v > 0f) {
            val alfa = if (v >= 1f) ALFA_PUNO else ALFA_POLA + (ALFA_PUNO - ALFA_POLA) * ((v - 0.5f) / 0.5f).coerceIn(0f, 1f)
            drawCircle(ispuna.copy(alpha = alfa * (v / 0.5f).coerceAtMost(1f)), r, straznjeSrediste)
        }

        drawCircle(shema.surface, r, prednjeSrediste)
        val krug = Path().apply {
            addOval(androidx.compose.ui.geometry.Rect(prednjeSrediste, r))
        }
        clipPath(krug) {
            val visina = 2 * r * razina.value
            drawRect(
                color = ispuna,
                topLeft = Offset(prednjeSrediste.x - r, prednjeSrediste.y + r - visina),
                size = Size(2 * r, visina)
            )
        }
        drawCircle(shema.onSurfaceVariant, r, prednjeSrediste, style = Stroke(obrub))
    }
}
