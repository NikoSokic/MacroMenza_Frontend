package com.niko.macromenza.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import com.niko.macromenza.ui.theme.MacroOblici
import com.niko.macromenza.ui.theme.Trajanje
import com.niko.macromenza.ui.theme.rememberSmanjeniPokret

/**
 * Polje unosa forme (pages/profil.md § Zajednički elementi): oznaka uvijek vidljiva, fokus u onSurface
 * (ne paprika), ispod pomoćni tekst ili greška. Greška zamjenjuje pomoćni tekst izblijeđivanjem.
 * Onemogućeno polje ostaje čitljivo (onSurfaceVariant, ne 38 %).
 */
@Composable
fun PoljeUnosa(
    vrijednost: String,
    onPromjena: (String) -> Unit,
    oznaka: String,
    modifier: Modifier = Modifier,
    pomoc: String? = null,
    greska: String? = null,
    enabled: Boolean = true,
    tipkovnica: KeyboardType = KeyboardType.Text,
    velikaSlova: KeyboardCapitalization = KeyboardCapitalization.None,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    OutlinedTextField(
        value = vrijednost,
        onValueChange = onPromjena,
        label = { Text(oznaka) },
        supportingText = donjiTekst(pomoc, greska),
        isError = greska != null,
        enabled = enabled,
        singleLine = true,
        shape = MacroOblici.utor,
        keyboardOptions = KeyboardOptions(capitalization = velikaSlova, keyboardType = tipkovnica, imeAction = imeAction),
        keyboardActions = keyboardActions,
        colors = bojePolja(),
        modifier = modifier.fillMaxWidth()
    )
}

/** Isto polje s [TextFieldValue], kad treba upravljati označenim tekstom (npr. ručni unos mase). */
@Composable
fun PoljeUnosa(
    vrijednost: TextFieldValue,
    onPromjena: (TextFieldValue) -> Unit,
    oznaka: String,
    modifier: Modifier = Modifier,
    pomoc: String? = null,
    greska: String? = null,
    tipkovnica: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Done,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    OutlinedTextField(
        value = vrijednost,
        onValueChange = onPromjena,
        label = { Text(oznaka) },
        supportingText = donjiTekst(pomoc, greska),
        isError = greska != null,
        singleLine = true,
        shape = MacroOblici.utor,
        keyboardOptions = KeyboardOptions(keyboardType = tipkovnica, imeAction = imeAction),
        keyboardActions = keyboardActions,
        colors = bojePolja(),
        modifier = modifier.fillMaxWidth()
    )
}

/** Pomoćni tekst ili greška ispod polja; greška zamjenjuje pomoćni tekst izblijeđivanjem. */
@Composable
private fun donjiTekst(pomoc: String?, greska: String?): (@Composable () -> Unit)? {
    if (greska == null && pomoc == null) return null
    val shema = MaterialTheme.colorScheme
    val smanjeno = rememberSmanjeniPokret()
    return {
        Crossfade(
            targetState = greska to pomoc,
            animationSpec = if (smanjeno) snap() else tween(Trajanje.kratko),
            label = "greskaPolja"
        ) { (g, p) ->
            Text(
                text = g ?: p.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = if (g != null) shema.error else shema.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun bojePolja(): TextFieldColors {
    val shema = MaterialTheme.colorScheme
    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor = shema.onSurface,
        unfocusedBorderColor = shema.outlineVariant,
        disabledBorderColor = shema.outlineVariant,
        focusedLabelColor = shema.onSurface,
        unfocusedLabelColor = shema.onSurfaceVariant,
        disabledLabelColor = shema.onSurfaceVariant,
        disabledTextColor = shema.onSurfaceVariant,
        disabledSupportingTextColor = shema.onSurfaceVariant,
        cursorColor = shema.onSurface
    )
}
