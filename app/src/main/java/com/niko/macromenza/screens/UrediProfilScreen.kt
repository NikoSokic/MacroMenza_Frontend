package com.niko.macromenza.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.niko.macromenza.R
import com.niko.macromenza.session.UserSessionManager
import com.niko.macromenza.ui.components.GumbSpremi
import com.niko.macromenza.ui.components.IzborOpcije
import com.niko.macromenza.ui.components.Opcija
import com.niko.macromenza.ui.components.PoljeUnosa
import com.niko.macromenza.ui.components.ZaglavljePodekrana
import com.niko.macromenza.ui.components.screenBottomPadding
import com.niko.macromenza.ui.theme.MacroMenzaTheme
import com.niko.macromenza.ui.theme.MacroRazmaci
import com.niko.macromenza.ui.theme.Trajanje
import com.niko.macromenza.ui.theme.rememberSmanjeniPokret
import com.niko.macromenza.util.UiTekst
import com.niko.macromenza.util.uString
import com.niko.macromenza.viewmodel.ProfilViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


@Composable
fun UrediProfilScreen(
    navController: NavController,
    viewModel: ProfilViewModel = viewModel()
) {
    val korisnik by viewModel.korisnik.collectAsState()
    val profil by viewModel.profil.collectAsState()
    val poruka by viewModel.poruka.collectAsState()

    val context = LocalContext.current
    val sessionManager = remember { UserSessionManager(context) }
    val prijavljeniKorisnikId by sessionManager.korisnikId.collectAsState(initial = null)

    var ime by rememberSaveable { mutableStateOf("") }
    var prezime by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var spol by rememberSaveable { mutableStateOf("M") }
    var pokusanoSpremanje by rememberSaveable { mutableStateOf(false) }
    var cekaSpremanje by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val spremljeno = stringResource(R.string.prof_urp_spremljeno)

    LaunchedEffect(prijavljeniKorisnikId) {
        prijavljeniKorisnikId?.let { id -> viewModel.ucitajProfil(id) }
    }

    LaunchedEffect(korisnik, profil) {
        korisnik?.let {
            ime = it.ime
            prezime = it.prezime
            email = it.email
        }
        profil?.let { spol = it.spol }
    }

    // Nakon uspješnog spremanja ViewModel ponovno učitava profil (ucitava true → false); poruka uspjeha
    // se odmah briše, pa je taj ciklus jedini pouzdan znak uspjeha. Tada snackbar i povratak.
    LaunchedEffect(cekaSpremanje) {
        if (!cekaSpremanje) return@LaunchedEffect
        viewModel.ucitava.first { it }
        viewModel.ucitava.first { !it }
        if (viewModel.poruka.value == null) {
            launch { snackbarHostState.showSnackbar(spremljeno) }
            delay(Trajanje.porukaPrijePovratka.toLong())
            navController.popBackStack()
        } else {
            cekaSpremanje = false
        }
    }

    // Poruka uspjeha iz [ProfilViewModel.spremiProfil]; sve ostale poruke su greške.
    val greska = poruka?.takeIf { it != UiTekst.Resurs(R.string.prof_urp_spremljeno) }?.uString()
    LaunchedEffect(greska) {
        if (greska != null) cekaSpremanje = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        UrediProfilSadrzaj(
            ime = ime,
            prezime = prezime,
            email = email,
            spol = spol,
            greskaIme = pokusanoSpremanje && ime.isBlank(),
            greska = greska,
            mozeSpremiti = korisnik != null && prijavljeniKorisnikId != null,
            onIme = { ime = it },
            onPrezime = { prezime = it },
            onSpol = { spol = it },
            onNatrag = { navController.popBackStack() },
            onSpremi = {
                pokusanoSpremanje = true
                val id = prijavljeniKorisnikId ?: return@UrediProfilSadrzaj
                if (ime.isBlank()) return@UrediProfilSadrzaj
                cekaSpremanje = true
                viewModel.spremiProfil(
                    idKorisnik = id,
                    ime = ime.trim(),
                    prezime = prezime.trim(),
                    email = email.trim(),
                    spol = spol
                )
            }
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = MacroRazmaci.rubEkrana, end = MacroRazmaci.rubEkrana, bottom = screenBottomPadding())
        )
    }
}

@Composable
private fun UrediProfilSadrzaj(
    ime: String,
    prezime: String,
    email: String,
    spol: String,
    greskaIme: Boolean,
    greska: String?,
    mozeSpremiti: Boolean,
    onIme: (String) -> Unit,
    onPrezime: (String) -> Unit,
    onSpol: (String) -> Unit,
    onNatrag: () -> Unit,
    onSpremi: () -> Unit
) {
    val shema = MaterialTheme.colorScheme
    val fokus = LocalFocusManager.current
    val smanjeno = rememberSmanjeniPokret()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(shema.background)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MacroRazmaci.rubEkrana)
            .padding(bottom = screenBottomPadding())
    ) {
        ZaglavljePodekrana(naslov = stringResource(R.string.prof_urp_naslov), onNatrag = onNatrag)

        NaslovSekcije(stringResource(R.string.prof_urp_osobni_podaci), prva = true)
        PoljeUnosa(
            vrijednost = ime,
            onPromjena = onIme,
            oznaka = stringResource(R.string.prof_urp_ime),
            greska = if (greskaIme) stringResource(R.string.prof_urp_greska_ime) else null,
            velikaSlova = KeyboardCapitalization.Words
        )
        Spacer(Modifier.height(MacroRazmaci.s))
        PoljeUnosa(
            vrijednost = prezime,
            onPromjena = onPrezime,
            oznaka = stringResource(R.string.prof_urp_prezime),
            velikaSlova = KeyboardCapitalization.Words,
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(onDone = { fokus.clearFocus() })
        )
        Spacer(Modifier.height(MacroRazmaci.s))
        PoljeUnosa(
            vrijednost = email,
            onPromjena = {},
            oznaka = stringResource(R.string.prof_urp_email),
            pomoc = stringResource(R.string.prof_urp_email_pomoc),
            enabled = false,
            tipkovnica = KeyboardType.Email
        )

        NaslovSekcije(stringResource(R.string.prof_urp_spol))
        IzborOpcije(
            opcije = listOf(
                Opcija("M", stringResource(R.string.prof_urp_musko)),
                Opcija("Z", stringResource(R.string.prof_urp_zensko))
            ),
            odabrana = spol,
            onOdabir = onSpol
        )

        GumbSpremi(
            tekst = stringResource(R.string.prof_urp_spremi),
            onClick = {
                fokus.clearFocus()
                onSpremi()
            },
            enabled = mozeSpremiti,
            modifier = Modifier.padding(top = MacroRazmaci.iznadSekcije)
        )

        Crossfade(
            targetState = greska,
            animationSpec = if (smanjeno) snap() else tween(Trajanje.kratko),
            label = "greskaProfila"
        ) { tekst ->
            tekst?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = shema.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = MacroRazmaci.m)
                        .semantics { liveRegion = LiveRegionMode.Polite }
                )
            }
        }
    }
}

@Composable
private fun NaslovSekcije(tekst: String, prva: Boolean = false) {
    Text(
        text = tekst,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .padding(
                top = if (prva) MacroRazmaci.xl else MacroRazmaci.iznadSekcije,
                bottom = MacroRazmaci.ispodNaslovaSekcije
            )
            .semantics { heading() }
    )
}

// ---------- Previewi ----------

@Composable
private fun PreviewOkvir(
    tamna: Boolean,
    ime: String = "",
    prezime: String = "",
    email: String = "",
    spol: String = "M",
    greskaIme: Boolean = false,
    greska: String? = null
) {
    MacroMenzaTheme(darkTheme = tamna) {
        UrediProfilSadrzaj(
            ime = ime,
            prezime = prezime,
            email = email,
            spol = spol,
            greskaIme = greskaIme,
            greska = greska,
            mozeSpremiti = true,
            onIme = {},
            onPrezime = {},
            onSpol = {},
            onNatrag = {},
            onSpremi = {}
        )
    }
}

private const val VISINA_PREVIEWA = 900

@Preview(name = "Svijetla · prazno", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewSvijetlaPrazno() = PreviewOkvir(false)

@Preview(name = "Tamna · prazno", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewTamnaPrazno() = PreviewOkvir(true)

@Preview(name = "Svijetla · popunjeno", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewSvijetlaPopunjeno() = PreviewOkvir(false, "Ana", "Marić", "ana.maric@fesb.hr", "Z")

@Preview(name = "Tamna · popunjeno", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewTamnaPopunjeno() = PreviewOkvir(true, "Ivan", "Perić", "ivan.peric@fesb.hr", "M")

@Preview(name = "Svijetla · greška", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewSvijetlaGreska() = PreviewOkvir(false, "", "Marić", "ana.maric@fesb.hr", "Z", greskaIme = true)

@Preview(name = "Tamna · greška", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewTamnaGreska() =
    PreviewOkvir(true, "Ivan", "Perić", "ivan.peric@fesb.hr", greska = "Failed to connect to /10.0.2.2:8080")

@Preview(name = "Svijetla · prazno · fontScale 1,3", widthDp = 360, heightDp = 1200, fontScale = 1.3f)
@Composable
private fun PreviewVelikiFontPrazno() = PreviewOkvir(false)

@Preview(name = "Tamna · popunjeno · fontScale 1,3", widthDp = 360, heightDp = 1200, fontScale = 1.3f)
@Composable
private fun PreviewVelikiFontPopunjeno() =
    PreviewOkvir(true, "Ana-Marija", "Kovačević Radić", "ana-marija.kovacevic@fesb.hr", "Z")

@Preview(name = "Svijetla · greška · fontScale 1,3", widthDp = 360, heightDp = 1200, fontScale = 1.3f)
@Composable
private fun PreviewVelikiFontGreska() = PreviewOkvir(false, "", "Marić", "ana.maric@fesb.hr", "Z", greskaIme = true)
