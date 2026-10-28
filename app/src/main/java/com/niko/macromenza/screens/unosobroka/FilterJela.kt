package com.niko.macromenza.screens.unosobroka

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import com.niko.macromenza.R
import com.niko.macromenza.model.KategorijaJela
import com.niko.macromenza.ui.components.MacroCip
import com.niko.macromenza.ui.theme.MacroDimenzije
import com.niko.macromenza.ui.theme.MacroOblici
import com.niko.macromenza.ui.theme.MacroRazmaci
import com.niko.macromenza.ui.theme.Opruge
import com.niko.macromenza.ui.theme.Trajanje
import com.niko.macromenza.ui.theme.Ublazavanje
import com.niko.macromenza.ui.theme.rememberSmanjeniPokret
import com.niko.macromenza.util.formatKcal
import com.niko.macromenza.util.formatKolicina
import com.niko.macromenza.viewmodel.MakroFilter

// Predlošci u sheetu filtera: najčešći rasponi po porciji (prije zasebni čipovi u traci).
private const val BRZI_MAX_KCAL = 500.0
private const val BRZI_MIN_PROTEINI = 25.0
private const val BRZI_MAX_MASTI = 15.0

/** Čipovi koji se pojave ili nestanu ("Poništi filtere") guraju susjede uz Opruge.slijeganje umjesto skoka. */
@OptIn(ExperimentalFoundationApi::class)
private fun LazyItemScope.pomakCipa(smanjeno: Boolean): Modifier =
    if (smanjeno) Modifier else Modifier.animateItem(
        fadeInSpec = tween(Trajanje.kratko),
        placementSpec = Opruge.slijeganje<IntOffset>(),
        fadeOutSpec = tween(Trajanje.kratko, easing = Ublazavanje.ubrzavanje)
    )

/**
 * Traka ispod pretrage: kategorije (vodoravno listanje), a na kraju prikvačen jedan čip "Filteri" s brojem
 * aktivnih filtera, koji otvara sheet s rasponima i predlošcima. Odabrana kategorija ima kvačicu;
 * ponovnim dodirom se odabir uklanja.
 */
@Composable
fun FilterTraka(
    filter: MakroFilter,
    kategorije: List<KategorijaJela>,
    kategorija: String?,
    onFilteri: () -> Unit,
    onKategorija: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val smanjeno = rememberSmanjeniPokret()
    val aktivnih = brojAktivnihFiltera(filter)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LazyRow(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = MacroRazmaci.rubEkrana, end = MacroRazmaci.s),
            horizontalArrangement = Arrangement.spacedBy(MacroRazmaci.s),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(kategorije, key = { "k-${it.naziv}" }) { k ->
                val odabrana = kategorija == k.naziv
                MacroCip(
                    modifier = pomakCipa(smanjeno),
                    oznaka = k.naziv,
                    odabran = odabrana,
                    onClick = { onKategorija(if (odabrana) null else k.naziv) }
                )
            }
        }
        if (kategorije.isNotEmpty()) {
            VerticalDivider(
                modifier = Modifier.height(MacroDimenzije.ikona),
                thickness = MacroDimenzije.linija,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
        val oznaka = if (aktivnih > 0) stringResource(R.string.uo_filteri_broj, aktivnih) else stringResource(R.string.uo_filteri)
        MacroCip(
            modifier = Modifier.padding(start = MacroRazmaci.s, end = MacroRazmaci.rubEkrana),
            oznaka = oznaka,
            odabran = aktivnih > 0,
            onClick = onFilteri,
            ikona = { Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(MacroDimenzije.ikonaSitna)) }
        )
    }
}

private fun uTekst(vrijednost: Double?): String = vrijednost?.let { formatKolicina(it) } ?: ""

private fun uBroj(tekst: String): Double? = tekst.replace(',', '.').toDoubleOrNull()

private fun jeDecimalniUnos(unos: String): Boolean =
    unos.count { it == ',' || it == '.' } <= 1 &&
        unos.all { it.isDigit() || it == ',' || it == '.' }

/**
 * Proizvoljan raspon (min/max po porciji) za kalorije i proteine, a pod "Još filtera" i za ugljikohidrate i masti.
 * Polja kreću od trenutnog filtera, pa brzi filteri i raspon uvijek pokazuju isto.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterRasponaSheet(
    filter: MakroFilter,
    onPrimijeni: (MakroFilter) -> Unit,
    onResetiraj: () -> Unit,
    onDismiss: () -> Unit
) {
    val shema = MaterialTheme.colorScheme

    var minKalorije by rememberSaveable { mutableStateOf(uTekst(filter.minKalorije)) }
    var maxKalorije by rememberSaveable { mutableStateOf(uTekst(filter.maxKalorije)) }
    var minProteini by rememberSaveable { mutableStateOf(uTekst(filter.minProteini)) }
    var maxProteini by rememberSaveable { mutableStateOf(uTekst(filter.maxProteini)) }
    var minUgljikohidrati by rememberSaveable { mutableStateOf(uTekst(filter.minUgljikohidrati)) }
    var maxUgljikohidrati by rememberSaveable { mutableStateOf(uTekst(filter.maxUgljikohidrati)) }
    var minMasti by rememberSaveable { mutableStateOf(uTekst(filter.minMasti)) }
    var maxMasti by rememberSaveable { mutableStateOf(uTekst(filter.maxMasti)) }
    var prikaziOstalo by rememberSaveable {
        mutableStateOf(listOf(filter.minUgljikohidrati, filter.maxUgljikohidrati, filter.minMasti, filter.maxMasti).any { it != null })
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = shema.background,
        shape = MacroOblici.pladanj
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MacroRazmaci.rubEkrana)
                .padding(bottom = MacroRazmaci.xxl)
        ) {
            Text(
                text = stringResource(R.string.uo_filter_naslov),
                style = MaterialTheme.typography.headlineSmall,
                color = shema.onSurface
            )
            Spacer(Modifier.height(MacroRazmaci.xs))
            Text(
                text = stringResource(R.string.uo_filter_tekst),
                style = MaterialTheme.typography.bodyMedium,
                color = shema.onSurfaceVariant
            )

            // Predlošci samo popunjavaju polja; primjenjuju se gumbom "Primijeni filtere", kao i ručni unos.
            Text(
                text = stringResource(R.string.uo_brzi_odabir),
                style = MaterialTheme.typography.titleSmall,
                color = shema.onSurface,
                modifier = Modifier.padding(top = MacroRazmaci.l, bottom = MacroRazmaci.s)
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MacroRazmaci.s),
                verticalArrangement = Arrangement.spacedBy(MacroRazmaci.xs)
            ) {
                val doKcal = uBroj(maxKalorije) == BRZI_MAX_KCAL
                MacroCip(
                    oznaka = stringResource(R.string.uo_do_kcal, formatKcal(BRZI_MAX_KCAL)),
                    odabran = doKcal,
                    onClick = { maxKalorije = if (doKcal) "" else uTekst(BRZI_MAX_KCAL) }
                )
                val proteini = uBroj(minProteini) == BRZI_MIN_PROTEINI
                MacroCip(
                    oznaka = stringResource(R.string.uo_proteini_od, formatKolicina(BRZI_MIN_PROTEINI)),
                    odabran = proteini,
                    onClick = { minProteini = if (proteini) "" else uTekst(BRZI_MIN_PROTEINI) }
                )
                val masti = uBroj(maxMasti) == BRZI_MAX_MASTI
                MacroCip(
                    oznaka = stringResource(R.string.uo_masti_do, formatKolicina(BRZI_MAX_MASTI)),
                    odabran = masti,
                    onClick = {
                        maxMasti = if (masti) "" else uTekst(BRZI_MAX_MASTI)
                        // Polje za masti je pod "Još filtera": otvori ga da se vidi što je predložak upisao.
                        if (!masti) prikaziOstalo = true
                    }
                )
            }

            RasponPolja(stringResource(R.string.uo_kalorije), minKalorije, maxKalorije, { minKalorije = it }, { maxKalorije = it })
            RasponPolja(stringResource(R.string.kom_proteini), minProteini, maxProteini, { minProteini = it }, { maxProteini = it })

            TextButton(
                onClick = { prikaziOstalo = !prikaziOstalo },
                contentPadding = PaddingValues(horizontal = MacroRazmaci.xs),
                modifier = Modifier
                    .padding(top = MacroRazmaci.s)
                    .heightIn(min = MacroDimenzije.dodirniCilj)
            ) {
                Text(
                    text = stringResource(if (prikaziOstalo) R.string.uo_sakrij_filtere else R.string.uo_jos_filtera),
                    style = MaterialTheme.typography.labelLarge,
                    color = shema.primary
                )
            }

            if (prikaziOstalo) {
                RasponPolja(
                    stringResource(R.string.kom_ugljikohidrati), minUgljikohidrati, maxUgljikohidrati,
                    { minUgljikohidrati = it }, { maxUgljikohidrati = it }
                )
                RasponPolja(stringResource(R.string.kom_masti), minMasti, maxMasti, { minMasti = it }, { maxMasti = it })
            }

            Spacer(Modifier.height(MacroRazmaci.xl))

            Button(
                onClick = {
                    onPrimijeni(
                        MakroFilter(
                            minKalorije = uBroj(minKalorije),
                            maxKalorije = uBroj(maxKalorije),
                            minProteini = uBroj(minProteini),
                            maxProteini = uBroj(maxProteini),
                            minUgljikohidrati = uBroj(minUgljikohidrati),
                            maxUgljikohidrati = uBroj(maxUgljikohidrati),
                            minMasti = uBroj(minMasti),
                            maxMasti = uBroj(maxMasti)
                        )
                    )
                },
                shape = MacroOblici.gumb,
                colors = ButtonDefaults.buttonColors(containerColor = shema.primary, contentColor = shema.onPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = MacroDimenzije.fabVisina)
            ) {
                Text(stringResource(R.string.uo_primijeni), style = MaterialTheme.typography.labelLarge)
            }

            TextButton(
                onClick = {
                    minKalorije = ""; maxKalorije = ""; minProteini = ""; maxProteini = ""
                    minUgljikohidrati = ""; maxUgljikohidrati = ""; minMasti = ""; maxMasti = ""
                    prikaziOstalo = false
                    onResetiraj()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MacroRazmaci.s)
                    .heightIn(min = MacroDimenzije.dodirniCilj)
            ) {
                Text(
                    text = stringResource(R.string.uo_resetiraj),
                    style = MaterialTheme.typography.labelLarge,
                    color = shema.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RasponPolja(
    naslov: String,
    min: String,
    max: String,
    onMin: (String) -> Unit,
    onMax: (String) -> Unit
) {
    Text(
        text = naslov,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = MacroRazmaci.l, bottom = MacroRazmaci.s)
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MacroRazmaci.m)
    ) {
        BrojPolje(min, onMin, stringResource(R.string.uo_min), Modifier.weight(1f))
        BrojPolje(max, onMax, stringResource(R.string.uo_max), Modifier.weight(1f))
    }
}

@Composable
private fun BrojPolje(vrijednost: String, onChange: (String) -> Unit, oznaka: String, modifier: Modifier) {
    val shema = MaterialTheme.colorScheme
    OutlinedTextField(
        value = vrijednost,
        onValueChange = { if (jeDecimalniUnos(it)) onChange(it) },
        label = { Text(oznaka) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = MacroOblici.utor,
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = shema.onSurface,
            focusedLabelColor = shema.onSurface,
            unfocusedLabelColor = shema.onSurfaceVariant,
            cursorColor = shema.onSurface,
            unfocusedBorderColor = shema.outlineVariant,
            focusedContainerColor = shema.surface,
            unfocusedContainerColor = shema.surface
        ),
        modifier = modifier
    )
}
