package com.niko.macromenza.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.niko.macromenza.R
import com.niko.macromenza.model.Obrok
import com.niko.macromenza.util.formatKcal

/**
 * Potvrda brisanja cijelog obroka ("Obrisati Ručak u 12:40?"). Koriste je Detalj dana i uređivanje obroka;
 * Home i kartica Dan u Povijesti brišu bez potvrde, uz "Poništi".
 */
@Composable
fun PotvrdaBrisanjaObrokaDialog(
    obrok: Obrok,
    onPotvrdi: () -> Unit,
    onOdustani: () -> Unit
) {
    val shema = MaterialTheme.colorScheme
    val brojStavki = obrok.stavke.size
    AlertDialog(
        onDismissRequest = onOdustani,
        containerColor = shema.surface,
        titleContentColor = shema.onSurface,
        textContentColor = shema.onSurfaceVariant,
        title = {
            Text(
                text = stringResource(R.string.kom_brisanje_naslov, nazivTipa(obrok.tipObroka.uppercase()), obrok.vrijeme.take(5)),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = stringResource(
                    R.string.kom_brisanje_tekst,
                    pluralStringResource(R.plurals.kom_stavki, brojStavki, brojStavki),
                    formatKcal(obrok.ukupnoKalorije)
                ),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onPotvrdi) {
                Text(stringResource(R.string.kom_obrisi), style = MaterialTheme.typography.labelLarge, color = shema.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onOdustani) {
                Text(stringResource(R.string.kom_odustani), style = MaterialTheme.typography.labelLarge, color = shema.onSurface)
            }
        }
    )
}
