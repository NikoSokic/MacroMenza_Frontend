package com.niko.macromenza.screens

import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.niko.macromenza.R
import com.niko.macromenza.model.Obrok
import com.niko.macromenza.screens.dnevnipregled.naslovDatuma
import com.niko.macromenza.ui.components.GumbSpremi
import com.niko.macromenza.ui.components.Makro
import com.niko.macromenza.ui.components.Tanjur
import com.niko.macromenza.ui.components.kcalIMakroKratko
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
import com.niko.macromenza.ui.theme.SKALA_TANJURA
import com.niko.macromenza.ui.theme.opruga
import com.niko.macromenza.ui.theme.rememberSmanjeniPokret
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.ZoneId

private val ZAGREB = ZoneId.of("Europe/Zagreb")

/** Ruta potvrde; datum, vrijeme i zbroj su neobavezni (stari ulaz šalje samo vrstu). */
const val RUTA_OBROK_SPREMLJEN = "obrok_spremljen/{tipObroka}?datum={datum}&vrijeme={vrijeme}&kcal={kcal}&p={p}&m={m}&u={u}"

/** Ruta s podacima spremljenog obroka iz odgovora backenda. */
fun rutaObrokSpremljen(obrok: Obrok): String =
    "obrok_spremljen/${obrok.tipObroka}?datum=${Uri.encode(obrok.datum)}&vrijeme=${Uri.encode(obrok.vrijeme)}" +
        "&kcal=${obrok.ukupnoKalorije.toFloat()}&p=${obrok.ukupnoProteini.toFloat()}" +
        "&m=${obrok.ukupnoMasti.toFloat()}&u=${obrok.ukupnoUgljikohidrati.toFloat()}"

@Composable
fun ObrokSpremljenScreen(
    tipObroka: String,
    navController: NavController,
    datum: LocalDate? = null,
    vrijeme: String? = null,
    makro: Makro? = null
) {
    val danas = remember { LocalDate.now(ZAGREB) }

    ObrokSpremljenSadrzaj(
        tip = tipObroka.uppercase(),
        datum = datum,
        vrijeme = vrijeme,
        makro = makro,
        danas = danas,
        onNatragNaPocetnu = {
            navController.navigate("home") {
                popUpTo("home") {
                    inclusive = false
                }
            }
        },
        onDodajJos = {
            navController.navigate("unos_obroka/$tipObroka") {
                popUpTo("unos_obroka/$tipObroka") {
                    inclusive = true
                }
            }
        }
    )
}

/**
 * Potvrda spremanja (pages/meal-logging.md § Obrok spremljen): tanjur spremljenog obroka slijeće,
 * ispod naslov, rečenica bez roda i makronutrijenti; „Natrag na početnu" je jedina paprika.
 */
@Composable
fun ObrokSpremljenSadrzaj(
    tip: String,
    datum: LocalDate?,
    vrijeme: String?,
    makro: Makro?,
    danas: LocalDate,
    onNatragNaPocetnu: () -> Unit,
    onDodajJos: () -> Unit
) {
    val shema = MaterialTheme.colorScheme
    val smanjeno = rememberSmanjeniPokret()
    val pregled = LocalInspectionMode.current

    // Slijetanje i punjenje ruba samo pri prvom prikazu; nakon rotacije tanjur je odmah na mjestu.
    var sletio by rememberSaveable { mutableStateOf(false) }
    val puniRub = remember { !sletio }
    val slijetanje = remember { Animatable(if (sletio || smanjeno || pregled) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (slijetanje.value < 1f) {
            delay(Kasnjenje.tanjuri.toLong())
            slijetanje.animateTo(1f, opruga(Opruge.slijeganje(), smanjeno))
        }
        sletio = true
    }

    val recenica = recenicaObroka(tip, datum, vrijeme, danas)
    val makroTekst = makro?.let { kcalIMakroKratko(it) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(shema.background)
            .padding(
                start = MacroRazmaci.rubEkrana,
                end = MacroRazmaci.rubEkrana,
                top = screenTopPadding(),
                bottom = screenBottomPadding()
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {},
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Kcal tanjura je i u redu ispod, pa čitač tanjur preskače.
            Tanjur(
                makro = makro,
                promjer = MacroDimenzije.tanjurMax,
                okvir = MacroDimenzije.tanjurMax,
                smanjeno = smanjeno,
                punjenjeRuba = puniRub,
                modifier = Modifier
                    .clearAndSetSemantics { }
                    .graphicsLayer {
                        val napredak = slijetanje.value
                        alpha = napredak.coerceIn(0f, 1f)
                        translationY = (1f - napredak) * -POMAK_TANJURA_DP.dp.toPx()
                        val skala = SKALA_TANJURA + (1f - SKALA_TANJURA) * napredak
                        scaleX = skala
                        scaleY = skala
                    }
            )

            Spacer(Modifier.height(MacroRazmaci.xl))

            Text(
                text = stringResource(R.string.os_naslov),
                style = MaterialTheme.typography.headlineSmall,
                color = shema.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() }
            )

            Spacer(Modifier.height(MacroRazmaci.s))

            Text(
                text = recenica,
                style = MaterialTheme.typography.bodyLarge,
                color = shema.onSurface,
                textAlign = TextAlign.Center
            )

            if (makroTekst != null) {
                Spacer(Modifier.height(MacroRazmaci.xs))
                Text(
                    text = makroTekst,
                    style = MaterialTheme.typography.bodyMedium,
                    color = shema.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.weight(1f))

        GumbSpremi(
            tekst = stringResource(R.string.os_natrag_na_pocetnu),
            onClick = onNatragNaPocetnu
        )

        Spacer(Modifier.height(MacroRazmaci.s))

        val izvor = remember { MutableInteractionSource() }
        OutlinedButton(
            onClick = onDodajJos,
            interactionSource = izvor,
            shape = MacroOblici.gumb,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MacroDimenzije.fabVisina)
                .skalaPritiska(izvor)
        ) {
            Text(
                text = stringResource(R.string.os_dodaj_jos),
                style = MaterialTheme.typography.labelLarge,
                color = shema.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** "Večera · 18:30 · danas" ili "Večera · Ponedjeljak, 26. 10. · 18:30"; bez podataka samo vrsta. */
@Composable
private fun recenicaObroka(tip: String, datum: LocalDate?, vrijeme: String?, danas: LocalDate): String {
    val naziv = nazivTipa(tip)
    val sat = vrijeme?.take(5)?.takeIf { it.isNotBlank() }
    if (datum == null || sat == null) return naziv
    if (datum == danas) return stringResource(R.string.os_danas, naziv, sat)
    val tekstDatuma = if (datum.year == danas.year) naslovDatuma(datum) else {
        val dani = stringArrayResource(R.array.dp_dani_u_tjednu)
        stringResource(R.string.os_datum_godina, dani[datum.dayOfWeek.value - 1], datum.dayOfMonth, datum.monthValue, datum.year)
    }
    return stringResource(R.string.os_datum, naziv, tekstDatuma, sat)
}

// ---------- Previewi ----------

private val DANAS_PRIMJERA = LocalDate.of(2026, 10, 28)
private val VECERA_PRIMJER = Makro(kcal = 612.0, proteini = 32.0, masti = 22.0, ugljikohidrati = 64.0)

@Composable
private fun PreviewOkvir(tamna: Boolean, datum: LocalDate?, vrijeme: String?, makro: Makro?, tip: String = "VECERA") {
    MacroMenzaTheme(darkTheme = tamna) {
        ObrokSpremljenSadrzaj(
            tip = tip,
            datum = datum,
            vrijeme = vrijeme,
            makro = makro,
            danas = DANAS_PRIMJERA,
            onNatragNaPocetnu = {},
            onDodajJos = {}
        )
    }
}

@Preview(name = "Obrok spremljen · svijetla · danas", widthDp = 360, heightDp = 720)
@Composable
private fun PreviewSpremljenSvijetla() = PreviewOkvir(false, DANAS_PRIMJERA, "18:30:00", VECERA_PRIMJER)

@Preview(name = "Obrok spremljen · tamna · danas", widthDp = 360, heightDp = 720)
@Composable
private fun PreviewSpremljenTamna() = PreviewOkvir(true, DANAS_PRIMJERA, "18:30:00", VECERA_PRIMJER)

@Preview(name = "Obrok spremljen · svijetla · drugi datum", widthDp = 360, heightDp = 720)
@Composable
private fun PreviewSpremljenDrugiDatum() =
    PreviewOkvir(false, LocalDate.of(2026, 10, 26), "12:40", VECERA_PRIMJER.copy(kcal = 1040.0, proteini = 52.0, masti = 38.0, ugljikohidrati = 120.0), tip = "RUCAK")

@Preview(name = "Obrok spremljen · tamna · bez podataka (stari ulaz)", widthDp = 360, heightDp = 720)
@Composable
private fun PreviewSpremljenBezPodataka() = PreviewOkvir(true, null, null, null, tip = "UZINA")

@Preview(name = "Obrok spremljen · svijetla · fontScale 1,3", widthDp = 360, heightDp = 640, fontScale = 1.3f)
@Composable
private fun PreviewSpremljenVelikiFont() = PreviewOkvir(false, LocalDate.of(2025, 12, 31), "18:30", VECERA_PRIMJER)
