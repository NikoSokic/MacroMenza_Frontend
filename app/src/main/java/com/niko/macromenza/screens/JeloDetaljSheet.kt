package com.niko.macromenza.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.niko.macromenza.model.JeloDetalj
import com.niko.macromenza.model.JeloSastojak
import com.niko.macromenza.ui.theme.MacroGreen
import com.niko.macromenza.util.formatCijena
import com.niko.macromenza.util.formatGrami
import com.niko.macromenza.util.formatKcal
import com.niko.macromenza.util.formatKolicina
import com.niko.macromenza.util.formatMakro
import com.niko.macromenza.viewmodel.DetaljStanje
import com.niko.macromenza.viewmodel.JeloDetaljViewModel

private val ProteinBlue = Color(0xFF4F8EF7)
private val CarbsOrange = Color(0xFFF4A340)
private val FatPurple = Color(0xFF9B6DE3)

/**
 * Detalj jela u donjem listu: vrijednosti po porciji, masa, cijena, napomena i sastojci iz recepture.
 * [onDodaj] dobiva odabrani broj porcija (koraci po 0,5); kad je null, sheet je samo pregled
 * (bez odabira porcija i gumba "Dodaj u obrok").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JeloDetaljSheet(
    idJelo: Long,
    onDismiss: () -> Unit,
    onDodaj: ((Double) -> Unit)? = null,
    viewModel: JeloDetaljViewModel = viewModel()
) {
    val stanje by viewModel.stanje.collectAsState()
    var porcije by remember(idJelo) { mutableStateOf(1.0) }

    LaunchedEffect(idJelo) {
        viewModel.ucitaj(idJelo)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(
            topStart = 28.dp,
            topEnd = 28.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 24.dp)
        ) {
            // Dok ViewModel ne učita traženo jelo, ne prikazuje se detalj prethodno otvorenog.
            val trenutno = stanje
            when {
                trenutno is DetaljStanje.Podaci && trenutno.detalj.id == idJelo -> {
                    DetaljSadrzaj(
                        detalj = trenutno.detalj,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                trenutno is DetaljStanje.Greska -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = trenutno.poruka,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(onClick = { viewModel.ucitaj(idJelo) }) {
                            Text("Pokušaj ponovno", color = MacroGreen)
                        }
                    }
                }

                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MacroGreen)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (onDodaj != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PorcijeStepper(
                        porcije = porcije,
                        onMinus = { if (porcije > 0.5) porcije -= 0.5 },
                        onPlus = { porcije += 0.5 }
                    )

                    Button(
                        onClick = { onDodaj(porcije) },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MacroGreen
                        )
                    ) {
                        Text(
                            text = "Dodaj u obrok",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            Text(
                text = "Vrijednosti su okvirne (USDA/EU tablice), izračunate iz normativa NN 120/2013.",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DetaljSadrzaj(
    detalj: JeloDetalj,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        item {
            Text(
                text = detalj.naziv,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            detalj.kategorija?.let {
                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "${formatKcal(detalj.kalorije)} kcal",
                        color = MacroGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    )

                    Text(
                        text = "po porciji",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DetaljVrijednost("${formatMakro(detalj.proteini)} g", "Proteini", ProteinBlue)
                        DetaljVrijednost("${formatMakro(detalj.ugljikohidrati)} g", "UH", CarbsOrange)
                        DetaljVrijednost("${formatMakro(detalj.masti)} g", "Masti", FatPurple)
                    }

                    if (detalj.masaG != null || detalj.cijenaEur != null) {
                        Spacer(modifier = Modifier.height(12.dp))

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.09f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            detalj.masaG?.let {
                                DetaljVrijednost(
                                    "${formatGrami(it)} g", "Masa porcije",
                                    MaterialTheme.colorScheme.onSurface
                                )
                            }
                            detalj.cijenaEur?.let {
                                DetaljVrijednost(
                                    "${formatCijena(it)} €", "Cijena",
                                    MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            if (!detalj.napomena.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = CarbsOrange.copy(alpha = 0.14f)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "Napomena",
                            tint = CarbsOrange,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = detalj.napomena,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Sastojci",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (detalj.sastojci.isEmpty()) {
                Text(
                    text = "Za ovo jelo nema recepture",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        items(detalj.sastojci) { sastojak ->
            SastojakRed(sastojak)
        }
    }
}

@Composable
private fun SastojakRed(sastojak: JeloSastojak) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = sastojak.nazivURecepturi,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp
            )

            Text(
                text = "${formatGrami(sastojak.grami)} g",
                modifier = Modifier.width(64.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                textAlign = TextAlign.End
            )

            Text(
                text = "${formatKcal(sastojak.kalorije)} kcal",
                modifier = Modifier.width(84.dp),
                color = MacroGreen,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                textAlign = TextAlign.End
            )
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.09f)
        )
    }
}

@Composable
private fun DetaljVrijednost(
    vrijednost: String,
    oznaka: String,
    color: Color
) {
    Column {
        Text(
            text = vrijednost,
            color = color,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp
        )

        Text(
            text = oznaka,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun PorcijeStepper(
    porcije: Double,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(4.dp)
        ) {
            IconButton(
                onClick = onMinus,
                enabled = porcije > 0.5,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Smanji",
                    tint =
                        if (porcije > 0.5) {
                            MacroGreen
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.30f)
                        }
                )
            }

            Text(
                text = formatKolicina(porcije),
                modifier = Modifier.width(36.dp),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MacroGreen),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onPlus,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Povećaj",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
