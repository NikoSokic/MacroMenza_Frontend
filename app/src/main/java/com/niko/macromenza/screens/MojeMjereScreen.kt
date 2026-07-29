package com.niko.macromenza.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.niko.macromenza.model.Mjerenje
import com.niko.macromenza.session.UserSessionManager
import com.niko.macromenza.ui.components.screenBottomPadding
import com.niko.macromenza.ui.components.screenTopPadding
import com.niko.macromenza.ui.theme.MacroGreen
import com.niko.macromenza.ui.theme.MacroLightGreen
import com.niko.macromenza.ui.theme.MacroText
import com.niko.macromenza.ui.theme.MacroTextSecondary
import com.niko.macromenza.viewmodel.MjereViewModel

@Composable
fun MojeMjereScreen(
    navController: NavController,
    viewModel: MjereViewModel = viewModel()
) {
    val mjerenja by viewModel.mjerenja.collectAsState()
    val greska by viewModel.greska.collectAsState()

    val context = LocalContext.current

    val sessionManager = remember {
        UserSessionManager(context)
    }

    val prijavljeniKorisnikId by
    sessionManager.korisnikId.collectAsState(initial = null)

    LaunchedEffect(prijavljeniKorisnikId) {
        prijavljeniKorisnikId?.let { id ->
            viewModel.ucitajMjerenja(id)
        }
    }

    val zadnjeMjerenje =
        mjerenja.maxByOrNull { it.datum ?: "" }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        // Dekoracija u pozadini
        Box(
            modifier = Modifier
                .size(230.dp)
                .offset(
                    x = 190.dp,
                    y = (-110).dp
                )
                .clip(CircleShape)
                .background(
                    MacroLightGreen.copy(alpha = 0.10f)
                )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 22.dp,
                end = 22.dp,
                top = screenTopPadding(),
                bottom = screenBottomPadding()
            ),
            verticalArrangement =
                Arrangement.spacedBy(16.dp)
        ) {

            // HEADER
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Surface(
                        onClick = {
                            navController.popBackStack()
                        },
                        modifier = Modifier.size(46.dp),
                        shape = CircleShape,
                        color = MacroLightGreen.copy(
                            alpha = 0.16f
                        )
                    ) {
                        Box(
                            contentAlignment =
                                Alignment.Center
                        ) {
                            Icon(
                                imageVector =
                                    Icons.AutoMirrored
                                        .Filled
                                        .ArrowBack,
                                contentDescription = "Nazad",
                                tint = MacroGreen
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier.width(14.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            text = "Moje mjere",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MacroText,
                            maxLines = 2,
                            overflow =
                                TextOverflow.Ellipsis
                        )

                        Text(
                            text =
                                "Prati promjene kroz vrijeme",
                            color = MacroTextSecondary,
                            style =
                                MaterialTheme.typography
                                    .bodyMedium,
                            maxLines = 2,
                            overflow =
                                TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // ZADNJE MJERENJE
            item {
                LatestMeasurementCard(
                    measurement = zadnjeMjerenje
                )
            }

            // GREŠKA
            if (greska != null) {
                item {
                    Surface(
                        modifier =
                            Modifier.fillMaxWidth(),
                        shape =
                            RoundedCornerShape(16.dp),
                        color =
                            MaterialTheme
                                .colorScheme
                                .error
                                .copy(alpha = 0.08f)
                    ) {
                        Text(
                            text =
                                greska
                                    ?: "Došlo je do greške.",
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .error,
                            modifier =
                                Modifier.padding(14.dp),
                            style =
                                MaterialTheme.typography
                                    .bodyMedium
                        )
                    }
                }
            }

            // POVIJEST HEADER
            item {
                Column {
                    Text(
                        text = "Povijest mjerenja",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MacroText,
                        maxLines = 1,
                        overflow =
                            TextOverflow.Ellipsis
                    )

                    Spacer(
                        modifier = Modifier.height(3.dp)
                    )

                    Text(
                        text =
                            "Pregled prethodnih mjerenja i ciljeva.",
                        color = MacroTextSecondary,
                        style =
                            MaterialTheme.typography
                                .bodySmall,
                        maxLines = 2,
                        overflow =
                            TextOverflow.Ellipsis
                    )
                }
            }

            if (mjerenja.isEmpty()) {

                item {
                    EmptyMeasurementsCard()
                }

            } else {

                items(
                    mjerenja.sortedByDescending {
                        it.id ?: 0
                    }
                ) { mjerenje ->

                    MjerenjeItem(
                        mjerenje = mjerenje
                    )
                }
            }
        }
    }
}

@Composable
private fun LatestMeasurementCard(
    measurement: Mjerenje?
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MacroGreen
    ) {

        Row(
            modifier = Modifier.padding(
                horizontal = 20.dp,
                vertical = 20.dp
            ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Surface(
                modifier = Modifier.size(64.dp),
                shape = RoundedCornerShape(20.dp),
                color =
                    Color.White.copy(alpha = 0.16f)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Scale,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(31.dp)
                    )
                }
            }

            Spacer(
                modifier = Modifier.width(16.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = "Zadnja masa",
                    color =
                        Color.White.copy(alpha = 0.75f),
                    style =
                        MaterialTheme.typography.bodyMedium,
                    maxLines = 1
                )

                Spacer(
                    modifier = Modifier.height(3.dp)
                )

                Text(
                    text =
                        if (measurement != null) {
                            "%.1f kg".format(
                                measurement.masa
                            )
                        } else {
                            "Nema mjerenja"
                        },
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow =
                        TextOverflow.Ellipsis
                )

                if (measurement?.datum != null) {

                    Spacer(
                        modifier = Modifier.height(3.dp)
                    )

                    Text(
                        text =
                            measurement.datum ?: "",
                        color =
                            Color.White.copy(
                                alpha = 0.70f
                            ),
                        style =
                            MaterialTheme.typography
                                .bodySmall,
                        maxLines = 1,
                        overflow =
                            TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun MjerenjeItem(
    mjerenje: Mjerenje
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {

        Column(
            modifier = Modifier.padding(17.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            mjerenje.datum
                                ?: "Bez datuma",
                        fontSize = 15.sp,
                        fontWeight =
                            FontWeight.SemiBold,
                        color = MacroTextSecondary,
                        maxLines = 1,
                        overflow =
                            TextOverflow.Ellipsis
                    )

                    Spacer(
                        modifier = Modifier.height(5.dp)
                    )

                    Text(
                        text =
                            "%.1f kg".format(
                                mjerenje.masa
                            ),
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                        color = MacroText,
                        maxLines = 1,
                        overflow =
                            TextOverflow.Ellipsis
                    )
                }

                Spacer(
                    modifier = Modifier.width(12.dp)
                )

                Surface(
                    modifier = Modifier.size(46.dp),
                    shape = RoundedCornerShape(15.dp),
                    color =
                        MacroLightGreen.copy(
                            alpha = 0.18f
                        )
                ) {
                    Box(
                        contentAlignment =
                            Alignment.Center
                    ) {
                        Icon(
                            imageVector =
                                Icons.Default.Scale,
                            contentDescription = null,
                            tint = MacroGreen,
                            modifier =
                                Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            HorizontalDivider(
                color =
                    MacroTextSecondary.copy(
                        alpha = 0.10f
                    )
            )

            Spacer(
                modifier = Modifier.height(13.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(10.dp),
                verticalAlignment =
                    Alignment.Top
            ) {

                MeasurementMeta(
                    icon = Icons.Default.FitnessCenter,
                    label = "Aktivnost",
                    value = formatAktivnost(
                        mjerenje.razinaAktivnosti
                    ),
                    modifier = Modifier.weight(1f)
                )

                MeasurementMeta(
                    icon = Icons.Default.Flag,
                    label = "Cilj",
                    value = formatCiljMjerenja(
                        mjerenje.tipCilja
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MeasurementMeta(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Surface(
            modifier = Modifier.size(34.dp),
            shape = RoundedCornerShape(11.dp),
            color =
                MacroLightGreen.copy(
                    alpha = 0.16f
                )
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MacroGreen,
                    modifier = Modifier.size(17.dp)
                )
            }
        }

        Spacer(
            modifier = Modifier.width(8.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = label,
                color = MacroTextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow =
                    TextOverflow.Ellipsis
            )

            Text(
                text = value,
                color = MacroText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow =
                    TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EmptyMeasurementsCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color =
            MacroLightGreen.copy(alpha = 0.12f)
    ) {

        Column(
            modifier = Modifier.padding(20.dp)
        ) {

            Text(
                text = "Još nema mjerenja",
                color = MacroText,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                maxLines = 1,
                overflow =
                    TextOverflow.Ellipsis
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text =
                    "Postavi ili promijeni cilj kako bi se spremilo novo mjerenje.",
                color = MacroTextSecondary,
                style =
                    MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun formatAktivnost(
    aktivnost: String?
): String {
    return when (aktivnost) {
        "sjedilacka" -> "Sjedilačka"
        "lagana" -> "Lagana"
        "umjerena" -> "Umjerena"
        "visoka" -> "Visoka"
        else -> aktivnost ?: "—"
    }
}

private fun formatCiljMjerenja(
    cilj: String?
): String {
    return when (cilj) {
        "mrsavljenje" ->
            "Mršavljenje"

        "odrzavanje" ->
            "Održavanje"

        "dobivanje_mase" ->
            "Dobivanje mase"

        else ->
            cilj ?: "—"
    }
}