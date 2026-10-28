package com.niko.macromenza.ui.components

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
    val brojStavki = obrok.stavke.size
    DijalogPotvrde(
        naslov = stringResource(R.string.kom_brisanje_naslov, nazivTipa(obrok.tipObroka.uppercase()), obrok.vrijeme.take(5)),
        tekst = stringResource(
            R.string.kom_brisanje_tekst,
            pluralStringResource(R.plurals.kom_stavki, brojStavki, brojStavki),
            formatKcal(obrok.ukupnoKalorije)
        ),
        oznakaPotvrde = stringResource(R.string.kom_obrisi),
        destruktivna = true,
        onPotvrdi = onPotvrdi,
        onOdustani = onOdustani
    )
}
