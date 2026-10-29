package com.niko.macromenza.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.niko.macromenza.R
import com.niko.macromenza.model.IzracunPreporuke
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
import com.niko.macromenza.util.filtrirajUnosMase
import com.niko.macromenza.util.parsirajMasu
import com.niko.macromenza.util.uString
import com.niko.macromenza.viewmodel.CiljeviViewModel


/** Unos forme; vrijednosti tipa cilja i aktivnosti su ključevi koje očekuje backend. */
@Immutable
data class CiljForma(
    val visina: String = "",
    val dob: String = "",
    val masa: String = "",
    val tipCilja: String = "odrzavanje",
    val aktivnost: String = "umjerena"
)

/** Validacija staje na prvoj grešci (redom: korisnik, visina, dob, masa), kao i prije. */
enum class GreskaCilja { KORISNIK, VISINA, DOB, MASA }

@Composable
fun PostavkeCiljevaScreen(
    navController: NavController,
    viewModel: CiljeviViewModel = viewModel()
) {
    val poruka by viewModel.poruka.collectAsState()
    val objasnjenje by viewModel.objasnjenje.collectAsState()

    val context = LocalContext.current
    val sessionManager = remember { UserSessionManager(context) }
    val prijavljeniKorisnikId by sessionManager.korisnikId.collectAsState(initial = null)

    LaunchedEffect(prijavljeniKorisnikId) {
        prijavljeniKorisnikId?.let { viewModel.ucitajObjasnjenje(it) }
    }

    var forma by rememberSaveable(stateSaver = CiljFormaSaver) { mutableStateOf(CiljForma()) }
    var greska by remember { mutableStateOf<GreskaCilja?>(null) }

    PostavkeCiljevaSadrzaj(
        forma = forma,
        greska = greska,
        poruka = poruka?.uString(),
        objasnjenje = objasnjenje,
        onForma = { nova ->
            // Greška polja nestaje čim se to polje promijeni.
            greska = when {
                greska == GreskaCilja.VISINA && nova.visina != forma.visina -> null
                greska == GreskaCilja.DOB && nova.dob != forma.dob -> null
                greska == GreskaCilja.MASA && nova.masa != forma.masa -> null
                else -> greska
            }
            forma = nova
        },
        onNatrag = { navController.popBackStack() },
        onSpremi = {
            val visinaBroj = forma.visina.toIntOrNull()
            val dobBroj = forma.dob.toIntOrNull()
            val masaBroj = parsirajMasu(forma.masa)
            val id = prijavljeniKorisnikId

            greska = when {
                id == null -> GreskaCilja.KORISNIK
                visinaBroj == null || visinaBroj !in 100..250 -> GreskaCilja.VISINA
                dobBroj == null || dobBroj !in 15..100 -> GreskaCilja.DOB
                masaBroj == null || masaBroj !in 20.0..400.0 -> GreskaCilja.MASA
                else -> null
            }

            if (greska == null && id != null && visinaBroj != null && dobBroj != null && masaBroj != null) {
                viewModel.spremiCilj(
                    idKorisnik = id,
                    visina = visinaBroj,
                    dob = dobBroj,
                    masa = masaBroj,
                    razinaAktivnosti = forma.aktivnost,
                    tipCilja = forma.tipCilja
                )
            }
        }
    )
}

@Composable
private fun PostavkeCiljevaSadrzaj(
    forma: CiljForma,
    greska: GreskaCilja?,
    poruka: String?,
    objasnjenje: CiljeviViewModel.Objasnjenje?,
    onForma: (CiljForma) -> Unit,
    onNatrag: () -> Unit,
    onSpremi: () -> Unit
) {
    val shema = MaterialTheme.colorScheme
    val tip = MaterialTheme.typography
    val fokus = LocalFocusManager.current
    val smanjeno = rememberSmanjeniPokret()

    val ciljevi = listOf(
        Opcija("mrsavljenje", stringResource(R.string.prof_cilj_mrsavljenje), stringResource(R.string.prof_cilj_mrsavljenje_opis)),
        Opcija("odrzavanje", stringResource(R.string.prof_cilj_odrzavanje), stringResource(R.string.prof_cilj_odrzavanje_opis)),
        Opcija("dobivanje_mase", stringResource(R.string.prof_cilj_dobivanje_mase), stringResource(R.string.prof_cilj_dobivanje_mase_opis))
    )
    val aktivnosti = listOf(
        Opcija("sjedilacka", stringResource(R.string.prof_akt_sjedilacka), stringResource(R.string.prof_akt_sjedilacka_opis)),
        Opcija("lagana", stringResource(R.string.prof_akt_lagana), stringResource(R.string.prof_akt_lagana_opis)),
        Opcija("umjerena", stringResource(R.string.prof_akt_umjerena), stringResource(R.string.prof_akt_umjerena_opis)),
        Opcija("visoka", stringResource(R.string.prof_akt_visoka), stringResource(R.string.prof_akt_visoka_opis))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(shema.background)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MacroRazmaci.rubEkrana)
            .padding(bottom = screenBottomPadding())
    ) {
        ZaglavljePodekrana(naslov = stringResource(R.string.prof_cilj_naslov), onNatrag = onNatrag)

        NaslovSekcijeForme(stringResource(R.string.prof_cilj_podaci), prva = true)
        PoljeUnosa(
            vrijednost = forma.visina,
            onPromjena = { unos -> if (unos.all { it.isDigit() }) onForma(forma.copy(visina = unos)) },
            oznaka = stringResource(R.string.prof_cilj_visina),
            pomoc = stringResource(R.string.prof_cilj_visina_pomoc),
            greska = if (greska == GreskaCilja.VISINA) stringResource(R.string.prof_cilj_greska_visina) else null,
            tipkovnica = KeyboardType.Number
        )
        Spacer(Modifier.height(MacroRazmaci.s))
        PoljeUnosa(
            vrijednost = forma.dob,
            onPromjena = { unos -> if (unos.all { it.isDigit() }) onForma(forma.copy(dob = unos)) },
            oznaka = stringResource(R.string.prof_cilj_dob),
            pomoc = stringResource(R.string.prof_cilj_dob_pomoc),
            greska = if (greska == GreskaCilja.DOB) stringResource(R.string.prof_cilj_greska_dob) else null,
            tipkovnica = KeyboardType.Number
        )
        Spacer(Modifier.height(MacroRazmaci.s))
        PoljeUnosa(
            vrijednost = forma.masa,
            onPromjena = { unos -> filtrirajUnosMase(unos)?.let { onForma(forma.copy(masa = it)) } },
            oznaka = stringResource(R.string.prof_cilj_masa),
            pomoc = stringResource(R.string.prof_cilj_masa_pomoc),
            greska = if (greska == GreskaCilja.MASA) stringResource(R.string.prof_cilj_greska_masa) else null,
            tipkovnica = KeyboardType.Decimal,
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(onDone = { fokus.clearFocus() })
        )

        NaslovSekcijeForme(stringResource(R.string.prof_tvoj_cilj))
        IzborOpcije(
            opcije = ciljevi,
            odabrana = forma.tipCilja,
            onOdabir = { onForma(forma.copy(tipCilja = it)) }
        )

        NaslovSekcijeForme(stringResource(R.string.prof_cilj_aktivnost))
        IzborOpcije(
            opcije = aktivnosti,
            odabrana = forma.aktivnost,
            onOdabir = { onForma(forma.copy(aktivnost = it)) }
        )

        GumbSpremi(
            tekst = stringResource(R.string.prof_cilj_spremi),
            onClick = {
                fokus.clearFocus()
                onSpremi()
            },
            modifier = Modifier.padding(top = MacroRazmaci.iznadSekcije)
        )

        // Greška bez polja (korisnik) i poruka spremanja ispod gumba; izblijede pri promjeni.
        // Poruka uspjeha iz [CiljeviViewModel.spremiCilj]; sve ostale poruke su greške.
        val porukaUspjeha = stringResource(R.string.prof_cilj_spremljen)
        val obavijest = when {
            greska == GreskaCilja.KORISNIK -> stringResource(R.string.prof_cilj_greska_korisnik) to true
            poruka != null -> poruka to (poruka != porukaUspjeha)
            else -> null
        }
        Crossfade(
            targetState = obavijest,
            animationSpec = if (smanjeno) snap() else tween(Trajanje.kratko),
            label = "porukaCilja"
        ) { stanje ->
            stanje?.let { (tekst, jeGreska) ->
                Text(
                    text = tekst,
                    style = tip.bodyMedium,
                    color = if (jeGreska) shema.error else shema.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = MacroRazmaci.m)
                        .semantics { liveRegion = LiveRegionMode.Polite }
                )
            }
        }

        Text(
            text = stringResource(R.string.prof_cilj_okvirno),
            style = tip.bodySmall,
            color = shema.onSurfaceVariant,
            modifier = Modifier.padding(top = MacroRazmaci.m)
        )

        objasnjenje?.let {
            KakoSmoIzracunaliKartica(
                izracun = it.izracun,
                masa = it.masa,
                naslov = stringResource(R.string.prof_cilj_kako_je_izracunato),
                modifier = Modifier.padding(top = MacroRazmaci.iznadSekcije)
            )
        }
    }
}

/** Naslov sekcije forme (headlineSmall); prva sekcija stoji bliže zaglavlju. */
@Composable
private fun NaslovSekcijeForme(tekst: String, prva: Boolean = false) {
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

private val CiljFormaSaver = androidx.compose.runtime.saveable.listSaver<CiljForma, String>(
    save = { listOf(it.visina, it.dob, it.masa, it.tipCilja, it.aktivnost) },
    restore = { CiljForma(it[0], it[1], it[2], it[3], it[4]) }
)

// ---------- Previewi ----------

private val PRIMJER_FORMA = CiljForma(visina = "182", dob = "22", masa = "78,4", tipCilja = "mrsavljenje", aktivnost = "lagana")

private val PRIMJER_OBJASNJENJE = CiljeviViewModel.Objasnjenje(
    izracun = IzracunPreporuke(
        bmr = 1833.0,
        bmrFormula = "10 × 78,4 + 6,25 × 182 − 5 × 22 + 5",
        faktorAktivnosti = 1.375,
        tdee = 2520.0,
        korekcijaCilja = -500.0,
        ciljKalorije = 2020,
        proteini = 157,
        masti = 56,
        ugljikohidrati = 222
    ),
    masa = 78.4
)

@Composable
private fun PreviewOkvir(
    tamna: Boolean,
    forma: CiljForma = CiljForma(),
    greska: GreskaCilja? = null,
    poruka: String? = null,
    objasnjenje: CiljeviViewModel.Objasnjenje? = null
) {
    MacroMenzaTheme(darkTheme = tamna) {
        PostavkeCiljevaSadrzaj(
            forma = forma,
            greska = greska,
            poruka = poruka,
            objasnjenje = objasnjenje,
            onForma = {},
            onNatrag = {},
            onSpremi = {}
        )
    }
}

private const val VISINA_PREVIEWA = 1800

@Preview(name = "Svijetla · prazno", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewSvijetlaPrazno() = PreviewOkvir(false)

@Preview(name = "Tamna · prazno", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewTamnaPrazno() = PreviewOkvir(true)

@Preview(name = "Svijetla · popunjeno", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewSvijetlaPopunjeno() = PreviewOkvir(false, PRIMJER_FORMA, poruka = stringResource(R.string.prof_cilj_spremljen), objasnjenje = PRIMJER_OBJASNJENJE)

@Preview(name = "Tamna · popunjeno", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewTamnaPopunjeno() = PreviewOkvir(true, PRIMJER_FORMA, poruka = stringResource(R.string.prof_cilj_spremljen), objasnjenje = PRIMJER_OBJASNJENJE)

@Preview(name = "Svijetla · greška", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewSvijetlaGreska() = PreviewOkvir(false, PRIMJER_FORMA.copy(masa = "18"), GreskaCilja.MASA)

@Preview(name = "Tamna · greška", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewTamnaGreska() = PreviewOkvir(true, PRIMJER_FORMA.copy(visina = "82"), GreskaCilja.VISINA)

@Preview(name = "Svijetla · prazno · fontScale 1,3", widthDp = 360, heightDp = 2200, fontScale = 1.3f)
@Composable
private fun PreviewVelikiFontPrazno() = PreviewOkvir(false)

@Preview(name = "Tamna · popunjeno · fontScale 1,3", widthDp = 360, heightDp = 2200, fontScale = 1.3f)
@Composable
private fun PreviewVelikiFontPopunjeno() = PreviewOkvir(true, PRIMJER_FORMA, objasnjenje = PRIMJER_OBJASNJENJE)

@Preview(name = "Svijetla · greška · fontScale 1,3", widthDp = 360, heightDp = 2200, fontScale = 1.3f)
@Composable
private fun PreviewVelikiFontGreska() = PreviewOkvir(false, PRIMJER_FORMA.copy(dob = "9"), GreskaCilja.DOB)
