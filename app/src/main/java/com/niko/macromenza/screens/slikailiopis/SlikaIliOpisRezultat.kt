package com.niko.macromenza.screens.slikailiopis

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.niko.macromenza.R
import com.niko.macromenza.model.PrijedlogObroka
import com.niko.macromenza.model.PrijedlogStavka
import com.niko.macromenza.ui.components.Makro
import com.niko.macromenza.ui.components.PorcijaGlif
import com.niko.macromenza.ui.components.Tanjur
import com.niko.macromenza.ui.components.kcalIMakroKratko
import com.niko.macromenza.ui.components.skalaPritiska
import com.niko.macromenza.ui.components.tekstPorcije
import com.niko.macromenza.ui.theme.Kasnjenje
import com.niko.macromenza.ui.theme.MAKS_ANIMIRANIH_REDOVA
import com.niko.macromenza.ui.theme.MacroDimenzije
import com.niko.macromenza.ui.theme.MacroOblici
import com.niko.macromenza.ui.theme.MacroRazmaci
import com.niko.macromenza.ui.theme.Opruge
import com.niko.macromenza.ui.theme.POMAK_TANJURA_DP
import com.niko.macromenza.ui.theme.POMAK_ULAZA_DP
import com.niko.macromenza.ui.theme.SKALA_TANJURA
import com.niko.macromenza.ui.theme.Trajanje
import com.niko.macromenza.ui.theme.Ublazavanje
import com.niko.macromenza.util.formatKolicina
import kotlinx.coroutines.delay

/**
 * Prikaz rezultata prepoznavanja: naslov, tanjur sa zbrojem, redovi stavki, neprepoznato, napomena i
 * "Koristi prijedlog" (samo kad ima stavki). Ništa se ovdje ne uređuje; uređivanje je u unosu obroka.
 * Pravila: design-system/macromenza/pages/slika-ili-opis.md.
 */
@Composable
fun RezultatPrijedloga(
    prijedlog: PrijedlogObroka,
    smanjeno: Boolean,
    onKoristi: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shema = MaterialTheme.colorScheme
    val imaStavki = prijedlog.stavke.isNotEmpty()

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.sio_prijedlog_naslov),
            style = MaterialTheme.typography.titleMedium,
            color = shema.onBackground,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(Modifier.size(MacroRazmaci.m))

        if (imaStavki) {
            UkupnoRed(ukupno = prijedlog.ukupno(), smanjeno = smanjeno)
            Spacer(Modifier.size(MacroRazmaci.s))
            prijedlog.stavke.forEachIndexed { indeks, stavka ->
                StavkaRed(stavka = stavka, indeks = indeks, smanjeno = smanjeno)
            }
        } else {
            PrazanRezultat(smanjeno)
        }

        if (prijedlog.neprepoznato.isNotEmpty()) {
            Spacer(Modifier.size(MacroRazmaci.l))
            Text(
                text = stringResource(R.string.sio_neprepoznato_naslov),
                style = MaterialTheme.typography.titleSmall,
                color = shema.onSurfaceVariant
            )
            prijedlog.neprepoznato.forEach {
                Text(
                    text = stringResource(R.string.sio_neprepoznato_stavka, it),
                    style = MaterialTheme.typography.bodyMedium,
                    color = shema.onSurfaceVariant
                )
            }
        }

        prijedlog.napomena?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.size(MacroRazmaci.m))
            Text(it, style = MaterialTheme.typography.bodySmall, color = shema.onSurfaceVariant)
        }

        if (imaStavki) {
            Spacer(Modifier.size(MacroRazmaci.xl))
            val izvor = remember { MutableInteractionSource() }
            Button(
                onClick = onKoristi,
                interactionSource = izvor,
                shape = MacroOblici.gumb,
                colors = ButtonDefaults.buttonColors(containerColor = shema.primary, contentColor = shema.onPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = MacroDimenzije.fabVisina)
                    .skalaPritiska(izvor)
            ) {
                Text(stringResource(R.string.sio_koristi_prijedlog), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** Tanjur sa zbrojem svih stavki i uz njega isti zbroj kao tekst. Tanjur sjeda, a rub mu se puni od nule. */
@Composable
private fun UkupnoRed(ukupno: Makro, smanjeno: Boolean) {
    val shema = MaterialTheme.colorScheme
    val slijetanje = remember { Animatable(if (smanjeno) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (slijetanje.value < 1f) {
            delay(Kasnjenje.tanjuri.toLong())
            slijetanje.animateTo(1f, Opruge.slijeganje())
        }
    }

    val zbroj = kcalIMakroKratko(ukupno)
    val opis = stringResource(R.string.sio_ukupno_opis, zbroj)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = opis },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Tanjur(
            makro = ukupno,
            promjer = MacroDimenzije.tanjurMax,
            okvir = MacroDimenzije.tanjurMax,
            smanjeno = smanjeno,
            punjenjeRuba = true,
            modifier = Modifier.graphicsLayer {
                val napredak = slijetanje.value
                alpha = napredak.coerceIn(0f, 1f)
                translationY = (1f - napredak) * POMAK_TANJURA_DP.dp.toPx()
                val skala = SKALA_TANJURA + (1f - SKALA_TANJURA) * napredak
                scaleX = skala
                scaleY = skala
            }
        )
        Spacer(Modifier.width(MacroRazmaci.l))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.sio_ukupno),
                style = MaterialTheme.typography.labelMedium,
                color = shema.onSurfaceVariant
            )
            Text(text = zbroj, style = MaterialTheme.typography.bodyLarge, color = shema.onSurface)
        }
    }
}

/**
 * Jedan red stavke: naziv, ispod kcal i makroi za tu količinu, desno glif porcije i količina.
 * Redovi se odvajaju dividerom (bez kartica). Čitač zaslona dobiva jednu rečenicu po redu.
 */
@Composable
private fun StavkaRed(stavka: PrijedlogStavka, indeks: Int, smanjeno: Boolean) {
    val shema = MaterialTheme.colorScheme
    val provjeri = stavka.trebaProvjeriti()
    val kcalIMakro = kcalIMakroKratko(stavka.uMakro())
    val opis = stringResource(R.string.sio_stavka_opis, stavka.naziv, tekstPorcije(stavka.kolicina), kcalIMakro)
    val provjeriOpis = stringResource(R.string.sio_provjeri_opis)

    Column(modifier = Modifier.ulazStavke(indeks, smanjeno)) {
        if (indeks > 0) {
            HorizontalDivider(thickness = MacroDimenzije.linija, color = shema.outlineVariant)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = MacroRazmaci.m)
                .clearAndSetSemantics { contentDescription = if (provjeri) "$opis $provjeriOpis" else opis },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stavka.naziv, style = MaterialTheme.typography.bodyLarge, color = shema.onSurface)
                Text(text = kcalIMakro, style = MaterialTheme.typography.bodySmall, color = shema.onSurfaceVariant)
                if (provjeri) {
                    Spacer(Modifier.size(MacroRazmaci.xs))
                    OznakaProvjeri()
                }
            }
            Spacer(Modifier.width(MacroRazmaci.m))
            PorcijaGlif(kolicina = stavka.kolicina, smanjeno = smanjeno)
            Spacer(Modifier.width(MacroRazmaci.xs))
            Text(
                text = formatKolicina(stavka.kolicina),
                style = MaterialTheme.typography.labelLarge,
                color = shema.onSurface
            )
        }
    }
}

/** Prigušena pilula "Provjeri": nije greška, pa bez paprike i boje greške. */
@Composable
private fun OznakaProvjeri() {
    val shema = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .background(shema.secondaryContainer, MacroOblici.pilula)
            .padding(horizontal = MacroRazmaci.s, vertical = MacroRazmaci.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.AutoMirrored.Rounded.HelpOutline,
            contentDescription = null,
            tint = shema.onSecondaryContainer,
            modifier = Modifier.size(MacroDimenzije.ikonaSitna)
        )
        Spacer(Modifier.width(MacroRazmaci.xs))
        Text(
            text = stringResource(R.string.sio_provjeri),
            style = MaterialTheme.typography.labelMedium,
            color = shema.onSecondaryContainer
        )
    }
}

/** Ništa nije prepoznato: prazan tanjur, objašnjenje i savjet. Bez "Koristi prijedlog". */
@Composable
private fun PrazanRezultat(smanjeno: Boolean) {
    val shema = MaterialTheme.colorScheme
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Tanjur(
            makro = null,
            promjer = MacroDimenzije.tanjurPrazan,
            okvir = MacroDimenzije.tanjurPrazan,
            smanjeno = smanjeno
        )
        Spacer(Modifier.width(MacroRazmaci.l))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.sio_nista_prepoznato),
                style = MaterialTheme.typography.titleSmall,
                color = shema.onSurface
            )
            Text(
                text = stringResource(R.string.sio_nista_savjet),
                style = MaterialTheme.typography.bodyMedium,
                color = shema.onSurfaceVariant
            )
        }
    }
}

/**
 * Redovi ulaze stupnjevano nakon slijetanja tanjura (2 × [Kasnjenje.korak] + indeks × [Kasnjenje.redovi]),
 * najviše [MAKS_ANIMIRANIH_REDOVA]; zadnji počinje na 380 ms i traje [Trajanje.srednje], dakle < 900 ms.
 * Samo graphicsLayer. Uz smanjeni pokret i za dalje redove sve je odmah tu.
 */
private fun Modifier.ulazStavke(indeks: Int, smanjeno: Boolean): Modifier =
    if (smanjeno || indeks >= MAKS_ANIMIRANIH_REDOVA) this else composed {
        val napredak = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            delay((Kasnjenje.korak * 2 + indeks * Kasnjenje.redovi).toLong())
            napredak.animateTo(1f, tween(Trajanje.srednje, easing = Ublazavanje.usporavanje))
        }
        graphicsLayer {
            alpha = napredak.value
            translationY = (1f - napredak.value) * POMAK_ULAZA_DP.dp.toPx()
        }
    }
