package com.niko.macromenza.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import com.niko.macromenza.R
import com.niko.macromenza.api.RetrofitInstance
import com.niko.macromenza.model.IzracunPreporuke
import com.niko.macromenza.model.IzracunPreporukeRequest
import com.niko.macromenza.model.RegistracijaProfilRequest
import com.niko.macromenza.session.UserSessionManager
import com.niko.macromenza.ui.components.GumbSpremi
import com.niko.macromenza.ui.components.IzborOpcije
import com.niko.macromenza.ui.components.Opcija
import com.niko.macromenza.ui.components.PoljeUnosa
import com.niko.macromenza.ui.components.screenBottomPadding
import com.niko.macromenza.ui.components.screenTopPadding
import com.niko.macromenza.ui.components.skalaPritiska
import com.niko.macromenza.ui.theme.MacroDimenzije
import com.niko.macromenza.ui.theme.MacroMenzaTheme
import com.niko.macromenza.ui.theme.MacroOblici
import com.niko.macromenza.ui.theme.MacroRazmaci
import com.niko.macromenza.util.filtrirajUnosMase
import com.niko.macromenza.util.parsirajMasu
import kotlinx.coroutines.launch

/** Unos forme; spol, aktivnost i cilj su ključevi koje očekuje backend. */
@Immutable
private data class OnboardingForma(
    val ime: String = "",
    val prezime: String = "",
    val spol: String = "M",
    val visina: String = "",
    val dob: String = "",
    val masa: String = "",
    val aktivnost: String = "umjerena",
    val cilj: String = "odrzavanje"
) {
    /** Pregled izračuna vrijedi samo za unose s kojima je dobiven; ime i prezime na njega ne utječu. */
    val unosiIzracuna: String get() = "$spol|$visina|$dob|${masa.trim()}|$aktivnost|$cilj"
}

/** Validacija staje na prvoj grešci (redom: visina, dob, masa), kao i prije. */
private enum class GreskaOnboarding { VISINA, DOB, MASA }

@Composable
fun OnboardingScreen(
    navController: NavController,
    korisnikId: Long
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val sessionManager = remember {
        UserSessionManager(context)
    }

    var forma by remember { mutableStateOf(OnboardingForma()) }
    var greska by remember { mutableStateOf<GreskaOnboarding?>(null) }

    var ucitavanje by remember { mutableStateOf(false) }
    var poruka by remember { mutableStateOf<Int?>(null) }

    // Pregled izračuna vrijedi samo za unose s kojima je dobiven; promjena bilo kojeg polja ga skriva.
    var izracun by remember { mutableStateOf<IzracunPreporuke?>(null) }
    var izracunUnosi by remember { mutableStateOf<String?>(null) }
    var racunanje by remember { mutableStateOf(false) }
    var porukaIzracuna by remember { mutableStateOf<Int?>(null) }

    // Nakon brisanja podataka račun postoji pa se ime i prezime ne moraju ponovno upisivati.
    LaunchedEffect(korisnikId) {
        try {
            val korisnik = RetrofitInstance.api.dohvatiKorisnika(korisnikId)
            if (forma.ime.isBlank() && korisnik.ime.isNotBlank() && !korisnik.ime.equals("EMPTY", true)) {
                forma = forma.copy(ime = korisnik.ime)
            }
            if (forma.prezime.isBlank() && korisnik.prezime.isNotBlank() && !korisnik.prezime.equals("EMPTY", true)) {
                forma = forma.copy(prezime = korisnik.prezime)
            }
        } catch (_: Exception) {
        }
    }

    val prikazaniIzracun = izracun?.takeIf { izracunUnosi == forma.unosiIzracuna }

    OnboardingSadrzaj(
        forma = forma,
        greska = greska,
        izracun = prikazaniIzracun,
        racunanje = racunanje,
        sprema = ucitavanje,
        porukaIzracuna = porukaIzracuna?.let { stringResource(it) },
        poruka = poruka?.let { stringResource(it) },
        onForma = { nova ->
            // Greška polja nestaje čim se to polje promijeni.
            greska = when {
                greska == GreskaOnboarding.VISINA && nova.visina != forma.visina -> null
                greska == GreskaOnboarding.DOB && nova.dob != forma.dob -> null
                greska == GreskaOnboarding.MASA && nova.masa != forma.masa -> null
                else -> greska
            }
            forma = nova
        },
        onPokaziIzracun = {
            val visinaBroj = forma.visina.toIntOrNull()
            val dobBroj = forma.dob.toIntOrNull()
            val masaBroj = parsirajMasu(forma.masa)

            greska = prvaGreska(visinaBroj, dobBroj, masaBroj)

            if (greska == null && visinaBroj != null && dobBroj != null && masaBroj != null) {
                val unosi = forma.unosiIzracuna
                scope.launch {
                    racunanje = true
                    porukaIzracuna = null
                    try {
                        izracun = RetrofitInstance.api.pregledPreporuke(
                            IzracunPreporukeRequest(
                                spol = forma.spol,
                                dob = dobBroj,
                                visina = visinaBroj,
                                masa = masaBroj,
                                razinaAktivnosti = forma.aktivnost,
                                tipCilja = forma.cilj
                            )
                        )
                        izracunUnosi = unosi
                    } catch (e: Exception) {
                        porukaIzracuna = R.string.onb_greska_izracun
                    } finally {
                        racunanje = false
                    }
                }
            }
        },
        onZavrsi = {
            val visinaBroj = forma.visina.toIntOrNull()
            val dobBroj = forma.dob.toIntOrNull()
            val masaBroj = parsirajMasu(forma.masa)

            greska = prvaGreska(visinaBroj, dobBroj, masaBroj)

            if (greska == null && visinaBroj != null && dobBroj != null && masaBroj != null) {
                scope.launch {
                    ucitavanje = true
                    poruka = null

                    try {
                        val korisnik =
                            RetrofitInstance.api.dohvatiKorisnika(korisnikId)

                        RetrofitInstance.api.registracijaProfil(
                            RegistracijaProfilRequest(
                                supabaseUid = korisnik.supabaseUid ?: "",
                                email = korisnik.email,
                                ime = forma.ime.trim(),
                                prezime = forma.prezime.trim(),
                                spol = forma.spol,
                                visina = visinaBroj,
                                dob = dobBroj,
                                masa = masaBroj,
                                razinaAktivnosti = forma.aktivnost,
                                tipCilja = forma.cilj
                            )
                        )

                        sessionManager.spremiSesiju(
                            korisnikId = korisnikId,
                            supabaseUid = korisnik.supabaseUid ?: ""
                        )

                        navController.navigate("home") {
                            popUpTo("onboarding/$korisnikId") {
                                inclusive = true
                            }
                        }

                    } catch (e: Exception) {
                        poruka = R.string.onb_greska_spremanje
                    } finally {
                        ucitavanje = false
                    }
                }
            }
        }
    )
}

private fun prvaGreska(visina: Int?, dob: Int?, masa: Double?): GreskaOnboarding? = when {
    visina == null || visina !in 100..250 -> GreskaOnboarding.VISINA
    dob == null || dob !in 15..100 -> GreskaOnboarding.DOB
    masa == null || masa !in 20.0..400.0 -> GreskaOnboarding.MASA
    else -> null
}

@Composable
private fun OnboardingSadrzaj(
    forma: OnboardingForma,
    greska: GreskaOnboarding?,
    izracun: IzracunPreporuke?,
    racunanje: Boolean,
    sprema: Boolean,
    porukaIzracuna: String?,
    poruka: String?,
    onForma: (OnboardingForma) -> Unit,
    onPokaziIzracun: () -> Unit,
    onZavrsi: () -> Unit
) {
    val shema = MaterialTheme.colorScheme
    val tip = MaterialTheme.typography
    val fokus = LocalFocusManager.current

    val spolovi = listOf(
        Opcija("M", stringResource(R.string.onb_musko)),
        Opcija("Z", stringResource(R.string.onb_zensko))
    )
    val aktivnosti = listOf(
        Opcija("sjedilacka", stringResource(R.string.onb_akt_sjedilacka), stringResource(R.string.onb_akt_sjedilacka_opis)),
        Opcija("lagana", stringResource(R.string.onb_akt_lagana), stringResource(R.string.onb_akt_lagana_opis)),
        Opcija("umjerena", stringResource(R.string.onb_akt_umjerena), stringResource(R.string.onb_akt_umjerena_opis)),
        Opcija("visoka", stringResource(R.string.onb_akt_visoka), stringResource(R.string.onb_akt_visoka_opis))
    )
    val ciljevi = listOf(
        Opcija("mrsavljenje", stringResource(R.string.onb_cilj_mrsavljenje), stringResource(R.string.onb_cilj_mrsavljenje_opis)),
        Opcija("odrzavanje", stringResource(R.string.onb_cilj_odrzavanje), stringResource(R.string.onb_cilj_odrzavanje_opis)),
        Opcija("dobivanje_mase", stringResource(R.string.onb_cilj_dobivanje_mase), stringResource(R.string.onb_cilj_dobivanje_mase_opis))
    )

    val brojeviPopunjeni = forma.visina.toIntOrNull() != null &&
        forma.dob.toIntOrNull() != null &&
        parsirajMasu(forma.masa) != null
    val masaZaPrikaz = parsirajMasu(forma.masa)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(shema.background)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MacroRazmaci.rubEkrana)
            .padding(bottom = screenBottomPadding(hasBottomNavigation = false))
    ) {
        Text(
            text = stringResource(R.string.onb_naslov),
            style = tip.headlineSmall,
            color = shema.onSurface,
            modifier = Modifier
                .padding(top = screenTopPadding() + MacroRazmaci.xl)
                .semantics { heading() }
        )
        Text(
            text = stringResource(R.string.onb_podnaslov),
            style = tip.bodyLarge,
            color = shema.onSurfaceVariant,
            modifier = Modifier.padding(top = MacroRazmaci.xs)
        )

        NaslovSekcijeOnboarding(stringResource(R.string.onb_osobni_podaci), prva = true)
        PoljeUnosa(
            vrijednost = forma.ime,
            onPromjena = { onForma(forma.copy(ime = it)) },
            oznaka = stringResource(R.string.onb_ime),
            velikaSlova = KeyboardCapitalization.Words
        )
        Spacer(Modifier.height(MacroRazmaci.s))
        PoljeUnosa(
            vrijednost = forma.prezime,
            onPromjena = { onForma(forma.copy(prezime = it)) },
            oznaka = stringResource(R.string.onb_prezime),
            velikaSlova = KeyboardCapitalization.Words
        )

        NaslovSekcijeOnboarding(stringResource(R.string.onb_spol))
        IzborOpcije(
            opcije = spolovi,
            odabrana = forma.spol,
            onOdabir = { onForma(forma.copy(spol = it)) }
        )

        NaslovSekcijeOnboarding(stringResource(R.string.onb_mjere))
        PoljeUnosa(
            vrijednost = forma.visina,
            onPromjena = { unos -> if (unos.all { it.isDigit() }) onForma(forma.copy(visina = unos)) },
            oznaka = stringResource(R.string.onb_visina),
            pomoc = stringResource(R.string.onb_visina_pomoc),
            greska = if (greska == GreskaOnboarding.VISINA) stringResource(R.string.onb_greska_visina) else null,
            tipkovnica = KeyboardType.Number
        )
        Spacer(Modifier.height(MacroRazmaci.s))
        PoljeUnosa(
            vrijednost = forma.dob,
            onPromjena = { unos -> if (unos.all { it.isDigit() }) onForma(forma.copy(dob = unos)) },
            oznaka = stringResource(R.string.onb_dob),
            pomoc = stringResource(R.string.onb_dob_pomoc),
            greska = if (greska == GreskaOnboarding.DOB) stringResource(R.string.onb_greska_dob) else null,
            tipkovnica = KeyboardType.Number
        )
        Spacer(Modifier.height(MacroRazmaci.s))
        PoljeUnosa(
            vrijednost = forma.masa,
            onPromjena = { unos -> filtrirajUnosMase(unos)?.let { onForma(forma.copy(masa = it)) } },
            oznaka = stringResource(R.string.onb_masa),
            pomoc = stringResource(R.string.onb_masa_pomoc),
            greska = if (greska == GreskaOnboarding.MASA) stringResource(R.string.onb_greska_masa) else null,
            tipkovnica = KeyboardType.Decimal,
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(onDone = { fokus.clearFocus() })
        )

        NaslovSekcijeOnboarding(stringResource(R.string.onb_aktivnost))
        IzborOpcije(
            opcije = aktivnosti,
            odabrana = forma.aktivnost,
            onOdabir = { onForma(forma.copy(aktivnost = it)) }
        )

        NaslovSekcijeOnboarding(stringResource(R.string.onb_cilj))
        IzborOpcije(
            opcije = ciljevi,
            odabrana = forma.cilj,
            onOdabir = { onForma(forma.copy(cilj = it)) }
        )

        if (izracun != null && masaZaPrikaz != null) {
            KakoSmoIzracunaliKartica(
                izracun = izracun,
                masa = masaZaPrikaz,
                modifier = Modifier.padding(top = MacroRazmaci.iznadSekcije)
            )
        } else {
            val izvor = remember { MutableInteractionSource() }
            OutlinedButton(
                onClick = {
                    fokus.clearFocus()
                    onPokaziIzracun()
                },
                enabled = brojeviPopunjeni && !racunanje && !sprema,
                interactionSource = izvor,
                shape = MacroOblici.gumb,
                border = BorderStroke(MacroDimenzije.linija, shema.outline),
                modifier = Modifier
                    .padding(top = MacroRazmaci.iznadSekcije)
                    .fillMaxWidth()
                    .heightIn(min = MacroDimenzije.fabVisina)
                    .skalaPritiska(izvor)
            ) {
                Text(
                    text = stringResource(if (racunanje) R.string.onb_racunam else R.string.onb_pokazi_izracun),
                    style = tip.labelLarge,
                    color = shema.onSurface,
                    textAlign = TextAlign.Center
                )
            }
            TihaPoruka(tekst = porukaIzracuna, uspjeh = false)
        }

        GumbSpremi(
            tekst = stringResource(R.string.onb_zavrsi),
            onClick = {
                fokus.clearFocus()
                onZavrsi()
            },
            enabled = forma.ime.isNotBlank() && forma.prezime.isNotBlank() && brojeviPopunjeni,
            sprema = sprema,
            modifier = Modifier.padding(top = MacroRazmaci.xl)
        )
        TihaPoruka(tekst = poruka, uspjeh = false)
    }
}

/** Naslov sekcije forme (headlineSmall); prva sekcija stoji bliže zaglavlju. */
@Composable
private fun NaslovSekcijeOnboarding(tekst: String, prva: Boolean = false) {
    Text(
        text = tekst,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .padding(
                top = if (prva) MacroRazmaci.xxl else MacroRazmaci.iznadSekcije,
                bottom = MacroRazmaci.ispodNaslovaSekcije
            )
            .semantics { heading() }
    )
}

// ---------- Previewi ----------

private val PRIMJER_FORMA = OnboardingForma(
    ime = "Marko",
    prezime = "Horvat",
    spol = "M",
    visina = "182",
    dob = "22",
    masa = "78,4",
    aktivnost = "lagana",
    cilj = "mrsavljenje"
)

private val PRIMJER_IZRACUN = IzracunPreporuke(
    bmr = 1833.0,
    bmrFormula = "10 × 78,4 + 6,25 × 182 − 5 × 22 + 5",
    faktorAktivnosti = 1.375,
    tdee = 2520.0,
    korekcijaCilja = -500.0,
    ciljKalorije = 2020,
    proteini = 157,
    masti = 56,
    ugljikohidrati = 222
)

@Composable
private fun PreviewOkvir(
    tamna: Boolean,
    forma: OnboardingForma = OnboardingForma(),
    greska: GreskaOnboarding? = null,
    izracun: IzracunPreporuke? = null,
    sprema: Boolean = false
) {
    MacroMenzaTheme(darkTheme = tamna) {
        OnboardingSadrzaj(
            forma = forma,
            greska = greska,
            izracun = izracun,
            racunanje = false,
            sprema = sprema,
            porukaIzracuna = null,
            poruka = null,
            onForma = {},
            onPokaziIzracun = {},
            onZavrsi = {}
        )
    }
}

private const val VISINA_PREVIEWA = 1900

@Preview(name = "Svijetla · prazno", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewSvijetlaPrazno() = PreviewOkvir(false)

@Preview(name = "Tamna · prazno", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewTamnaPrazno() = PreviewOkvir(true)

@Preview(name = "Svijetla · popunjeno", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewSvijetlaPopunjeno() = PreviewOkvir(false, PRIMJER_FORMA)

@Preview(name = "Tamna · popunjeno", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewTamnaPopunjeno() = PreviewOkvir(true, PRIMJER_FORMA)

@Preview(name = "Svijetla · greška u polju", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewSvijetlaGreska() = PreviewOkvir(false, PRIMJER_FORMA.copy(masa = "18"), GreskaOnboarding.MASA)

@Preview(name = "Tamna · greška u polju", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewTamnaGreska() = PreviewOkvir(true, PRIMJER_FORMA.copy(visina = "82"), GreskaOnboarding.VISINA)

@Preview(name = "Svijetla · prikazan izračun", widthDp = 360, heightDp = 2500)
@Composable
private fun PreviewSvijetlaIzracun() = PreviewOkvir(false, PRIMJER_FORMA, izracun = PRIMJER_IZRACUN)

@Preview(name = "Tamna · prikazan izračun", widthDp = 360, heightDp = 2500)
@Composable
private fun PreviewTamnaIzracun() = PreviewOkvir(true, PRIMJER_FORMA, izracun = PRIMJER_IZRACUN)

@Preview(name = "Svijetla · spremanje u tijeku", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewSvijetlaSprema() = PreviewOkvir(false, PRIMJER_FORMA, sprema = true)

@Preview(name = "Tamna · spremanje u tijeku", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewTamnaSprema() = PreviewOkvir(true, PRIMJER_FORMA, sprema = true)

@Preview(name = "Svijetla · prazno · fontScale 1,3", widthDp = 360, heightDp = 2400, fontScale = 1.3f)
@Composable
private fun PreviewVelikiFontPrazno() = PreviewOkvir(false)

@Preview(name = "Tamna · prikazan izračun · fontScale 1,3", widthDp = 360, heightDp = 3200, fontScale = 1.3f)
@Composable
private fun PreviewVelikiFontIzracun() = PreviewOkvir(true, PRIMJER_FORMA, izracun = PRIMJER_IZRACUN)

@Preview(name = "Svijetla · greška · fontScale 1,3", widthDp = 360, heightDp = 2400, fontScale = 1.3f)
@Composable
private fun PreviewVelikiFontGreska() = PreviewOkvir(false, PRIMJER_FORMA.copy(dob = "9"), GreskaOnboarding.DOB)

@Preview(name = "Tamna · spremanje · fontScale 1,3", widthDp = 360, heightDp = 2400, fontScale = 1.3f)
@Composable
private fun PreviewVelikiFontSprema() = PreviewOkvir(true, PRIMJER_FORMA, sprema = true)
