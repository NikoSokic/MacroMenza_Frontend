package com.niko.macromenza.screens.unosobroka

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.niko.macromenza.R
import com.niko.macromenza.model.MeniDTO
import com.niko.macromenza.ui.components.Makro
import com.niko.macromenza.ui.components.PorcijaGlif
import com.niko.macromenza.ui.components.kcalIMakroKratko
import com.niko.macromenza.ui.components.nazivTipa
import com.niko.macromenza.ui.components.skalaPritiska
import com.niko.macromenza.ui.components.tekstPorcije
import com.niko.macromenza.ui.theme.MacroDimenzije
import com.niko.macromenza.ui.theme.MacroOblici
import com.niko.macromenza.ui.theme.MacroOmjeri
import com.niko.macromenza.ui.theme.MacroRazmaci
import com.niko.macromenza.ui.theme.SKALA_GUMBA
import com.niko.macromenza.ui.theme.Trajanje
import com.niko.macromenza.ui.theme.Ublazavanje
import com.niko.macromenza.ui.theme.macroBoje
import com.niko.macromenza.ui.theme.rememberSmanjeniPokret
import com.niko.macromenza.util.formatKolicina

/**
 * Red jela (jedini oblik jela na ekranu unosa): naziv, kompaktan makro red za 1 porciju i gumb za dodavanje.
 * Dodir na red otvara detalj jela; gumb dodaje porciju. Kad je jelo u obroku, gumb pokazuje glif i količinu.
 * [crta] crta tanki divider iznad reda (prvi red skupine ga nema).
 */
@Composable
fun JeloRed(
    naziv: String,
    makro: Makro,
    kolicina: Double,
    onDodaj: () -> Unit,
    onDetalj: () -> Unit,
    modifier: Modifier = Modifier,
    pogodak: IntRange? = null,
    dodatak: String? = null,
    crta: Boolean = true
) {
    val shema = MaterialTheme.colorScheme
    val makroTekst = kcalIMakroKratko(makro)
    val opisReda = stringResource(R.string.uo_jelo_detalj, naziv, makroTekst)
    val izvor = remember { MutableInteractionSource() }

    Column(modifier = modifier.fillMaxWidth()) {
        if (crta) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = MacroRazmaci.rubEkrana),
                thickness = MacroDimenzije.linija,
                color = shema.outlineVariant
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MacroDimenzije.dodirniCilj)
                .padding(start = MacroRazmaci.rubEkrana, end = MacroRazmaci.s),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(MacroOblici.cip)
                    .clickable(interactionSource = izvor, indication = null, role = Role.Button, onClick = onDetalj)
                    .clearAndSetSemantics { contentDescription = opisReda }
                    .skalaPritiska(izvor)
                    .padding(vertical = MacroRazmaci.s)
            ) {
                Text(
                    text = nazivSPogotkom(naziv, pogodak, rememberSmanjeniPokret()),
                    style = MaterialTheme.typography.bodyLarge,
                    color = shema.onSurface
                )
                Text(
                    text = if (dodatak != null) stringResource(R.string.uo_jelo_dodatak, makroTekst, dodatak) else makroTekst,
                    style = MaterialTheme.typography.bodySmall,
                    color = shema.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(MacroRazmaci.s))
            DodajGumb(
                kolicina = kolicina,
                opis = if (kolicina > 0.0) stringResource(R.string.uo_dodano_jelo, naziv, tekstPorcije(kolicina))
                else stringResource(R.string.uo_dodaj_jelo, naziv),
                onClick = onDodaj
            )
        }
    }
}

/**
 * Označava dio naziva koji odgovara pretrazi pozadinom u boji teksta, a ne težinom fonta:
 * druga težina mijenja širinu slova pa bi se redovi pomicali dok tipkaš, a pozadina ne mijenja layout.
 * Oznaka se pojavljuje blagim izblijeđivanjem.
 */
@Composable
private fun nazivSPogotkom(naziv: String, pogodak: IntRange?, smanjeno: Boolean): AnnotatedString {
    val boja = MaterialTheme.colorScheme.onSurface
    val alfa by animateFloatAsState(
        targetValue = if (pogodak != null) MacroOmjeri.oznakaPogotka else 0f,
        animationSpec = if (smanjeno) snap() else tween(Trajanje.kratko, easing = Ublazavanje.usporavanje),
        label = "oznakaPogotka"
    )
    if (pogodak == null) return AnnotatedString(naziv)
    return buildAnnotatedString {
        append(naziv.substring(0, pogodak.first))
        withStyle(SpanStyle(background = boja.copy(alpha = alfa))) { append(naziv.substring(pogodak.first, pogodak.last + 1)) }
        append(naziv.substring(pogodak.last + 1))
    }
}

/** Najšire stanje gumba za dodavanje: glif s polumjesecom (iznad 1 porcije) i količina "1,5". */
private const val KOLICINA_NAJSIRA = 1.5

/** Pilula s glifom porcije i količinom (stanje "dodano"). */
@Composable
private fun KolicinaPilula(
    kolicina: Double,
    smanjeno: Boolean,
    animirajUlaz: Boolean,
    modifier: Modifier = Modifier
) {
    val shema = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .background(shema.primaryContainer, MacroOblici.pilula)
            .padding(horizontal = MacroRazmaci.s, vertical = MacroRazmaci.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PorcijaGlif(
            kolicina = kolicina,
            smanjeno = smanjeno,
            ispuna = shema.primary,
            animirajUlaz = animirajUlaz
        )
        Spacer(Modifier.width(MacroRazmaci.xs))
        Text(
            text = formatKolicina(kolicina),
            style = MaterialTheme.typography.labelLarge,
            color = shema.onPrimaryContainer
        )
    }
}

/**
 * Gumb za dodavanje (dodirni cilj 48 dp): krug s "+", a kad je nešto već dodano, glif porcije i količina
 * u tonu paprike (dodir dodaje još pola porcije). "+" se pretvara u glif crossfadeom uz blagi rast, a glif
 * se pritom nalijeva. Veličina se ne animira (snap), pa se layout ne pomiče.
 */
@Composable
private fun DodajGumb(kolicina: Double, opis: String, onClick: () -> Unit) {
    val shema = MaterialTheme.colorScheme
    val smanjeno = rememberSmanjeniPokret()
    val izvor = remember { MutableInteractionSource() }
    val dodano = kolicina > 0.0

    // Zadnja količina veća od nule: pri odlasku glif ne prikazuje "0" dok blijedi.
    val zadnja = remember { doubleArrayOf(kolicina) }.also { if (kolicina > 0.0) it[0] = kolicina }

    // Glif se nalijeva samo za jelo dodano u ovom prikazu; red koji je već bio dodan pri prikazu odmah je pun.
    var odmahPun by remember { mutableStateOf(dodano) }
    LaunchedEffect(dodano) { if (!dodano) odmahPun = false }

    Box(
        modifier = Modifier
            .sizeIn(minWidth = MacroDimenzije.dodirniCilj, minHeight = MacroDimenzije.dodirniCilj)
            .clip(MacroOblici.pilula)
            .clickable(interactionSource = izvor, indication = null, role = Role.Button, onClick = onClick)
            .clearAndSetSemantics { contentDescription = opis }
            .skalaPritiska(izvor),
        contentAlignment = Alignment.CenterEnd
    ) {
        // Nevidljiva kopija najšireg stanja (glif + "1,5") određuje širinu utora, pa se naziv jela ne
        // prelama kad "+" postane glif; veličina prati fontScale jer se mjeri isti sadržaj.
        KolicinaPilula(
            kolicina = KOLICINA_NAJSIRA,
            smanjeno = true,
            animirajUlaz = false,
            modifier = Modifier
                .graphicsLayer { alpha = 0f }
                .clearAndSetSemantics { }
        )
        AnimatedContent(
            targetState = dodano,
            transitionSpec = {
                if (smanjeno) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else {
                    (fadeIn(tween(Trajanje.kratko, easing = Ublazavanje.usporavanje)) +
                        scaleIn(initialScale = SKALA_GUMBA, animationSpec = tween(Trajanje.kratko, easing = Ublazavanje.usporavanje))) togetherWith
                        fadeOut(tween(Trajanje.izlaz, easing = Ublazavanje.ubrzavanje)) using
                        SizeTransform(clip = false) { _, _ -> snap() }
                }
            },
            contentAlignment = Alignment.CenterEnd,
            label = "dodajGumb"
        ) { jeDodano ->
            if (jeDodano) {
                KolicinaPilula(kolicina = zadnja[0], smanjeno = smanjeno, animirajUlaz = !odmahPun)
            } else {
                Box(
                    modifier = Modifier
                        .size(MacroDimenzije.gumbDodaj)
                        .background(shema.surfaceVariant, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        tint = shema.onSurface,
                        modifier = Modifier.size(MacroDimenzije.ikonaMala)
                    )
                }
            }
        }
    }
}

/** "Ručak 12" */
@Composable
fun nazivMenija(meni: MeniDTO): String =
    meni.redniBroj?.let { stringResource(R.string.uo_meni_naziv, nazivTipa(meni.tip), it.toString()) } ?: nazivTipa(meni.tip)

/** Red menija: naziv, stavke (≈ približne, s objašnjenjem na dugi pritisak), zbroj i "+" za dodavanje cijelog menija. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeniRed(meni: MeniDTO, onDodaj: () -> Unit, crta: Boolean, modifier: Modifier = Modifier) {
    val shema = MaterialTheme.colorScheme
    val naziv = nazivMenija(meni)

    Column(modifier = modifier.fillMaxWidth()) {
        if (crta) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = MacroRazmaci.rubEkrana),
                thickness = MacroDimenzije.linija,
                color = shema.outlineVariant
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = MacroRazmaci.rubEkrana, end = MacroRazmaci.s, top = MacroRazmaci.m, bottom = MacroRazmaci.m),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = naziv,
                        style = MaterialTheme.typography.titleSmall,
                        color = shema.onSurface
                    )
                    if (meni.vegetarijanski) {
                        Spacer(Modifier.width(MacroRazmaci.s))
                        Text(
                            text = stringResource(R.string.uo_vegetarijanski),
                            style = MaterialTheme.typography.labelMedium,
                            color = shema.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(MacroRazmaci.xxs))
                meni.stavke.forEach { stavka ->
                    val tekst = buildString {
                        if (stavka.priblizno) append("≈ ")
                        if (stavka.kolicina != 1.0) append("${formatKolicina(stavka.kolicina)}× ")
                        append(stavka.oznaka)
                    }
                    if (stavka.priblizno) {
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                            tooltip = { PlainTooltip { Text(stringResource(R.string.uo_priblizno)) } },
                            state = rememberTooltipState()
                        ) {
                            Text(text = tekst, style = MaterialTheme.typography.bodySmall, color = shema.onSurfaceVariant)
                        }
                    } else {
                        Text(text = tekst, style = MaterialTheme.typography.bodySmall, color = shema.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(MacroRazmaci.xs))
                Text(
                    text = kcalIMakroKratko(Makro(meni.kalorije, meni.proteini, meni.masti, meni.ugljikohidrati)),
                    style = MaterialTheme.typography.labelMedium,
                    color = shema.onSurface
                )
            }
            Spacer(Modifier.width(MacroRazmaci.s))
            DodajGumb(kolicina = 0.0, opis = stringResource(R.string.uo_dodaj_meni, naziv), onClick = onDodaj)
        }
    }
}

/** Naslov kategorije (ljepljiv): Fraunces malim stilom na pozadini ekrana. */
@Composable
fun NaslovKategorije(tekst: String, modifier: Modifier = Modifier) {
    Text(
        text = tekst,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = MacroRazmaci.rubEkrana)
            .padding(top = MacroRazmaci.m, bottom = MacroRazmaci.xs)
            .semantics { heading() }
    )
}

/** Naslov sekcije (Meniji, Često jedeš, Sva jela) s ritmom iz MASTER-a: 32 dp iznad (prva 16 dp), 12 dp ispod. */
@Composable
fun NaslovSekcije(
    tekst: String,
    podnaslov: String? = null,
    prva: Boolean = false,
    modifier: Modifier = Modifier,
    dodatak: @Composable () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MacroRazmaci.rubEkrana)
            // Prva sekcija stoji odmah ispod filtera, pa ne treba puni razmak sekcije.
            .padding(top = if (prva) MacroRazmaci.l else MacroRazmaci.iznadSekcije, bottom = MacroRazmaci.ispodNaslovaSekcije),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tekst,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics { heading() }
            )
            if (podnaslov != null) {
                Text(
                    text = podnaslov,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        dodatak()
    }
}

/** Kostur učitavanja u obliku redova jela (naziv, makro red, krug "+"), bez spinnera i bez pokreta. */
@Composable
fun KosturRedova(broj: Int, opis: String) {
    val shema = MaterialTheme.colorScheme
    val boja = MaterialTheme.macroBoje.kostur

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = opis },
        verticalArrangement = Arrangement.Top
    ) {
        repeat(broj) { i ->
            if (i > 0) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = MacroRazmaci.rubEkrana),
                    thickness = MacroDimenzije.linija,
                    color = shema.outlineVariant
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = MacroDimenzije.dodirniCilj)
                    .padding(start = MacroRazmaci.rubEkrana, end = MacroRazmaci.s, top = MacroRazmaci.m, bottom = MacroRazmaci.m),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        Modifier
                            .fillMaxWidth(MacroOmjeri.kosturNaziv)
                            .height(MacroDimenzije.kosturNaziv)
                            .background(boja, MacroOblici.traka)
                    )
                    Spacer(Modifier.height(MacroRazmaci.s))
                    Box(
                        Modifier
                            .fillMaxWidth(MacroOmjeri.kosturMakro)
                            .height(MacroDimenzije.kosturMakro)
                            .background(boja, MacroOblici.traka)
                    )
                }
                Box(
                    modifier = Modifier.size(MacroDimenzije.dodirniCilj),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier
                            .size(MacroDimenzije.gumbDodaj)
                            .background(boja, CircleShape)
                    )
                }
            }
        }
    }
}
