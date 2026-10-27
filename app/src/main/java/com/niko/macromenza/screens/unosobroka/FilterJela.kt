package com.niko.macromenza.screens.unosobroka

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
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

// Brzi filteri: najčešći rasponi po porciji. Proizvoljni raspon i dalje ide kroz "Raspon".
private const val BRZI_MAX_KCAL = 500.0
private const val BRZI_MIN_PROTEINI = 25.0
private const val BRZI_MAX_MASTI = 15.0

private class BrziFilter(
    val oznaka: @Composable () -> String,
    val aktivan: (MakroFilter) -> Boolean,
    val prebaci: (MakroFilter, Boolean) -> MakroFilter
)

private val BRZI_FILTERI = listOf(
    BrziFilter(
        oznaka = { stringResource(R.string.uo_do_kcal, formatKcal(BRZI_MAX_KCAL)) },
        aktivan = { it.maxKalorije == BRZI_MAX_KCAL },
        prebaci = { f, ukljuci -> f.copy(maxKalorije = if (ukljuci) BRZI_MAX_KCAL else null) }
    ),
    BrziFilter(
        oznaka = { stringResource(R.string.uo_proteini_od, formatKolicina(BRZI_MIN_PROTEINI)) },
        aktivan = { it.minProteini == BRZI_MIN_PROTEINI },
        prebaci = { f, ukljuci -> f.copy(minProteini = if (ukljuci) BRZI_MIN_PROTEINI else null) }
    ),
    BrziFilter(
        oznaka = { stringResource(R.string.uo_masti_do, formatKolicina(BRZI_MAX_MASTI)) },
        aktivan = { it.maxMasti == BRZI_MAX_MASTI },
        prebaci = { f, ukljuci -> f.copy(maxMasti = if (ukljuci) BRZI_MAX_MASTI else null) }
    )
)

/** Je li u filteru nešto osim brzih filtera (tada je "Raspon" označen). */
private fun imaVlastitiRaspon(f: MakroFilter): Boolean =
    BRZI_FILTERI.fold(f) { a, b -> if (b.aktivan(a)) b.prebaci(a, false) else a }.aktivan

/**
 * Čip filtera: odabir animira boju spremnika, teksta i ruba (Trajanje.kratko) i uvodi kvačicu koja se
 * povećava od nule. Čipovi s vlastitom ikonom ([ikona]) nemaju kvačicu, samo promjenu boje.
 * Kvačica proširuje čip pa se susjedi pomaknu odmah (bez animiranja širine); sama kvačica skalira graphicsLayerom.
 */
@Composable
fun MacroCip(
    oznaka: String,
    odabran: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    ikona: (@Composable () -> Unit)? = null
) {
    val shema = MaterialTheme.colorScheme
    val smanjeno = rememberSmanjeniPokret()
    val prijelaz = if (smanjeno) snap<Color>() else tween(Trajanje.kratko, easing = Ublazavanje.usporavanje)

    val spremnik by animateColorAsState(if (odabran) shema.secondaryContainer else shema.surface, prijelaz, label = "cipSpremnik")
    val tekst by animateColorAsState(if (odabran) shema.onSecondaryContainer else shema.onSurface, prijelaz, label = "cipTekst")
    val rub by animateColorAsState(if (odabran) shema.secondaryContainer else shema.outlineVariant, prijelaz, label = "cipRub")

    FilterChip(
        selected = odabran,
        onClick = onClick,
        modifier = modifier,
        label = { Text(oznaka, style = MaterialTheme.typography.labelLarge, maxLines = 1) },
        leadingIcon = ikona ?: if (odabran) {
            { Kvacica(tekst) }
        } else null,
        shape = MacroOblici.cip,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = spremnik,
            labelColor = tekst,
            iconColor = tekst,
            selectedContainerColor = spremnik,
            selectedLabelColor = tekst,
            selectedLeadingIconColor = tekst
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = odabran,
            borderColor = rub,
            selectedBorderColor = rub,
            borderWidth = MacroDimenzije.linija
        )
        // FilterChip sam proširuje dodirni cilj na 48 dp (minimumInteractiveComponentSize).
    )
}

/** Kvačica odabranog čipa: raste od nule pri prvom prikazu (Trajanje.kratko); previewi i smanjeni pokret je odmah pokazuju. */
@Composable
private fun Kvacica(boja: Color) {
    val bezPokreta = rememberSmanjeniPokret() || LocalInspectionMode.current
    val skala = remember { Animatable(if (bezPokreta) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (skala.value < 1f) skala.animateTo(1f, tween(Trajanje.kratko, easing = Ublazavanje.usporavanje))
    }
    Icon(
        imageVector = Icons.Rounded.Check,
        contentDescription = null,
        tint = boja,
        modifier = Modifier
            .size(MacroDimenzije.ikonaSitna)
            .graphicsLayer {
                scaleX = skala.value
                scaleY = skala.value
            }
    )
}

/** Čipovi koji se pojave ili nestanu ("Poništi filtere") guraju susjede uz Opruge.slijeganje umjesto skoka. */
@OptIn(ExperimentalFoundationApi::class)
private fun LazyItemScope.pomakCipa(smanjeno: Boolean): Modifier =
    if (smanjeno) Modifier else Modifier.animateItem(
        fadeInSpec = tween(Trajanje.kratko),
        placementSpec = Opruge.slijeganje<IntOffset>(),
        fadeOutSpec = tween(Trajanje.kratko, easing = Ublazavanje.ubrzavanje)
    )

/**
 * Traka filtera ispod pretrage: brzi rasponi, "Raspon" (sheet s min/max za kcal, P, U, M) i kategorije.
 * Odabrani čip ima kvačicu; ponovnim dodirom se odabir uklanja.
 */
@Composable
fun FilterTraka(
    filter: MakroFilter,
    kategorije: List<KategorijaJela>,
    kategorija: String?,
    onFilter: (MakroFilter) -> Unit,
    onRaspon: () -> Unit,
    onKategorija: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val smanjeno = rememberSmanjeniPokret()
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = MacroRazmaci.rubEkrana),
        horizontalArrangement = Arrangement.spacedBy(MacroRazmaci.s),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item(key = "raspon") {
            MacroCip(
                modifier = pomakCipa(smanjeno),
                oznaka = stringResource(R.string.uo_raspon),
                odabran = imaVlastitiRaspon(filter),
                onClick = onRaspon,
                ikona = { Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(MacroDimenzije.ikonaSitna)) }
            )
        }
        items(BRZI_FILTERI.size, key = { "brzi$it" }) { i ->
            val brzi = BRZI_FILTERI[i]
            val aktivan = brzi.aktivan(filter)
            MacroCip(
                modifier = pomakCipa(smanjeno),
                oznaka = brzi.oznaka(),
                odabran = aktivan,
                onClick = { onFilter(brzi.prebaci(filter, !aktivan)) }
            )
        }
        // Iza brzih filtera (ne na početku), da se pojavi uz čip koji je upravo dodirnut.
        if (filter.aktivan) {
            item(key = "ponisti") {
                MacroCip(
                    modifier = pomakCipa(smanjeno),
                    oznaka = stringResource(R.string.uo_ponisti_filtere),
                    odabran = false,
                    onClick = { onFilter(MakroFilter()) },
                    ikona = { Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(MacroDimenzije.ikonaSitna)) }
                )
            }
        }
        if (kategorije.isNotEmpty()) {
            item(key = "razdjelnik") {
                VerticalDivider(
                    modifier = Modifier.height(MacroDimenzije.ikona),
                    thickness = MacroDimenzije.linija,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
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
@OptIn(ExperimentalMaterial3Api::class)
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
            focusedBorderColor = shema.secondary,
            focusedLabelColor = shema.secondary,
            cursorColor = shema.onSurface,
            unfocusedBorderColor = shema.outlineVariant,
            focusedContainerColor = shema.surface,
            unfocusedContainerColor = shema.surface
        ),
        modifier = modifier
    )
}
