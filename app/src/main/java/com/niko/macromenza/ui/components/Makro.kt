package com.niko.macromenza.ui.components

import androidx.compose.runtime.Immutable
import com.niko.macromenza.model.NutritivneVrijednosti

/** Nutritivne vrijednosti jednog obroka, jela ili dana (kcal + P/M/U u gramima). */
@Immutable
data class Makro(
    val kcal: Double,
    val proteini: Double,
    val masti: Double,
    val ugljikohidrati: Double
) {
    operator fun plus(o: Makro) = Makro(kcal + o.kcal, proteini + o.proteini, masti + o.masti, ugljikohidrati + o.ugljikohidrati)

    /** Udjeli kcal iz P (×4), M (×9) i U (×4) za rub tanjura; zbroj je 1 ili su svi 0. */
    fun udjeli(): Triple<Float, Float, Float> {
        val p = proteini * 4
        val m = masti * 9
        val u = ugljikohidrati * 4
        val ukupno = p + m + u
        if (ukupno <= 0.0) return Triple(0f, 0f, 0f)
        return Triple((p / ukupno).toFloat(), (m / ukupno).toFloat(), (u / ukupno).toFloat())
    }

    companion object {
        val NULA = Makro(0.0, 0.0, 0.0, 0.0)
    }
}

fun NutritivneVrijednosti.uMakro() = Makro(kalorije, proteini, masti, ugljikohidrati)
