package com.niko.macromenza.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.niko.macromenza.R

/*
 * Fraunces (display: broj u luku, naslovi, kcal na tanjurima) + Hanken Grotesk (sve ostalo).
 * Oba fonta su varijabilni TTF-ovi u res/font (OFL licence u assets/licence).
 * Hanken Grotesk ima tablične znamenke kao zadane; Fraunces nema "tnum", pa se njegovi brojevi
 * crtaju komponentom TablicniBroj (fiksna ćelija po znamenci).
 */

@OptIn(ExperimentalTextApi::class)
private fun fraunces(opticka: Float, tezina: Int) = Font(
    resId = R.font.fraunces,
    weight = FontWeight(tezina),
    variationSettings = FontVariation.Settings(
        FontVariation.weight(tezina),
        FontVariation.Setting("opsz", opticka),
        FontVariation.Setting("SOFT", 100f),
        FontVariation.Setting("WONK", 0f)
    )
)

@OptIn(ExperimentalTextApi::class)
private fun hanken(tezina: Int) = Font(
    resId = R.font.hanken_grotesk,
    weight = FontWeight(tezina),
    variationSettings = FontVariation.Settings(FontVariation.weight(tezina))
)

/** Velike brojke (luk): optička veličina 144. */
val FrauncesDisplay = FontFamily(fraunces(144f, 500), fraunces(144f, 600))

/** Naslovi sekcija i srednje brojke: optička veličina 72. */
val FrauncesNaslov = FontFamily(fraunces(72f, 500), fraunces(72f, 600))

/** Mali naslovi (naziv obroka, kcal na tanjuru): optička veličina 36. */
val FrauncesMali = FontFamily(fraunces(36f, 500), fraunces(36f, 600))

val HankenGrotesk = FontFamily(hanken(400), hanken(500), hanken(600), hanken(700))

private const val TABLICNE = "tnum"

private fun hankenStil(velicina: Float, proreda: Float, tezina: FontWeight, razmak: Float = 0f) = TextStyle(
    fontFamily = HankenGrotesk,
    fontWeight = tezina,
    fontSize = velicina.sp,
    lineHeight = proreda.sp,
    letterSpacing = razmak.em,
    fontFeatureSettings = TABLICNE
)

val Typography = Typography(
    displayLarge = TextStyle(fontFamily = FrauncesDisplay, fontWeight = FontWeight.SemiBold, fontSize = 57.sp, lineHeight = 60.sp),
    displayMedium = TextStyle(fontFamily = FrauncesDisplay, fontWeight = FontWeight.SemiBold, fontSize = 45.sp, lineHeight = 50.sp),
    displaySmall = TextStyle(fontFamily = FrauncesNaslov, fontWeight = FontWeight.SemiBold, fontSize = 36.sp, lineHeight = 42.sp),
    headlineLarge = TextStyle(fontFamily = FrauncesNaslov, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontFamily = FrauncesNaslov, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 34.sp),
    headlineSmall = TextStyle(fontFamily = FrauncesNaslov, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.01).em),
    titleLarge = TextStyle(fontFamily = FrauncesNaslov, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = FrauncesMali, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
    titleSmall = hankenStil(15f, 20f, FontWeight.SemiBold),
    bodyLarge = hankenStil(16f, 24f, FontWeight.Normal),
    bodyMedium = hankenStil(14.5f, 21f, FontWeight.Normal),
    bodySmall = hankenStil(13f, 18f, FontWeight.Normal),
    labelLarge = hankenStil(15f, 20f, FontWeight.SemiBold),
    labelMedium = hankenStil(12.5f, 16f, FontWeight.Medium),
    labelSmall = hankenStil(12f, 16f, FontWeight.Medium, 0.02f)
)

/** Stilovi izvan Material Typographyja. */
@Immutable
data class MacroTipografija(
    val brojLuka: TextStyle,
    val brojSrednji: TextStyle,
    val kcalTanjura: TextStyle,
    val nadnaslov: TextStyle
)

val MacroTipografijaZadana = MacroTipografija(
    brojLuka = TextStyle(fontFamily = FrauncesDisplay, fontWeight = FontWeight.SemiBold, fontSize = 64.sp, lineHeight = 64.sp, letterSpacing = (-0.02).em),
    brojSrednji = TextStyle(fontFamily = FrauncesNaslov, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 28.sp),
    kcalTanjura = TextStyle(fontFamily = FrauncesMali, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 18.sp),
    nadnaslov = hankenStil(12f, 16f, FontWeight.SemiBold, 0.1f)
)

val LocalMacroTipografija = staticCompositionLocalOf { MacroTipografijaZadana }

val MaterialTheme.macroTipografija: MacroTipografija
    @Composable
    @ReadOnlyComposable
    get() = LocalMacroTipografija.current
