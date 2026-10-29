package com.niko.macromenza.screens.povijest

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.niko.macromenza.R
import com.niko.macromenza.ui.components.MakroTrake
import com.niko.macromenza.ui.components.ObrokRed
import com.niko.macromenza.ui.components.ObrokUi
import com.niko.macromenza.ui.components.Tanjur
import com.niko.macromenza.ui.components.screenBottomPadding
import com.niko.macromenza.ui.components.screenTopPadding
import com.niko.macromenza.ui.components.skalaPritiska
import com.niko.macromenza.ui.theme.MacroDimenzije
import com.niko.macromenza.ui.theme.MacroMenzaTheme
import com.niko.macromenza.ui.theme.MacroOblici
import com.niko.macromenza.ui.theme.MacroOmjeri
import com.niko.macromenza.ui.theme.MacroRazmaci
import com.niko.macromenza.ui.theme.Opruge
import com.niko.macromenza.ui.theme.Trajanje
import com.niko.macromenza.ui.theme.macroBoje
import com.niko.macromenza.ui.theme.macroTipografija
import com.niko.macromenza.ui.theme.rememberSmanjeniPokret
import com.niko.macromenza.util.formatKcal
import java.time.LocalDate

/** "Utorak, 27. 10."; dan iz druge kalendarske godine dodaje godinu ("Srijeda, 31. 12. 2025."). */
@Composable
fun naslovDanaPovijesti(datum: LocalDate, danas: LocalDate): String {
    val dani = stringArrayResource(R.array.dp_dani_u_tjednu)
    val dan = dani[datum.dayOfWeek.value - 1]
    return if (datum.year == danas.year) stringResource(R.string.dp_datum, dan, datum.dayOfMonth, datum.monthValue)
    else stringResource(R.string.pov_datum_godina, dan, datum.dayOfMonth, datum.monthValue, datum.year)
}

/** Kartica Dan u Povijesti: ‹ datum ›, › onemogućena za danas (budući dani nemaju unos), ‹ bez granice. */
@Composable
fun ZaglavljeDana(
    datum: LocalDate,
    danas: LocalDate,
    onPrethodni: () -> Unit,
    onSljedeci: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shema = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = MacroRazmaci.xs, end = MacroRazmaci.xs, top = MacroRazmaci.s),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrethodni) {
            Icon(Icons.Rounded.ChevronLeft, contentDescription = stringResource(R.string.pov_prethodni_dan), tint = shema.onSurface)
        }
        Text(
            text = naslovDanaPovijesti(datum, danas),
            style = MaterialTheme.typography.titleMedium,
            color = shema.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() }
        )
        IconButton(onClick = onSljedeci, enabled = datum.isBefore(danas)) {
            Icon(Icons.Rounded.ChevronRight, contentDescription = stringResource(R.string.pov_sljedeci_dan))
        }
    }
}

/**
 * Sadržaj dana kao stavke liste: zbroj (Tanjur, kcal prema cilju, MakroTrake), naslov "Obroci" i ObrokRed po obroku.
 * Isti za karticu Dan u Povijesti i Detalj dana. [ukras] dodaje stavci pokret koji određuje ekran (animateItem, listanje).
 * [brisanjeSPotvrdom]: Detalj dana traži potvrdu, kartica Dan briše odmah uz "Poništi" (kao Home).
 */
fun LazyListScope.stavkeDana(
    stanje: DanStanje,
    prosireni: List<Long>,
    smanjeno: Boolean,
    onProsiri: (Long) -> Unit,
    onUredi: (ObrokUi) -> Unit,
    onObrisi: (ObrokUi) -> Unit,
    brisanjeSPotvrdom: Boolean = false,
    ukras: LazyItemScope.() -> Modifier = { Modifier }
) {
    val rub = Modifier.padding(horizontal = MacroRazmaci.rubEkrana)
    when (stanje) {
        DanStanje.Ucitavanje -> item(key = "dan_kostur") {
            KosturDana(smanjeno, ukras().then(rub).padding(top = MacroRazmaci.l))
        }

        is DanStanje.Sadrzaj -> {
            val dan = stanje.dan
            if (dan.jePrazan) {
                item(key = "dan_prazno") {
                    PrazanDan(ukras().then(rub).padding(top = MacroRazmaci.l))
                }
            } else {
                item(key = "dan_zbroj") {
                    ZbrojDana(dan, smanjeno, ukras().then(rub).padding(top = MacroRazmaci.l))
                }
                item(key = "dan_naslov_obroci") {
                    Text(
                        text = stringResource(R.string.pov_obroci),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = ukras()
                            .then(rub)
                            .padding(top = MacroRazmaci.iznadSekcije, bottom = MacroRazmaci.ispodNaslovaSekcije)
                            .semantics { heading() }
                    )
                }
                items(dan.obroci, key = { it.id }) { obrok ->
                    ObrokRed(
                        obrok = obrok,
                        prosiren = obrok.id in prosireni,
                        smanjeno = smanjeno,
                        onProsiri = { onProsiri(obrok.id) },
                        onUredi = { onUredi(obrok) },
                        onObrisi = { onObrisi(obrok) },
                        brisanjeSPotvrdom = brisanjeSPotvrdom,
                        modifier = ukras()
                            .then(rub)
                            .padding(bottom = MacroRazmaci.s)
                    )
                }
            }
        }
    }
}

/** Tanjur (tanjurMax) i "1510 kcal" + "od 2400 kcal" ili "Preko cilja 257 kcal", ispod MakroTrake. */
@Composable
private fun ZbrojDana(dan: DanUi, smanjeno: Boolean, modifier: Modifier = Modifier) {
    val shema = MaterialTheme.colorScheme
    val boje = MaterialTheme.macroBoje
    val tip = MaterialTheme.typography
    val kcal = formatKcal(dan.ukupno.kcal)
    val cilj = dan.ciljKcal?.takeIf { it > 0 }
    val drugiRed = when {
        cilj == null -> null
        dan.prekoCilja -> stringResource(R.string.pov_dan_preko_cilja, formatKcal(dan.ukupno.kcal - cilj))
        else -> stringResource(R.string.pov_dan_od_cilja, formatKcal(cilj))
    }
    val opis = when {
        cilj == null -> stringResource(R.string.pov_dan_zbroj_opis_bez_cilja, kcal)
        dan.prekoCilja -> stringResource(R.string.pov_dan_zbroj_opis_preko, kcal, formatKcal(cilj), formatKcal(dan.ukupno.kcal - cilj))
        else -> stringResource(R.string.pov_dan_zbroj_opis, kcal, formatKcal(cilj))
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clearAndSetSemantics { contentDescription = opis },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Promjer je stalan; udjeli ruba prelaze na novi dan uz Opruge.blaga (u Tanjuru).
            Tanjur(
                makro = dan.ukupno,
                promjer = MacroDimenzije.tanjurMax,
                okvir = MacroDimenzije.tanjurMax,
                smanjeno = smanjeno
            )
            Spacer(Modifier.width(MacroRazmaci.l))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.pov_kcal, kcal), style = tip.titleMedium, color = shema.onSurface)
                drugiRed?.let {
                    Text(it, style = tip.bodyMedium, color = if (dan.prekoCilja) boje.visnja else shema.onSurfaceVariant)
                }
            }
        }
        MakroTrake(
            pojedeno = dan.ukupno,
            cilj = dan.ciljMakro,
            animirajUlaz = false,
            smanjeno = smanjeno,
            modifier = Modifier.padding(top = MacroRazmaci.xl)
        )
    }
}

@Composable
private fun PrazanDan(modifier: Modifier = Modifier) {
    val shema = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(MacroDimenzije.obrubIsprekidan, shema.outlineVariant, MacroOblici.kartica)
            .padding(MacroRazmaci.xl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.pov_dan_prazno_naslov),
            style = MaterialTheme.typography.titleMedium,
            color = shema.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(MacroRazmaci.xs))
        Text(
            text = stringResource(R.string.pov_dan_prazno_tekst),
            style = MaterialTheme.typography.bodyMedium,
            color = shema.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/** Učitavanje dana: kostur tanjura, traka i dva reda obroka (bez spinnera), s blagim pulsiranjem. */
@Composable
private fun KosturDana(smanjeno: Boolean, modifier: Modifier = Modifier) {
    val boje = MaterialTheme.macroBoje
    val opis = stringResource(R.string.pov_dan_ucitavanje)
    val puls = if (smanjeno) null else rememberInfiniteTransition(label = "kosturDana").animateFloat(
        initialValue = 0.55f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(Trajanje.disanje / 2), RepeatMode.Reverse),
        label = "puls"
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = puls?.value ?: 1f }
            .semantics { contentDescription = opis }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(MacroDimenzije.tanjurMax).background(boje.kostur, CircleShape))
            Spacer(Modifier.width(MacroRazmaci.l))
            Box(
                Modifier
                    .fillMaxWidth(MacroOmjeri.kosturNaziv)
                    .height(MacroDimenzije.kosturNaziv)
                    .background(boje.kostur, MacroOblici.traka)
            )
        }
        Spacer(Modifier.height(MacroRazmaci.iznadSekcije))
        repeat(2) {
            Box(
                Modifier
                    .padding(top = MacroRazmaci.s)
                    .fillMaxWidth()
                    .height(MacroDimenzije.dodirniCilj + MacroRazmaci.l)
                    .background(boje.kostur, MacroOblici.kartica)
            )
        }
    }
}

/**
 * Detalj dana (otvara se s Home ‹): ← natrag, "DETALJ DANA" i datum, zatim isti sadržaj kao kartica Dan.
 * Bez listanja dana; brisanje traži potvrdu ([onObrisi] otvara dijalog).
 */
@Composable
fun DetaljDanaSadrzaj(
    datum: LocalDate,
    danas: LocalDate,
    stanje: DanStanje,
    onNatrag: () -> Unit,
    onUredi: (ObrokUi) -> Unit,
    onObrisi: (ObrokUi) -> Unit,
    modifier: Modifier = Modifier,
    pocetnoProsireni: List<Long> = emptyList(),
    snackbarHost: @Composable BoxScope.() -> Unit = {}
) {
    val shema = MaterialTheme.colorScheme
    val smanjeno = rememberSmanjeniPokret()
    var prosireni by rememberSaveable { mutableStateOf(pocetnoProsireni) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(shema.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = screenTopPadding(), bottom = screenBottomPadding())
        ) {
            item(key = "zaglavlje") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MacroRazmaci.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val izvor = remember { MutableInteractionSource() }
                    IconButton(onClick = onNatrag, interactionSource = izvor, modifier = Modifier.skalaPritiska(izvor)) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.kom_natrag), tint = shema.onSurface)
                    }
                    Spacer(Modifier.width(MacroRazmaci.xs))
                    Column(modifier = Modifier.semantics(mergeDescendants = true) { heading() }) {
                        Text(
                            text = stringResource(R.string.pov_detalj_dana).uppercase(),
                            style = MaterialTheme.macroTipografija.nadnaslov,
                            color = shema.onSurfaceVariant
                        )
                        Text(
                            text = naslovDanaPovijesti(datum, danas),
                            style = MaterialTheme.typography.titleMedium,
                            color = shema.onSurface
                        )
                    }
                }
            }
            stavkeDana(
                stanje = stanje,
                prosireni = prosireni,
                smanjeno = smanjeno,
                onProsiri = { id -> prosireni = if (id in prosireni) prosireni - id else prosireni + id },
                onUredi = onUredi,
                onObrisi = onObrisi,
                brisanjeSPotvrdom = true,
                ukras = {
                    if (smanjeno) Modifier else Modifier.animateItem(
                        fadeInSpec = tween(Trajanje.kratko),
                        placementSpec = Opruge.slijeganje(),
                        fadeOutSpec = tween(Trajanje.kratko)
                    )
                }
            )
        }
        snackbarHost()
    }
}

// ---------- Previewi (lažni podaci iz PovijestPrimjeri.kt) ----------

@Composable
private fun PreviewDan(tamna: Boolean, stanje: DanStanje, prosireni: List<Long> = emptyList()) {
    MacroMenzaTheme(darkTheme = tamna) {
        PovijestSadrzaj(
            stanje = sadrzajTjedna(),
            odabraniTab = PovijestTab.DNEVNI,
            primjerUTijeku = false,
            odabraniDan = stanjeDatum(stanje),
            danStanje = stanje,
            danas = DANAS_PRIMJERA,
            akcije = PovijestAkcije(),
            puniUlaz = false,
            pocetnoProsireni = prosireni
        )
    }
}

private fun sadrzajTjedna() = tjedanStanje(PRIMJER_TJEDAN_NORMALNO, greska = false, danas = DANAS_PRIMJERA)

private fun stanjeDatum(stanje: DanStanje) = (stanje as? DanStanje.Sadrzaj)?.dan?.datum ?: DATUM_DANA_PRIMJERA

@Composable
private fun PreviewDetalj(tamna: Boolean, stanje: DanStanje, prosireni: List<Long> = emptyList()) {
    MacroMenzaTheme(darkTheme = tamna) {
        DetaljDanaSadrzaj(
            datum = stanjeDatum(stanje),
            danas = DANAS_PRIMJERA,
            stanje = stanje,
            onNatrag = {},
            onUredi = {},
            onObrisi = {},
            pocetnoProsireni = prosireni
        )
    }
}

private const val VISINA_DANA = 1300

@Preview(name = "Dan · svijetla · s obrocima (cilj P/M/U iz tjednog prosjeka)", widthDp = 360, heightDp = VISINA_DANA)
@Composable
private fun PreviewDanSvijetlaObroci() = PreviewDan(false, DanStanje.Sadrzaj(DAN_PRIMJER_U_CILJU))

@Preview(name = "Dan · tamna · s obrocima", widthDp = 360, heightDp = VISINA_DANA)
@Composable
private fun PreviewDanTamnaObroci() = PreviewDan(true, DanStanje.Sadrzaj(DAN_PRIMJER_U_CILJU))

@Preview(name = "Dan · svijetla · preko cilja", widthDp = 360, heightDp = VISINA_DANA)
@Composable
private fun PreviewDanSvijetlaPreko() = PreviewDan(false, DanStanje.Sadrzaj(DAN_PRIMJER_PREKO_CILJA))

@Preview(name = "Dan · tamna · preko cilja", widthDp = 360, heightDp = VISINA_DANA)
@Composable
private fun PreviewDanTamnaPreko() = PreviewDan(true, DanStanje.Sadrzaj(DAN_PRIMJER_PREKO_CILJA))

@Preview(name = "Dan · svijetla · prazan dan", widthDp = 360, heightDp = 800)
@Composable
private fun PreviewDanSvijetlaPrazan() = PreviewDan(false, DanStanje.Sadrzaj(DAN_PRIMJER_PRAZAN))

@Preview(name = "Dan · tamna · prazan dan", widthDp = 360, heightDp = 800)
@Composable
private fun PreviewDanTamnaPrazan() = PreviewDan(true, DanStanje.Sadrzaj(DAN_PRIMJER_PRAZAN))

@Preview(name = "Dan · svijetla · učitavanje", widthDp = 360, heightDp = 800)
@Composable
private fun PreviewDanSvijetlaUcitavanje() = PreviewDan(false, DanStanje.Ucitavanje)

@Preview(name = "Dan · tamna · učitavanje", widthDp = 360, heightDp = 800)
@Composable
private fun PreviewDanTamnaUcitavanje() = PreviewDan(true, DanStanje.Ucitavanje)

@Preview(name = "Detalj dana · svijetla · proširen ručak", widthDp = 360, heightDp = VISINA_DANA)
@Composable
private fun PreviewDetaljSvijetla() = PreviewDetalj(false, DanStanje.Sadrzaj(DAN_PRIMJER_PREKO_CILJA), prosireni = listOf(ID_RUCKA_PRIMJERA))

@Preview(name = "Detalj dana · tamna", widthDp = 360, heightDp = VISINA_DANA)
@Composable
private fun PreviewDetaljTamna() = PreviewDetalj(true, DanStanje.Sadrzaj(DAN_PRIMJER_U_CILJU))

@Preview(name = "Dan · svijetla · preko cilja · fontScale 1,3", widthDp = 360, heightDp = 1800, fontScale = 1.3f)
@Composable
private fun PreviewDanVelikiFont() = PreviewDan(false, DanStanje.Sadrzaj(DAN_PRIMJER_PREKO_CILJA), prosireni = listOf(ID_RUCKA_PRIMJERA))

@Preview(name = "Detalj dana · tamna · fontScale 1,3", widthDp = 360, heightDp = 1800, fontScale = 1.3f)
@Composable
private fun PreviewDetaljVelikiFont() = PreviewDetalj(true, DanStanje.Sadrzaj(DAN_PRIMJER_U_CILJU), prosireni = listOf(ID_RUCKA_PRIMJERA))
