package com.niko.macromenza.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.lerp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.niko.macromenza.R
import com.niko.macromenza.model.JeloDetalj
import com.niko.macromenza.model.JeloSastojak
import com.niko.macromenza.ui.components.GumbSpremi
import com.niko.macromenza.ui.components.KoracnikPorcija
import com.niko.macromenza.ui.components.KosturOblik
import com.niko.macromenza.ui.components.Makro
import com.niko.macromenza.ui.components.TablicniBroj
import com.niko.macromenza.ui.components.makroKratko
import com.niko.macromenza.ui.components.skalaPritiska
import com.niko.macromenza.ui.components.Tanjur
import com.niko.macromenza.ui.components.tekstPorcije
import com.niko.macromenza.ui.theme.MacroDimenzije
import com.niko.macromenza.ui.theme.MacroMenzaTheme
import com.niko.macromenza.ui.theme.MacroOmjeri
import com.niko.macromenza.ui.theme.MacroRazmaci
import com.niko.macromenza.ui.theme.Opruge
import com.niko.macromenza.ui.theme.macroTipografija
import com.niko.macromenza.ui.theme.opruga
import com.niko.macromenza.ui.theme.rememberSmanjeniPokret
import com.niko.macromenza.util.formatCijena
import com.niko.macromenza.util.formatGrami
import com.niko.macromenza.util.formatKcal
import com.niko.macromenza.viewmodel.DetaljStanje
import com.niko.macromenza.viewmodel.JeloDetaljViewModel

/** Tanjur raste s količinom: 0,5 porcije je najmanji, 3 porcije i više najveći. */
private const val KOLICINA_MIN_TANJURA = 0.5
private const val RASPON_KOLICINE_TANJURA = 2.5

/**
 * Detalj jela u donjem listu: tanjur i vrijednosti za odabranu količinu, masa, cijena, napomena i sastojci iz recepture.
 * [onDodaj] dobiva odabrani broj porcija (koraci po 0,5); kad je null, sheet je samo pregled
 * (bez odabira porcija i gumba "Dodaj u obrok").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JeloDetaljSheet(
    idJelo: Long,
    onDismiss: () -> Unit,
    onDodaj: ((Double) -> Unit)? = null,
    onOdakleSuKalorije: (() -> Unit)? = null,
    viewModel: JeloDetaljViewModel = viewModel()
) {
    val stanje by viewModel.stanje.collectAsState()
    var porcije by remember(idJelo) { mutableDoubleStateOf(1.0) }

    LaunchedEffect(idJelo) {
        viewModel.ucitaj(idJelo)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = MacroOmjeri.rucka)
            )
        }
    ) {
        JeloDetaljSadrzaj(
            stanje = stanje,
            idJelo = idJelo,
            porcije = porcije,
            onPorcije = { porcije = it },
            onDodaj = onDodaj,
            onOdakleSuKalorije = onOdakleSuKalorije,
            onPonovi = { viewModel.ucitaj(idJelo) }
        )
    }
}

/** Sadržaj sheeta bez ViewModela (i za previewe). */
@Composable
private fun JeloDetaljSadrzaj(
    stanje: DetaljStanje,
    idJelo: Long,
    porcije: Double,
    onPorcije: (Double) -> Unit,
    onDodaj: ((Double) -> Unit)?,
    onOdakleSuKalorije: (() -> Unit)?,
    onPonovi: () -> Unit
) {
    val smanjeno = rememberSmanjeniPokret()
    // Bez odabira porcija (samo pregled) vrijednosti su uvijek za jednu porciju.
    val kolicina = if (onDodaj != null) porcije else 1.0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MacroRazmaci.rubEkrana)
            .padding(bottom = MacroRazmaci.xl)
    ) {
        // Dok ViewModel ne učita traženo jelo, ne prikazuje se detalj prethodno otvorenog.
        when {
            stanje is DetaljStanje.Podaci && stanje.detalj.id == idJelo -> {
                DetaljPodaci(
                    detalj = stanje.detalj,
                    kolicina = kolicina,
                    smanjeno = smanjeno,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            stanje is DetaljStanje.Greska -> GreskaDetalja(poruka = stanje.poruka, onPonovi = onPonovi)

            else -> KosturDetalja()
        }

        Spacer(modifier = Modifier.height(MacroRazmaci.l))

        if (onDodaj != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MacroRazmaci.m)
            ) {
                KoracnikPorcija(
                    kolicina = porcije,
                    onMinus = { if (porcije > 0.5) onPorcije(porcije - 0.5) },
                    onPlus = { onPorcije(porcije + 0.5) },
                    smanjeno = smanjeno,
                    minusOmogucen = porcije > 0.5
                )

                GumbSpremi(
                    tekst = stringResource(R.string.jd_dodaj),
                    onClick = { onDodaj(porcije) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(MacroRazmaci.m))
        }

        Okvirnost(onOdakleSuKalorije)
    }
}

@Composable
private fun DetaljPodaci(
    detalj: JeloDetalj,
    kolicina: Double,
    smanjeno: Boolean,
    modifier: Modifier = Modifier
) {
    val shema = MaterialTheme.colorScheme
    val tip = MaterialTheme.typography

    // Brojevi i tanjur prelaze oprugom kad se promijeni količina.
    val prikaz = remember(detalj.id) { Animatable(kolicina.toFloat()) }
    LaunchedEffect(kolicina) { prikaz.animateTo(kolicina.toFloat(), opruga(Opruge.blaga(), smanjeno)) }

    val makro = Makro(detalj.kalorije, detalj.proteini, detalj.masti, detalj.ugljikohidrati)
    val k = prikaz.value.toDouble()
    val prikazano = Makro(makro.kcal * k, makro.proteini * k, makro.masti * k, makro.ugljikohidrati * k)
    val konacno = Makro(makro.kcal * kolicina, makro.proteini * kolicina, makro.masti * kolicina, makro.ugljikohidrati * kolicina)

    val udioTanjura = ((kolicina - KOLICINA_MIN_TANJURA) / RASPON_KOLICINE_TANJURA).coerceIn(0.0, 1.0).toFloat()
    val promjer = lerp(MacroDimenzije.tanjurMin, MacroDimenzije.tanjurMax, udioTanjura)
    val opisTanjura = stringResource(R.string.jd_tanjur_opis, formatKcal(konacno.kcal), makroKratko(konacno))

    LazyColumn(modifier = modifier) {
        item {
            Text(
                text = detalj.naziv,
                style = tip.titleMedium,
                color = shema.onSurface,
                modifier = Modifier.semantics { heading() }
            )

            detalj.kategorija?.let {
                Text(text = it, style = tip.bodyMedium, color = shema.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(MacroRazmaci.l))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Tanjur(
                    makro = makro,
                    promjer = promjer,
                    okvir = MacroDimenzije.tanjurMax,
                    smanjeno = smanjeno,
                    prikaziKcal = false,
                    modifier = Modifier.semantics { contentDescription = opisTanjura }
                )

                Spacer(modifier = Modifier.width(MacroRazmaci.l))

                Column(modifier = Modifier
                    .weight(1f)
                    .clearAndSetSemantics { }
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        TablicniBroj(
                            tekst = { formatKcal(prikazano.kcal) },
                            stil = MaterialTheme.macroTipografija.brojSrednji,
                            boja = shema.onSurface
                        )
                        Spacer(Modifier.width(MacroRazmaci.xs))
                        Text(text = stringResource(R.string.jd_kcal), style = tip.bodySmall, color = shema.onSurfaceVariant)
                    }
                    Text(
                        text = if (kolicina == 1.0) stringResource(R.string.jd_po_porciji)
                        else stringResource(R.string.jd_za_kolicinu, tekstPorcije(kolicina)),
                        style = tip.labelMedium,
                        color = shema.onSurfaceVariant
                    )
                    Spacer(Modifier.height(MacroRazmaci.xs))
                    Text(text = makroKratko(prikazano), style = tip.bodyMedium, color = shema.onSurface)
                }
            }

            val masaICijena = listOfNotNull(
                detalj.masaG?.let { stringResource(R.string.jd_masa, formatGrami(it)) },
                detalj.cijenaEur?.let { stringResource(R.string.jd_cijena, formatCijena(it)) }
            )
            if (masaICijena.isNotEmpty()) {
                Spacer(modifier = Modifier.height(MacroRazmaci.s))
                Text(
                    text = masaICijena.joinToString(stringResource(R.string.jd_razdjelnik)),
                    style = tip.bodySmall,
                    color = shema.onSurfaceVariant
                )
            }

            if (!detalj.napomena.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(MacroRazmaci.l))

                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = stringResource(R.string.jd_napomena),
                        tint = shema.onSurfaceVariant,
                        modifier = Modifier
                            .padding(top = MacroRazmaci.xxs)
                            .size(MacroDimenzije.ikonaMala)
                    )
                    Spacer(modifier = Modifier.width(MacroRazmaci.s))
                    Text(text = detalj.napomena, style = tip.bodyMedium, color = shema.onSurface)
                }
            }

            Spacer(modifier = Modifier.height(MacroRazmaci.xl))

            Text(
                text = stringResource(R.string.jd_sastojci),
                style = tip.titleMedium,
                color = shema.onSurface,
                modifier = Modifier.semantics { heading() }
            )

            Spacer(modifier = Modifier.height(MacroRazmaci.xs))

            if (detalj.sastojci.isEmpty()) {
                Text(
                    text = stringResource(R.string.jd_nema_recepture),
                    style = tip.bodyMedium,
                    color = shema.onSurfaceVariant
                )
            }
        }

        itemsIndexed(detalj.sastojci) { i, sastojak ->
            SastojakRed(sastojak, crta = i > 0)
        }
    }
}

@Composable
private fun SastojakRed(sastojak: JeloSastojak, crta: Boolean) {
    val shema = MaterialTheme.colorScheme
    val tip = MaterialTheme.typography
    Column {
        if (crta) HorizontalDivider(color = shema.outlineVariant, thickness = MacroDimenzije.linija)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MacroDimenzije.dodirniCilj)
                .padding(vertical = MacroRazmaci.s),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = sastojak.nazivURecepturi,
                modifier = Modifier.weight(1f),
                style = tip.bodyMedium,
                color = shema.onSurface
            )
            Text(
                text = stringResource(R.string.jd_grami, formatGrami(sastojak.grami)),
                modifier = Modifier.widthIn(min = MacroDimenzije.stupacGrami),
                style = tip.bodyMedium,
                color = shema.onSurface,
                textAlign = TextAlign.End
            )
            Text(
                text = stringResource(R.string.jd_kcal_vrijednost, formatKcal(sastojak.kalorije)),
                modifier = Modifier.widthIn(min = MacroDimenzije.stupacKcal),
                style = tip.bodyMedium,
                color = shema.onSurfaceVariant,
                textAlign = TextAlign.End
            )
        }
    }
}

/** Okvirnost i link „Odakle su kalorije?" (kao podnožje Home). */
@Composable
private fun Okvirnost(onOdakleSuKalorije: (() -> Unit)?) {
    val shema = MaterialTheme.colorScheme
    Column {
        Text(
            text = stringResource(R.string.jd_okvirno),
            style = MaterialTheme.typography.bodySmall,
            color = shema.onSurfaceVariant
        )
        if (onOdakleSuKalorije != null) {
            val izvor = remember { MutableInteractionSource() }
            TextButton(
                onClick = onOdakleSuKalorije,
                interactionSource = izvor,
                contentPadding = PaddingValues(),
                modifier = Modifier
                    .heightIn(min = MacroDimenzije.dodirniCilj)
                    .skalaPritiska(izvor)
            ) {
                Text(stringResource(R.string.jd_odakle), style = MaterialTheme.typography.labelLarge, color = shema.primary)
            }
        }
    }
}

/** Tihi red greške s „Pokušaj ponovno" u primary (kao Moje mjere). */
@Composable
private fun GreskaDetalja(poruka: String, onPonovi: () -> Unit) {
    val shema = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MacroRazmaci.l),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = shema.error,
            modifier = Modifier.size(MacroDimenzije.ikona)
        )
        Spacer(Modifier.width(MacroRazmaci.m))
        Text(
            text = poruka,
            style = MaterialTheme.typography.bodyMedium,
            color = shema.onSurface,
            modifier = Modifier.weight(1f)
        )
        val izvor = remember { MutableInteractionSource() }
        TextButton(
            onClick = onPonovi,
            interactionSource = izvor,
            modifier = Modifier
                .heightIn(min = MacroDimenzije.dodirniCilj)
                .skalaPritiska(izvor)
        ) {
            Text(stringResource(R.string.jd_pokusaj_ponovno), style = MaterialTheme.typography.labelLarge, color = shema.primary)
        }
    }
}

/** Kostur: naslov, tanjur s brojem i tri reda sastojaka. */
@Composable
private fun KosturDetalja() {
    val opis = stringResource(R.string.jd_ucitavanje)
    Column(modifier = Modifier
        .fillMaxWidth()
        .semantics { contentDescription = opis }
    ) {
        KosturOblik(Modifier.fillMaxWidth(MacroOmjeri.kosturNaziv).height(MacroDimenzije.kosturNaziv))
        Spacer(Modifier.height(MacroRazmaci.l))
        Row(verticalAlignment = Alignment.CenterVertically) {
            KosturOblik(Modifier.size(MacroDimenzije.tanjurMax), oblik = CircleShape)
            Spacer(Modifier.width(MacroRazmaci.l))
            Column(modifier = Modifier.weight(1f)) {
                KosturOblik(Modifier.fillMaxWidth(MacroOmjeri.kosturNaziv).height(MacroDimenzije.kosturNaziv))
                Spacer(Modifier.height(MacroRazmaci.s))
                KosturOblik(Modifier.fillMaxWidth(MacroOmjeri.kosturMakro).height(MacroDimenzije.kosturMakro))
            }
        }
        Spacer(Modifier.height(MacroRazmaci.xl))
        repeat(3) { i ->
            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = MacroDimenzije.linija)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = MacroDimenzije.dodirniCilj),
                contentAlignment = Alignment.CenterStart
            ) {
                KosturOblik(Modifier.fillMaxWidth(MacroOmjeri.kosturNaziv).height(MacroDimenzije.kosturMakro))
            }
        }
    }
}

// ---------- Previewi (⚠ lažni podaci: Sarma iz jela.csv / jelo_sastojak.csv, vrijednosti okvirne i ručno upisane) ----------

private fun sastojak(naziv: String, grami: Double, kcal: Double) =
    JeloSastojak(naziv, "", grami, kcal, 0.0, 0.0, 0.0)

private val SARMA = JeloDetalj(
    id = 37, naziv = "Sarma", opis = null,
    kalorije = 452.0, proteini = 22.0, ugljikohidrati = 26.0, masti = 29.0,
    sifra = null, kategorija = "Glavna jela", cijenaEur = 1.79, napomena = null, masaG = 412.0,
    sastojci = listOf(
        sastojak("kiseli kupus – glavica", 250.0, 48.0),
        sastojak("svinjetina I i II (obr. za mljev.)", 72.0, 190.0),
        sastojak("junetina I i II (obr. za mljev.)", 48.0, 96.0),
        sastojak("suha slanina", 10.0, 54.0),
        sastojak("riža", 10.0, 36.0),
        sastojak("crveni luk", 10.0, 4.0),
        sastojak("ulje", 5.0, 44.0)
    )
)

private val JUHA_GRAH = JeloDetalj(
    id = 4, naziv = "Juha od graha-krem", opis = null,
    kalorije = 60.0, proteini = 3.0, ugljikohidrati = 9.0, masti = 1.0,
    sifra = null, kategorija = "Juhe", cijenaEur = 0.39,
    napomena = "Gotova juha iz vrećice; okvirna vrijednost.", masaG = 200.0,
    sastojci = emptyList()
)

@Composable
private fun PreviewDetalj(tamna: Boolean, stanje: DetaljStanje, idJelo: Long = 37, porcije: Double = 1.5, dodavanje: Boolean = true) {
    MacroMenzaTheme(darkTheme = tamna) {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(top = MacroRazmaci.l)) {
            JeloDetaljSadrzaj(
                stanje = stanje,
                idJelo = idJelo,
                porcije = porcije,
                onPorcije = {},
                onDodaj = if (dodavanje) ({}) else null,
                onOdakleSuKalorije = {},
                onPonovi = {}
            )
        }
    }
}

@Preview(name = "Detalj jela · svijetla · normalno", widthDp = 360, heightDp = 900)
@Composable
private fun PreviewDetaljSvijetla() = PreviewDetalj(false, DetaljStanje.Podaci(SARMA))

@Preview(name = "Detalj jela · tamna · normalno", widthDp = 360, heightDp = 900)
@Composable
private fun PreviewDetaljTamna() = PreviewDetalj(true, DetaljStanje.Podaci(SARMA))

@Preview(name = "Detalj jela · svijetla · samo pregled", widthDp = 360, heightDp = 900)
@Composable
private fun PreviewDetaljPregled() = PreviewDetalj(false, DetaljStanje.Podaci(SARMA), dodavanje = false)

@Preview(name = "Detalj jela · svijetla · s napomenom", widthDp = 360, heightDp = 600)
@Composable
private fun PreviewDetaljNapomenaSvijetla() = PreviewDetalj(false, DetaljStanje.Podaci(JUHA_GRAH), idJelo = 4, porcije = 1.0)

@Preview(name = "Detalj jela · tamna · s napomenom", widthDp = 360, heightDp = 600)
@Composable
private fun PreviewDetaljNapomenaTamna() = PreviewDetalj(true, DetaljStanje.Podaci(JUHA_GRAH), idJelo = 4, porcije = 1.0)

@Preview(name = "Detalj jela · svijetla · učitavanje", widthDp = 360, heightDp = 600)
@Composable
private fun PreviewDetaljUcitavanjeSvijetla() = PreviewDetalj(false, DetaljStanje.Ucitavanje)

@Preview(name = "Detalj jela · tamna · učitavanje", widthDp = 360, heightDp = 600)
@Composable
private fun PreviewDetaljUcitavanjeTamna() = PreviewDetalj(true, DetaljStanje.Ucitavanje)

@Preview(name = "Detalj jela · svijetla · greška", widthDp = 360, heightDp = 400)
@Composable
private fun PreviewDetaljGreskaSvijetla() = PreviewDetalj(false, DetaljStanje.Greska("Detalj jela se nije učitao."))

@Preview(name = "Detalj jela · tamna · greška", widthDp = 360, heightDp = 400)
@Composable
private fun PreviewDetaljGreskaTamna() = PreviewDetalj(true, DetaljStanje.Greska("Detalj jela se nije učitao."))

@Preview(name = "Detalj jela · normalno · fontScale 1,3", widthDp = 360, heightDp = 1000, fontScale = 1.3f)
@Composable
private fun PreviewDetaljVelikiFont() = PreviewDetalj(false, DetaljStanje.Podaci(SARMA))

@Preview(name = "Detalj jela · s napomenom · fontScale 1,3", widthDp = 360, heightDp = 700, fontScale = 1.3f)
@Composable
private fun PreviewDetaljNapomenaVelikiFont() = PreviewDetalj(true, DetaljStanje.Podaci(JUHA_GRAH), idJelo = 4, porcije = 1.0)
