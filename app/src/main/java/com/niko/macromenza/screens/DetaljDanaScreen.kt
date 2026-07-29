package com.niko.macromenza.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.niko.macromenza.model.StavkaObroka
import com.niko.macromenza.session.UserSessionManager
import com.niko.macromenza.ui.theme.MacroGreen
import com.niko.macromenza.ui.theme.MacroLightGreen
import com.niko.macromenza.viewmodel.PovijestViewModel

private val DetailProteinBlue = Color(0xFF4F8EF7)
private val DetailCarbsOrange = Color(0xFFF4A340)
private val DetailFatPurple = Color(0xFF9B6DE3)

@Composable
fun DetaljDanaScreen(
    datum: String,
    viewModel: PovijestViewModel = viewModel()
) {
    val povijestDana by viewModel.povijestDana.collectAsState()

    val context = LocalContext.current
    val sessionManager = remember {
        UserSessionManager(context)
    }

    val prijavljeniKorisnikId by
    sessionManager.korisnikId.collectAsState(initial = null)

    LaunchedEffect(datum, prijavljeniKorisnikId) {
        prijavljeniKorisnikId?.let { id ->
            viewModel.ucitajDan(id, datum)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        Box(
            modifier = Modifier
                .size(230.dp)
                .offset(x = 170.dp, y = (-100).dp)
                .clip(CircleShape)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.09f)
                )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 22.dp,
                end = 22.dp,
                top = 28.dp,
                bottom = 110.dp
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {

            item {
                Column {
                    Text(
                        text = "Detalj dana",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = datum,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            povijestDana?.let { dan ->

                val imaObroka =
                    dan.obroci.any { (_, stavke) ->
                        stavke.isNotEmpty()
                    }

                if (!imaObroka) {
                    item {
                        EmptyDetailDay()
                    }
                } else {

                    item {
                        DetailSummaryCard(
                            kalorije = dan.ukupnoKalorije,
                            proteini = dan.ukupnoProteini,
                            uh = dan.ukupnoUgljikohidrati,
                            masti = dan.ukupnoMasti
                        )
                    }

                    item {
                        Text(
                            text = "Obroci",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    dan.obroci.forEach { (tip, stavke) ->

                        if (stavke.isNotEmpty()) {
                            item {
                                SekcijaObrokaDetalj(
                                    naslov = tip,
                                    stavke = stavke
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailSummaryCard(
    kalorije: Double,
    proteini: Double,
    uh: Double,
    masti: Double
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MacroGreen
    ) {
        Column(
            modifier = Modifier.padding(22.dp)
        ) {

            Text(
                text = "Ukupni dnevni unos",
                color = Color.White.copy(alpha = 0.78f),
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = "%.0f kcal".format(kalorije),
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            HorizontalDivider(
                color = Color.White.copy(alpha = 0.18f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                DetailSummaryMacro(
                    title = "Proteini",
                    value = proteini
                )

                DetailSummaryMacro(
                    title = "UH",
                    value = uh
                )

                DetailSummaryMacro(
                    title = "Masti",
                    value = masti
                )
            }
        }
    }
}

@Composable
private fun DetailSummaryMacro(
    title: String,
    value: Double
) {
    Column {
        Text(
            text = "%.0f g".format(value),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp
        )

        Text(
            text = title,
            color = Color.White.copy(alpha = 0.70f),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun SekcijaObrokaDetalj(
    naslov: String,
    stavke: List<StavkaObroka>
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        Text(
            text = formatDetaljTipObroka(naslov),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        stavke.forEach { stavka ->
            DetaljObrokItem(stavka)
        }
    }
}

@Composable
private fun DetaljObrokItem(
    stavka: StavkaObroka
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = MacroGreen,
                        modifier = Modifier.size(23.dp)
                    )
                }

                Spacer(modifier = Modifier.width(13.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stavka.nazivJela,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = "Evidentirano jelo",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "%.0f".format(stavka.kalorije),
                        color = MacroGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    )

                    Text(
                        text = "kcal",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.10f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                DetaljMacroValue(
                    label = "Proteini",
                    value = stavka.proteini,
                    color = DetailProteinBlue
                )

                DetaljMacroValue(
                    label = "UH",
                    value = stavka.ugljikohidrati,
                    color = DetailCarbsOrange
                )

                DetaljMacroValue(
                    label = "Masti",
                    value = stavka.masti,
                    color = DetailFatPurple
                )
            }
        }
    }
}

@Composable
private fun DetaljMacroValue(
    label: String,
    value: Double,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "%.0f g".format(value),
            fontWeight = FontWeight.SemiBold,
            color = color,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun EmptyDetailDay() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    ) {
        Column(
            modifier = Modifier.padding(22.dp)
        ) {
            Text(
                text = "Nema evidentiranih obroka",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Za ovaj datum nije evidentirano nijedno jelo.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun formatDetaljTipObroka(
    tip: String
): String {
    return when (tip.uppercase()) {
        "DORUCAK" -> "Doručak"
        "RUCAK" -> "Ručak"
        "VECERA" -> "Večera"
        "UZINA" -> "Užina"
        else -> tip
    }
}