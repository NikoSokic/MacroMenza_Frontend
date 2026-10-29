package com.niko.macromenza.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BakeryDining
import androidx.compose.material.icons.rounded.BreakfastDining
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DinnerDining
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.LunchDining
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import com.niko.macromenza.R
import com.niko.macromenza.model.Obrok
import com.niko.macromenza.ui.theme.MacroDimenzije
import com.niko.macromenza.ui.theme.MacroOblici
import com.niko.macromenza.ui.theme.MacroRazmaci
import com.niko.macromenza.ui.theme.Opruge
import com.niko.macromenza.ui.theme.Trajanje
import com.niko.macromenza.ui.theme.Ublazavanje
import com.niko.macromenza.ui.theme.opruga
import com.niko.macromenza.util.formatKcal
import kotlin.math.abs
import kotlinx.coroutines.launch

@Immutable
data class JeloUi(
    val naziv: String,
    val kolicina: Double,
    val makro: Makro
)

/**
 * Jedan spremljeni obrok. [vrijeme] je "HH:mm" ili null (lažni podaci bez vremena).
 * [izvor] je izvorni objekt s backenda (za brisanje, poništavanje i uređivanje); u previewima je null.
 */
@Immutable
data class ObrokUi(
    val id: Long,
    val tip: String,
    val vrijeme: String?,
    val makro: Makro,
    val jela: List<JeloUi>,
    val izvor: Obrok?
)

fun Obrok.uUi() = ObrokUi(
    id = id,
    tip = tipObroka.uppercase(),
    vrijeme = vrijeme.take(5),
    makro = Makro(ukupnoKalorije, ukupnoProteini, ukupnoMasti, ukupnoUgljikohidrati),
    jela = stavke.map { JeloUi(it.naziv, it.kolicina, Makro(it.kalorije, it.proteini, it.masti, it.ugljikohidrati)) },
    izvor = this
)

fun ikonaTipa(tip: String): ImageVector = when (tip) {
    "DORUCAK" -> Icons.Rounded.BreakfastDining
    "RUCAK" -> Icons.Rounded.LunchDining
    "UZINA" -> Icons.Rounded.BakeryDining
    else -> Icons.Rounded.DinnerDining
}

/** Brzina zamaha (px/s) iznad koje se red briše i bez prelaska praga. */
private const val BRZINA_BRISANJA = 2200f

/**
 * Red obroka: "Ručak · 12:40 · 1040 kcal · 52P 38M 120U". Dodir ga proširuje na jela (s glifom porcije),
 * a povlačenje ulijevo briše obrok (alternativa bez geste: gumb "Obriši" u proširenom redu i radnja pristupačnosti).
 */
@Composable
fun ObrokRed(
    obrok: ObrokUi,
    prosiren: Boolean,
    smanjeno: Boolean,
    onProsiri: () -> Unit,
    onUredi: () -> Unit,
    onObrisi: () -> Unit,
    modifier: Modifier = Modifier,
    /** True (Detalj dana): povlačenje ne odvodi red s ekrana nego ga vraća i traži potvrdu kroz [onObrisi]. */
    brisanjeSPotvrdom: Boolean = false
) {
    val shema = MaterialTheme.colorScheme
    val tip = MaterialTheme.typography
    val gustoca = LocalDensity.current
    val scope = rememberCoroutineScope()

    val pomak = remember { Animatable(0f) }
    val sirina = remember { floatArrayOf(0f) }
    val prag = with(gustoca) { MacroDimenzije.pragBrisanja.toPx() }

    val naziv = nazivTipa(obrok.tip)
    val kcalTekst = stringResource(R.string.kom_kcal_vrijednost, formatKcal(obrok.makro.kcal))
    val makroTekst = makroKratko(obrok.makro)
    val tekstObrisi = stringResource(R.string.kom_obrisi_obrok)
    val tekstUredi = stringResource(R.string.kom_uredi)
    val stanjeTekst = stringResource(if (prosiren) R.string.kom_sazmi else R.string.kom_prosiri)
    val opis = stringResource(R.string.kom_red_opis, naziv, obrok.vrijeme.orEmpty(), formatKcal(obrok.makro.kcal), makroTekst)

    fun obrisiAnimirano() {
        scope.launch {
            if (brisanjeSPotvrdom) {
                onObrisi()
                pomak.animateTo(0f, opruga(Opruge.slijeganje(), smanjeno))
                return@launch
            }
            pomak.animateTo(-sirina[0], if (smanjeno) opruga(Opruge.brza(), true) else tween(Trajanje.kratko, easing = Ublazavanje.ubrzavanje))
            onObrisi()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { sirina[0] = it.width.toFloat() }
    ) {
        // Pozadina koja se otkriva povlačenjem
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(MacroOblici.kartica)
                .drawBehind {
                    val napredak = (abs(pomak.value) / prag).coerceIn(0f, 1f)
                    drawRect(shema.errorContainer.copy(alpha = napredak))
                }
                .padding(horizontal = MacroRazmaci.xl),
            contentAlignment = Alignment.CenterEnd
        ) {
            Icon(
                imageVector = Icons.Rounded.DeleteOutline,
                contentDescription = null,
                tint = shema.onErrorContainer,
                modifier = Modifier.graphicsLayer { alpha = (abs(pomak.value) / prag).coerceIn(0f, 1f) }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = pomak.value }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        scope.launch { pomak.snapTo((pomak.value + delta).coerceAtMost(0f)) }
                    },
                    onDragStopped = { brzina ->
                        if (-pomak.value > prag || brzina < -BRZINA_BRISANJA) obrisiAnimirano()
                        else pomak.animateTo(0f, opruga(Opruge.slijeganje(), smanjeno))
                    }
                )
                .clip(MacroOblici.kartica)
                .background(shema.surface)
                .border(MacroDimenzije.linija, shema.outlineVariant, MacroOblici.kartica)
        ) {
            val izvor = remember { MutableInteractionSource() }
            val rotacija by animateFloatAsState(
                targetValue = if (prosiren) 180f else 0f,
                animationSpec = opruga(Opruge.blaga(), smanjeno),
                label = "strelica"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = MacroDimenzije.dodirniCilj)
                    .clickable(interactionSource = izvor, indication = null, onClick = onProsiri)
                    .skalaPritiska(izvor)
                    .clearAndSetSemantics {
                        contentDescription = opis
                        stateDescription = stanjeTekst
                        onClick(label = stanjeTekst) { onProsiri(); true }
                        customActions = listOf(
                            CustomAccessibilityAction(tekstUredi) { onUredi(); true },
                            CustomAccessibilityAction(tekstObrisi) { obrisiAnimirano(); true }
                        )
                    }
                    .padding(horizontal = MacroRazmaci.m, vertical = MacroRazmaci.m),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(MacroDimenzije.ikonaObroka)
                        .background(shema.secondaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(ikonaTipa(obrok.tip), contentDescription = null, tint = shema.onSecondaryContainer, modifier = Modifier.size(MacroDimenzije.ikonaMala))
                }
                Spacer(Modifier.width(MacroRazmaci.m))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(naziv, style = tip.titleMedium, color = shema.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        obrok.vrijeme?.let {
                            Text(stringResource(R.string.kom_red_vrijeme, it), style = tip.bodySmall, color = shema.onSurfaceVariant, maxLines = 1)
                        }
                    }
                    Text(makroTekst, style = tip.bodySmall, color = shema.onSurfaceVariant, maxLines = 1)
                }
                Spacer(Modifier.width(MacroRazmaci.s))
                Text(kcalTekst, style = tip.titleSmall, color = shema.onSurface, maxLines = 1)
                Icon(
                    imageVector = Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = shema.onSurfaceVariant,
                    modifier = Modifier.graphicsLayer { rotationZ = rotacija }
                )
            }

            AnimatedVisibility(
                visible = prosiren,
                enter = if (smanjeno) fadeIn(tween(0)) else expandVertically(Opruge.slijeganje()) + fadeIn(tween(Trajanje.kratko)),
                exit = if (smanjeno) fadeOut(tween(0)) else shrinkVertically(Opruge.blaga()) + fadeOut(tween(Trajanje.kratko))
            ) {
                Column(modifier = Modifier.padding(start = MacroRazmaci.m, end = MacroRazmaci.m, bottom = MacroRazmaci.xs)) {
                    obrok.jela.forEach { jelo ->
                        HorizontalDivider(color = shema.outlineVariant)
                        JeloRed(jelo, smanjeno)
                    }
                    HorizontalDivider(color = shema.outlineVariant)
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        AkcijaGumb(tekstUredi, Icons.Rounded.Edit, shema.onSurface, onUredi)
                        AkcijaGumb(stringResource(R.string.kom_obrisi), Icons.Rounded.DeleteOutline, shema.error) { obrisiAnimirano() }
                    }
                }
            }
        }
    }
}

@Composable
private fun JeloRed(jelo: JeloUi, smanjeno: Boolean) {
    val shema = MaterialTheme.colorScheme
    val tip = MaterialTheme.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MacroDimenzije.dodirniCilj)
            .semantics(mergeDescendants = true) {}
            .padding(vertical = MacroRazmaci.s),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PorcijaGlif(kolicina = jelo.kolicina, smanjeno = smanjeno)
        Spacer(Modifier.width(MacroRazmaci.s))
        Column(modifier = Modifier.weight(1f)) {
            Text(jelo.naziv, style = tip.bodyMedium, color = shema.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                text = stringResource(R.string.kom_jelo_detalj, tekstPorcije(jelo.kolicina), makroKratko(jelo.makro)),
                style = tip.bodySmall,
                color = shema.onSurfaceVariant,
                modifier = Modifier.clearAndSetSemantics { },
                maxLines = 2
            )
        }
        Spacer(Modifier.width(MacroRazmaci.s))
        Text(formatKcal(jelo.makro.kcal), style = tip.titleSmall, color = shema.onSurface, maxLines = 1)
    }
}

@Composable
private fun AkcijaGumb(tekst: String, ikona: ImageVector, boja: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    val izvor = remember { MutableInteractionSource() }
    TextButton(
        onClick = onClick,
        interactionSource = izvor,
        modifier = Modifier
            .heightIn(min = MacroDimenzije.dodirniCilj)
            .skalaPritiska(izvor)
    ) {
        Icon(ikona, contentDescription = null, tint = boja, modifier = Modifier.size(MacroDimenzije.ikonaMala))
        Spacer(Modifier.width(MacroRazmaci.xs))
        Text(tekst, style = MaterialTheme.typography.labelLarge, color = boja)
    }
}
