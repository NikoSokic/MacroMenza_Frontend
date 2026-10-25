package com.niko.macromenza.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niko.macromenza.model.Jelo
import com.niko.macromenza.ui.theme.MacroGreen
import com.niko.macromenza.util.formatKcal
import com.niko.macromenza.util.formatKcalTisuce
import com.niko.macromenza.util.formatKolicina
import com.niko.macromenza.util.formatMakro
import com.niko.macromenza.viewmodel.OdabranaStavka
import com.niko.macromenza.viewmodel.SazetakObroka

private val ProteinBlue = Color(0xFF4F8EF7)
private val CarbsOrange = Color(0xFFF4A340)
private val FatPurple = Color(0xFF9B6DE3)

private val PRAG_POVLACENJA = 40.dp

/**
 * Stalni sažetak obroka pri dnu ekrana. Tap ili povlačenje prema gore otvara listu odabranih stavki.
 * [onVisina] javlja visinu zatvorenog bara, da ekran može ostaviti prostor ispod liste.
 */
@Composable
fun SazetakObrokaBar(
    sazetak: SazetakObroka,
    stavke: List<OdabranaStavka>,
    spremanjeUTijeku: Boolean,
    onPlus: (Jelo) -> Unit,
    onMinus: (Long) -> Unit,
    onUkloni: (Long) -> Unit,
    onSpremi: () -> Unit,
    onVisina: (Dp) -> Unit,
    modifier: Modifier = Modifier,
    tekstSpremi: String = "Spremi"
) {
    val prazno = stavke.isEmpty()
    var otvoren by remember { mutableStateOf(false) }

    // Zadnja stavka uklonjena: lista se zatvara.
    LaunchedEffect(prazno) {
        if (prazno) otvoren = false
    }

    BackHandler(enabled = otvoren) { otvoren = false }

    val density = LocalDensity.current
    val pragPx = with(density) { PRAG_POVLACENJA.toPx() }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        tonalElevation = 2.dp
    ) {
        Column {
            AnimatedVisibility(
                visible = otvoren && !prazno,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 4.dp)
                ) {
                    items(stavke, key = { it.jelo.id }) { stavka ->
                        StavkaSazetka(
                            stavka = stavka,
                            onPlus = { onPlus(stavka.jelo) },
                            onMinus = { onMinus(stavka.jelo.id) },
                            onUkloni = { onUkloni(stavka.jelo.id) }
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .onSizeChanged { onVisina(with(density) { it.height.toDp() }) }
                    .pointerInput(prazno) {
                        if (prazno) return@pointerInput

                        var povuceno = 0f
                        detectVerticalDragGestures(
                            onDragStart = { povuceno = 0f },
                            onDragEnd = {
                                if (povuceno <= -pragPx) otvoren = true
                                else if (povuceno >= pragPx) otvoren = false
                            },
                            onDragCancel = { povuceno = 0f },
                            onVerticalDrag = { _, delta -> povuceno += delta }
                        )
                    }
                    .clickable(enabled = !prazno) { otvoren = !otvoren }
                    .padding(start = 18.dp, end = 14.dp, top = 8.dp, bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.30f))
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (prazno) {
                    PrazanSazetak(tekstSpremi)
                } else {
                    PuniSazetak(
                        sazetak = sazetak,
                        spremanjeUTijeku = spremanjeUTijeku,
                        tekstSpremi = tekstSpremi,
                        onSpremi = onSpremi
                    )
                }
            }
        }
    }
}

@Composable
private fun PrazanSazetak(tekstSpremi: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Dodaj jelo ili meni",
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 15.sp
        )

        Button(
            onClick = {},
            enabled = false,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.height(46.dp)
        ) {
            Text(text = tekstSpremi, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun PuniSazetak(
    sazetak: SazetakObroka,
    spremanjeUTijeku: Boolean,
    tekstSpremi: String,
    onSpremi: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = if (sazetak.brojStavki == 1) "1 stavka" else "${sazetak.brojStavki} stavki",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = "${formatKcal(sazetak.kalorije)} kcal",
                color = MacroGreen,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MakroKratko("P", sazetak.proteini, ProteinBlue)
            MakroKratko("UH", sazetak.ugljikohidrati, CarbsOrange)
            MakroKratko("M", sazetak.masti, FatPurple)
        }

        Spacer(modifier = Modifier.width(8.dp))

        Button(
            onClick = onSpremi,
            enabled = !spremanjeUTijeku,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.height(46.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MacroGreen,
                contentColor = Color.White
            )
        ) {
            Text(
                text = if (spremanjeUTijeku) "Spremam..." else tekstSpremi,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }

    val nakon = sazetak.nakonObroka
    if (nakon != null) {
        val cilj = sazetak.ciljKalorije
        val prelazi = cilj != null && nakon > cilj

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = buildString {
                append("Nakon ovog obroka: ")
                append(formatKcalTisuce(nakon))
                if (cilj != null) append(" / ${formatKcalTisuce(cilj)}")
                append(" kcal")
            },
            color = if (prelazi) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (prelazi) FontWeight.SemiBold else FontWeight.Normal,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MakroKratko(
    oznaka: String,
    vrijednost: Double,
    boja: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "${formatMakro(vrijednost)} g",
            color = boja,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            maxLines = 1
        )

        Text(
            text = oznaka,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun StavkaSazetka(
    stavka: OdabranaStavka,
    onPlus: () -> Unit,
    onMinus: () -> Unit,
    onUkloni: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stavka.jelo.naziv,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "${formatKcal(stavka.jelo.kalorije * stavka.kolicina)} kcal",
                color = MacroGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(2.dp)
            ) {
                IconButton(onClick = onMinus, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Smanji",
                        tint = MacroGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = formatKolicina(stavka.kolicina),
                    modifier = Modifier.widthIn(min = 30.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                IconButton(onClick = onPlus, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Povećaj",
                        tint = MacroGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        IconButton(onClick = onUkloni, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Ukloni",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
