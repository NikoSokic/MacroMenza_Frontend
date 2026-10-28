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
 * [punaSirina] = false: gumb je širok koliko tekst (npr. u retku sažetka obroka).
 */
@Composable
fun GumbSpremi(
    tekst: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    sprema: Boolean = false,
    punaSirina: Boolean = true
) {
    val izvor = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        enabled = enabled && !sprema,
        interactionSource = izvor,
        shape = MacroOblici.gumb,
        modifier = modifier
            .then(if (punaSirina) Modifier.fillMaxWidth() else Modifier)
            .heightIn(min = MacroDimenzije.fabVisina)
            .skalaPritiska(izvor)
    ) {
        Text(
            text = if (sprema) stringResource(R.string.kom_spremam) else tekst,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            maxLines = if (punaSirina) Int.MAX_VALUE else 1
        )
    }
}
