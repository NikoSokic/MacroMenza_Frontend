package com.niko.macromenza.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.niko.macromenza.R
import com.niko.macromenza.ui.theme.MacroDimenzije
import com.niko.macromenza.ui.theme.MacroMenzaTheme
import com.niko.macromenza.ui.theme.MacroOblici

/**
 * Jedini oblik dijaloga potvrde (MASTER § 1 i § 8): potvrda u dijalogu nije primarna akcija ekrana, pa je
 * destruktivna potvrda `Button` u error, a nedestruktivna potvrda i odustajanje su `TextButton` u onSurface.
 */
@Composable
fun DijalogPotvrde(
    naslov: String,
    tekst: String,
    oznakaPotvrde: String,
    destruktivna: Boolean,
    onPotvrdi: () -> Unit,
    onOdustani: () -> Unit,
    oznakaOdustani: String = stringResource(R.string.kom_odustani)
) {
    val shema = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onOdustani,
        shape = MacroOblici.pladanj,
        containerColor = shema.surface,
        title = {
            Text(naslov, style = MaterialTheme.typography.headlineSmall, color = shema.onSurface)
        },
        text = {
            Text(tekst, style = MaterialTheme.typography.bodyMedium, color = shema.onSurfaceVariant)
        },
        confirmButton = {
            val izvor = remember { MutableInteractionSource() }
            val oblik = Modifier
                .heightIn(min = MacroDimenzije.dodirniCilj)
                .skalaPritiska(izvor)
            if (destruktivna) {
                Button(
                    onClick = onPotvrdi,
                    interactionSource = izvor,
                    shape = MacroOblici.gumb,
                    colors = ButtonDefaults.buttonColors(containerColor = shema.error, contentColor = shema.onError),
                    modifier = oblik
                ) {
                    Text(oznakaPotvrde, style = MaterialTheme.typography.labelLarge)
                }
            } else {
                TextButton(onClick = onPotvrdi, interactionSource = izvor, modifier = oblik) {
                    Text(oznakaPotvrde, style = MaterialTheme.typography.labelLarge, color = shema.onSurface)
                }
            }
        },
        dismissButton = {
            val izvor = remember { MutableInteractionSource() }
            TextButton(
                onClick = onOdustani,
                interactionSource = izvor,
                modifier = Modifier
                    .heightIn(min = MacroDimenzije.dodirniCilj)
                    .skalaPritiska(izvor)
            ) {
                Text(oznakaOdustani, style = MaterialTheme.typography.labelLarge, color = shema.onSurface)
            }
        }
    )
}

// ---------- Previewi ----------

@Composable
private fun PreviewDijalog(tamna: Boolean, destruktivna: Boolean) {
    MacroMenzaTheme(darkTheme = tamna) {
        DijalogPotvrde(
            naslov = if (destruktivna) "Obrisati Ručak u 12:40?" else "Odjava",
            tekst = if (destruktivna) "3 stavke, 1040 kcal bit će trajno obrisane." else "Želiš li se odjaviti iz MacroMenze?",
            oznakaPotvrde = if (destruktivna) "Obriši" else "U redu",
            destruktivna = destruktivna,
            onPotvrdi = {},
            onOdustani = {}
        )
    }
}

@Preview(name = "Destruktivni, svijetla", showBackground = true)
@Composable
private fun PreviewDestruktivniSvijetla() = PreviewDijalog(tamna = false, destruktivna = true)

@Preview(name = "Destruktivni, tamna", showBackground = true)
@Composable
private fun PreviewDestruktivniTamna() = PreviewDijalog(tamna = true, destruktivna = true)

@Preview(name = "Nedestruktivni, svijetla", showBackground = true)
@Composable
private fun PreviewNedestruktivniSvijetla() = PreviewDijalog(tamna = false, destruktivna = false)

@Preview(name = "Nedestruktivni, tamna", showBackground = true)
@Composable
private fun PreviewNedestruktivniTamna() = PreviewDijalog(tamna = true, destruktivna = false)

@Preview(name = "Destruktivni, svijetla, fontScale 1,3", showBackground = true, fontScale = 1.3f)
@Composable
private fun PreviewDestruktivniVelikiFont() = PreviewDijalog(tamna = false, destruktivna = true)

@Preview(name = "Nedestruktivni, tamna, fontScale 1,3", showBackground = true, fontScale = 1.3f)
@Composable
private fun PreviewNedestruktivniVelikiFont() = PreviewDijalog(tamna = true, destruktivna = false)
