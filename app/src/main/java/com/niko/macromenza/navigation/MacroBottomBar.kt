package com.niko.macromenza.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.niko.macromenza.ui.components.skalaPritiska
import com.niko.macromenza.ui.theme.MacroDimenzije
import com.niko.macromenza.ui.theme.MacroMenzaTheme
import com.niko.macromenza.ui.theme.MacroOblici
import com.niko.macromenza.ui.theme.MacroRazmaci
import com.niko.macromenza.ui.theme.Opruge
import com.niko.macromenza.ui.theme.Trajanje
import com.niko.macromenza.ui.theme.opruga
import com.niko.macromenza.ui.theme.rememberSmanjeniPokret

/**
 * Plutajuća donja navigacija (MASTER §8, "Donja navigacija"): plosnata traka s rubom i četiri stavke
 * jednake širine; odabir je jedna pilula koja klizi kroz graphicsLayer. Ukupna visina s odmakom
 * odgovara [com.niko.macromenza.ui.components.ScreenSpacing.FloatingBottomBar].
 */
@Composable
fun MacroBottomBar(
    items: List<BottomNavItem>,
    currentRoute: String?,
    onItemClick: (BottomNavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val shema = MaterialTheme.colorScheme
    val smanjeno = rememberSmanjeniPokret()
    val odabrani = items.indexOfFirst { it.route == currentRoute }

    // Izvan tabova indikator ostaje na zadnjem položaju i izblijedi.
    var zadnji by remember { mutableIntStateOf(odabrani.coerceAtLeast(0)) }
    if (odabrani >= 0) zadnji = odabrani

    val polozaj by animateFloatAsState(
        targetValue = zadnji.toFloat(),
        animationSpec = opruga(Opruge.blaga(), smanjeno),
        label = "indikatorNavigacije"
    )
    val vidljivost by animateFloatAsState(
        targetValue = if (odabrani >= 0) 1f else 0f,
        animationSpec = tween(if (smanjeno) 0 else Trajanje.kratko),
        label = "vidljivostIndikatora"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = MacroRazmaci.rubEkrana, vertical = MacroDimenzije.trakaNavOdmak)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(MacroDimenzije.trakaNavVisina),
            shape = MacroOblici.pladanj,
            color = shema.surface,
            border = BorderStroke(MacroDimenzije.linija, shema.outlineVariant)
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(MacroRazmaci.s)) {
                if (items.isNotEmpty()) {
                    Box(
                        Modifier
                            .fillMaxWidth(1f / items.size)
                            .fillMaxHeight()
                            .graphicsLayer {
                                translationX = polozaj * size.width
                                alpha = vidljivost
                            }
                            .background(shema.secondaryContainer, MacroOblici.utor)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .selectableGroup(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEachIndexed { indeks, item ->
                        BottomBarItem(
                            item = item,
                            selected = indeks == odabrani,
                            smanjeno = smanjeno,
                            onClick = { onItemClick(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.BottomBarItem(
    item: BottomNavItem,
    selected: Boolean,
    smanjeno: Boolean,
    onClick: () -> Unit
) {
    val shema = MaterialTheme.colorScheme
    val boja by animateColorAsState(
        targetValue = if (selected) shema.onSecondaryContainer else shema.onSurfaceVariant,
        animationSpec = tween(if (smanjeno) 0 else Trajanje.kratko),
        label = "bojaStavke"
    )
    val izvor = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(MacroOblici.utor)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = izvor,
                indication = null
            )
            .skalaPritiska(izvor)
            .padding(horizontal = MacroRazmaci.xxs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Tekst je uvijek vidljiv, pa ikona ne čita naslov drugi put.
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            modifier = Modifier.size(MacroDimenzije.ikona),
            tint = boja
        )
        Text(
            text = stringResource(item.titleRes),
            style = MaterialTheme.typography.labelMedium,
            color = boja,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private val STAVKE = listOf(
    BottomNavItem.Home,
    BottomNavItem.Konzumacija,
    BottomNavItem.Povijest,
    BottomNavItem.Profil
)

/** Svijetla i tamna tema jedna ispod druge, odabrana stavka je [BottomNavItem.route]. */
@Composable
private fun PreviewOkvir(odabrana: BottomNavItem) {
    Column(verticalArrangement = Arrangement.spacedBy(MacroRazmaci.s)) {
        listOf(false, true).forEach { tamna ->
            MacroMenzaTheme(darkTheme = tamna) {
                Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
                    MacroBottomBar(items = STAVKE, currentRoute = odabrana.route, onItemClick = {})
                }
            }
        }
    }
}

@Preview(name = "Početna · svijetla i tamna", widthDp = 360)
@Preview(name = "Početna · 320 dp · fontScale 1,3", widthDp = 320, fontScale = 1.3f)
@Composable
private fun PreviewPocetna() = PreviewOkvir(BottomNavItem.Home)

@Preview(name = "Unos · svijetla i tamna", widthDp = 360)
@Preview(name = "Unos · 320 dp · fontScale 1,3", widthDp = 320, fontScale = 1.3f)
@Composable
private fun PreviewUnos() = PreviewOkvir(BottomNavItem.Konzumacija)

@Preview(name = "Povijest · svijetla i tamna", widthDp = 360)
@Preview(name = "Povijest · 320 dp · fontScale 1,3", widthDp = 320, fontScale = 1.3f)
@Composable
private fun PreviewPovijest() = PreviewOkvir(BottomNavItem.Povijest)

@Preview(name = "Profil · svijetla i tamna", widthDp = 360)
@Preview(name = "Profil · 320 dp · fontScale 1,3", widthDp = 320, fontScale = 1.3f)
@Composable
private fun PreviewProfil() = PreviewOkvir(BottomNavItem.Profil)
