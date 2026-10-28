package com.niko.macromenza.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.niko.macromenza.R
import com.niko.macromenza.ui.theme.MacroDimenzije

/**
 * Znak aplikacije: „M" u Frauncesu na porculanskom krugu (ispuna surface, rub outlineVariant),
 * kao u „O aplikaciji" u Postavkama. Ukras, pa je skriven čitaču zaslona.
 */
@Composable
fun ZnakAplikacije(modifier: Modifier = Modifier) {
    val shema = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(MacroDimenzije.avatarProfila)
            .background(shema.surface, CircleShape)
            .border(MacroDimenzije.linija, shema.outlineVariant, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.kom_znak_aplikacije),
            style = MaterialTheme.typography.headlineSmall,
            color = shema.onSurface,
            modifier = Modifier.clearAndSetSemantics { }
        )
    }
}
