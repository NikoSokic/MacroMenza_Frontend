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

/** Početak poruke kojom AuthViewModel javlja uspjeh registracije (postojeće prepoznavanje uspjeha). */
private const val POCETAK_PORUKE_USPJEHA = "Poslali smo ti email"

@Composable
fun RegisterScreen(
    navController: NavController,
    viewModel: AuthViewModel
) {
    val poruka by viewModel.poruka.collectAsState()
    val ucitavanje by viewModel.ucitavanje.collectAsState()

    var email by remember { mutableStateOf("") }
    var lozinka by remember { mutableStateOf("") }
    var ponovljenaLozinka by remember { mutableStateOf("") }
    val prviUlaz = rememberPrviUlaz(UlazUSesiji.REGISTRACIJA)

    DisposableEffect(Unit) {
        viewModel.ocistiPoruku()

        onDispose {
            viewModel.ocistiPoruku()
        }
    }

    RegisterSadrzaj(
        email = email,
        onEmail = { email = it },
        lozinka = lozinka,
        onLozinka = { lozinka = it },
        ponovljenaLozinka = ponovljenaLozinka,
        onPonovljenaLozinka = { ponovljenaLozinka = it },
        poruka = poruka,
        ucitavanje = ucitavanje,
        animirajUlaz = prviUlaz,
        onRegistracija = {
            viewModel.registracija(
                email = email.trim(),
                lozinka = lozinka
            )
        },
        onPrijava = {
            viewModel.ocistiPoruku()
            navController.navigate("login")
        }
    )
}

/** Izgled registracije bez ViewModela (pages/prijava.md); koristi ga ekran i previewi. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun RegisterSadrzaj(
    email: String,
    onEmail: (String) -> Unit,
    lozinka: String,
    onLozinka: (String) -> Unit,
    ponovljenaLozinka: String,
    onPonovljenaLozinka: (String) -> Unit,
    poruka: String?,
    ucitavanje: Boolean,
    animirajUlaz: Boolean,
    onRegistracija: () -> Unit,
    onPrijava: () -> Unit
) {
    val shema = MaterialTheme.colorScheme
    val tip = MaterialTheme.typography
    val fokus = LocalFocusManager.current

    val lozinkeSePoklapaju = lozinka.isNotBlank() && lozinka == ponovljenaLozinka
    val moze = email.isNotBlank() && lozinkeSePoklapaju && !ucitavanje
    val uspjeh = poruka?.contains(POCETAK_PORUKE_USPJEHA, ignoreCase = true) == true

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
                text = stringResource(R.string.auth_registracija_naslov),
                style = tip.headlineSmall,
                color = shema.onSurface,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = stringResource(R.string.auth_registracija_podnaslov),
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
            imeAction = ImeAction.Next,
            sadrzaj = ContentType.NewPassword,
            modifier = Modifier.ulazAuth(3, animirajUlaz)
        )

        Spacer(Modifier.height(MacroRazmaci.l))

        PoljeUnosa(
            vrijednost = ponovljenaLozinka,
            onPromjena = onPonovljenaLozinka,
            oznaka = stringResource(R.string.auth_ponovi_lozinku),
            greska = if (ponovljenaLozinka.isNotBlank() && !lozinkeSePoklapaju) {
                stringResource(R.string.auth_lozinke_ne_poklapaju)
            } else null,
            lozinka = true,
            imeAction = ImeAction.Done,
            sadrzaj = ContentType.NewPassword,
            keyboardActions = KeyboardActions(onDone = {
                fokus.clearFocus()
                if (moze) onRegistracija()
            }),
            modifier = Modifier.ulazAuth(4, animirajUlaz)
        )

        Spacer(Modifier.height(MacroRazmaci.xl))

        GumbSpremi(
            tekst = stringResource(R.string.auth_registracija_gumb),
            onClick = onRegistracija,
            enabled = moze,
            sprema = ucitavanje,
            tekstStanja = stringResource(R.string.auth_registracija_u_tijeku),
            modifier = Modifier.ulazAuth(5, animirajUlaz)
        )

        TihaPoruka(tekst = poruka, uspjeh = uspjeh)

        Spacer(Modifier.height(MacroRazmaci.s))

        LinkAuth(
            pitanje = stringResource(R.string.auth_imas_racun),
            radnja = stringResource(R.string.auth_prijavi_link),
            onClick = onPrijava,
            modifier = Modifier.ulazAuth(6, animirajUlaz)
        )
    }
}

// ---------- Previewi ----------

private const val GRESKA_PRIMJER = "Korisnik s ovom email adresom već postoji."
private const val USPJEH_PRIMJER = "Poslali smo ti email za potvrdu računa. Otvori mail i potvrdi registraciju."

@Composable
private fun PreviewRegister(
    tamna: Boolean,
    email: String = "",
    lozinka: String = "",
    ponovljena: String = "",
    poruka: String? = null,
    ucitavanje: Boolean = false
) {
    MacroMenzaTheme(darkTheme = tamna) {
        RegisterSadrzaj(
            email = email,
            onEmail = {},
            lozinka = lozinka,
            onLozinka = {},
            ponovljenaLozinka = ponovljena,
            onPonovljenaLozinka = {},
            poruka = poruka,
            ucitavanje = ucitavanje,
            animirajUlaz = false,
            onRegistracija = {},
            onPrijava = {}
        )
    }
}

@Preview(name = "Svijetla · prazno", widthDp = 360, heightDp = 860)
@Composable
private fun PreviewSvijetlaPrazno() = PreviewRegister(false)

@Preview(name = "Tamna · prazno", widthDp = 360, heightDp = 860)
@Composable
private fun PreviewTamnaPrazno() = PreviewRegister(true)

@Preview(name = "Svijetla · popunjeno", widthDp = 360, heightDp = 860)
@Composable
private fun PreviewSvijetlaPopunjeno() = PreviewRegister(false, "niko@student.fesb.hr", "tajna123", "tajna123")

@Preview(name = "Tamna · popunjeno", widthDp = 360, heightDp = 860)
@Composable
private fun PreviewTamnaPopunjeno() = PreviewRegister(true, "niko@student.fesb.hr", "tajna123", "tajna123")

@Preview(name = "Svijetla · lozinke se ne poklapaju", widthDp = 360, heightDp = 860)
@Composable
private fun PreviewSvijetlaNePoklapaju() = PreviewRegister(false, "niko@student.fesb.hr", "tajna123", "tajna12")

@Preview(name = "Svijetla · registracija u tijeku", widthDp = 360, heightDp = 860)
@Composable
private fun PreviewSvijetlaUTijeku() = PreviewRegister(false, "niko@student.fesb.hr", "tajna123", "tajna123", ucitavanje = true)

@Preview(name = "Tamna · registracija u tijeku", widthDp = 360, heightDp = 860)
@Composable
private fun PreviewTamnaUTijeku() = PreviewRegister(true, "niko@student.fesb.hr", "tajna123", "tajna123", ucitavanje = true)

@Preview(name = "Svijetla · greška", widthDp = 360, heightDp = 860)
@Composable
private fun PreviewSvijetlaGreska() = PreviewRegister(false, "niko@student.fesb.hr", "tajna123", "tajna123", GRESKA_PRIMJER)

@Preview(name = "Tamna · greška", widthDp = 360, heightDp = 860)
@Composable
private fun PreviewTamnaGreska() = PreviewRegister(true, "niko@student.fesb.hr", "tajna123", "tajna123", GRESKA_PRIMJER)

@Preview(name = "Svijetla · uspjeh registracije", widthDp = 360, heightDp = 860)
@Composable
private fun PreviewSvijetlaUspjeh() = PreviewRegister(false, "niko@student.fesb.hr", "tajna123", "tajna123", USPJEH_PRIMJER)

@Preview(name = "Tamna · uspjeh registracije", widthDp = 360, heightDp = 860)
@Composable
private fun PreviewTamnaUspjeh() = PreviewRegister(true, "niko@student.fesb.hr", "tajna123", "tajna123", USPJEH_PRIMJER)

@Preview(name = "Svijetla · greška · fontScale 1,3", widthDp = 360, heightDp = 1200, fontScale = 1.3f)
@Composable
private fun PreviewSvijetlaGreskaVelika() = PreviewRegister(false, "niko@student.fesb.hr", "tajna123", "tajna12", GRESKA_PRIMJER)

@Preview(name = "Tamna · uspjeh · fontScale 1,3", widthDp = 360, heightDp = 1200, fontScale = 1.3f)
@Composable
private fun PreviewTamnaUspjehVelika() = PreviewRegister(true, "niko@student.fesb.hr", "tajna123", "tajna123", USPJEH_PRIMJER)
