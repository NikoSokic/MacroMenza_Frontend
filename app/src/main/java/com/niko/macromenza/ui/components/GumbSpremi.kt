package com.niko.macromenza.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.niko.macromenza.R
import com.niko.macromenza.ui.theme.MacroDimenzije
import com.niko.macromenza.ui.theme.MacroOblici

/**
 * Primarni gumb forme (jedina paprika na ekranu). Dok [sprema], gumb je onemogućen i piše „Spremam…".
 */
@Composable
fun GumbSpremi(
    tekst: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    sprema: Boolean = false
) {
    val izvor = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        enabled = enabled && !sprema,
        interactionSource = izvor,
        shape = MacroOblici.gumb,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MacroDimenzije.fabVisina)
            .skalaPritiska(izvor)
    ) {
        Text(
            text = if (sprema) stringResource(R.string.kom_spremam) else tekst,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center
        )
    }
}
