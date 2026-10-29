package com.niko.macromenza.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import com.niko.macromenza.R
import com.niko.macromenza.ui.components.GumbSpremi
import com.niko.macromenza.ui.components.PoljeUnosa
import com.niko.macromenza.ui.components.TablicniBroj
import com.niko.macromenza.ui.components.skalaPritiska
import com.niko.macromenza.ui.theme.MacroDimenzije
import com.niko.macromenza.ui.theme.MacroOblici
import com.niko.macromenza.ui.theme.MacroRazmaci
import com.niko.macromenza.ui.theme.Opruge
import com.niko.macromenza.ui.theme.Trajanje
import com.niko.macromenza.ui.theme.macroTipografija
import com.niko.macromenza.ui.theme.opruga
import com.niko.macromenza.ui.theme.rememberSmanjeniPokret
import com.niko.macromenza.util.filtrirajUnosMase
import com.niko.macromenza.util.formatMasa
import com.niko.macromenza.util.parsirajMasu
import com.niko.macromenza.util.uString
import com.niko.macromenza.viewmodel.PorukaMjerenja
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt

/** Raspon mase u desetinkama kilograma (20–400 kg), isto kao CHECK u bazi. */
private val RASPON_DESETINKI = 200..4000
private const val ZADANE_DESETINKE = 700
private const val KASNJENJE_PONAVLJANJA_MS = 400L
private const val RAZMAK_PONAVLJANJA_MS = 80L

/**
 * Brzi unos današnje mase, bez kartice (pages/profil.md § Moje mjere): ±0,1 kg (držanje ponavlja),
 * dodir na broj otvara ručni unos. Masa se drži u desetinkama (Int), da ±0,1 ne nakuplja grešku Doublea.
 */
@Composable
fun BrzoMjerenjeKartica(
    zadnjaMasa: Double?,
    danasnjaMasa: Double?,
    sprema: Boolean,
    poruka: PorukaMjerenja?,
    onSpremi: (desetinke: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val shema = MaterialTheme.colorScheme
    val tip = MaterialTheme.typography
    val smanjeno = rememberSmanjeniPokret()
    val pocetne = zadnjaMasa?.let { (it * 10).roundToInt() } ?: ZADANE_DESETINKE

    var desetinke by remember(pocetne) {
        mutableIntStateOf(pocetne.coerceIn(RASPON_DESETINKI))
    }
    var rucniUnos by remember { mutableStateOf(false) }

    // Prikazani broj prelazi oprugom; spremljena vrijednost je uvijek točno [desetinke].
    val prikaz = remember { Animatable(desetinke / 10f) }
    LaunchedEffect(desetinke) { prikaz.animateTo(desetinke / 10f, opruga(Opruge.blaga(), smanjeno)) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            KorakGumb(
                ikona = Icons.Rounded.Remove,
                opis = stringResource(R.string.mj_smanji),
                onKorak = { desetinke = (desetinke - 1).coerceIn(RASPON_DESETINKI) }
            )

            val izvor = remember { MutableInteractionSource() }
            val opisVrijednosti = stringResource(R.string.mj_vrijednost_opis, masaBezJedinice(desetinke / 10.0))
            Row(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = MacroDimenzije.dodirniCilj)
                    .skalaPritiska(izvor)
                    .clickable(
                        interactionSource = izvor,
                        indication = null,
                        role = Role.Button,
                        onClickLabel = stringResource(R.string.mj_upisi_rucno)
                    ) { rucniUnos = true }
                    .semantics(mergeDescendants = true) { contentDescription = opisVrijednosti },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TablicniBroj(
                    tekst = { masaBezJedinice(prikaz.value.toDouble()) },
                    stil = MaterialTheme.macroTipografija.brojSrednji,
                    boja = shema.onSurface,
                    modifier = Modifier.alignByBaseline()
                )
                Spacer(Modifier.width(MacroRazmaci.xs))
                Text(
                    text = stringResource(R.string.mj_kg),
                    style = tip.titleMedium,
                    color = shema.onSurfaceVariant,
                    modifier = Modifier.alignByBaseline()
                )
            }

            KorakGumb(
                ikona = Icons.Rounded.Add,
                opis = stringResource(R.string.mj_povecaj),
                onKorak = { desetinke = (desetinke + 1).coerceIn(RASPON_DESETINKI) }
            )
        }

        GumbSpremi(
            tekst = stringResource(R.string.mj_spremi),
            onClick = { onSpremi(desetinke) },
            sprema = sprema,
            modifier = Modifier.padding(top = MacroRazmaci.l)
        )

        if (danasnjaMasa != null) {
            Text(
                text = stringResource(R.string.mj_danas_upisano, formatMasa(danasnjaMasa)),
                style = tip.bodySmall,
                color = shema.onSurfaceVariant,
                modifier = Modifier.padding(top = MacroRazmaci.s)
            )
        }

        // Poruka spremanja: uspjeh u onSurface s kvačicom, greška u error s ikonom (nikad samo boja).
        Crossfade(
            targetState = poruka,
            animationSpec = if (smanjeno) snap() else tween(Trajanje.kratko),
            label = "porukaMjerenja"
        ) { p ->
            p?.let {
                Row(
                    modifier = Modifier
                        .padding(top = MacroRazmaci.s)
                        .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val boja = if (it.uspjeh) shema.onSurface else shema.error
                    Icon(
                        imageVector = if (it.uspjeh) Icons.Rounded.Check else Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = boja,
                        modifier = Modifier.size(MacroDimenzije.ikonaMala)
                    )
                    Spacer(Modifier.width(MacroRazmaci.s))
                    Text(text = it.tekst.uString(), style = tip.bodyMedium, color = boja)
                }
            }
        }
    }

    if (rucniUnos) {
        RucniUnosMaseDialog(
            pocetno = desetinke,
            onPotvrdi = {
                desetinke = it
                rucniUnos = false
            },
            onOdustani = { rucniUnos = false }
        )
    }
}

/** Tonski okrugli gumb (secondaryContainer, 48 dp): dodir napravi jedan korak, a dok se drži ponavlja ga. */
@Composable
private fun KorakGumb(
    ikona: ImageVector,
    opis: String,
    onKorak: () -> Unit
) {
    val trenutniKorak by rememberUpdatedState(onKorak)
    val izvor = remember { MutableInteractionSource() }
    val shema = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .size(MacroDimenzije.dodirniCilj)
            .skalaPritiska(izvor)
            .background(shema.secondaryContainer, CircleShape)
            .semantics {
                role = Role.Button
                contentDescription = opis
                onClick {
                    trenutniKorak()
                    true
                }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val dolje = awaitFirstDown()
                    val pritisak = PressInteraction.Press(dolje.position)
                    izvor.tryEmit(pritisak)
                    trenutniKorak()

                    // null = istekao rok, a prst je još dolje, pa se korak ponavlja.
                    val pusten = withTimeoutOrNull(KASNJENJE_PONAVLJANJA_MS) {
                        waitForUpOrCancellation()
                        true
                    }

                    if (pusten == null) {
                        do {
                            trenutniKorak()
                        } while (
                            withTimeoutOrNull(RAZMAK_PONAVLJANJA_MS) {
                                waitForUpOrCancellation()
                                true
                            } == null
                        )
                    }
                    izvor.tryEmit(PressInteraction.Release(pritisak))
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = ikona,
            contentDescription = null,
            tint = shema.onSecondaryContainer,
            modifier = Modifier.size(MacroDimenzije.ikona)
        )
    }
}

/** Ručni unos mase (20–400 kg, jedna decimala); tekst je pri otvaranju označen, pa ga upisivanje zamjenjuje. */
@Composable
private fun RucniUnosMaseDialog(
    pocetno: Int,
    onPotvrdi: (desetinke: Int) -> Unit,
    onOdustani: () -> Unit
) {
    val shema = MaterialTheme.colorScheme
    var polje by remember {
        val pocetniTekst = masaBezJedinice(pocetno / 10.0)
        mutableStateOf(TextFieldValue(pocetniTekst, selection = TextRange(0, pocetniTekst.length)))
    }
    val tekst = polje.text

    val fokus = remember { FocusRequester() }
    LaunchedEffect(Unit) { fokus.requestFocus() }

    val masa = parsirajMasu(tekst)
    val ispravno = masa != null && (masa * 10).roundToInt() in RASPON_DESETINKI
    val potvrdi = { masa?.takeIf { ispravno }?.let { onPotvrdi((it * 10).roundToInt()) } }
    val pomoc = stringResource(R.string.mj_rucno_pomoc)

    AlertDialog(
        onDismissRequest = onOdustani,
        shape = MacroOblici.pladanj,
        containerColor = shema.surface,
        title = {
            Text(stringResource(R.string.mj_rucno_naslov), style = MaterialTheme.typography.headlineSmall, color = shema.onSurface)
        },
        text = {
            PoljeUnosa(
                vrijednost = polje,
                onPromjena = { novo -> if (filtrirajUnosMase(novo.text) != null) polje = novo },
                oznaka = stringResource(R.string.mj_rucno_oznaka),
                pomoc = pomoc,
                // Ista rečenica kao pomoć, ali u error: govori koji je raspon dopušten.
                greska = if (tekst.isNotEmpty() && !ispravno) pomoc else null,
                tipkovnica = KeyboardType.Decimal,
                imeAction = ImeAction.Done,
                keyboardActions = KeyboardActions(onDone = { potvrdi() }),
                modifier = Modifier.focusRequester(fokus)
            )
        },
        confirmButton = {
            DijaloskiGumb(stringResource(R.string.mj_u_redu), enabled = ispravno, onClick = { potvrdi() })
        },
        dismissButton = {
            DijaloskiGumb(stringResource(R.string.mj_odustani), onClick = onOdustani)
        }
    )
}

/** Tekstni gumb dijaloga u onSurface: paprika na ovom ekranu pripada samo gumbu "Spremi". */
@Composable
private fun DijaloskiGumb(tekst: String, onClick: () -> Unit, enabled: Boolean = true) {
    val izvor = remember { MutableInteractionSource() }
    TextButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = izvor,
        modifier = Modifier
            .heightIn(min = MacroDimenzije.dodirniCilj)
            .skalaPritiska(izvor)
    ) {
        Text(
            text = tekst,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
