package com.niko.macromenza.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import com.niko.macromenza.R
import com.niko.macromenza.ui.components.GumbSpremi
import com.niko.macromenza.ui.components.PoljeUnosa
import com.niko.macromenza.ui.components.ZnakAplikacije
import com.niko.macromenza.ui.components.screenBottomPadding
import com.niko.macromenza.ui.components.screenTopPadding
import com.niko.macromenza.ui.theme.MacroMenzaTheme
import com.niko.macromenza.ui.theme.MacroRazmaci
import com.niko.macromenza.ui.theme.UlazUSesiji
import com.niko.macromenza.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: AuthViewModel
) {
    val poruka by viewModel.poruka.collectAsState()
    val ucitavanje by viewModel.ucitavanje.collectAsState()
    val korisnikId by viewModel.korisnikId.collectAsState()
    val onboardingZavrsen by viewModel.onboardingZavrsen.collectAsState()

    var email by remember { mutableStateOf("") }
    var lozinka by remember { mutableStateOf("") }
    val prviUlaz = rememberPrviUlaz(UlazUSesiji.PRIJAVA)

    DisposableEffect(Unit) {
        viewModel.ocistiPoruku()

        onDispose {
            viewModel.ocistiPoruku()
        }
    }

    LaunchedEffect(korisnikId, onboardingZavrsen) {
        if (korisnikId != null && onboardingZavrsen != null) {
            if (onboardingZavrsen == true) {
                navController.navigate("home") {
                    popUpTo("login") {
                        inclusive = true
                    }
                }
            } else {
                navController.navigate("onboarding/$korisnikId") {
                    popUpTo("login") {
                        inclusive = true
                    }
                }
            }
        }
    }

    LoginSadrzaj(
        email = email,
        onEmail = { email = it },
        lozinka = lozinka,
        onLozinka = { lozinka = it },
        poruka = poruka,
        ucitavanje = ucitavanje,
        animirajUlaz = prviUlaz,
        onPrijava = {
            viewModel.prijava(
                email = email.trim(),
                lozinka = lozinka
            )
        },
        onRegistracija = {
            viewModel.ocistiPoruku()
            navController.navigate("register")
        }
    )
}

/** Izgled prijave bez ViewModela (pages/prijava.md); koristi ga ekran i previewi. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun LoginSadrzaj(
    email: String,
    onEmail: (String) -> Unit,
    lozinka: String,
    onLozinka: (String) -> Unit,
    poruka: String?,
    ucitavanje: Boolean,
    animirajUlaz: Boolean,
    onPrijava: () -> Unit,
    onRegistracija: () -> Unit
) {
    val shema = MaterialTheme.colorScheme
    val tip = MaterialTheme.typography
    val fokus = LocalFocusManager.current
    val moze = email.isNotBlank() && lozinka.isNotBlank() && !ucitavanje

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(shema.background)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MacroRazmaci.rubEkrana)
            .padding(top = screenTopPadding() + MacroRazmaci.xl, bottom = screenBottomPadding(hasBottomNavigation = false)),
        horizontalAlignment = Alignment.Start
    ) {
        ZnakAplikacije(Modifier.ulazAuth(0, animirajUlaz))

        Column(
            modifier = Modifier
                .padding(top = MacroRazmaci.xl)
                .ulazAuth(1, animirajUlaz)
        ) {
            Text(
                text = stringResource(R.string.auth_prijava_naslov),
                style = tip.headlineSmall,
                color = shema.onSurface,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = stringResource(R.string.auth_prijava_podnaslov),
                style = tip.bodyLarge,
                color = shema.onSurfaceVariant,
                modifier = Modifier.padding(top = MacroRazmaci.xs)
            )
        }

        Spacer(Modifier.height(MacroRazmaci.xxl))

        PoljeUnosa(
            vrijednost = email,
            onPromjena = onEmail,
            oznaka = stringResource(R.string.auth_email),
            tipkovnica = KeyboardType.Email,
            imeAction = ImeAction.Next,
            sadrzaj = ContentType.EmailAddress,
            modifier = Modifier.ulazAuth(2, animirajUlaz)
        )

        Spacer(Modifier.height(MacroRazmaci.l))

        PoljeUnosa(
            vrijednost = lozinka,
            onPromjena = onLozinka,
            oznaka = stringResource(R.string.auth_lozinka),
            lozinka = true,
            imeAction = ImeAction.Done,
            sadrzaj = ContentType.Password,
            keyboardActions = KeyboardActions(onDone = {
                fokus.clearFocus()
                if (moze) onPrijava()
            }),
            modifier = Modifier.ulazAuth(3, animirajUlaz)
        )

        Spacer(Modifier.height(MacroRazmaci.xl))

        GumbSpremi(
            tekst = stringResource(R.string.auth_prijava_gumb),
            onClick = onPrijava,
            enabled = moze,
            sprema = ucitavanje,
            tekstStanja = stringResource(R.string.auth_prijava_u_tijeku),
            modifier = Modifier.ulazAuth(4, animirajUlaz)
        )

        TihaPoruka(tekst = poruka, uspjeh = false)

        Spacer(Modifier.height(MacroRazmaci.s))

        LinkAuth(
            pitanje = stringResource(R.string.auth_nema_racun),
            radnja = stringResource(R.string.auth_registriraj_link),
            onClick = onRegistracija,
            modifier = Modifier.ulazAuth(5, animirajUlaz)
        )
    }
}

// ---------- Previewi ----------

private const val GRESKA_PRIMJER = "Pogrešan email ili lozinka."

@Composable
private fun PreviewLogin(
    tamna: Boolean,
    email: String = "",
    lozinka: String = "",
    poruka: String? = null,
    ucitavanje: Boolean = false
) {
    MacroMenzaTheme(darkTheme = tamna) {
        LoginSadrzaj(
            email = email,
            onEmail = {},
            lozinka = lozinka,
            onLozinka = {},
            poruka = poruka,
            ucitavanje = ucitavanje,
            animirajUlaz = false,
            onPrijava = {},
            onRegistracija = {}
        )
    }
}

@Preview(name = "Svijetla · prazno", widthDp = 360, heightDp = 760)
@Composable
private fun PreviewSvijetlaPrazno() = PreviewLogin(false)

@Preview(name = "Tamna · prazno", widthDp = 360, heightDp = 760)
@Composable
private fun PreviewTamnaPrazno() = PreviewLogin(true)

@Preview(name = "Svijetla · popunjeno", widthDp = 360, heightDp = 760)
@Composable
private fun PreviewSvijetlaPopunjeno() = PreviewLogin(false, "niko@student.fesb.hr", "tajna123")

@Preview(name = "Tamna · popunjeno", widthDp = 360, heightDp = 760)
@Composable
private fun PreviewTamnaPopunjeno() = PreviewLogin(true, "niko@student.fesb.hr", "tajna123")

@Preview(name = "Svijetla · prijava u tijeku", widthDp = 360, heightDp = 760)
@Composable
private fun PreviewSvijetlaUTijeku() = PreviewLogin(false, "niko@student.fesb.hr", "tajna123", ucitavanje = true)

@Preview(name = "Tamna · prijava u tijeku", widthDp = 360, heightDp = 760)
@Composable
private fun PreviewTamnaUTijeku() = PreviewLogin(true, "niko@student.fesb.hr", "tajna123", ucitavanje = true)

@Preview(name = "Svijetla · greška", widthDp = 360, heightDp = 760)
@Composable
private fun PreviewSvijetlaGreska() = PreviewLogin(false, "niko@student.fesb.hr", "kriva", GRESKA_PRIMJER)

@Preview(name = "Tamna · greška", widthDp = 360, heightDp = 760)
@Composable
private fun PreviewTamnaGreska() = PreviewLogin(true, "niko@student.fesb.hr", "kriva", GRESKA_PRIMJER)

@Preview(name = "Svijetla · greška · fontScale 1,3", widthDp = 360, heightDp = 1000, fontScale = 1.3f)
@Composable
private fun PreviewSvijetlaGreskaVelika() = PreviewLogin(false, "niko@student.fesb.hr", "kriva", GRESKA_PRIMJER)

@Preview(name = "Tamna · greška · fontScale 1,3", widthDp = 360, heightDp = 1000, fontScale = 1.3f)
@Composable
private fun PreviewTamnaGreskaVelika() = PreviewLogin(true, "niko@student.fesb.hr", "kriva", GRESKA_PRIMJER)
