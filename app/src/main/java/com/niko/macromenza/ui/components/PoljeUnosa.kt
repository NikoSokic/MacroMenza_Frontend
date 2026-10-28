package com.niko.macromenza.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import com.niko.macromenza.R
import com.niko.macromenza.ui.theme.MacroOblici
import com.niko.macromenza.ui.theme.Trajanje
import com.niko.macromenza.ui.theme.rememberSmanjeniPokret

/**
 * Polje unosa forme (pages/profil.md § Zajednički elementi): oznaka uvijek vidljiva, fokus u onSurface
 * (ne paprika), ispod pomoćni tekst ili greška. Greška zamjenjuje pomoćni tekst izblijeđivanjem.
 * Onemogućeno polje ostaje čitljivo (onSurfaceVariant, ne 38 %).
 *
 * [lozinka] = true: tekst je skriven uz gumb za prikaz i skrivanje (pages/prijava.md § Lozinka), a
 * tipkovnica je Password. [sadrzaj] je vrsta sadržaja za autofill (email, lozinka…).
 */
@OptIn(ExperimentalComposeUiApi::class)
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
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    lozinka: Boolean = false,
    sadrzaj: ContentType? = null
) {
    var vidljiva by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = vrijednost,
        onValueChange = onPromjena,
        label = { Text(oznaka) },
        supportingText = donjiTekst(pomoc, greska),
        isError = greska != null,
        enabled = enabled,
        singleLine = true,
        shape = MacroOblici.utor,
        visualTransformation = if (lozinka && !vidljiva) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = if (lozinka) {
            { GumbPrikazaLozinke(vidljiva = vidljiva, onPromjena = { vidljiva = it }) }
        } else null,
        keyboardOptions = if (lozinka) {
            KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Password,
                imeAction = imeAction
            )
        } else {
            KeyboardOptions(capitalization = velikaSlova, keyboardType = tipkovnica, imeAction = imeAction)
        },
        keyboardActions = keyboardActions,
        colors = bojePolja(),
        modifier = modifier
            .fillMaxWidth()
            .then(if (sadrzaj != null) Modifier.semantics { contentType = sadrzaj } else Modifier)
    )
}

/** Oko u polju lozinke: 48 dp, opis prema stanju, skala pritiska. */
@Composable
private fun GumbPrikazaLozinke(vidljiva: Boolean, onPromjena: (Boolean) -> Unit) {
    val izvor = remember { MutableInteractionSource() }
    IconButton(
        onClick = { onPromjena(!vidljiva) },
        interactionSource = izvor,
        modifier = Modifier.skalaPritiska(izvor)
    ) {
        Icon(
            imageVector = if (vidljiva) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
            contentDescription = stringResource(if (vidljiva) R.string.kom_sakrij_lozinku else R.string.kom_prikazi_lozinku)
        )
    }
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
