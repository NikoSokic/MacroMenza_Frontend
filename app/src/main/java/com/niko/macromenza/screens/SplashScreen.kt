package com.niko.macromenza.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.niko.macromenza.R
import com.niko.macromenza.ui.components.Tanjur
import com.niko.macromenza.ui.components.ZnakAplikacije
import com.niko.macromenza.ui.theme.MacroDimenzije
import com.niko.macromenza.ui.theme.MacroMenzaTheme
import com.niko.macromenza.ui.theme.MacroOmjeri
import com.niko.macromenza.ui.theme.MacroRazmaci
import com.niko.macromenza.ui.theme.Trajanje
import com.niko.macromenza.ui.theme.rememberSmanjeniPokret

/** Prikazuje se samo dok se čita sesija (NavGraph); bez umjetnog čekanja. */
@Composable
fun SplashScreen() {
    SplashSadrzaj(smanjeno = rememberSmanjeniPokret())
}

/** Izgled Splasha (pages/prijava.md § Splash); koristi ga ekran i previewi. */
@Composable
internal fun SplashSadrzaj(smanjeno: Boolean) {
    val shema = MaterialTheme.colorScheme
    val tip = MaterialTheme.typography
    val mirno = smanjeno || LocalInspectionMode.current
    val disanje = if (mirno) null else rememberInfiniteTransition(label = "disanjeSplash").animateFloat(
        initialValue = MacroOmjeri.disanjeMin,
        targetValue = MacroOmjeri.disanjeMaks,
        animationSpec = infiniteRepeatable(tween(Trajanje.disanje), RepeatMode.Reverse),
        label = "disanje"
    )
    val opisUcitavanja = stringResource(R.string.spl_ucitavam)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(shema.background)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = MacroRazmaci.xxl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ZnakAplikacije()

            Text(
                text = stringResource(R.string.spl_naslov),
                style = tip.headlineMedium,
                color = shema.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = MacroRazmaci.xl)
            )
            Text(
                text = stringResource(R.string.spl_slogan),
                style = tip.bodyMedium,
                color = shema.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = MacroRazmaci.xs)
            )

            Spacer(Modifier.height(MacroRazmaci.xxl))

            // Prazan tanjur koji diše umjesto spinnera (kao u Slici ili opisu).
            Tanjur(
                makro = null,
                promjer = MacroDimenzije.tanjurMin,
                okvir = MacroDimenzije.tanjurMin,
                smanjeno = smanjeno,
                modifier = Modifier
                    .semantics { contentDescription = opisUcitavanja }
                    .graphicsLayer { alpha = disanje?.value ?: MacroOmjeri.disanjeMaks }
            )
        }
    }
}

// ---------- Previewi ----------

@Composable
private fun PreviewSplash(tamna: Boolean) {
    MacroMenzaTheme(darkTheme = tamna) { SplashSadrzaj(smanjeno = true) }
}

@Preview(name = "Svijetla", widthDp = 360, heightDp = 760)
@Composable
private fun PreviewSvijetla() = PreviewSplash(false)

@Preview(name = "Tamna", widthDp = 360, heightDp = 760)
@Composable
private fun PreviewTamna() = PreviewSplash(true)

@Preview(name = "Svijetla · fontScale 1,3", widthDp = 360, heightDp = 760, fontScale = 1.3f)
@Composable
private fun PreviewSvijetlaVelika() = PreviewSplash(false)

@Preview(name = "Tamna · fontScale 1,3", widthDp = 360, heightDp = 760, fontScale = 1.3f)
@Composable
private fun PreviewTamnaVelika() = PreviewSplash(true)

@Preview(name = "Svijetla · mali ekran", widthDp = 320, heightDp = 568)
@Composable
private fun PreviewSvijetlaMala() = PreviewSplash(false)

@Preview(name = "Tamna · mali ekran · fontScale 1,3", widthDp = 320, heightDp = 568, fontScale = 1.3f)
@Composable
private fun PreviewTamnaMalaVelika() = PreviewSplash(true)
