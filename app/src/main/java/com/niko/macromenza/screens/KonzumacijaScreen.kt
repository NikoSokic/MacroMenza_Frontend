package com.niko.macromenza.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.niko.macromenza.R
import com.niko.macromenza.model.CestoJedenoJelo
import com.niko.macromenza.model.Jelo
import com.niko.macromenza.model.Obrok
import com.niko.macromenza.navigation.BottomNavItem
import com.niko.macromenza.screens.dnevnipregled.naslovDatuma
import com.niko.macromenza.screens.unosobroka.JeloRed
import com.niko.macromenza.screens.unosobroka.NaslovSekcije
import com.niko.macromenza.screens.unosobroka.uMakro
import com.niko.macromenza.session.UserSessionManager
import com.niko.macromenza.ui.components.Makro
import com.niko.macromenza.ui.components.Pladanj
import com.niko.macromenza.ui.components.TanjurUi
import com.niko.macromenza.ui.components.screenBottomPadding
import com.niko.macromenza.ui.components.screenTopPadding
import com.niko.macromenza.ui.components.skalaPritiska
import com.niko.macromenza.ui.theme.MacroDimenzije
import com.niko.macromenza.ui.theme.MacroMenzaTheme
import com.niko.macromenza.ui.theme.MacroOblici
import com.niko.macromenza.ui.theme.MacroRazmaci
import com.niko.macromenza.ui.theme.UlazUSesiji
import com.niko.macromenza.ui.theme.rememberSmanjeniPokret
import com.niko.macromenza.viewmodel.JelaViewModel
import com.niko.macromenza.viewmodel.PovijestViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

private val ZAGREB_UNOS = ZoneId.of("Europe/Zagreb")

/** Redoslijed utora isti kao na dnevnom pregledu: užina stoji između ručka i večere. */
private val TIPOVI_PLADNJA = listOf("DORUCAK", "RUCAK", "UZINA", "VECERA")

/** Prečaci "Često jedeš" ispod pladnja. */
private const val BROJ_PRECACA = 3

/** Tanjuri za pladanj iz obroka dana (zbroj po vrsti, vrijeme prvog obroka te vrste). */
private fun tanjuriDana(obroci: List<Obrok>): List<TanjurUi> = TIPOVI_PLADNJA.map { tip ->
    val tipa = obroci.filter { it.tipObroka.uppercase() == tip }
    TanjurUi(
        tip = tip,
        makro = tipa.takeIf { it.isNotEmpty() }?.fold(Makro.NULA) { a, o ->
            a + Makro(o.ukupnoKalorije, o.ukupnoProteini, o.ukupnoMasti, o.ukupnoUgljikohidrati)
        },
        vrijeme = tipa.firstOrNull()?.vrijeme?.take(5),
        jeUzina = tip == "UZINA"
    )
}

/**
 * Obroci dana dijele se s Home ekranom (isti PovijestViewModel, bez izmjena): Home ih je već učitao, pa pladanj
 * pri ponovnom ulazu odmah pokazuje završno stanje, a animira se samo kad podaci stignu nakon prvog prikaza.
 * Ako Home nije na stogu, kartica koristi vlastiti ViewModel.
 */
@Composable
private fun obrociViewModelIzPocetne(navController: NavController): PovijestViewModel {
    val pocetna = remember(navController) {
        runCatching { navController.getBackStackEntry(BottomNavItem.Home.route) }.getOrNull()
    }
    return if (pocetna != null) viewModel(viewModelStoreOwner = pocetna) else viewModel()
}

/**
 * Kartica "Unos" (smjer A "Pladanj"): isti pladanj kao na dnevnom pregledu (orah, utori, veličine tanjura, legenda),
 * s oznakom "Predloženo" ispod vrste koja odgovara dobu dana, i prečacima "Često jedeš" ispod.
 * Obroci dana dolaze iz PovijestViewModela (kao na Home), a "Često jedeš" iz JelaViewModela (isti poziv kao u unosu).
 */
@Composable
fun KonzumacijaScreen(
    navController: NavController,
    obrociViewModel: PovijestViewModel = obrociViewModelIzPocetne(navController),
    jelaViewModel: JelaViewModel = viewModel()
) {
    val obroci by obrociViewModel.obrociDana.collectAsState()
    val cesto by jelaViewModel.cestoJedeno.collectAsState()

    val context = LocalContext.current
    val sessionManager = remember { UserSessionManager(context) }
    val prijavljeniKorisnikId by sessionManager.korisnikId.collectAsState(initial = null)
    val danas = remember { LocalDate.now(ZAGREB_UNOS) }
    val predlozeniTip = remember { zadaniTipZaVrijeme(LocalTime.now(ZAGREB_UNOS)) }
    val snackbarHostState = remember { SnackbarHostState() }
    var detaljJelo by remember { mutableStateOf<Jelo?>(null) }

    // Puni ulaz (tanjuri slijeću, rub se puni) samo pri prvom otvaranju kartice u ovoj sesiji.
    val puniUlaz = remember { UlazUSesiji.jePrvi(UlazUSesiji.KARTICA_UNOS) }
    SideEffect { UlazUSesiji.oznaci(UlazUSesiji.KARTICA_UNOS) }

    // Osvježava se pri svakom povratku (nakon spremanja, uređivanja ili brisanja obroka).
    LifecycleResumeEffect(prijavljeniKorisnikId) {
        prijavljeniKorisnikId?.let {
            obrociViewModel.ucitajDan(it, danas.toString())
            jelaViewModel.ucitajCestoJedeno(it)
        }
        onPauseOrDispose { }
    }

    // Obrok obrisan iz uređivanja: "Obrok obrisan · Poništi", kao na Home.
    BrisanjeObrokaEfekti(
        viewModel = obrociViewModel,
        navController = navController,
        snackbarHostState = snackbarHostState,
        idKorisnik = prijavljeniKorisnikId
    )

    KonzumacijaSadrzaj(
        datum = danas,
        tanjuri = tanjuriDana(obroci),
        predlozeniTip = predlozeniTip,
        cesto = cesto.take(BROJ_PRECACA),
        puniUlaz = puniUlaz,
        onTanjur = { tanjur ->
            val tipa = obroci.filter { it.tipObroka.uppercase() == tanjur.tip }
            when {
                tipa.isEmpty() -> navController.navigate("unos_obroka/${tanjur.tip}")
                tipa.size == 1 -> navController.navigate(rutaUredivanjaObroka(tipa.single()))
                // Više obroka iste vrste: Detalj dana, gdje se bira koji se uređuje.
                else -> navController.navigate("detalj_dana/$danas")
            }
        },
        onJosObrok = { navController.navigate("unos_obroka/$predlozeniTip") },
        onDodajCesto = { jelo -> navController.navigate("unos_obroka/$predlozeniTip?dodajJelo=${jelo.id}") },
        onDetaljCesto = { detaljJelo = it },
        onSlikaIliOpis = { navController.navigate(RUTA_SLIKA_ILI_OPIS) },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = MacroRazmaci.rubEkrana,
                        end = MacroRazmaci.rubEkrana,
                        bottom = screenBottomPadding()
                    )
            )
        }
    )

    // Detalj jela iz prečaca: samo pregled (dodaje se gumbom "+" u retku).
    detaljJelo?.let { jelo ->
        JeloDetaljSheet(
            idJelo = jelo.id,
            onOdakleSuKalorije = {
                detaljJelo = null
                navController.navigate("odakle_su_kalorije")
            },
            onDismiss = { detaljJelo = null }
        )
    }
}

@Composable
fun KonzumacijaSadrzaj(
    datum: LocalDate,
    tanjuri: List<TanjurUi>,
    predlozeniTip: String,
    cesto: List<CestoJedenoJelo>,
    puniUlaz: Boolean,
    onTanjur: (TanjurUi) -> Unit,
    onJosObrok: () -> Unit,
    onDodajCesto: (Jelo) -> Unit,
    onDetaljCesto: (Jelo) -> Unit,
    onSlikaIliOpis: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHost: @Composable BoxScope.() -> Unit = {}
) {
    val shema = MaterialTheme.colorScheme
    val smanjeno = rememberSmanjeniPokret()
    // Previewi (statični kadar) prikazuju završno stanje.
    val animiraj = puniUlaz && !smanjeno && !LocalInspectionMode.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(shema.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = screenTopPadding(), bottom = screenBottomPadding())
        ) {
            Column(modifier = Modifier.padding(horizontal = MacroRazmaci.rubEkrana)) {
                Text(
                    text = stringResource(R.string.ko_naslov),
                    style = MaterialTheme.typography.headlineMedium,
                    color = shema.onBackground,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = naslovDatuma(datum),
                    style = MaterialTheme.typography.bodyLarge,
                    color = shema.onSurfaceVariant
                )

                Spacer(Modifier.height(MacroRazmaci.xl))

                // Isti pladanj kao na dnevnom pregledu; ovdje bez disanja praznih utora i bez proslave.
                Pladanj(
                    tanjuri = tanjuri,
                    danJePrazan = false,
                    animirajUlaz = animiraj,
                    smanjeno = smanjeno,
                    proslava = false,
                    onProslavaGotova = {},
                    onTanjur = onTanjur,
                    predlozeniTip = predlozeniTip,
                    punjenjeRuba = animiraj
                )

                if (tanjuri.any { it.makro != null }) {
                    val izvor = remember { MutableInteractionSource() }
                    TextButton(
                        onClick = onJosObrok,
                        interactionSource = izvor,
                        contentPadding = PaddingValues(horizontal = MacroRazmaci.xs),
                        modifier = Modifier
                            .padding(top = MacroRazmaci.xs)
                            .heightIn(min = MacroDimenzije.dodirniCilj)
                            .skalaPritiska(izvor)
                    ) {
                        Text(
                            text = stringResource(R.string.ko_jos_obrok),
                            style = MaterialTheme.typography.labelLarge,
                            color = shema.primary
                        )
                    }
                }
            }

            // Prečaci: "+" otvara unos predložene vrste s već dodanim jelom; dodir na red otvara detalj jela.
            if (cesto.isNotEmpty()) {
                NaslovSekcije(
                    tekst = stringResource(R.string.uo_cesto),
                    podnaslov = stringResource(R.string.ko_cesto_tekst)
                )
                cesto.forEachIndexed { i, c ->
                    JeloRed(
                        naziv = c.jelo.naziv,
                        makro = c.jelo.uMakro(),
                        kolicina = 0.0,
                        onDodaj = { onDodajCesto(c.jelo) },
                        onDetalj = { onDetaljCesto(c.jelo) },
                        dodatak = stringResource(R.string.uo_cesto_puta, c.brojPuta),
                        crta = i > 0
                    )
                }
            }

            Spacer(Modifier.height(MacroRazmaci.xxl))

            SlikaIliOpisKartica(
                onClick = onSlikaIliOpis,
                modifier = Modifier.padding(horizontal = MacroRazmaci.rubEkrana)
            )
        }

        snackbarHost()
    }
}

/** Sporedni ulaz u prepoznavanje obroka: mirna porculanska kartica, ne natječe se s pladnjem. */
@Composable
private fun SlikaIliOpisKartica(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shema = MaterialTheme.colorScheme
    val izvor = remember { MutableInteractionSource() }

    Surface(
        onClick = onClick,
        interactionSource = izvor,
        shape = MacroOblici.kartica,
        color = shema.surface,
        border = androidx.compose.foundation.BorderStroke(MacroDimenzije.linija, shema.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .skalaPritiska(izvor)
    ) {
        Row(
            modifier = Modifier.padding(MacroRazmaci.l),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(MacroDimenzije.ikonaObroka)
                    .background(shema.secondaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PhotoCamera,
                    contentDescription = null,
                    tint = shema.onSecondaryContainer,
                    modifier = Modifier.size(MacroDimenzije.ikonaMala)
                )
            }
            Spacer(Modifier.width(MacroRazmaci.m))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.ko_slika_naslov),
                    style = MaterialTheme.typography.titleSmall,
                    color = shema.onSurface
                )
                Text(
                    text = stringResource(R.string.ko_slika_tekst),
                    style = MaterialTheme.typography.bodySmall,
                    color = shema.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = shema.onSurfaceVariant
            )
        }
    }
}

// ---------- Previewi (⚠ lažni podaci, samo za @Preview; prava jela iz menze, okvirne vrijednosti) ----------

private val TANJURI_PRAZNO = TIPOVI_PLADNJA.map { TanjurUi(it, null, jeUzina = it == "UZINA") }

private val TANJURI_DJELOMICNO = listOf(
    TanjurUi("DORUCAK", Makro(kcal = 470.0, proteini = 14.0, masti = 19.0, ugljikohidrati = 59.0), "08:10"),
    TanjurUi("RUCAK", Makro(kcal = 1040.0, proteini = 52.0, masti = 38.0, ugljikohidrati = 120.0), "12:40"),
    TanjurUi("UZINA", null, jeUzina = true),
    TanjurUi("VECERA", null)
)

private fun jeloPrimjera(id: Long, naziv: String, kcal: Double, p: Double, m: Double, u: Double) =
    Jelo(id, naziv, null, kcal, p, u, m, null, null, null, null)

private val CESTO_PRIMJERA = listOf(
    CestoJedenoJelo(jeloPrimjera(72, "Pileći file na žaru", 253.0, 44.0, 8.0, 1.0), 9, "2026-10-23"),
    CestoJedenoJelo(jeloPrimjera(171, "Grah varivo", 472.0, 24.0, 14.0, 62.0), 6, "2026-10-21"),
    CestoJedenoJelo(jeloPrimjera(209, "Jogurt", 114.0, 6.0, 6.0, 9.0), 5, "2026-10-25")
)

@Composable
private fun PreviewKonzumacija(tamna: Boolean, tanjuri: List<TanjurUi>, cesto: List<CestoJedenoJelo>) {
    MacroMenzaTheme(darkTheme = tamna) {
        KonzumacijaSadrzaj(
            datum = LocalDate.of(2026, 10, 26),
            tanjuri = tanjuri,
            predlozeniTip = "VECERA",
            cesto = cesto,
            puniUlaz = false,
            onTanjur = {},
            onJosObrok = {},
            onDodajCesto = {},
            onDetaljCesto = {},
            onSlikaIliOpis = {}
        )
    }
}

@Preview(name = "Unos · svijetla · prazan dan", widthDp = 360, heightDp = 1000)
@Composable
private fun PreviewKonzumacijaSvijetlaPrazno() = PreviewKonzumacija(false, TANJURI_PRAZNO, emptyList())

@Preview(name = "Unos · svijetla · djelomično", widthDp = 360, heightDp = 1000)
@Composable
private fun PreviewKonzumacijaSvijetlaDjelomicno() = PreviewKonzumacija(false, TANJURI_DJELOMICNO, CESTO_PRIMJERA)

@Preview(name = "Unos · tamna · prazan dan", widthDp = 360, heightDp = 1000)
@Composable
private fun PreviewKonzumacijaTamnaPrazno() = PreviewKonzumacija(true, TANJURI_PRAZNO, CESTO_PRIMJERA)

@Preview(name = "Unos · tamna · djelomično", widthDp = 360, heightDp = 1000)
@Composable
private fun PreviewKonzumacijaTamnaDjelomicno() = PreviewKonzumacija(true, TANJURI_DJELOMICNO, CESTO_PRIMJERA)

@Preview(name = "Unos · fontScale 1,3", widthDp = 360, heightDp = 1200, fontScale = 1.3f)
@Composable
private fun PreviewKonzumacijaVelikiFont() = PreviewKonzumacija(false, TANJURI_DJELOMICNO, CESTO_PRIMJERA)
