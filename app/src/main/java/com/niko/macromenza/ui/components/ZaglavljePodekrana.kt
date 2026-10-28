package com.niko.macromenza.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.niko.macromenza.R
import com.niko.macromenza.ui.theme.MacroDimenzije
import com.niko.macromenza.ui.theme.MacroRazmaci

/**
 * Zaglavlje podekrana (pages/profil.md § Zajednički elementi): strelica natrag i naslov u
 * headlineSmall, bez podnaslova i ukrasnog kruga. Razmak ispod statusne trake je uključen.
 */
@Composable
fun ZaglavljePodekrana(naslov: String, onNatrag: () -> Unit, modifier: Modifier = Modifier) {
    val izvor = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = screenTopPadding()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onNatrag,
            interactionSource = izvor,
            modifier = Modifier
                .size(MacroDimenzije.dodirniCilj)
                .skalaPritiska(izvor)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.kom_natrag),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(MacroDimenzije.ikona)
            )
        }
        Spacer(Modifier.width(MacroRazmaci.xs))
        Text(
            text = naslov,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() }
        )
    }
}
