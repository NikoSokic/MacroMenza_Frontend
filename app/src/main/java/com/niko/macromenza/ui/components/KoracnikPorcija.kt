package com.niko.macromenza.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.niko.macromenza.R
import com.niko.macromenza.ui.theme.MacroDimenzije
import com.niko.macromenza.ui.theme.MacroOmjeri
import com.niko.macromenza.ui.theme.MacroRazmaci
import com.niko.macromenza.util.formatKolicina

/** Najšira uobičajena količina: rezervira širinu broja, pa se gumbi ne pomiču pri promjeni (1 → 1,5). */
private const val NAJSIRA_KOLICINA = "0,5"

/**
 * Koračnik porcija (sažetak obroka, detalj jela): − i + kao tonski krugovi od 48 dp, u sredini [PorcijaGlif]
 * i količina („1,5"). Korak je 0,5; što − radi na 0,5 (ukloni stavku ili ništa) odlučuje pozivatelj,
 * a [minusOmogucen] = false onemogućuje gumb (detalj jela ne ide ispod 0,5).
 */
@Composable
fun KoracnikPorcija(
    kolicina: Double,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    smanjeno: Boolean,
    modifier: Modifier = Modifier,
    minusOmogucen: Boolean = true
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MacroRazmaci.s)
    ) {
        TonskiKorak(
            ikona = Icons.Rounded.Remove,
            opis = stringResource(R.string.kom_smanji_pola),
            enabled = minusOmogucen,
            onClick = onMinus
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            PorcijaGlif(kolicina = kolicina, smanjeno = smanjeno)
            Spacer(Modifier.width(MacroRazmaci.xs))
            // Glif već čitaču daje „1,5 porcije", pa broj ne treba čitati dvaput.
            Box(contentAlignment = Alignment.Center, modifier = Modifier.clearAndSetSemantics { }) {
                Text(
                    text = NAJSIRA_KOLICINA,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.Transparent,
                    maxLines = 1
                )
                Text(
                    text = formatKolicina(kolicina),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
        }

        TonskiKorak(
            ikona = Icons.Rounded.Add,
            opis = stringResource(R.string.kom_povecaj_pola),
            enabled = true,
            onClick = onPlus
        )
    }
}

/** Tonski okrugli gumb (secondaryContainer, 48 dp), kao ±0,1 kg u brzom mjerenju. */
@Composable
private fun TonskiKorak(
    ikona: ImageVector,
    opis: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val shema = MaterialTheme.colorScheme
    val izvor = remember { MutableInteractionSource() }
    val alfa = if (enabled) 1f else MacroOmjeri.onemoguceno

    Box(
        modifier = Modifier
            .size(MacroDimenzije.dodirniCilj)
            .skalaPritiska(izvor)
            .clip(CircleShape)
            .background(shema.secondaryContainer.copy(alpha = shema.secondaryContainer.alpha * alfa))
            .clickable(
                interactionSource = izvor,
                indication = ripple(),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .semantics { contentDescription = opis },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = ikona,
            contentDescription = null,
            tint = shema.onSecondaryContainer.copy(alpha = alfa),
            modifier = Modifier.size(MacroDimenzije.ikona)
        )
    }
}
