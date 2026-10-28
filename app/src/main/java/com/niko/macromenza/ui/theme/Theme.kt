package com.niko.macromenza.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Paprika,
    onPrimary = Color.White,
    primaryContainer = PaprikaKontejner,
    onPrimaryContainer = NaPaprikaKontejneru,
    secondary = Orah,
    onSecondary = Krem,
    secondaryContainer = PorculanTamniji,
    onSecondaryContainer = Tinta,
    tertiary = ProteiniTamni,
    onTertiary = Porculan,
    background = Papir,
    onBackground = Tinta,
    surface = Porculan,
    onSurface = Tinta,
    surfaceVariant = PorculanTamniji,
    onSurfaceVariant = TintaPrigusena,
    surfaceContainerLowest = Porculan,
    surfaceContainerLow = Porculan,
    surfaceContainer = Papir,
    surfaceContainerHigh = PorculanTamniji,
    surfaceContainerHighest = PorculanTamniji,
    inverseSurface = Orah,
    inverseOnSurface = Krem,
    inversePrimary = PaprikaSvijetla,
    outline = TintaPrigusena,
    outlineVariant = LinijaSvijetla,
    error = GreskaSvijetla,
    onError = Porculan
)

private val DarkColorScheme = darkColorScheme(
    primary = PaprikaSvijetla,
    onPrimary = Noc,
    primaryContainer = PaprikaKontejnerTamni,
    onPrimaryContainer = Krem,
    secondary = Krem,
    onSecondary = Noc,
    secondaryContainer = NocPovrsinaVisa,
    onSecondaryContainer = Krem,
    tertiary = ProteiniSvijetli,
    onTertiary = Noc,
    background = Noc,
    onBackground = Krem,
    surface = NocPovrsina,
    onSurface = Krem,
    surfaceVariant = NocPovrsinaVisa,
    onSurfaceVariant = KremPrigusena,
    surfaceContainerLowest = Noc,
    surfaceContainerLow = NocPovrsina,
    surfaceContainer = NocPovrsina,
    surfaceContainerHigh = NocPovrsinaVisa,
    surfaceContainerHighest = NocPovrsinaVisa,
    inverseSurface = Krem,
    inverseOnSurface = Noc,
    inversePrimary = Paprika,
    outline = KremPrigusena,
    outlineVariant = LinijaTamna,
    error = GreskaTamna,
    onError = Noc
)

@Composable
fun MacroMenzaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(
        LocalMacroBoje provides if (darkTheme) MacroBojeTamne else MacroBojeSvijetle,
        LocalMacroTipografija provides MacroTipografijaZadana
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = MacroShapes,
            content = content
        )
    }
}
