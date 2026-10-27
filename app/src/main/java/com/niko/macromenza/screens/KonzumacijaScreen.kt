package com.niko.macromenza.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import com.niko.macromenza.R
import com.niko.macromenza.model.Obrok
import com.niko.macromenza.screens.dnevnipregled.naslovDatuma
import com.niko.macromenza.session.UserSessionManager
import com.niko.macromenza.ui.components.Makro
import com.niko.macromenza.ui.components.Tanjur
import com.niko.macromenza.ui.components.nazivTipa
import com.niko.macromenza.ui.components.screenBottomPadding
import com.niko.macromenza.ui.components.screenTopPadding
import com.niko.macromenza.ui.components.skalaPritiska
import com.niko.macromenza.ui.theme.Kasnjenje
import com.niko.macromenza.ui.theme.MacroDimenzije
import com.niko.macromenza.ui.theme.MacroMenzaTheme
import com.niko.macromenza.ui.theme.MacroOblici
import com.niko.macromenza.ui.theme.MacroRazmaci
import com.niko.macromenza.ui.theme.Opruge
import com.niko.macromenza.ui.theme.POMAK_TANJURA_DP
import com.niko.macromenza.ui.theme.SKALA_PULSA
import com.niko.macromenza.ui.theme.SKALA_TANJURA
import com.niko.macromenza.ui.theme.Trajanje
import com.niko.macromenza.ui.theme.Ublazavanje
import com.niko.macromenza.ui.theme.macroBoje
import com.niko.macromenza.ui.theme.opruga
import com.niko.macromenza.ui.theme.rememberSmanjeniPokret
import com.niko.macromenza.util.formatKcal
import com.niko.macromenza.viewmodel.PovijestViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

private val ZAGREB_UNOS = ZoneId.of("Europe/Zagreb")

/** Redoslijed utora na pladnju ovog ekrana. */
private val TIPOVI_PLADNJA = listOf("DORUCAK", "RUCAK", "VECERA", "UZINA")

/**
 * Jedan utor: [makro] je zbroj obroka tog tipa danas (null = prazan utor).
 * [obrok] je jedini obrok tog tipa (za izravno uređivanje); kad ih ima više, "Uredi" otvara detalj dana.
 */
@Immutable
data class UtorUnosa(
    val tip: String,
    val makro: Makro?,
    val brojObroka: Int,
    val obrok: Obrok?
)

private fun utoriDana(obroci: List<Obrok>): List<UtorUnosa> = TIPOVI_PLADNJA.map { tip ->
    val tipa = obroci.filter { it.tipObroka.uppercase() == tip }
    UtorUnosa(
        tip = tip,
        makro = tipa.takeIf { it.isNotEmpty() }?.fold(Makro.NULA) { a, o ->
            a + Makro(o.ukupnoKalorije, o.ukupnoProteini, o.ukupnoMasti, o.ukupnoUgljikohidrati)
        },
        brojObroka = tipa.size,
        obrok = tipa.singleOrNull()
    )
}

/**
 * Kartica "Unos" (smjer A "Pladanj"). Obroci dana dolaze iz postojećeg PovijestViewModela (isto kao na Home),
 * pa popunjeni utor pokazuje tanjur s kcal i vodi na uređivanje.
 */
@Composable
fun KonzumacijaScreen(
    navController: NavController,
    obrociViewModel: PovijestViewModel = viewModel()
) {
    val obroci by obrociViewModel.obrociDana.collectAsState()

    val context = LocalContext.current
    val sessionManager = remember { UserSessionManager(context) }
    val prijavljeniKorisnikId by sessionManager.korisnikId.collectAsState(initial = null)
    val danas = remember { LocalDate.now(ZAGREB_UNOS) }
    val predlozeniTip = remember { zadaniTipZaVrijeme(LocalTime.now(ZAGREB_UNOS)) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Osvježava se pri svakom povratku (nakon spremanja, uređivanja ili brisanja obroka).
    LifecycleResumeEffect(prijavljeniKorisnikId) {
        prijavljeniKorisnikId?.let { obrociViewModel.ucitajDan(it, danas.toString()) }
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
        utori = utoriDana(obroci),
        predlozeniTip = predlozeniTip,
        onUtor = { utor ->
            val jedini = utor.obrok
            when {
                utor.makro == null -> navController.navigate("unos_obroka/${utor.tip}")
                jedini != null -> navController.navigate(rutaUredivanjaObroka(jedini))
                else -> navController.navigate("detalj_dana/$danas")
            }
        },
        onJosObrok = { navController.navigate("unos_obroka/$predlozeniTip") },
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
}

@Composable
fun KonzumacijaSadrzaj(
    datum: LocalDate,
    utori: List<UtorUnosa>,
    predlozeniTip: String,
    onUtor: (UtorUnosa) -> Unit,
    onJosObrok: () -> Unit,
    onSlikaIliOpis: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHost: @Composable BoxScope.() -> Unit = {}
) {
    val shema = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(shema.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MacroRazmaci.rubEkrana)
                .padding(top = screenTopPadding(), bottom = screenBottomPadding())
        ) {
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

            PladanjUnosa(utori = utori, predlozeniTip = predlozeniTip, onUtor = onUtor)

            if (utori.any { it.makro != null }) {
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

            Spacer(Modifier.height(MacroRazmaci.xxl))

            SlikaIliOpisKartica(onClick = onSlikaIliOpis)
        }

        snackbarHost()
    }
}

/** Pladanj s četiri utora (doručak, ručak, večera, užina); predloženi tip je blago istaknut. */
@Composable
private fun PladanjUnosa(
    utori: List<UtorUnosa>,
    predlozeniTip: String,
    onUtor: (UtorUnosa) -> Unit
) {
    val boje = MaterialTheme.macroBoje
    val smanjeno = rememberSmanjeniPokret()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(boje.orah, MacroOblici.pladanj)
            .border(MacroDimenzije.linija, boje.pladanjRub, MacroOblici.pladanj)
            .padding(MacroDimenzije.pladanjOkvir),
        horizontalArrangement = Arrangement.spacedBy(MacroRazmaci.xs)
    ) {
        utori.forEachIndexed { indeks, utor ->
            UtorPladnja(
                utor = utor,
                indeks = indeks,
                predlozen = utor.tip == predlozeniTip,
                smanjeno = smanjeno,
                onClick = { onUtor(utor) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun UtorPladnja(
    utor: UtorUnosa,
    indeks: Int,
    predlozen: Boolean,
    smanjeno: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val boje = MaterialTheme.macroBoje
    val naziv = nazivTipa(utor.tip)
    val makro = utor.makro
    val pun = makro != null
    val osnovniOpis = if (makro != null) stringResource(R.string.ko_utor_pun, naziv, formatKcal(makro.kcal))
    else stringResource(R.string.ko_utor_prazan, naziv)
    val opis = if (predlozen) stringResource(R.string.ko_predlozeno, osnovniOpis) else osnovniOpis
    val izvor = remember { MutableInteractionSource() }

    // Previewi i smanjeni pokret odmah prikazuju završno stanje.
    val bezPokreta = smanjeno || LocalInspectionMode.current

    // Utor sjeda na pladanj (stupnjevano), kao tanjuri na Home.
    val ulaz = remember { Animatable(if (bezPokreta) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (ulaz.value < 1f) {
            delay((indeks * Kasnjenje.tanjuri).toLong())
            ulaz.animateTo(1f, Opruge.slijeganje())
        }
    }

    // Tanjur sjeda i rub mu se puni kad obrok stigne (ili odmah pri prvom prikazu); prazan utor ostaje isprekidan.
    val sletanje = remember { Animatable(if (pun && bezPokreta) 1f else 0f) }
    LaunchedEffect(pun) {
        if (pun) sletanje.animateTo(1f, opruga(Opruge.slijeganje(), smanjeno)) else sletanje.snapTo(0f)
    }

    // Predloženi utor: jedan blagi puls nakon ulaza, nikad u petlji.
    val puls = remember { Animatable(1f) }
    LaunchedEffect(predlozen) {
        if (predlozen && !bezPokreta) {
            delay(Kasnjenje.pulsa.toLong())
            puls.animateTo(SKALA_PULSA, tween(Trajanje.kratko, easing = Ublazavanje.usporavanje))
            puls.animateTo(1f, tween(Trajanje.kratko, easing = Ublazavanje.usporavanje))
        }
    }

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = puls.value
                scaleY = puls.value
            }
            .clip(MacroOblici.utor)
            .background(if (predlozen) boje.utorIstaknut else boje.orah)
            .clickable(interactionSource = izvor, indication = null, role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = opis }
            .skalaPritiska(izvor)
            .padding(vertical = MacroRazmaci.s),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(MacroDimenzije.tanjurMax)
                .graphicsLayer {
                    val s = ulaz.value
                    alpha = s.coerceIn(0f, 1f)
                    translationY = (1f - s) * -POMAK_TANJURA_DP.dp.toPx()
                    val skala = SKALA_TANJURA + (1f - SKALA_TANJURA) * s
                    scaleX = skala
                    scaleY = skala
                },
            contentAlignment = Alignment.Center
        ) {
            // Prazan utor: isprekidani obris s "+" (nestaje dok tanjur slijeće, kao na Home)
            Canvas(modifier = Modifier.size(MacroDimenzije.tanjurPrazan)) {
                val alfa = (1f - sletanje.value).coerceIn(0f, 1f)
                if (alfa > 0f) {
                    val obrub = MacroDimenzije.obrubIsprekidan.toPx()
                    val crtica = MacroDimenzije.crtica.toPx()
                    drawCircle(
                        color = boje.naOrahuPrigusen.copy(alpha = alfa),
                        radius = size.minDimension / 2 - obrub,
                        style = Stroke(width = obrub, pathEffect = PathEffect.dashPathEffect(floatArrayOf(crtica, crtica)))
                    )
                }
            }
            if (!pun) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    tint = boje.naOrahu,
                    modifier = Modifier.size(MacroDimenzije.ikona)
                )
            }
            if (makro != null) {
                Tanjur(
                    makro = makro,
                    promjer = MacroDimenzije.tanjurPrazan,
                    okvir = MacroDimenzije.tanjurPrazan,
                    smanjeno = smanjeno,
                    punjenjeRuba = true,
                    modifier = Modifier.graphicsLayer {
                        val s = sletanje.value
                        alpha = s.coerceIn(0f, 1f)
                        translationY = (1f - s) * -POMAK_TANJURA_DP.dp.toPx()
                        val skala = SKALA_TANJURA + (1f - SKALA_TANJURA) * s
                        scaleX = skala
                        scaleY = skala
                    }
                )
            }
        }

        Text(
            text = naziv,
            style = MaterialTheme.typography.labelLarge,
            color = boje.naOrahu,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = stringResource(if (makro != null) R.string.ko_uredi else R.string.ko_dodaj),
            style = MaterialTheme.typography.labelMedium,
            color = boje.naOrahuPrigusen,
            maxLines = 1
        )
    }
}

/** Sporedni ulaz u prepoznavanje obroka: mirna porculanska kartica, ne natječe se s pladnjem. */
@Composable
private fun SlikaIliOpisKartica(onClick: () -> Unit) {
    val shema = MaterialTheme.colorScheme
    val izvor = remember { MutableInteractionSource() }

    Surface(
        onClick = onClick,
        interactionSource = izvor,
        shape = MacroOblici.kartica,
        color = shema.surface,
        border = androidx.compose.foundation.BorderStroke(MacroDimenzije.linija, shema.outlineVariant),
        modifier = Modifier
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

// ---------- Previewi (⚠ lažni podaci, samo za @Preview) ----------

private val UTORI_PRAZNO = TIPOVI_PLADNJA.map { UtorUnosa(it, null, 0, null) }

private val UTORI_DJELOMICNO = listOf(
    UtorUnosa("DORUCAK", Makro(kcal = 470.0, proteini = 14.0, masti = 19.0, ugljikohidrati = 59.0), 1, null),
    UtorUnosa("RUCAK", Makro(kcal = 1040.0, proteini = 52.0, masti = 38.0, ugljikohidrati = 120.0), 1, null),
    UtorUnosa("VECERA", null, 0, null),
    UtorUnosa("UZINA", null, 0, null)
)

@Composable
private fun PreviewKonzumacija(tamna: Boolean, utori: List<UtorUnosa>) {
    MacroMenzaTheme(darkTheme = tamna) {
        KonzumacijaSadrzaj(
            datum = LocalDate.of(2026, 10, 26),
            utori = utori,
            predlozeniTip = "VECERA",
            onUtor = {},
            onJosObrok = {},
            onSlikaIliOpis = {}
        )
    }
}

@Preview(name = "Unos · svijetla · prazan dan", widthDp = 360, heightDp = 800)
@Composable
private fun PreviewKonzumacijaSvijetlaPrazno() = PreviewKonzumacija(false, UTORI_PRAZNO)

@Preview(name = "Unos · svijetla · djelomično", widthDp = 360, heightDp = 800)
@Composable
private fun PreviewKonzumacijaSvijetlaDjelomicno() = PreviewKonzumacija(false, UTORI_DJELOMICNO)

@Preview(name = "Unos · tamna · prazan dan", widthDp = 360, heightDp = 800)
@Composable
private fun PreviewKonzumacijaTamnaPrazno() = PreviewKonzumacija(true, UTORI_PRAZNO)

@Preview(name = "Unos · tamna · djelomično", widthDp = 360, heightDp = 800)
@Composable
private fun PreviewKonzumacijaTamnaDjelomicno() = PreviewKonzumacija(true, UTORI_DJELOMICNO)

@Preview(name = "Unos · fontScale 1,3", widthDp = 360, heightDp = 900, fontScale = 1.3f)
@Composable
private fun PreviewKonzumacijaVelikiFont() = PreviewKonzumacija(false, UTORI_DJELOMICNO)
