package com.niko.macromenza.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.niko.macromenza.model.StavkaObroka
import com.niko.macromenza.session.UserSessionManager
import com.niko.macromenza.ui.components.screenBottomPadding
import com.niko.macromenza.ui.components.screenTopPadding
import com.niko.macromenza.ui.theme.MacroGreen
import com.niko.macromenza.ui.theme.MacroLightGreen
import com.niko.macromenza.ui.theme.MacroText
import com.niko.macromenza.ui.theme.MacroTextSecondary
import com.niko.macromenza.viewmodel.HomeViewModel

// Boje koristimo samo za razlikovanje makronutrijenata
private val ProteinBlue = Color(0xFF4F8EF7)
private val CarbsOrange = Color(0xFFF4A340)
private val FatPurple = Color(0xFF9B6DE3)

@Composable
fun HomeScreen(
    navController: NavController,
    refreshKey: Int = 0,
    viewModel: HomeViewModel = viewModel()
) {
    val ukupniUnos by viewModel.ukupniUnos.collectAsState()
    val danasnjiObroci by viewModel.danasnjiObroci.collectAsState()
    val preporuka by viewModel.preporuka.collectAsState()
    val ucitava by viewModel.ucitava.collectAsState()

    val ciljKalorije = preporuka?.kalorije ?: 2400.0
    val ciljProteini = preporuka?.proteini ?: 160.0
    val ciljUh = preporuka?.ugljikohidrati ?: 300.0
    val ciljMasti = preporuka?.masti ?: 80.0

    val context = LocalContext.current

    val sessionManager = remember {
        UserSessionManager(context)
    }

    val prijavljeniKorisnikId by
    sessionManager.korisnikId.collectAsState(initial = null)

    LaunchedEffect(refreshKey, prijavljeniKorisnikId) {
        prijavljeniKorisnikId?.let { id ->
            viewModel.ucitajHome(id)
        }
    }

    val kalorije = ukupniUnos?.kalorije ?: 0.0
    val proteini = ukupniUnos?.proteini ?: 0.0
    val uh = ukupniUnos?.ugljikohidrati ?: 0.0
    val masti = ukupniUnos?.masti ?: 0.0

    if (ucitava) {
        SplashScreen()
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        // Suptilna dekoracija u pozadini
        Box(
            modifier = Modifier
                .size(230.dp)
                .offset(x = 150.dp, y = (-110).dp)
                .clip(CircleShape)
                .background(MacroLightGreen.copy(alpha = 0.08f))
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                top = screenTopPadding(),
                end = 20.dp,
                bottom = screenBottomPadding()
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {

            // HEADER
            item {
                Column {
                    Text(
                        text = "Danas",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = MacroText
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = "Tvoj dnevni pregled",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MacroTextSecondary
                    )
                }
            }

            // Ako nema preporuke
            if (preporuka == null) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = MacroLightGreen.copy(alpha = 0.14f)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp)
                        ) {
                            Text(
                                text = "Postavi svoje ciljeve",
                                fontWeight = FontWeight.SemiBold,
                                color = MacroText
                            )

                            Spacer(modifier = Modifier.height(5.dp))

                            Text(
                                text = "Otvori Profil → Postavke ciljeva kako bismo izračunali tvoju dnevnu preporuku.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MacroTextSecondary
                            )
                        }
                    }
                }
            }

            // GLAVNA KALORIJSKA KARTICA
            item {
                CaloriesHeroCard(
                    kalorije = kalorije,
                    cilj = ciljKalorije
                )
            }

            // MAKRO HEADER
            item {
                SectionTitle(
                    title = "Makronutrijenti",
                    subtitle = "Današnji napredak"
                )
            }

            // 3 MACRO KARTICE
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    MacroMiniCard(
                        modifier = Modifier.weight(1f),
                        naziv = "Proteini",
                        kratko = "P",
                        vrijednost = proteini,
                        cilj = ciljProteini,
                        color = ProteinBlue
                    )

                    MacroMiniCard(
                        modifier = Modifier.weight(1f),
                        naziv = "Ugljikoh.",
                        kratko = "UH",
                        vrijednost = uh,
                        cilj = ciljUh,
                        color = CarbsOrange
                    )

                    MacroMiniCard(
                        modifier = Modifier.weight(1f),
                        naziv = "Masti",
                        kratko = "M",
                        vrijednost = masti,
                        cilj = ciljMasti,
                        color = FatPurple
                    )
                }
            }

            item {
                InsightCard(
                    proteini = proteini,
                    ciljProteini = ciljProteini,
                    kalorije = kalorije,
                    ciljKalorije = ciljKalorije
                )
            }

            // QUICK ADD
            item {
                SectionTitle(
                    title = "Brzo dodaj",
                    subtitle = "Što upravo jedeš?"
                )
            }

            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickMealCard(
                            label = "Doručak",
                            tipObroka = "DORUCAK",
                            navController = navController,
                            modifier = Modifier.weight(1f)
                        )

                        QuickMealCard(
                            label = "Ručak",
                            tipObroka = "RUCAK",
                            navController = navController,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickMealCard(
                            label = "Večera",
                            tipObroka = "VECERA",
                            navController = navController,
                            modifier = Modifier.weight(1f)
                        )

                        QuickMealCard(
                            label = "Užina",
                            tipObroka = "UZINA",
                            navController = navController,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // DANAŠNJI OBROCI
            item {
                SectionTitle(
                    title = "Današnji obroci",
                    subtitle = "Sve što si danas evidentirao"
                )
            }

            val imaObroka =
                danasnjiObroci?.obroci?.any { (_, stavke) ->
                    stavke.isNotEmpty()
                } == true

            if (!imaObroka) {
                item {
                    EmptyMealsCard()
                }
            } else {
                danasnjiObroci?.obroci?.forEach { (tip, stavke) ->

                    if (stavke.isNotEmpty()) {

                        item {
                            MealTypeHeader(tip)
                        }

                        items(stavke) { stavka ->
                            DanasnjiObrokItem(stavka)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CaloriesHeroCard(
    kalorije: Double,
    cilj: Double
) {
    val progress =
        if (cilj > 0) {
            (kalorije / cilj)
                .toFloat()
                .coerceIn(0f, 1f)
        } else {
            0f
        }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 900),
        label = "calorieProgress"
    )

    val preostalo = (cilj - kalorije).coerceAtLeast(0.0)
    val postotak = (progress * 100).toInt()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(
            containerColor = MacroGreen
        )
    ) {
        Column(
            modifier = Modifier.padding(22.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Dnevne kalorije",
                        color = Color.White.copy(alpha = 0.78f),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "%.0f".format(kalorije),
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = "/ %.0f kcal".format(cilj),
                            color = Color.White.copy(alpha = 0.70f),
                            fontSize = 15.sp,
                            modifier = Modifier.padding(bottom = 7.dp)
                        )
                    }
                }

                Text(
                    text = "$postotak%",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(9.dp)
                    .clip(CircleShape),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.20f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text =
                        if (kalorije <= cilj)
                            "Preostalo"
                        else
                            "Premašeno",
                    color = Color.White.copy(alpha = 0.70f),
                    fontSize = 13.sp
                )

                Text(
                    text =
                        if (kalorije <= cilj)
                            "%.0f kcal".format(preostalo)
                        else
                            "%.0f kcal".format(kalorije - cilj),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}


@Composable
private fun MacroMiniCard(
    modifier: Modifier,
    naziv: String,
    kratko: String,
    vrijednost: Double,
    cilj: Double,
    color: Color
) {
    val progress =
        if (cilj > 0) {
            (vrijednost / cilj)
                .toFloat()
                .coerceIn(0f, 1f)
        } else {
            0f
        }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(700),
        label = naziv
    )

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {

        Column(
            modifier = Modifier.padding(13.dp)
        ) {

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = kratko,
                    color = color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = naziv,
                color = MacroTextSecondary,
                fontSize = 12.sp,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = "%.0fg".format(vrijednost),
                color = MacroText,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )

            Text(
                text = "/ %.0fg".format(cilj),
                color = MacroTextSecondary,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape),
                color = color,
                trackColor = color.copy(alpha = 0.12f)
            )
        }
    }
}

@Composable
private fun InsightCard(
    proteini: Double,
    ciljProteini: Double,
    kalorije: Double,
    ciljKalorije: Double
) {
    val preostaliProteini =
        (ciljProteini - proteini).coerceAtLeast(0.0)

    val kalorijskiProgress =
        if (ciljKalorije > 0)
            kalorije / ciljKalorije
        else
            0.0

    val naslov: String
    val poruka: String

    when {
        proteini >= ciljProteini -> {
            naslov = "Proteinski cilj ostvaren"
            poruka = "Odlično, danas si već dosegnuo cilj proteina."
        }

        kalorijskiProgress < 0.35 -> {
            naslov = "Dobar početak"
            poruka =
                "Do cilja ti nedostaje još %.0f g proteina. Imaš dovoljno prostora za kvalitetan obrok.".format(
                    preostaliProteini
                )
        }

        else -> {
            naslov = "Na dobrom si putu"
            poruka =
                "Do cilja ti nedostaje još %.0f g proteina.".format(
                    preostaliProteini
                )
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MacroLightGreen.copy(alpha = 0.14f)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MacroGreen.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "i",
                    color = MacroGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = naslov,
                    color = MacroText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = poruka,
                    color = MacroTextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun QuickMealCard(
    label: String,
    tipObroka: String,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = {
            navController.navigate("unos_obroka/$tipObroka")
        },
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 10.dp,
                vertical = 13.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        MacroLightGreen.copy(alpha = 0.20f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Restaurant,
                    contentDescription = null,
                    tint = MacroGreen,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = label,
                modifier = Modifier.weight(1f),
                color = MacroText,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.width(6.dp))

            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(MacroGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Dodaj $label",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
@Composable
private fun EmptyMealsCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 24.dp,
                    vertical = 28.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(
                        MacroLightGreen.copy(alpha = 0.18f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Restaurant,
                    contentDescription = null,
                    tint = MacroGreen,
                    modifier = Modifier.size(27.dp)
                )
            }

            Spacer(modifier = Modifier.height(13.dp))

            Text(
                text = "Još nema obroka",
                color = MacroText,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Dodaj prvi današnji obrok i počni pratiti svoj unos.",
                color = MacroTextSecondary,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    subtitle: String
) {
    Column {
        Text(
            text = title,
            color = MacroText,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = subtitle,
            color = MacroTextSecondary,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun MealTypeHeader(
    tip: String
) {
    Text(
        text = formatMealType(tip),
        color = MacroText,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold
    )
}

private fun formatMealType(tip: String): String {
    return when (tip.uppercase()) {
        "DORUCAK" -> "Doručak"
        "RUCAK" -> "Ručak"
        "VECERA" -> "Večera"
        "UZINA" -> "Užina"
        else -> tip
    }
}

@Composable
fun DanasnjiObrokItem(
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
                        .size(50.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            MacroLightGreen.copy(alpha = 0.18f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = MacroGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(13.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stavka.nazivJela,
                        color = MacroText,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = "Evidentirani obrok",
                        color = MacroTextSecondary,
                        fontSize = 12.sp
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
                        color = MacroTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            HorizontalDivider(
                color = MacroTextSecondary.copy(alpha = 0.10f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MacroValue(
                    label = "Proteini",
                    value = "%.0f g".format(stavka.proteini),
                    color = ProteinBlue
                )

                MacroValue(
                    label = "UH",
                    value = "%.0f g".format(stavka.ugljikohidrati),
                    color = CarbsOrange
                )

                MacroValue(
                    label = "Masti",
                    value = "%.0f g".format(stavka.masti),
                    color = FatPurple
                )
            }
        }
    }
}

@Composable
private fun MacroValue(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontWeight = FontWeight.SemiBold,
            color = color,
            fontSize = 14.sp
        )

        Text(
            text = label,
            color = MacroTextSecondary,
            fontSize = 11.sp
        )
    }
}