package com.niko.macromenza.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.niko.macromenza.R
import com.niko.macromenza.model.Mjerenje
import com.niko.macromenza.model.MjerenjeMase
import com.niko.macromenza.model.Preporuka
import com.niko.macromenza.screens.profil.nazivCilja
import com.niko.macromenza.session.UserSessionManager
import com.niko.macromenza.ui.components.TablicniBroj
import com.niko.macromenza.ui.components.ZaglavljePodekrana
import com.niko.macromenza.ui.components.screenBottomPadding
import com.niko.macromenza.ui.components.skalaPritiska
import com.niko.macromenza.ui.theme.Kasnjenje
import com.niko.macromenza.ui.theme.MacroDimenzije
import com.niko.macromenza.ui.theme.MacroMenzaTheme
import com.niko.macromenza.ui.theme.MacroRazmaci
import com.niko.macromenza.ui.theme.Opruge
import com.niko.macromenza.ui.theme.Trajanje
import com.niko.macromenza.ui.theme.UlazUSesiji
import com.niko.macromenza.ui.theme.macroTipografija
import com.niko.macromenza.ui.theme.opruga
import com.niko.macromenza.ui.theme.rememberSmanjeniPokret
import com.niko.macromenza.ui.theme.ulazniTween
import com.niko.macromenza.util.formatMasa
import com.niko.macromenza.util.formatPromjenaMase
import com.niko.macromenza.viewmodel.MjereViewModel
import com.niko.macromenza.viewmodel.PorukaMjerenja
import java.time.LocalDate
import java.time.ZoneId

private val ZAGREB = ZoneId.of("Europe/Zagreb")

@Composable
fun MojeMjereScreen(
    navController: NavController,
    viewModel: MjereViewModel = viewModel()
) {
    val mjerenja by viewModel.mjerenja.collectAsState()
    val mase by viewModel.mase.collectAsState()
    val preporuke by viewModel.preporuke.collectAsState()
    val greska by viewModel.greska.collectAsState()
    val sprema by viewModel.sprema.collectAsState()
    val poruka by viewModel.poruka.collectAsState()

    val context = LocalContext.current
    val sessionManager = remember { UserSessionManager(context) }
    val prijavljeniKorisnikId by sessionManager.korisnikId.collectAsState(initial = null)

    LaunchedEffect(prijavljeniKorisnikId) {
        prijavljeniKorisnikId?.let { id -> viewModel.ucitajMjerenja(id) }
    }

    // Puni ulaz (brojanje mase, iscrtavanje linije) samo prvi put u sesiji, i to kad stignu podaci.
    val puniUlaz = remember { UlazUSesiji.jePrvi(UlazUSesiji.MOJE_MJERE) }
    val imaPodataka = mjerenja.isNotEmpty() || mase.isNotEmpty()
    LaunchedEffect(imaPodataka) {
        if (imaPodataka) UlazUSesiji.oznaci(UlazUSesiji.MOJE_MJERE)
    }

    MojeMjereSadrzaj(
        mjerenja = mjerenja,
        mase = mase,
        preporuke = preporuke,
        greska = greska != null,
        sprema = sprema,
        poruka = poruka,
        puniUlaz = puniUlaz,
        onNatrag = { navController.popBackStack() },
        onPonovi = { prijavljeniKorisnikId?.let { viewModel.ucitajMjerenja(it) } },
        onSpremi = { desetinke -> prijavljeniKorisnikId?.let { viewModel.spremiBrzo(it, desetinke) } }
    )
}

@Composable
private fun MojeMjereSadrzaj(
    mjerenja: List<Mjerenje>,
    mase: List<MjerenjeMase>,
    preporuke: List<Preporuka>,
    greska: Boolean,
    sprema: Boolean,
    poruka: PorukaMjerenja?,
    puniUlaz: Boolean,
    onNatrag: () -> Unit,
    onPonovi: () -> Unit,
    onSpremi: (desetinke: Int) -> Unit
) {
    val shema = MaterialTheme.colorScheme
    val smanjeno = rememberSmanjeniPokret()

    // Aktivnost i cilj iz zadnjeg mjerenja; masa i datum iz zadnje točke dana (mase su uzlazno po datumu).
    val zadnjeMjerenje = mjerenja.maxWithOrNull(compareBy<Mjerenje>({ it.datum ?: "" }, { it.id ?: 0L }))
    val zadnja = mase.lastOrNull()
    val prethodna = mase.getOrNull(mase.lastIndex - 1)
    val danas = LocalDate.now(ZAGREB).toString()
    val danasnjaMasa = mase.lastOrNull { it.datum == danas }?.masa

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(shema.background),
        contentPadding = PaddingValues(
            start = MacroRazmaci.rubEkrana,
            end = MacroRazmaci.rubEkrana,
            bottom = screenBottomPadding()
        )
    ) {
        item(key = "zaglavlje") {
            ZaglavljePodekrana(naslov = stringResource(R.string.mj_naslov), onNatrag = onNatrag)
        }

        if (greska) {
            item(key = "greska") { GreskaRed(onPonovi = onPonovi) }
        }

        if (zadnjeMjerenje != null) {
            item(key = "hero") {
                ZadnjaMasa(
                    masa = zadnja?.masa ?: zadnjeMjerenje.masa,
                    datum = (zadnja?.datum ?: zadnjeMjerenje.datum)?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
                    mjerenje = zadnjeMjerenje,
                    prethodna = prethodna,
                    animirajUlaz = puniUlaz,
                    smanjeno = smanjeno
                )
            }

            // Brzo mjerenje samo uz postojeće mjerenje: aktivnost i cilj preuzimaju se iz zadnjeg.
            item(key = "brzo") {
                Column {
                    NaslovSekcije(stringResource(R.string.mj_brzo_mjerenje))
                    BrzoMjerenjeKartica(
                        zadnjaMasa = zadnja?.masa ?: zadnjeMjerenje.masa,
                        danasnjaMasa = danasnjaMasa,
                        sprema = sprema,
                        poruka = poruka,
                        onSpremi = onSpremi
                    )
                }
            }
        } else if (!greska) {
            item(key = "prazno") { PraznoStanje() }
        }

        if (mase.isNotEmpty()) {
            item(key = "graf") {
                Column {
                    NaslovSekcije(stringResource(R.string.mj_graf_naslov))
                    GrafMaseKartica(mase = mase, preporuke = preporuke, animirajUlaz = puniUlaz)
                }
            }
        }

        if (mjerenja.isNotEmpty()) {
            item(key = "povijest") { NaslovSekcije(stringResource(R.string.mj_povijest)) }

            val poredana = mjerenja.sortedByDescending { it.id ?: 0 }
            itemsIndexed(poredana, key = { indeks, m -> m.id ?: -indeks.toLong() - 1 }) { indeks, mjerenje ->
                Column(
                    modifier = if (smanjeno) Modifier else Modifier.animateItem(
                        fadeInSpec = tween(Trajanje.srednje),
                        placementSpec = Opruge.slijeganje<IntOffset>(),
                        fadeOutSpec = tween(Trajanje.kratko)
                    )
                ) {
                    if (indeks > 0) HorizontalDivider(thickness = MacroDimenzije.linija, color = shema.outlineVariant)
                    RedMjerenja(mjerenje)
                }
            }
        }
    }
}

/** Hero zadnje mase: broj u Frauncesu uz "kg", ispod datum, aktivnost i cilj te promjena od prethodnog mjerenja. */
@Composable
private fun ZadnjaMasa(
    masa: Double,
    datum: LocalDate?,
    mjerenje: Mjerenje,
    prethodna: MjerenjeMase?,
    animirajUlaz: Boolean,
    smanjeno: Boolean
) {
    val shema = MaterialTheme.colorScheme
    val tip = MaterialTheme.typography
    val prikaz by animiranaMasa(masa.toFloat(), animirajUlaz, smanjeno)
    val opis = stringResource(R.string.mj_zadnja_masa_opis, masaBezJedinice(masa))

    Column(modifier = Modifier.padding(top = MacroRazmaci.xl)) {
        Row(modifier = Modifier.clearAndSetSemantics { contentDescription = opis }) {
            TablicniBroj(
                tekst = { masaBezJedinice(prikaz.toDouble()) },
                stil = MaterialTheme.macroTipografija.brojLuka,
                boja = shema.onSurface,
                modifier = Modifier.alignByBaseline()
            )
            Spacer(Modifier.width(MacroRazmaci.s))
            Text(
                text = stringResource(R.string.mj_kg),
                style = tip.titleMedium,
                color = shema.onSurfaceVariant,
                modifier = Modifier.alignByBaseline()
            )
        }
        datum?.let {
            Text(
                text = stringResource(R.string.mj_izmjereno, kratkiDatum(it)),
                style = tip.bodySmall,
                color = shema.onSurfaceVariant
            )
        }
        Text(
            text = opisMjerenja(mjerenje),
            style = tip.bodySmall,
            color = shema.onSurfaceVariant
        )
        // Promjena je u onSurface: smjer ovisi o cilju, pa nije ni dobar ni loš sam po sebi.
        prethodna?.let { p ->
            Text(
                text = stringResource(
                    R.string.mj_od_prethodnog,
                    formatPromjenaMase(masa - p.masa),
                    kratkiDatum(LocalDate.parse(p.datum))
                ),
                style = tip.bodyMedium,
                color = shema.onSurface,
                modifier = Modifier.padding(top = MacroRazmaci.xs)
            )
        }
    }
}

/** Broji od nule samo pri prvom ulazu u sesiji; kasnije promjene prelaze oprugom od trenutne vrijednosti. */
@Composable
private fun animiranaMasa(cilj: Float, animirajUlaz: Boolean, smanjeno: Boolean): State<Float> {
    val vrijednost = remember { Animatable(if (animirajUlaz && !smanjeno) 0f else cilj) }
    var prviPut by remember { mutableStateOf(true) }
    LaunchedEffect(cilj) {
        val spec = if (prviPut && animirajUlaz) ulazniTween<Float>(Trajanje.ulaz, Kasnjenje.nakonPrijelaza, smanjeno)
        else opruga(Opruge.blaga(), smanjeno)
        prviPut = false
        vrijednost.animateTo(cilj, spec)
    }
    return vrijednost.asState()
}

/** Red povijesti: "27. 10. · 78,4 kg", ispod aktivnost i cilj prigušeno. */
@Composable
private fun RedMjerenja(mjerenje: Mjerenje) {
    val shema = MaterialTheme.colorScheme
    val datum = mjerenje.datum?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MacroDimenzije.dodirniCilj)
            .padding(vertical = MacroRazmaci.m)
            .semantics(mergeDescendants = true) { }
    ) {
        Text(
            text = if (datum != null) stringResource(R.string.mj_red, kratkiDatum(datum), formatMasa(mjerenje.masa))
            else formatMasa(mjerenje.masa),
            style = MaterialTheme.typography.titleSmall,
            color = shema.onSurface
        )
        Text(text = opisMjerenja(mjerenje), style = MaterialTheme.typography.bodySmall, color = shema.onSurfaceVariant)
    }
}

/** "Lagana aktivnost · Mršavljenje" */
@Composable
private fun opisMjerenja(mjerenje: Mjerenje): String {
    val aktivnost = when (mjerenje.razinaAktivnosti) {
        "sjedilacka" -> stringResource(R.string.prof_akt_sjedilacka)
        "lagana" -> stringResource(R.string.prof_akt_lagana)
        "umjerena" -> stringResource(R.string.prof_akt_umjerena)
        "visoka" -> stringResource(R.string.prof_akt_visoka)
        else -> mjerenje.razinaAktivnosti
    }
    return stringResource(R.string.mj_aktivnost_cilj, aktivnost, nazivCilja(mjerenje.tipCilja) ?: mjerenje.tipCilja)
}

@Composable
private fun NaslovSekcije(tekst: String) {
    Text(
        text = tekst,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .padding(top = MacroRazmaci.iznadSekcije, bottom = MacroRazmaci.ispodNaslovaSekcije)
            .semantics { heading() }
    )
}

/** Prazno stanje: isprekidana kartica i uputa gdje se masa upisuje. */
@Composable
private fun PraznoStanje() {
    val shema = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .padding(top = MacroRazmaci.xl)
            .fillMaxWidth()
            .isprekidaniObris()
            .padding(MacroRazmaci.xl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.MonitorWeight,
            contentDescription = null,
            tint = shema.onSurfaceVariant,
            modifier = Modifier.size(MacroDimenzije.ikona)
        )
        Text(
            text = stringResource(R.string.mj_prazno_naslov),
            style = MaterialTheme.typography.titleMedium,
            color = shema.onSurface,
            modifier = Modifier.padding(top = MacroRazmaci.m)
        )
        Text(
            text = stringResource(R.string.mj_prazno_tekst),
            style = MaterialTheme.typography.bodyMedium,
            color = shema.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = MacroRazmaci.xs)
        )
    }
}

/** Tihi red greške: ikona u error, tekst i "Pokušaj ponovno" (ucitajMjerenja). */
@Composable
private fun GreskaRed(onPonovi: () -> Unit) {
    val shema = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = MacroRazmaci.xl),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = shema.error,
            modifier = Modifier.size(MacroDimenzije.ikona)
        )
        Spacer(Modifier.width(MacroRazmaci.m))
        Text(
            text = stringResource(R.string.mj_greska),
            style = MaterialTheme.typography.bodyMedium,
            color = shema.onSurface,
            modifier = Modifier.weight(1f)
        )
        val izvor = remember { MutableInteractionSource() }
        TextButton(
            onClick = onPonovi,
            interactionSource = izvor,
            modifier = Modifier
                .heightIn(min = MacroDimenzije.dodirniCilj)
                .skalaPritiska(izvor)
        ) {
            Text(stringResource(R.string.mj_pokusaj_ponovno), style = MaterialTheme.typography.labelLarge, color = shema.primary)
        }
    }
}

// ---------- Previewi (datumi su relativni prema danas, da graf ima točke u razdoblju od 30 dana) ----------

private fun primjerMjerenja(): Pair<List<Mjerenje>, List<MjerenjeMase>> {
    val danas = LocalDate.now(ZAGREB)
    val mase = listOf(80.6, 80.2, 80.4, 79.9, 79.5, 79.6, 79.1, 78.8, 78.9, 78.4)
    val dani = listOf(27, 24, 21, 18, 15, 12, 9, 6, 3, 0)
    val mjerenja = mase.indices.map { i ->
        Mjerenje(
            id = i + 1L,
            idKorisnik = 1,
            masa = mase[i],
            razinaAktivnosti = if (i < 4) "umjerena" else "lagana",
            tipCilja = "mrsavljenje",
            datum = danas.minusDays(dani[i].toLong()).toString()
        )
    }
    return mjerenja to mjerenja.map { MjerenjeMase(it.id!!, it.datum!!, it.masa) }
}

private val PRIMJER_PREPORUKE = listOf(
    Preporuka(id = 1, idKorisnik = 1, kalorije = 2350.0, proteini = 161.0, ugljikohidrati = 260.0, masti = 65.0, datum = "2026-08-01"),
    Preporuka(id = 2, idKorisnik = 1, kalorije = 2020.0, proteini = 157.0, ugljikohidrati = 222.0, masti = 56.0, datum = LocalDate.now(ZAGREB).toString())
)

@Composable
private fun PreviewOkvir(tamna: Boolean, broj: Int?) {
    val (mjerenja, mase) = primjerMjerenja()
    val m = if (broj == null) mjerenja else mjerenja.takeLast(broj)
    val t = if (broj == null) mase else mase.takeLast(broj)
    MacroMenzaTheme(darkTheme = tamna) {
        MojeMjereSadrzaj(
            mjerenja = m,
            mase = t,
            preporuke = if (broj == null) PRIMJER_PREPORUKE else emptyList(),
            greska = false,
            sprema = false,
            poruka = if (broj == null) PorukaMjerenja("Spremljeno · cilj 2.020 kcal", uspjeh = true) else null,
            puniUlaz = false,
            onNatrag = {},
            onPonovi = {},
            onSpremi = {}
        )
    }
}

private const val VISINA_PREVIEWA = 1900

@Preview(name = "Svijetla · prazno", widthDp = 360, heightDp = 900)
@Composable
private fun PreviewSvijetlaPrazno() = PreviewOkvir(false, 0)

@Preview(name = "Tamna · prazno", widthDp = 360, heightDp = 900)
@Composable
private fun PreviewTamnaPrazno() = PreviewOkvir(true, 0)

@Preview(name = "Svijetla · jedno mjerenje", widthDp = 360, heightDp = 1200)
@Composable
private fun PreviewSvijetlaJedno() = PreviewOkvir(false, 1)

@Preview(name = "Tamna · jedno mjerenje", widthDp = 360, heightDp = 1200)
@Composable
private fun PreviewTamnaJedno() = PreviewOkvir(true, 1)

@Preview(name = "Svijetla · puno mjerenja", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewSvijetlaPuno() = PreviewOkvir(false, null)

@Preview(name = "Tamna · puno mjerenja", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewTamnaPuno() = PreviewOkvir(true, null)

@Preview(name = "Svijetla · prazno · fontScale 1,3", widthDp = 360, heightDp = 900, fontScale = 1.3f)
@Composable
private fun PreviewVelikiFontPrazno() = PreviewOkvir(false, 0)

@Preview(name = "Tamna · jedno mjerenje · fontScale 1,3", widthDp = 360, heightDp = 1400, fontScale = 1.3f)
@Composable
private fun PreviewVelikiFontJedno() = PreviewOkvir(true, 1)

@Preview(name = "Svijetla · puno mjerenja · fontScale 1,3", widthDp = 360, heightDp = 2400, fontScale = 1.3f)
@Composable
private fun PreviewVelikiFontPuno() = PreviewOkvir(false, null)
