package com.niko.macromenza.screens.slikailiopis

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.niko.macromenza.R
import com.niko.macromenza.model.PrijedlogObroka
import com.niko.macromenza.ui.components.Tanjur
import com.niko.macromenza.ui.components.screenBottomPadding
import com.niko.macromenza.ui.components.screenTopPadding
import com.niko.macromenza.ui.components.skalaPritiska
import com.niko.macromenza.ui.theme.Kasnjenje
import com.niko.macromenza.ui.theme.MacroDimenzije
import com.niko.macromenza.ui.theme.MacroMenzaTheme
import com.niko.macromenza.ui.theme.MacroOblici
import com.niko.macromenza.ui.theme.MacroOmjeri
import com.niko.macromenza.ui.theme.MacroRazmaci
import com.niko.macromenza.ui.theme.POMAK_ULAZA_DP
import com.niko.macromenza.ui.theme.SKALA_EKRANA
import com.niko.macromenza.ui.theme.Trajanje
import com.niko.macromenza.ui.theme.Ublazavanje
import com.niko.macromenza.ui.theme.macroBoje
import com.niko.macromenza.ui.theme.rememberSmanjeniPokret
import com.niko.macromenza.viewmodel.NAJVISE_ZNAKOVA_OPISA
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** Pregled odabrane slike: nema je, učitava se (kostur) ili je gotov ([bitmapa] null = pregled nije dostupan). */
@Immutable
sealed interface PregledSlike {
    data object Nema : PregledSlike
    data object Ucitava : PregledSlike
    data class Gotov(val bitmapa: ImageBitmap?) : PregledSlike
}

/** Stanje ekrana "Slika ili opis" kako ga vidi UI (puni ga SlikaIliOpisScreen iz ViewModela). */
@Immutable
data class SlikaIliOpisUi(
    val opis: String = "",
    val pregled: PregledSlike = PregledSlike.Nema,
    val ucitava: Boolean = false,
    val greska: String? = null,
    val rezultat: PrijedlogObroka? = null,
    /** Korisnik je učitan iz sesije (bez njega se ne može prepoznavati). */
    val korisnikSpreman: Boolean = true,
    val najviseZnakova: Int = NAJVISE_ZNAKOVA_OPISA
) {
    val imaSliku: Boolean get() = pregled != PregledSlike.Nema
    val imaUnosa: Boolean get() = imaSliku || opis.isNotBlank()
    val mozePrepoznati: Boolean get() = !ucitava && korisnikSpreman && imaUnosa
}

class SlikaIliOpisAkcije(
    val onNatrag: () -> Unit = {},
    val onFotografiraj: () -> Unit = {},
    val onGalerija: () -> Unit = {},
    val onUkloniSliku: () -> Unit = {},
    val onOpis: (String) -> Unit = {},
    val onPrepoznaj: () -> Unit = {},
    val onKoristiPrijedlog: () -> Unit = {}
)

/** Što stoji ispod "Prepoznaj": analiza, razlog zašto je onemogućen ili ništa. */
private sealed interface IspodGumba {
    data object Analiza : IspodGumba
    data class Razlog(val tekst: String) : IspodGumba
    data object Nista : IspodGumba
}

/**
 * Ekran "Slika ili opis" (smjer A "Pladanj"), bez ViewModela. Pravila: design-system/macromenza/pages/slika-ili-opis.md.
 * [puniUlaz] je istina samo pri prvom otvaranju u sesiji: zaglavlje, izvori, pa opis i gumb ulaze stupnjevano.
 */
@Composable
fun SlikaIliOpisSadrzaj(
    ui: SlikaIliOpisUi,
    akcije: SlikaIliOpisAkcije,
    modifier: Modifier = Modifier,
    puniUlaz: Boolean = true
) {
    val shema = MaterialTheme.colorScheme
    // Previewi (statični kadar) i smanjeni pokret odmah prikazuju završno stanje.
    val smanjeno = rememberSmanjeniPokret() || LocalInspectionMode.current
    val ulaz = puniUlaz && !smanjeno
    val pregled = LocalInspectionMode.current
    val gustoca = LocalDensity.current
    val rezultat = ui.rezultat

    // Kad rezultat stigne, ekran se pomakne do njega. Položaji (px u korijenu): [0] vrh okvira, [1] vrh rezultata.
    val scroll = rememberScrollState()
    val polozaji = remember { floatArrayOf(0f, 0f) }
    LaunchedEffect(rezultat) {
        if (rezultat == null || pregled) return@LaunchedEffect
        // Pričekaj da se rezultat rasporedi, inače bi najveći pomak još bio premalen.
        withFrameNanos { }
        withFrameNanos { }
        val margina = with(gustoca) { MacroRazmaci.l.toPx() }
        val cilj = (scroll.value + polozaji[1] - polozaji[0] - margina).roundToInt().coerceAtLeast(0)
        if (smanjeno) scroll.scrollTo(cilj)
        else scroll.animateScrollTo(cilj, tween(Trajanje.srednje, easing = Ublazavanje.usporavanje))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { polozaji[0] = it.positionInRoot().y }
            .background(shema.background)
            .verticalScroll(scroll)
            .padding(horizontal = MacroRazmaci.rubEkrana)
            .padding(top = screenTopPadding(), bottom = screenBottomPadding())
    ) {
        Zaglavlje(onNatrag = akcije.onNatrag, modifier = Modifier.ulazSio(0, ulaz))

        Spacer(Modifier.height(MacroRazmaci.xl))
        Izvori(
            omoguceno = !ui.ucitava,
            onFotografiraj = akcije.onFotografiraj,
            onGalerija = akcije.onGalerija,
            modifier = Modifier.ulazSio(1, ulaz)
        )

        Column(modifier = Modifier.ulazSio(2, ulaz)) {
            AnimatedVisibility(
                visible = ui.imaSliku,
                enter = if (smanjeno) EnterTransition.None else
                    fadeIn(tween(Trajanje.srednje, easing = Ublazavanje.usporavanje)) +
                        scaleIn(tween(Trajanje.srednje, easing = Ublazavanje.usporavanje), initialScale = SKALA_EKRANA),
                exit = if (smanjeno) ExitTransition.None else fadeOut(tween(Trajanje.kratko, easing = Ublazavanje.ubrzavanje))
            ) {
                PregledSlikeOkvir(
                    pregled = ui.pregled,
                    omoguceno = !ui.ucitava,
                    smanjeno = smanjeno,
                    onUkloni = akcije.onUkloniSliku,
                    modifier = Modifier.padding(top = MacroRazmaci.l)
                )
            }

            Spacer(Modifier.height(MacroRazmaci.l))
            PoljeOpisa(
                opis = ui.opis,
                najvise = ui.najviseZnakova,
                omoguceno = !ui.ucitava,
                onOpis = akcije.onOpis
            )

            Spacer(Modifier.height(MacroRazmaci.l))
            val ispod = when {
                ui.ucitava -> IspodGumba.Analiza
                !ui.imaUnosa -> IspodGumba.Razlog(stringResource(R.string.sio_razlog_prazno))
                !ui.korisnikSpreman -> IspodGumba.Razlog(stringResource(R.string.sio_razlog_korisnik))
                else -> IspodGumba.Nista
            }
            GumbPrepoznaj(
                ucitava = ui.ucitava,
                omogucen = ui.mozePrepoznati,
                ponovno = rezultat != null,
                // Uz rezultat sa stavkama primarna akcija je "Koristi prijedlog", pa je ovdje tonski:
                // na ekranu je uvijek najviše jedna paprika. Prazan rezultat nema "Koristi prijedlog".
                primarni = rezultat == null || rezultat.stavke.isEmpty(),
                razlog = (ispod as? IspodGumba.Razlog)?.tekst,
                smanjeno = smanjeno,
                onClick = akcije.onPrepoznaj
            )

            Crossfade(
                targetState = ispod,
                animationSpec = if (smanjeno) snap() else tween(Trajanje.kratko),
                label = "ispodGumba"
            ) { stanje ->
                when (stanje) {
                    IspodGumba.Analiza -> Analiza(smanjeno, Modifier.padding(top = MacroRazmaci.l))
                    is IspodGumba.Razlog -> Text(
                        text = stanje.tekst,
                        style = MaterialTheme.typography.bodySmall,
                        color = shema.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = MacroRazmaci.s)
                            // Isti tekst čitač dobiva kao stanje gumba.
                            .clearAndSetSemantics { }
                    )
                    IspodGumba.Nista -> Spacer(Modifier.fillMaxWidth())
                }
            }
        }

        // Zadnja poruka ostaje vidljiva dok greška izblijeđuje.
        val zadnjaGreska = remember { mutableStateOf("") }
        ui.greska?.let { zadnjaGreska.value = it }
        AnimatedVisibility(
            visible = ui.greska != null && !ui.ucitava,
            enter = if (smanjeno) EnterTransition.None else fadeIn(tween(Trajanje.kratko, easing = Ublazavanje.usporavanje)),
            exit = if (smanjeno) ExitTransition.None else fadeOut(tween(Trajanje.kratko, easing = Ublazavanje.ubrzavanje))
        ) {
            Greska(poruka = zadnjaGreska.value, onPonovi = akcije.onPrepoznaj, modifier = Modifier.padding(top = MacroRazmaci.l))
        }

        rezultat?.let { prijedlog ->
            Spacer(Modifier.height(MacroRazmaci.xxl))
            RezultatPrijedloga(
                prijedlog = prijedlog,
                smanjeno = smanjeno,
                onKoristi = akcije.onKoristiPrijedlog,
                modifier = Modifier.onGloballyPositioned { polozaji[1] = it.positionInRoot().y }
            )
        }

        Spacer(Modifier.height(MacroRazmaci.xl))
        Text(
            text = stringResource(R.string.sio_napomena),
            style = MaterialTheme.typography.bodySmall,
            color = shema.onSurfaceVariant
        )
    }
}

// ---------- Zaglavlje i izvori ----------

@Composable
private fun Zaglavlje(onNatrag: () -> Unit, modifier: Modifier = Modifier) {
    val shema = MaterialTheme.colorScheme
    Column(modifier = modifier) {
        val izvor = remember { MutableInteractionSource() }
        // IconButton ima dodirni cilj 48 dp.
        IconButton(
            onClick = onNatrag,
            interactionSource = izvor,
            modifier = Modifier.skalaPritiska(izvor)
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.kom_natrag), tint = shema.onSurface)
        }
        Spacer(Modifier.height(MacroRazmaci.xs))
        Text(
            text = stringResource(R.string.sio_naslov),
            style = MaterialTheme.typography.headlineSmall,
            color = shema.onBackground,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(Modifier.height(MacroRazmaci.xxs))
        Text(
            text = stringResource(R.string.sio_podnaslov),
            style = MaterialTheme.typography.bodyMedium,
            color = shema.onSurfaceVariant
        )
    }
}

@Composable
private fun Izvori(omoguceno: Boolean, onFotografiraj: () -> Unit, onGalerija: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MacroRazmaci.m)
    ) {
        GumbIzvora(
            tekst = stringResource(R.string.sio_fotografiraj),
            ikona = { Icon(Icons.Rounded.PhotoCamera, contentDescription = null, modifier = Modifier.size(MacroDimenzije.ikonaMala)) },
            omoguceno = omoguceno,
            onClick = onFotografiraj,
            modifier = Modifier.weight(1f)
        )
        GumbIzvora(
            tekst = stringResource(R.string.sio_iz_galerije),
            ikona = { Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(MacroDimenzije.ikonaMala)) },
            omoguceno = omoguceno,
            onClick = onGalerija,
            modifier = Modifier.weight(1f)
        )
    }
}

/** Tonski gumb izvora (secondaryContainer, nikad paprika); pri velikom fontu tekst se prelama. */
@Composable
private fun GumbIzvora(
    tekst: String,
    ikona: @Composable () -> Unit,
    omoguceno: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val izvor = remember { MutableInteractionSource() }
    FilledTonalButton(
        onClick = onClick,
        enabled = omoguceno,
        interactionSource = izvor,
        shape = MacroOblici.gumb,
        modifier = modifier
            .heightIn(min = MacroDimenzije.dodirniCilj)
            .skalaPritiska(izvor)
    ) {
        ikona()
        Spacer(Modifier.width(MacroRazmaci.s))
        Text(tekst, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}

// ---------- Pregled slike ----------

@Composable
private fun PregledSlikeOkvir(
    pregled: PregledSlike,
    omoguceno: Boolean,
    smanjeno: Boolean,
    onUkloni: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shema = MaterialTheme.colorScheme
    // Okvir ima stalni omjer, pa se kostur, slika i poruka izmjenjuju bez pomaka sadržaja ispod.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(MacroOmjeri.pregledSlike)
            .clip(MacroOblici.kartica)
            .background(shema.surface)
            .border(MacroDimenzije.linija, shema.outlineVariant, MacroOblici.kartica)
    ) {
        Crossfade(
            targetState = pregled,
            animationSpec = if (smanjeno) snap() else tween(Trajanje.kratko),
            label = "pregledSlike"
        ) { stanje ->
            when (stanje) {
                is PregledSlike.Gotov -> if (stanje.bitmapa != null) {
                    Image(
                        bitmap = stanje.bitmapa,
                        contentDescription = stringResource(R.string.sio_odabrana_slika),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.sio_pregled_nedostupan),
                            style = MaterialTheme.typography.bodyMedium,
                            color = shema.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(MacroRazmaci.l)
                        )
                    }
                }
                // Prazan okvir zauzima isto mjesto kao kostur (vidi se samo tijekom izlaza).
                else -> KosturSlike(smanjeno)
            }
        }

        val izvor = remember { MutableInteractionSource() }
        Surface(
            onClick = onUkloni,
            enabled = omoguceno,
            interactionSource = izvor,
            shape = CircleShape,
            color = shema.surface,
            contentColor = shema.onSurface,
            border = BorderStroke(MacroDimenzije.linija, shema.outlineVariant),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(MacroRazmaci.s)
                .size(MacroDimenzije.dodirniCilj)
                .skalaPritiska(izvor)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.sio_ukloni_sliku),
                    modifier = Modifier.size(MacroDimenzije.ikonaMala)
                )
            }
        }
    }
}

/** Kostur pregleda: ploha u boji kostura koja blago pulsira (kao kostur na Home). */
@Composable
private fun KosturSlike(smanjeno: Boolean) {
    val opis = stringResource(R.string.sio_ucitavam_pregled)
    val puls = if (smanjeno) null else rememberInfiniteTransition(label = "kosturSlike").animateFloat(
        initialValue = MacroOmjeri.disanjeMin,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(Trajanje.disanje / 2), RepeatMode.Reverse),
        label = "puls"
    )
    val boja = MaterialTheme.macroBoje.kostur
    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = opis }
            .graphicsLayer { alpha = puls?.value ?: 1f }
            .background(boja)
    )
}

// ---------- Opis i gumb ----------

@Composable
private fun PoljeOpisa(opis: String, najvise: Int, omoguceno: Boolean, onOpis: (String) -> Unit) {
    val shema = MaterialTheme.colorScheme
    val brojacOpis = stringResource(R.string.sio_brojac_opis, opis.length, najvise)
    OutlinedTextField(
        value = opis,
        onValueChange = onOpis,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.sio_opis_oznaka)) },
        placeholder = { Text(stringResource(R.string.sio_opis_primjer)) },
        minLines = 3,
        maxLines = 6,
        enabled = omoguceno,
        shape = MacroOblici.kartica,
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = shema.surface,
            unfocusedContainerColor = shema.surface,
            disabledContainerColor = shema.surface,
            focusedBorderColor = shema.secondary,
            unfocusedBorderColor = shema.outlineVariant,
            disabledBorderColor = shema.outlineVariant,
            focusedLabelColor = shema.secondary,
            cursorColor = shema.onSurface
        ),
        supportingText = {
            Text(
                text = stringResource(R.string.sio_brojac, opis.length, najvise),
                style = MaterialTheme.typography.labelMedium,
                color = if (opis.length >= najvise) shema.onSurface else shema.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .clearAndSetSemantics { contentDescription = brojacOpis }
            )
        }
    )
}

/**
 * "Prepoznaj" / "Prepoznaj ponovno": paprika dok je primarna akcija, tonski kad je primarna "Koristi prijedlog".
 * Onemogućeni gumb čitaču zaslona uz naziv javlja i [razlog].
 */
@Composable
private fun GumbPrepoznaj(
    ucitava: Boolean,
    omogucen: Boolean,
    ponovno: Boolean,
    primarni: Boolean,
    razlog: String?,
    smanjeno: Boolean,
    onClick: () -> Unit
) {
    val shema = MaterialTheme.colorScheme
    val prijelaz: FiniteAnimationSpec<Color> =
        if (smanjeno) snap() else tween(Trajanje.kratko, easing = Ublazavanje.usporavanje)
    val spremnik by animateColorAsState(if (primarni) shema.primary else shema.secondaryContainer, prijelaz, label = "prepoznajSpremnik")
    val sadrzaj by animateColorAsState(if (primarni) shema.onPrimary else shema.onSecondaryContainer, prijelaz, label = "prepoznajSadrzaj")
    val izvor = remember { MutableInteractionSource() }

    Button(
        onClick = onClick,
        enabled = omogucen,
        interactionSource = izvor,
        shape = MacroOblici.gumb,
        colors = ButtonDefaults.buttonColors(containerColor = spremnik, contentColor = sadrzaj),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MacroDimenzije.fabVisina)
            .semantics { if (razlog != null) stateDescription = razlog }
            .skalaPritiska(izvor)
    ) {
        Text(
            text = stringResource(
                when {
                    ucitava -> R.string.sio_prepoznajem
                    ponovno -> R.string.sio_prepoznaj_ponovno
                    else -> R.string.sio_prepoznaj
                }
            ),
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center
        )
    }
}

/** Analiza u tijeku: prazan tanjur diše kao prazni utori na Home, uz objašnjenje. */
@Composable
private fun Analiza(smanjeno: Boolean, modifier: Modifier = Modifier) {
    val shema = MaterialTheme.colorScheme
    val disanje = if (smanjeno) null else rememberInfiniteTransition(label = "disanjeTanjura").animateFloat(
        initialValue = MacroOmjeri.disanjeMin,
        targetValue = MacroOmjeri.disanjeMaks,
        animationSpec = infiniteRepeatable(tween(Trajanje.disanje), RepeatMode.Reverse),
        label = "disanje"
    )
    val naslov = stringResource(R.string.sio_analiza_naslov)
    val tekst = stringResource(R.string.sio_analiza_tekst)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = "$naslov $tekst"
                liveRegion = LiveRegionMode.Polite
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Tanjur(
            makro = null,
            promjer = MacroDimenzije.tanjurMin,
            okvir = MacroDimenzije.tanjurMin,
            smanjeno = smanjeno,
            modifier = Modifier.graphicsLayer { alpha = disanje?.value ?: MacroOmjeri.disanjeMaks }
        )
        Spacer(Modifier.width(MacroRazmaci.l))
        Column(modifier = Modifier.weight(1f)) {
            Text(naslov, style = MaterialTheme.typography.titleSmall, color = shema.onSurface)
            Text(tekst, style = MaterialTheme.typography.bodySmall, color = shema.onSurfaceVariant)
        }
    }
}

/** Greška kao tihi red (bez obojene kartice): ikona u boji greške, poruka i link "Pokušaj ponovno". */
@Composable
private fun Greska(poruka: String, onPonovi: () -> Unit, modifier: Modifier = Modifier) {
    val shema = MaterialTheme.colorScheme
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = shema.error,
                modifier = Modifier.size(MacroDimenzije.ikonaMala)
            )
            Spacer(Modifier.width(MacroRazmaci.s))
            Text(poruka, style = MaterialTheme.typography.bodyMedium, color = shema.onSurface, modifier = Modifier.weight(1f))
        }
        val izvor = remember { MutableInteractionSource() }
        TextButton(
            onClick = onPonovi,
            interactionSource = izvor,
            modifier = Modifier
                .padding(start = MacroRazmaci.l)
                .heightIn(min = MacroDimenzije.dodirniCilj)
                .skalaPritiska(izvor)
        ) {
            Text(stringResource(R.string.kom_pokusaj_ponovno), style = MaterialTheme.typography.labelLarge, color = shema.primary)
        }
    }
}

// ---------- Pokret ----------

/**
 * Prvi ulaz u sesiji: element izblijedi uz uzlazni pomak od 12 dp, stupnjevano po [indeks] nakon prijelaza ekrana
 * (140 + 2 × 50 + 320 = 560 ms). Kad [aktivan] nije istina, element je odmah tu. Samo graphicsLayer.
 */
private fun Modifier.ulazSio(indeks: Int, aktivan: Boolean): Modifier = composed {
    val napredak = remember { Animatable(if (aktivan) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (napredak.value < 1f) {
            delay((Kasnjenje.nakonPrijelaza + indeks * Kasnjenje.korak).toLong())
            napredak.animateTo(1f, tween(Trajanje.srednje, easing = Ublazavanje.usporavanje))
        }
    }
    graphicsLayer {
        alpha = napredak.value
        translationY = (1f - napredak.value) * POMAK_ULAZA_DP.dp.toPx()
    }
}

// ---------- Previewi (⚠ lažni podaci iz SlikaIliOpisPrimjeri.kt) ----------

@Composable
private fun PreviewSio(tamna: Boolean, ui: @Composable () -> SlikaIliOpisUi) {
    MacroMenzaTheme(darkTheme = tamna) {
        SlikaIliOpisSadrzaj(ui = ui(), akcije = SlikaIliOpisAkcije(), puniUlaz = false)
    }
}

@Composable
private fun slikaPrimjera(): ImageBitmap {
    val shema = MaterialTheme.colorScheme
    val boje = MaterialTheme.macroBoje
    return remember(shema, boje) { primjerSlike(stol = boje.orah, tanjur = boje.tanjur, hrana = boje.ugljikohidrati) }
}

private const val VISINA_PREVIEWA = 1000

@Preview(name = "Slika ili opis · svijetla · prazno", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewSvijetlaPrazno() = PreviewSio(false) { PRIMJER_PRAZNO }

@Preview(name = "Slika ili opis · tamna · prazno", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewTamnaPrazno() = PreviewSio(true) { PRIMJER_PRAZNO }

@Preview(name = "Slika ili opis · svijetla · slika i opis", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewSvijetlaSlikaIOpis() = PreviewSio(false) { primjerSlikaIOpis(slikaPrimjera()) }

@Preview(name = "Slika ili opis · tamna · slika i opis", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewTamnaSlikaIOpis() = PreviewSio(true) { primjerSlikaIOpis(slikaPrimjera()) }

@Preview(name = "Slika ili opis · svijetla · pregled se učitava", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewSvijetlaPregledUcitava() = PreviewSio(false) { PRIMJER_PREGLED_UCITAVA }

@Preview(name = "Slika ili opis · svijetla · analiza", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewSvijetlaAnaliza() = PreviewSio(false) { PRIMJER_ANALIZA }

@Preview(name = "Slika ili opis · tamna · analiza", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewTamnaAnaliza() = PreviewSio(true) { PRIMJER_ANALIZA }

@Preview(name = "Slika ili opis · svijetla · greška", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewSvijetlaGreska() = PreviewSio(false) { PRIMJER_GRESKA }

@Preview(name = "Slika ili opis · tamna · greška", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewTamnaGreska() = PreviewSio(true) { PRIMJER_GRESKA }

private const val VISINA_REZULTATA = 1800

@Preview(name = "Slika ili opis · svijetla · rezultat", widthDp = 360, heightDp = VISINA_REZULTATA)
@Composable
private fun PreviewSvijetlaRezultat() = PreviewSio(false) { PRIMJER_REZULTAT }

@Preview(name = "Slika ili opis · tamna · rezultat", widthDp = 360, heightDp = VISINA_REZULTATA)
@Composable
private fun PreviewTamnaRezultat() = PreviewSio(true) { PRIMJER_REZULTAT }

@Preview(name = "Slika ili opis · svijetla · rezultat Provjeri", widthDp = 360, heightDp = VISINA_REZULTATA)
@Composable
private fun PreviewSvijetlaRezultatProvjeri() = PreviewSio(false) { PRIMJER_REZULTAT_PROVJERI }

@Preview(name = "Slika ili opis · tamna · rezultat Provjeri", widthDp = 360, heightDp = VISINA_REZULTATA)
@Composable
private fun PreviewTamnaRezultatProvjeri() = PreviewSio(true) { PRIMJER_REZULTAT_PROVJERI }

@Preview(name = "Slika ili opis · svijetla · prazan rezultat", widthDp = 360, heightDp = VISINA_REZULTATA)
@Composable
private fun PreviewSvijetlaRezultatPrazan() = PreviewSio(false) { PRIMJER_REZULTAT_PRAZAN }

@Preview(name = "Slika ili opis · tamna · prazan rezultat", widthDp = 360, heightDp = VISINA_REZULTATA)
@Composable
private fun PreviewTamnaRezultatPrazan() = PreviewSio(true) { PRIMJER_REZULTAT_PRAZAN }

@Preview(name = "Slika ili opis · fontScale 1,3 · rezultat Provjeri", widthDp = 360, heightDp = 2200, fontScale = 1.3f)
@Composable
private fun PreviewVelikiFontRezultatProvjeri() = PreviewSio(false) { PRIMJER_REZULTAT_PROVJERI }

@Preview(name = "Slika ili opis · fontScale 1,3 · prazan rezultat", widthDp = 360, heightDp = 1800, fontScale = 1.3f)
@Composable
private fun PreviewVelikiFontRezultatPrazan() = PreviewSio(true) { PRIMJER_REZULTAT_PRAZAN }

@Preview(name = "Slika ili opis · fontScale 1,3 · prazno", widthDp = 360, heightDp = VISINA_PREVIEWA, fontScale = 1.3f)
@Composable
private fun PreviewVelikiFontPrazno() = PreviewSio(false) { PRIMJER_PRAZNO }

@Preview(name = "Slika ili opis · fontScale 1,3 · slika i opis", widthDp = 360, heightDp = 1100, fontScale = 1.3f)
@Composable
private fun PreviewVelikiFontSlikaIOpis() = PreviewSio(false) { primjerSlikaIOpis(slikaPrimjera()) }

@Preview(name = "Slika ili opis · fontScale 1,3 · analiza", widthDp = 360, heightDp = VISINA_PREVIEWA, fontScale = 1.3f)
@Composable
private fun PreviewVelikiFontAnaliza() = PreviewSio(false) { PRIMJER_ANALIZA }

@Preview(name = "Slika ili opis · fontScale 1,3 · greška", widthDp = 360, heightDp = VISINA_PREVIEWA, fontScale = 1.3f)
@Composable
private fun PreviewVelikiFontGreska() = PreviewSio(false) { PRIMJER_GRESKA }
