package com.niko.macromenza.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import com.niko.macromenza.R
import com.niko.macromenza.ui.components.GumbSpremi
import com.niko.macromenza.ui.components.MonogramMakro
import com.niko.macromenza.ui.components.screenBottomPadding
import com.niko.macromenza.ui.components.screenTopPadding
import com.niko.macromenza.ui.components.skalaPritiska
import com.niko.macromenza.ui.theme.MacroDimenzije
import com.niko.macromenza.ui.theme.MacroMenzaTheme
import com.niko.macromenza.ui.theme.MacroOblici
import com.niko.macromenza.ui.theme.MacroRazmaci
import com.niko.macromenza.ui.theme.UlazUSesiji
import com.niko.macromenza.ui.theme.rememberSmanjeniPokret

@Composable
fun WelcomeScreen(
    navController: NavController
) {
    val smanjeno = rememberSmanjeniPokret()
    val prviUlaz = rememberPrviUlaz(UlazUSesiji.WELCOME)

    WelcomeSadrzaj(
        animirajUlaz = prviUlaz,
        smanjeno = smanjeno,
        onRegistracija = { navController.navigate("register") },
        onPrijava = { navController.navigate("login") }
    )
}

/** Izgled Welcomea bez navigacije (pages/prijava.md § Welcome); koristi ga ekran i previewi. */
@Composable
internal fun WelcomeSadrzaj(
    animirajUlaz: Boolean,
    smanjeno: Boolean,
    onRegistracija: () -> Unit,
    onPrijava: () -> Unit
) {
    val shema = MaterialTheme.colorScheme
    val tip = MaterialTheme.typography

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(shema.background)
    ) {
        // Sadržaj se lista; gumbi ispod njega stalno ostaju na ekranu.
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MacroRazmaci.rubEkrana)
                .padding(top = screenTopPadding() + MacroRazmaci.xl)
        ) {
            MonogramMakro(
                animirajUlaz = animirajUlaz,
                smanjeno = smanjeno,
                modifier = Modifier.ulazAuth(0, animirajUlaz)
            )

            Text(
                text = stringResource(R.string.auth_welcome_naslov),
                style = tip.headlineMedium,
                color = shema.onBackground,
                modifier = Modifier
                    .padding(top = MacroRazmaci.xl)
                    .semantics { heading() }
                    .ulazAuth(1, animirajUlaz)
            )
            Text(
                text = stringResource(R.string.auth_welcome_podnaslov),
                style = tip.bodyLarge,
                color = shema.onSurfaceVariant,
                modifier = Modifier
                    .padding(top = MacroRazmaci.xs)
                    .ulazAuth(2, animirajUlaz)
            )

            Column(
                modifier = Modifier.padding(top = MacroRazmaci.xl),
                verticalArrangement = Arrangement.spacedBy(MacroRazmaci.l)
            ) {
                ZnacajkaRed(
                    ikona = Icons.Rounded.Restaurant,
                    naslov = stringResource(R.string.auth_znacajka_menza_naslov),
                    opis = stringResource(R.string.auth_znacajka_menza_opis),
                    modifier = Modifier.ulazAuth(3, animirajUlaz)
                )
                ZnacajkaRed(
                    ikona = Icons.Rounded.CameraAlt,
                    naslov = stringResource(R.string.auth_znacajka_slika_naslov),
                    opis = stringResource(R.string.auth_znacajka_slika_opis),
                    modifier = Modifier.ulazAuth(4, animirajUlaz)
                )
                ZnacajkaRed(
                    ikona = Icons.Rounded.TrackChanges,
                    naslov = stringResource(R.string.auth_znacajka_cilj_naslov),
                    opis = stringResource(R.string.auth_znacajka_cilj_opis),
                    modifier = Modifier.ulazAuth(5, animirajUlaz)
                )
            }

            Spacer(Modifier.height(MacroRazmaci.xl))
        }

        Column(
            modifier = Modifier
                .padding(horizontal = MacroRazmaci.rubEkrana)
                .padding(top = MacroRazmaci.m, bottom = screenBottomPadding(hasBottomNavigation = false))
        ) {
            GumbSpremi(
                tekst = stringResource(R.string.auth_registracija_gumb),
                onClick = onRegistracija,
                modifier = Modifier.ulazAuth(6, animirajUlaz)
            )

            val izvor = remember { MutableInteractionSource() }
            OutlinedButton(
                onClick = onPrijava,
                interactionSource = izvor,
                shape = MacroOblici.gumb,
                border = BorderStroke(MacroDimenzije.linija, shema.onSurface),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = shema.onSurface),
                modifier = Modifier
                    .padding(top = MacroRazmaci.s)
                    .fillMaxWidth()
                    .heightIn(min = MacroDimenzije.fabVisina)
                    .ulazAuth(7, animirajUlaz)
                    .skalaPritiska(izvor)
            ) {
                Text(
                    text = stringResource(R.string.auth_prijava_gumb),
                    style = tip.labelLarge,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** Jedna značajka: ikona u onSurfaceVariant bez podloge, naslov i prigušeni opis. */
@Composable
private fun ZnacajkaRed(ikona: ImageVector, naslov: String, opis: String, modifier: Modifier = Modifier) {
    val shema = MaterialTheme.colorScheme
    val tip = MaterialTheme.typography
    Row(modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) { }) {
        Icon(
            imageVector = ikona,
            contentDescription = null,
            tint = shema.onSurfaceVariant,
            modifier = Modifier.size(MacroDimenzije.ikona)
        )
        Spacer(Modifier.width(MacroRazmaci.m))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = naslov, style = tip.titleSmall, color = shema.onSurface)
            Text(
                text = opis,
                style = tip.bodyMedium,
                color = shema.onSurfaceVariant,
                modifier = Modifier.padding(top = MacroRazmaci.xxs)
            )
        }
    }
}

// ---------- Previewi ----------

@Composable
private fun PreviewWelcome(tamna: Boolean) {
    MacroMenzaTheme(darkTheme = tamna) {
        WelcomeSadrzaj(animirajUlaz = false, smanjeno = true, onRegistracija = {}, onPrijava = {})
    }
}

@Preview(name = "Svijetla", widthDp = 360, heightDp = 760)
@Composable
private fun PreviewSvijetla() = PreviewWelcome(false)

@Preview(name = "Tamna", widthDp = 360, heightDp = 760)
@Composable
private fun PreviewTamna() = PreviewWelcome(true)

@Preview(name = "Svijetla · fontScale 1,3", widthDp = 360, heightDp = 760, fontScale = 1.3f)
@Composable
private fun PreviewSvijetlaVelika() = PreviewWelcome(false)

@Preview(name = "Tamna · fontScale 1,3", widthDp = 360, heightDp = 760, fontScale = 1.3f)
@Composable
private fun PreviewTamnaVelika() = PreviewWelcome(true)

@Preview(name = "Svijetla · mali ekran", widthDp = 320, heightDp = 568)
@Composable
private fun PreviewSvijetlaMala() = PreviewWelcome(false)

@Preview(name = "Tamna · mali ekran · fontScale 1,3", widthDp = 320, heightDp = 568, fontScale = 1.3f)
@Composable
private fun PreviewTamnaMalaVelika() = PreviewWelcome(true)
