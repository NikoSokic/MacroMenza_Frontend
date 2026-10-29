package com.niko.macromenza.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Smjer A „Pladanj" (design-system/macromenza/MASTER.md): topli neutralni tonovi, orah, paprika samo za akciju.
// Ikona i početni prozor kopiraju dio ovih vrijednosti u res/values(-night)/colors.xml; mijenjaju se zajedno.

// Svijetla tema
internal val Papir = Color(0xFFF5EEE2)
internal val Porculan = Color(0xFFFFFBF3)
internal val PorculanTamniji = Color(0xFFEDE3D3)
internal val Tinta = Color(0xFF2B211A)
internal val TintaPrigusena = Color(0xFF6B5B4E)
internal val LinijaSvijetla = Color(0xFFE6DAC8)
internal val Paprika = Color(0xFFC23B1F)
internal val PaprikaKontejner = Color(0xFFF7DDD4)
internal val NaPaprikaKontejneru = Color(0xFF5A1A0C)
internal val Orah = Color(0xFF3A2E26)
internal val Visnja = Color(0xFFA3304C)
internal val GreskaSvijetla = Color(0xFFB3261E)

// Tamna tema (topla smeđe-crna, nikad #000000)
internal val Noc = Color(0xFF1B1511)
internal val NocPovrsina = Color(0xFF261E18)
internal val NocPovrsinaVisa = Color(0xFF322820)
internal val Krem = Color(0xFFF3E9DC)
internal val KremPrigusena = Color(0xFFB8A998)
internal val LinijaTamna = Color(0xFF3A2F27)
internal val PaprikaSvijetla = Color(0xFFF0714F)
internal val PaprikaKontejnerTamni = Color(0xFF5A2414)
internal val OrahTamni = Color(0xFF2E241C)
internal val VisnjaSvijetla = Color(0xFFF28CA5)
internal val GreskaTamna = Color(0xFFF2B8B5)

internal val OrahTekstPrigusen = Color(0xFFCDBCA8)
internal val TanjurTamni = Color(0xFFEFE4D5)

// Makronutrijenti: tamnije nijanse za svijetle površine i porculanske tanjure, svjetlije za tamne površine
internal val ProteiniTamni = Color(0xFF3F7A5A)
internal val MastiTamne = Color(0xFFB0614A)
internal val UgljikohidratiTamni = Color(0xFFA8720F)
internal val ProteiniSvijetli = Color(0xFF6FB08E)
internal val MastiSvijetle = Color(0xFFE39A7F)
internal val UgljikohidratiSvijetli = Color(0xFFE0B04A)

/** Boje izvan Material ColorSchemea: pladanj, luk, prekoračenje i makronutrijenti. */
@Immutable
data class MacroBoje(
    val orah: Color,
    val naOrahu: Color,
    val naOrahuPrigusen: Color,
    val heroPozadina: Color,
    val naHeroju: Color,
    val naHerojuPrigusen: Color,
    val heroRub: Color,
    val lukIspuna: Color,
    val lukStaza: Color,
    val visnja: Color,
    val proteini: Color,
    val masti: Color,
    val ugljikohidrati: Color,
    val tanjur: Color,
    val tanjurUdubljenje: Color,
    val tanjurTekst: Color,
    val tanjurProteini: Color,
    val tanjurMasti: Color,
    val tanjurUgljikohidrati: Color,
    val sjenaTanjura: Color,
    val pladanjUdubljenje: Color,
    val pladanjRub: Color,
    val pladanjZrnoSvijetlo: Color,
    val pladanjZrnoTamno: Color,
    val kostur: Color
)

val MacroBojeSvijetle = MacroBoje(
    orah = Orah,
    naOrahu = Krem,
    naOrahuPrigusen = OrahTekstPrigusen,
    heroPozadina = Porculan,
    naHeroju = Tinta,
    naHerojuPrigusen = TintaPrigusena,
    heroRub = LinijaSvijetla,
    lukIspuna = Orah,
    lukStaza = PorculanTamniji,
    visnja = Visnja,
    proteini = ProteiniTamni,
    masti = MastiTamne,
    ugljikohidrati = UgljikohidratiTamni,
    tanjur = Porculan,
    tanjurUdubljenje = Tinta.copy(alpha = 0.08f),
    tanjurTekst = Tinta,
    tanjurProteini = ProteiniTamni,
    tanjurMasti = MastiTamne,
    tanjurUgljikohidrati = UgljikohidratiTamni,
    sjenaTanjura = Color.Black.copy(alpha = 0.30f),
    pladanjUdubljenje = Color.Black.copy(alpha = 0.14f),
    pladanjRub = Krem.copy(alpha = 0.07f),
    pladanjZrnoSvijetlo = Krem.copy(alpha = 0.035f),
    pladanjZrnoTamno = Color.Black.copy(alpha = 0.09f),
    kostur = PorculanTamniji
)

val MacroBojeTamne = MacroBoje(
    orah = OrahTamni,
    naOrahu = Krem,
    naOrahuPrigusen = OrahTekstPrigusen,
    heroPozadina = OrahTamni,
    naHeroju = Krem,
    naHerojuPrigusen = OrahTekstPrigusen,
    heroRub = Color.Transparent,
    lukIspuna = Krem,
    lukStaza = Krem.copy(alpha = 0.15f),
    visnja = VisnjaSvijetla,
    proteini = ProteiniSvijetli,
    masti = MastiSvijetle,
    ugljikohidrati = UgljikohidratiSvijetli,
    tanjur = TanjurTamni,
    tanjurUdubljenje = Tinta.copy(alpha = 0.08f),
    tanjurTekst = Tinta,
    tanjurProteini = ProteiniTamni,
    tanjurMasti = MastiTamne,
    tanjurUgljikohidrati = UgljikohidratiTamni,
    sjenaTanjura = Color.Black.copy(alpha = 0.30f),
    pladanjUdubljenje = Color.Black.copy(alpha = 0.18f),
    pladanjRub = Krem.copy(alpha = 0.06f),
    pladanjZrnoSvijetlo = Krem.copy(alpha = 0.03f),
    pladanjZrnoTamno = Color.Black.copy(alpha = 0.10f),
    kostur = NocPovrsinaVisa
)

val LocalMacroBoje = staticCompositionLocalOf { MacroBojeSvijetle }

val MaterialTheme.macroBoje: MacroBoje
    @Composable
    @ReadOnlyComposable
    get() = LocalMacroBoje.current
