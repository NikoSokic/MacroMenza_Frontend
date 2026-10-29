package com.niko.macromenza.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.niko.macromenza.BuildConfig
import com.niko.macromenza.R
import com.niko.macromenza.api.RetrofitInstance
import com.niko.macromenza.navigation.BottomNavItem
import com.niko.macromenza.session.ThemeManager
import com.niko.macromenza.session.UserSessionManager
import com.niko.macromenza.ui.components.IzborOpcije
import com.niko.macromenza.ui.components.KosturOblik
import com.niko.macromenza.ui.components.Opcija
import com.niko.macromenza.ui.components.PoljeUnosa
import com.niko.macromenza.ui.components.ZaglavljePodekrana
import com.niko.macromenza.ui.components.screenBottomPadding
import com.niko.macromenza.ui.components.skalaPritiska
import com.niko.macromenza.ui.theme.AppTheme
import com.niko.macromenza.ui.theme.MacroDimenzije
import com.niko.macromenza.ui.theme.MacroMenzaTheme
import com.niko.macromenza.ui.theme.MacroOblici
import com.niko.macromenza.ui.theme.MacroRazmaci
import com.niko.macromenza.ui.theme.Trajanje
import com.niko.macromenza.ui.theme.macroBoje
import com.niko.macromenza.ui.theme.rememberSmanjeniPokret
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.HttpException
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Riječ koju korisnik upisuje za potvrdu brisanja; backend prima "OBRISI" (bez dijakritika), kao i prije. */
private const val POTVRDA_BRISANJA = "OBRIŠI"

/**
 * Javni health endpoint Supabase Autha (isti projekt kao u SupabaseAuthInstance). Poziv ne nosi ključ:
 * svaki HTTP odgovor (i 401) znači da servis radi, a prekid veze da je nedostupan.
 */
private const val AUTH_ZDRAVLJE_URL = "https://nncjmtrxderijntybnhb.supabase.co/auth/v1/health"

private const val AUTH_PROVJERA_TIMEOUT_S = 5L

enum class StanjeServisa { PROVJERA, RADI, NEDOSTUPNO }

/** Backend i baza: upit izvora podataka ide u bazu, pa jedan odgovor kaže za oboje. */
private suspend fun provjeriBackend(): Pair<StanjeServisa, StanjeServisa> =
    try {
        RetrofitInstance.api.getIzvoriPodataka()
        StanjeServisa.RADI to StanjeServisa.RADI
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        // Backend je odgovorio greškom: radi, ali baza nije dala podatke.
        StanjeServisa.RADI to StanjeServisa.NEDOSTUPNO
    } catch (e: Exception) {
        // Aplikacija do baze dolazi samo preko backenda, pa bez njega baza nije provjerljiva.
        StanjeServisa.NEDOSTUPNO to StanjeServisa.NEDOSTUPNO
    }

private suspend fun provjeriAuth(): StanjeServisa = withContext(Dispatchers.IO) {
    try {
        val klijent = OkHttpClient.Builder().callTimeout(AUTH_PROVJERA_TIMEOUT_S, TimeUnit.SECONDS).build()
        klijent.newCall(Request.Builder().url(AUTH_ZDRAVLJE_URL).build()).execute().use { StanjeServisa.RADI }
    } catch (e: IOException) {
        StanjeServisa.NEDOSTUPNO
    }
}

@Composable
fun PostavkeAplikacijeScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val themeManager = remember { ThemeManager(context) }
    val scope = rememberCoroutineScope()
    val sessionManager = remember { UserSessionManager(context) }
    val korisnikId by sessionManager.korisnikId.collectAsState(initial = null)

    var prikaziBrisanje by remember { mutableStateOf(false) }
    var potvrdaTekst by remember { mutableStateOf("") }
    var brisanjeUTijeku by remember { mutableStateOf(false) }
    var greskaBrisanja by remember { mutableStateOf<String?>(null) }

    // Dok se tema ne učita nijedna opcija nije odabrana, pa odabir ne treperi s pogrešne vrijednosti.
    val spremljenaTema by themeManager.theme.collectAsState(initial = null)

    var backend by remember { mutableStateOf(StanjeServisa.PROVJERA) }
    var baza by remember { mutableStateOf(StanjeServisa.PROVJERA) }
    var auth by remember { mutableStateOf(StanjeServisa.PROVJERA) }
    var provjera by remember { mutableIntStateOf(0) }
    LaunchedEffect(provjera) {
        backend = StanjeServisa.PROVJERA
        baza = StanjeServisa.PROVJERA
        auth = StanjeServisa.PROVJERA
        launch {
            val (b, d) = provjeriBackend()
            backend = b
            baza = d
        }
        launch { auth = provjeriAuth() }
    }

    PostavkeAplikacijeSadrzaj(
        tema = spremljenaTema,
        backend = backend,
        baza = baza,
        auth = auth,
        adresaBackenda = android.net.Uri.parse(BuildConfig.BASE_URL).authority.orEmpty(),
        brisanjeMoguce = korisnikId != null,
        onNatrag = { navController.popBackStack() },
        onTema = { tema -> scope.launch { themeManager.saveTheme(tema) } },
        onProvjeri = { provjera++ },
        onBrisanje = {
            potvrdaTekst = ""
            greskaBrisanja = null
            prikaziBrisanje = true
        }
    )

    if (prikaziBrisanje) {
        BrisanjePodatakaDialog(
            potvrdaTekst = potvrdaTekst,
            onPotvrdaTekst = { potvrdaTekst = it },
            brisanjeUTijeku = brisanjeUTijeku,
            greska = greskaBrisanja,
            onOdustani = { prikaziBrisanje = false },
            onObrisi = {
                val id = korisnikId ?: return@BrisanjePodatakaDialog

                scope.launch {
                    brisanjeUTijeku = true
                    greskaBrisanja = null

                    try {
                        val odgovor = RetrofitInstance.api.obrisiSvePodatke(id, "OBRISI")

                        if (!odgovor.isSuccessful) {
                            greskaBrisanja = context.getString(R.string.kom_brisanje_greska_http, odgovor.code())
                            return@launch
                        }

                        val onboardingZavrsen = try {
                            RetrofitInstance.api.dohvatiOnboardingStatus(id).onboardingZavrsen
                        } catch (_: Exception) {
                            false
                        }

                        prikaziBrisanje = false

                        if (onboardingZavrsen) {
                            navController.navigate(BottomNavItem.Home.route) {
                                popUpTo(0)
                            }
                        } else {
                            navController.navigate("onboarding/$id") {
                                popUpTo(0)
                            }
                        }
                    } catch (e: Exception) {
                        greskaBrisanja = context.getString(R.string.kom_brisanje_greska_veza)
                    } finally {
                        brisanjeUTijeku = false
                    }
                }
            }
        )
    }
}

@Composable
private fun PostavkeAplikacijeSadrzaj(
    tema: AppTheme?,
    backend: StanjeServisa,
    baza: StanjeServisa,
    auth: StanjeServisa,
    adresaBackenda: String,
    brisanjeMoguce: Boolean,
    onNatrag: () -> Unit,
    onTema: (AppTheme) -> Unit,
    onProvjeri: () -> Unit,
    onBrisanje: () -> Unit
) {
    val shema = MaterialTheme.colorScheme
    val tip = MaterialTheme.typography

    val teme = listOf(
        Opcija<AppTheme?>(AppTheme.SYSTEM, stringResource(R.string.post_tema_sustav), stringResource(R.string.post_tema_sustav_opis)),
        Opcija<AppTheme?>(AppTheme.LIGHT, stringResource(R.string.post_tema_svijetla)),
        Opcija<AppTheme?>(AppTheme.DARK, stringResource(R.string.post_tema_tamna))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(shema.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MacroRazmaci.rubEkrana)
            .padding(bottom = screenBottomPadding())
    ) {
        ZaglavljePodekrana(naslov = stringResource(R.string.post_naslov), onNatrag = onNatrag)

        NaslovSekcije(stringResource(R.string.post_tema), prva = true)
        IzborOpcije(opcije = teme, odabrana = tema, onOdabir = { it?.let(onTema) })

        NaslovSekcije(stringResource(R.string.post_status))
        RedStatusa(stringResource(R.string.post_status_backend), adresaBackenda, backend)
        HorizontalDivider(thickness = MacroDimenzije.linija, color = shema.outlineVariant)
        RedStatusa(stringResource(R.string.post_status_baza), stringResource(R.string.post_status_baza_vrijednost), baza)
        HorizontalDivider(thickness = MacroDimenzije.linija, color = shema.outlineVariant)
        RedStatusa(stringResource(R.string.post_status_auth), stringResource(R.string.post_status_auth_vrijednost), auth)
        val izvor = remember { MutableInteractionSource() }
        TextButton(
            onClick = onProvjeri,
            enabled = listOf(backend, baza, auth).none { it == StanjeServisa.PROVJERA },
            interactionSource = izvor,
            modifier = Modifier
                .heightIn(min = MacroDimenzije.dodirniCilj)
                .skalaPritiska(izvor)
        ) {
            Text(stringResource(R.string.post_status_provjeri), style = tip.labelLarge, color = shema.onSurface)
        }

        NaslovSekcije(stringResource(R.string.post_podaci))
        Text(
            text = stringResource(R.string.post_podaci_opis),
            style = tip.bodyMedium,
            color = shema.onSurfaceVariant
        )
        val izvorBrisanja = remember { MutableInteractionSource() }
        Button(
            onClick = onBrisanje,
            enabled = brisanjeMoguce,
            interactionSource = izvorBrisanja,
            shape = MacroOblici.gumb,
            colors = ButtonDefaults.buttonColors(
                containerColor = shema.secondaryContainer,
                contentColor = shema.error,
                disabledContainerColor = shema.secondaryContainer,
                disabledContentColor = shema.onSurfaceVariant
            ),
            modifier = Modifier
                .padding(top = MacroRazmaci.l)
                .fillMaxWidth()
                .heightIn(min = MacroDimenzije.dodirniCilj)
                .skalaPritiska(izvorBrisanja)
        ) {
            Icon(Icons.Rounded.DeleteForever, contentDescription = null, modifier = Modifier.size(MacroDimenzije.ikona))
            Spacer(Modifier.width(MacroRazmaci.s))
            Text(stringResource(R.string.post_obrisi_podatke), style = tip.labelLarge)
        }

        NaslovSekcije(stringResource(R.string.post_o_aplikaciji))
        OAplikaciji()
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

/** Red servisa: naziv i prigušena vrijednost lijevo, desno marker (ikona ili kostur točke) i tekst stanja. */
@Composable
private fun RedStatusa(naziv: String, vrijednost: String, stanje: StanjeServisa) {
    val shema = MaterialTheme.colorScheme
    val smanjeno = rememberSmanjeniPokret()
    val tekstStanja = stringResource(
        when (stanje) {
            StanjeServisa.PROVJERA -> R.string.post_status_provjera
            StanjeServisa.RADI -> R.string.post_status_radi
            StanjeServisa.NEDOSTUPNO -> R.string.post_status_nedostupno
        }
    )
    val opis = stringResource(R.string.post_status_opis, naziv, tekstStanja)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MacroDimenzije.dodirniCilj)
            .padding(vertical = MacroRazmaci.m)
            .clearAndSetSemantics { contentDescription = opis },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = naziv, style = MaterialTheme.typography.bodyLarge, color = shema.onSurface)
            Text(text = vrijednost, style = MaterialTheme.typography.bodySmall, color = shema.onSurfaceVariant)
        }
        Spacer(Modifier.width(MacroRazmaci.m))
        Crossfade(
            targetState = stanje,
            animationSpec = if (smanjeno) snap() else tween(Trajanje.kratko),
            label = "stanjeServisa"
        ) { s ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (s) {
                    StanjeServisa.PROVJERA -> KosturOblik(Modifier.size(MacroDimenzije.ikonaMala), CircleShape)
                    StanjeServisa.RADI -> Icon(
                        Icons.Rounded.Check, contentDescription = null,
                        tint = MaterialTheme.macroBoje.lukIspuna, modifier = Modifier.size(MacroDimenzije.ikonaMala)
                    )
                    StanjeServisa.NEDOSTUPNO -> Icon(
                        Icons.Rounded.CloudOff, contentDescription = null,
                        tint = shema.error, modifier = Modifier.size(MacroDimenzije.ikonaMala)
                    )
                }
                Spacer(Modifier.width(MacroRazmaci.s))
                Text(
                    text = tekstStanja,
                    style = MaterialTheme.typography.labelLarge,
                    color = when (s) {
                        StanjeServisa.PROVJERA -> shema.onSurfaceVariant
                        StanjeServisa.RADI -> MaterialTheme.macroBoje.lukIspuna
                        StanjeServisa.NEDOSTUPNO -> shema.error
                    }
                )
            }
        }
    }
}

/** „M” u Frauncesu na porculanskom krugu (kao inicijali u Profilu), pa opis i parovi oznaka–vrijednost. */
@Composable
private fun OAplikaciji() {
    val shema = MaterialTheme.colorScheme
    val tip = MaterialTheme.typography
    val verzija = BuildConfig.VERSION_NAME

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(MacroDimenzije.avatarProfila)
                .background(shema.surface, CircleShape)
                .border(MacroDimenzije.linija, shema.outlineVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "M",
                style = tip.headlineSmall,
                color = shema.onSurface,
                modifier = Modifier.clearAndSetSemantics { }
            )
        }
        Spacer(Modifier.width(MacroRazmaci.l))
        Column {
            Text(text = stringResource(R.string.post_o_ime), style = tip.titleMedium, color = shema.onSurface)
            Text(
                text = stringResource(R.string.post_o_verzija, verzija),
                style = tip.bodySmall,
                color = shema.onSurfaceVariant
            )
        }
    }

    Text(
        text = stringResource(R.string.post_o_opis),
        style = tip.bodyMedium,
        color = shema.onSurfaceVariant,
        modifier = Modifier.padding(top = MacroRazmaci.l)
    )

    Column(modifier = Modifier.padding(top = MacroRazmaci.s)) {
        ParOznakaVrijednost(stringResource(R.string.post_o_platforma), stringResource(R.string.post_o_platforma_vrijednost))
        HorizontalDivider(thickness = MacroDimenzije.linija, color = shema.outlineVariant)
        ParOznakaVrijednost(
            stringResource(R.string.post_o_aplikacija),
            stringResource(R.string.post_o_aplikacija_vrijednost, verzija)
        )
    }
}

@Composable
private fun ParOznakaVrijednost(oznaka: String, vrijednost: String) {
    val shema = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MacroDimenzije.dodirniCilj)
            .padding(vertical = MacroRazmaci.m)
            .semantics(mergeDescendants = true) { },
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = oznaka,
            style = MaterialTheme.typography.bodyMedium,
            color = shema.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(MacroRazmaci.m))
        Text(
            text = vrijednost,
            style = MaterialTheme.typography.bodyMedium,
            color = shema.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

// ---------- Dijalog brisanja ----------

@Composable
private fun BrisanjePodatakaDialog(
    potvrdaTekst: String,
    onPotvrdaTekst: (String) -> Unit,
    brisanjeUTijeku: Boolean,
    greska: String?,
    onObrisi: () -> Unit,
    onOdustani: () -> Unit
) {
    Dialog(onDismissRequest = { if (!brisanjeUTijeku) onOdustani() }) {
        Surface(shape = MacroOblici.pladanj, color = MaterialTheme.colorScheme.surface) {
            BrisanjePodatakaSadrzaj(
                potvrdaTekst = potvrdaTekst,
                onPotvrdaTekst = onPotvrdaTekst,
                brisanjeUTijeku = brisanjeUTijeku,
                greska = greska,
                onObrisi = onObrisi,
                onOdustani = onOdustani
            )
        }
    }
}

/** Sadržaj dijaloga bez prozora, da se može pregledati u previewu. */
@Composable
private fun BrisanjePodatakaSadrzaj(
    potvrdaTekst: String,
    onPotvrdaTekst: (String) -> Unit,
    brisanjeUTijeku: Boolean,
    greska: String?,
    onObrisi: () -> Unit,
    onOdustani: () -> Unit
) {
    val shema = MaterialTheme.colorScheme
    val tip = MaterialTheme.typography
    val potvrdeno = potvrdaTekst.trim() == POTVRDA_BRISANJA

    Column(modifier = Modifier.padding(MacroRazmaci.xl)) {
        Text(
            text = stringResource(R.string.post_brisanje_naslov),
            style = tip.headlineSmall,
            color = shema.onSurface,
            modifier = Modifier.semantics { heading() }
        )

        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .padding(top = MacroRazmaci.l)
                .verticalScroll(rememberScrollState())
        ) {
            Text(stringResource(R.string.post_brisanje_brise), style = tip.titleSmall, color = shema.onSurface)
            Natuknica(stringResource(R.string.post_brisanje_obroci))
            Natuknica(stringResource(R.string.post_brisanje_mjerenja))
            Natuknica(stringResource(R.string.post_brisanje_ciljevi))

            Text(
                text = stringResource(R.string.post_brisanje_ostaje),
                style = tip.titleSmall,
                color = shema.onSurface,
                modifier = Modifier.padding(top = MacroRazmaci.m)
            )
            Natuknica(stringResource(R.string.post_brisanje_racun))
            Natuknica(stringResource(R.string.post_brisanje_profil))

            Text(
                text = stringResource(R.string.post_brisanje_upozorenje),
                style = tip.bodySmall,
                color = shema.error,
                modifier = Modifier.padding(top = MacroRazmaci.m)
            )

            PoljeUnosa(
                vrijednost = potvrdaTekst,
                onPromjena = onPotvrdaTekst,
                oznaka = stringResource(R.string.post_brisanje_polje),
                enabled = !brisanjeUTijeku,
                imeAction = ImeAction.Done,
                modifier = Modifier.padding(top = MacroRazmaci.l)
            )

            greska?.let {
                Row(
                    modifier = Modifier.padding(top = MacroRazmaci.s),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.ErrorOutline, contentDescription = null,
                        tint = shema.error, modifier = Modifier.size(MacroDimenzije.ikonaMala)
                    )
                    Spacer(Modifier.width(MacroRazmaci.s))
                    Text(text = it, style = tip.bodySmall, color = shema.error)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = MacroRazmaci.l),
            horizontalArrangement = Arrangement.spacedBy(MacroRazmaci.s, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val izvorOdustani = remember { MutableInteractionSource() }
            TextButton(
                onClick = onOdustani,
                enabled = !brisanjeUTijeku,
                interactionSource = izvorOdustani,
                modifier = Modifier
                    .heightIn(min = MacroDimenzije.dodirniCilj)
                    .skalaPritiska(izvorOdustani)
            ) {
                Text(stringResource(R.string.kom_odustani), style = tip.labelLarge, color = shema.onSurface)
            }
            val izvorObrisi = remember { MutableInteractionSource() }
            Button(
                onClick = onObrisi,
                enabled = potvrdeno && !brisanjeUTijeku,
                interactionSource = izvorObrisi,
                shape = MacroOblici.gumb,
                colors = ButtonDefaults.buttonColors(containerColor = shema.error, contentColor = shema.onError),
                modifier = Modifier
                    .heightIn(min = MacroDimenzije.dodirniCilj)
                    .skalaPritiska(izvorObrisi)
            ) {
                Text(
                    text = stringResource(if (brisanjeUTijeku) R.string.post_brisanje_u_tijeku else R.string.post_brisanje_potvrdi),
                    style = tip.labelLarge
                )
            }
        }
    }
}

/** Natuknica s visećom uvlakom: tekst se prelama ispod teksta, ne ispod točke. */
@Composable
private fun Natuknica(tekst: String) {
    Row(modifier = Modifier.padding(top = MacroRazmaci.xs)) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clearAndSetSemantics { }
        )
        Spacer(Modifier.width(MacroRazmaci.s))
        Text(text = tekst, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

// ---------- Previewi ----------

@Composable
private fun PreviewOkvir(
    tamna: Boolean,
    tema: AppTheme? = AppTheme.SYSTEM,
    backend: StanjeServisa = StanjeServisa.RADI,
    baza: StanjeServisa = StanjeServisa.RADI,
    auth: StanjeServisa = StanjeServisa.RADI
) {
    MacroMenzaTheme(darkTheme = tamna) {
        PostavkeAplikacijeSadrzaj(
            tema = tema,
            backend = backend,
            baza = baza,
            auth = auth,
            adresaBackenda = "10.0.2.2:8080",
            brisanjeMoguce = true,
            onNatrag = {},
            onTema = {},
            onProvjeri = {},
            onBrisanje = {}
        )
    }
}

@Composable
private fun PreviewDijalog(tamna: Boolean, tekst: String, ucitava: Boolean = false, greska: String? = null) {
    MacroMenzaTheme(darkTheme = tamna) {
        Box(Modifier.background(MaterialTheme.colorScheme.background).padding(MacroRazmaci.l)) {
            Surface(shape = MacroOblici.pladanj, color = MaterialTheme.colorScheme.surface) {
                BrisanjePodatakaSadrzaj(
                    potvrdaTekst = tekst,
                    onPotvrdaTekst = {},
                    brisanjeUTijeku = ucitava,
                    greska = greska,
                    onObrisi = {},
                    onOdustani = {}
                )
            }
        }
    }
}

private const val VISINA_PREVIEWA = 1500

@Preview(name = "Svijetla · svi rade", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewSvijetlaRadi() = PreviewOkvir(false)

@Preview(name = "Tamna · svi rade", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewTamnaRadi() = PreviewOkvir(true, AppTheme.DARK)

@Preview(name = "Svijetla · jedan nedostupan", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewSvijetlaNedostupan() = PreviewOkvir(false, AppTheme.LIGHT, auth = StanjeServisa.NEDOSTUPNO)

@Preview(name = "Tamna · backend nedostupan", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewTamnaNedostupan() =
    PreviewOkvir(true, AppTheme.DARK, StanjeServisa.NEDOSTUPNO, StanjeServisa.NEDOSTUPNO, StanjeServisa.RADI)

@Preview(name = "Svijetla · provjera", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewSvijetlaProvjera() =
    PreviewOkvir(false, null, StanjeServisa.PROVJERA, StanjeServisa.PROVJERA, StanjeServisa.PROVJERA)

@Preview(name = "Svijetla · dijalog", widthDp = 360, heightDp = 640)
@Composable
private fun PreviewSvijetlaDijalog() = PreviewDijalog(false, "OBRIŠ")

@Preview(name = "Tamna · dijalog potvrđen", widthDp = 360, heightDp = 640)
@Composable
private fun PreviewTamnaDijalog() = PreviewDijalog(true, "OBRIŠI")

@Preview(name = "Svijetla · dijalog brisanje u tijeku", widthDp = 360, heightDp = 640)
@Composable
private fun PreviewSvijetlaDijalogBrisanje() = PreviewDijalog(false, "OBRIŠI", ucitava = true)

@Preview(name = "Tamna · dijalog greška", widthDp = 360, heightDp = 700)
@Composable
private fun PreviewTamnaDijalogGreska() =
    PreviewDijalog(true, "OBRIŠI", greska = "Brisanje nije uspjelo. Provjeri vezu s poslužiteljem.")

@Preview(name = "Svijetla · jedan nedostupan · fontScale 1,3", widthDp = 360, heightDp = 2000, fontScale = 1.3f)
@Composable
private fun PreviewVelikiFontPostavke() = PreviewOkvir(false, AppTheme.LIGHT, auth = StanjeServisa.NEDOSTUPNO)

@Preview(name = "Tamna · dijalog · fontScale 1,3", widthDp = 360, heightDp = 900, fontScale = 1.3f)
@Composable
private fun PreviewVelikiFontDijalog() = PreviewDijalog(true, "OBRIŠI")
