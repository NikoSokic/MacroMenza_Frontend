package com.niko.macromenza.screens

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import com.niko.macromenza.R
import com.niko.macromenza.api.RetrofitInstance
import com.niko.macromenza.model.IzvoriPodataka
import com.niko.macromenza.ui.components.KosturOblik
import com.niko.macromenza.ui.components.TablicniBroj
import com.niko.macromenza.ui.components.ZaglavljePodekrana
import com.niko.macromenza.ui.components.screenBottomPadding
import com.niko.macromenza.ui.theme.MacroDimenzije
import com.niko.macromenza.ui.theme.MacroMenzaTheme
import com.niko.macromenza.ui.theme.MacroRazmaci
import com.niko.macromenza.ui.theme.macroTipografija
import kotlinx.coroutines.CancellationException

/** Brojke su dodatak: dok se učitavaju stoji kostur, a ako ne stignu ekran radi i bez njih. */
private sealed interface StanjeBrojki {
    data object Ucitava : StanjeBrojki
    data object Nema : StanjeBrojki
    data class Gotovo(val izvori: IzvoriPodataka) : StanjeBrojki
}

private const val ZADANI_NORMATIV = "NN 120/2013"

@Composable
fun OdakleSuKalorijeScreen(
    navController: NavController
) {
    var brojke by remember { mutableStateOf<StanjeBrojki>(StanjeBrojki.Ucitava) }

    LaunchedEffect(Unit) {
        brojke = try {
            StanjeBrojki.Gotovo(RetrofitInstance.api.getIzvoriPodataka())
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            StanjeBrojki.Nema
        }
    }

    OdakleSuKalorijeSadrzaj(brojke = brojke, onNatrag = { navController.popBackStack() })
}

@Composable
private fun OdakleSuKalorijeSadrzaj(brojke: StanjeBrojki, onNatrag: () -> Unit) {
    val shema = MaterialTheme.colorScheme
    val tip = MaterialTheme.typography
    val normativ = (brojke as? StanjeBrojki.Gotovo)?.izvori?.izvorNormativa ?: ZADANI_NORMATIV

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(shema.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MacroRazmaci.rubEkrana)
            .padding(bottom = screenBottomPadding())
    ) {
        ZaglavljePodekrana(naslov = stringResource(R.string.odakle_naslov), onNatrag = onNatrag)

        // Pravilo iz CLAUDE.md: aplikacija mora jasno reći da su vrijednosti okvirne, i to je to mjesto.
        Text(
            text = stringResource(R.string.odakle_okvirno_naslov),
            style = tip.headlineSmall,
            color = shema.onSurface,
            modifier = Modifier
                .padding(top = MacroRazmaci.xl)
                .semantics { heading() }
        )
        Text(
            text = stringResource(R.string.odakle_okvirno_tekst, normativ),
            style = tip.bodyLarge,
            color = shema.onSurface,
            modifier = Modifier.padding(top = MacroRazmaci.s)
        )

        when (brojke) {
            StanjeBrojki.Ucitava -> KosturBrojki()
            StanjeBrojki.Nema -> Unit
            is StanjeBrojki.Gotovo -> Brojke(brojke.izvori)
        }

        Objasnjenje(
            naslov = stringResource(R.string.odakle_recepture),
            tekst = stringResource(R.string.odakle_recepture_tekst, normativ),
            prvo = true
        )
        Objasnjenje(stringResource(R.string.odakle_ili), stringResource(R.string.odakle_ili_tekst))
        Objasnjenje(stringResource(R.string.odakle_meniji), stringResource(R.string.odakle_meniji_tekst))
        Objasnjenje(stringResource(R.string.odakle_zasto), stringResource(R.string.odakle_zasto_tekst))
    }
}

/** Tri broja u redu (jela, namirnica, menija); pri fontScale 1,3 se prelamaju u novi red. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Brojke(izvori: IzvoriPodataka) {
    val opis = stringResource(
        R.string.odakle_brojke_opis,
        izvori.brojJela.toString(),
        izvori.brojNamirnica.toString(),
        izvori.brojMenija.toString()
    )
    FlowRow(
        modifier = Modifier
            .padding(top = MacroRazmaci.iznadSekcije)
            .clearAndSetSemantics { contentDescription = opis },
        horizontalArrangement = Arrangement.spacedBy(MacroRazmaci.xl),
        verticalArrangement = Arrangement.spacedBy(MacroRazmaci.l)
    ) {
        Broj(izvori.brojJela.toString(), stringResource(R.string.odakle_jela))
        Broj(izvori.brojNamirnica.toString(), stringResource(R.string.odakle_namirnica))
        Broj(izvori.brojMenija.toString(), stringResource(R.string.odakle_menija))
    }
}

@Composable
private fun Broj(vrijednost: String, oznaka: String) {
    Column {
        TablicniBroj(
            tekst = { vrijednost },
            stil = MaterialTheme.macroTipografija.brojSrednji,
            boja = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = oznaka,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Kostur istih tri broja, da sadržaj ispod ne skače kad brojke stignu. */
@Composable
private fun KosturBrojki() {
    val opis = stringResource(R.string.odakle_ucitavanje)
    FlowRow(
        modifier = Modifier
            .padding(top = MacroRazmaci.iznadSekcije)
            .semantics { contentDescription = opis },
        horizontalArrangement = Arrangement.spacedBy(MacroRazmaci.xl),
        verticalArrangement = Arrangement.spacedBy(MacroRazmaci.l)
    ) {
        repeat(3) {
            Column {
                KosturOblik(Modifier.width(MacroDimenzije.avatarProfila).height(MacroDimenzije.ikona))
                KosturOblik(Modifier.padding(top = MacroRazmaci.s).width(MacroDimenzije.avatarProfila).height(MacroDimenzije.kosturMakro))
            }
        }
    }
}

/** Sekcija objašnjenja: naslov (titleSmall) i tekst (bodyMedium), odvojene dividerom, bez kartica. */
@Composable
private fun Objasnjenje(naslov: String, tekst: String, prvo: Boolean = false) {
    val shema = MaterialTheme.colorScheme
    HorizontalDivider(
        thickness = MacroDimenzije.linija,
        color = shema.outlineVariant,
        modifier = Modifier.padding(top = if (prvo) MacroRazmaci.iznadSekcije else MacroRazmaci.xl)
    )
    Text(
        text = naslov,
        style = MaterialTheme.typography.titleSmall,
        color = shema.onSurface,
        modifier = Modifier
            .padding(top = MacroRazmaci.xl)
            .semantics { heading() }
    )
    Text(
        text = tekst,
        style = MaterialTheme.typography.bodyMedium,
        color = shema.onSurfaceVariant,
        modifier = Modifier.padding(top = MacroRazmaci.xs)
    )
}

// ---------- Previewi ----------

private val PRIMJER_IZVORI = IzvoriPodataka(
    brojJela = 311,
    brojNamirnica = 182,
    brojMenija = 84,
    izvorNormativa = ZADANI_NORMATIV
)

@Composable
private fun PreviewOkvir(tamna: Boolean, brojke: StanjeBrojki) {
    MacroMenzaTheme(darkTheme = tamna) {
        OdakleSuKalorijeSadrzaj(brojke = brojke, onNatrag = {})
    }
}

@Preview(name = "Svijetla", widthDp = 360, heightDp = 1100)
@Composable
private fun PreviewSvijetla() = PreviewOkvir(false, StanjeBrojki.Gotovo(PRIMJER_IZVORI))

@Preview(name = "Tamna", widthDp = 360, heightDp = 1100)
@Composable
private fun PreviewTamna() = PreviewOkvir(true, StanjeBrojki.Gotovo(PRIMJER_IZVORI))

@Preview(name = "Svijetla · učitavanje", widthDp = 360, heightDp = 1100)
@Composable
private fun PreviewSvijetlaUcitavanje() = PreviewOkvir(false, StanjeBrojki.Ucitava)

@Preview(name = "Tamna · bez brojki", widthDp = 360, heightDp = 1100)
@Composable
private fun PreviewTamnaBezBrojki() = PreviewOkvir(true, StanjeBrojki.Nema)

@Preview(name = "Svijetla · fontScale 1,3", widthDp = 360, heightDp = 1600, fontScale = 1.3f)
@Composable
private fun PreviewVelikiFont() = PreviewOkvir(false, StanjeBrojki.Gotovo(PRIMJER_IZVORI))

@Preview(name = "Tamna · fontScale 1,3", widthDp = 360, heightDp = 1600, fontScale = 1.3f)
@Composable
private fun PreviewVelikiFontTamna() = PreviewOkvir(true, StanjeBrojki.Gotovo(PRIMJER_IZVORI))
