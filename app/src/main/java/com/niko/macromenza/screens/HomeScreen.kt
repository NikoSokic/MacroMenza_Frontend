package com.niko.macromenza.screens

import com.niko.macromenza.util.formatKcal
import com.niko.macromenza.util.formatMakro
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PhotoCamera
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
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.niko.macromenza.model.Savjet
import com.niko.macromenza.model.StavkaObroka
import com.niko.macromenza.session.UserSessionManager
import com.niko.macromenza.ui.components.screenBottomPadding
import com.niko.macromenza.ui.components.screenTopPadding
import com.niko.macromenza.ui.theme.MacroGreen
import com.niko.macromenza.ui.theme.MacroLightGreen
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
    val danPregled by viewModel.danPregled.collectAsState()
    val danasnjiObroci by viewModel.danasnjiObroci.collectAsState()
    val ucitava by viewModel.ucitava.collectAsState()

    val context = LocalContext.current

    val sessionManager = remember {
        UserSessionManager(context)
    }

    val prijavljeniKorisnikId by
    sessionManager.korisnikId.collectAsState(initial = null)

    // Osvježava se pri svakom povratku na Home (nakon spremanja, uređivanja ili brisanja obroka).
    LifecycleResumeEffect(refreshKey, prijavljeniKorisnikId) {
        prijavljeniKorisnikId?.let { id ->
            viewModel.ucitajHome(id)
        }
        onPauseOrDispose { }
    }

    var detaljJeloId by remember { mutableStateOf<Long?>(null) }

    val pregled = danPregled
    val cilj = pregled?.cilj
    val pojedeno = pregled?.pojedeno

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
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
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
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = "Tvoj dnevni pregled",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (pregled == null || pojedeno == null) {
                // Prvo učitavanje nije uspjelo (backend nedostupan)
                item {
                    PregledNedostupanCard(
                        onPonovi = {
                            prijavljeniKorisnikId?.let { viewModel.ucitajHome(it) }
                        }
                    )
                }
            } else {
                // PRSTEN KALORIJA PO OBROCIMA
                item {
                    PrstenKalorija(
                        danPregled = pregled,
                        onPostaviCilj = { navController.navigate("postavke_ciljeva") }
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
                            vrijednost = pojedeno.proteini,
                            cilj = cilj?.proteini,
                            color = ProteinBlue
                        )

                        MacroMiniCard(
                            modifier = Modifier.weight(1f),
                            naziv = "Ugljikoh.",
                            kratko = "UH",
                            vrijednost = pojedeno.ugljikohidrati,
                            cilj = cilj?.ugljikohidrati,
                            color = CarbsOrange
                        )

                        MacroMiniCard(
                            modifier = Modifier.weight(1f),
                            naziv = "Masti",
                            kratko = "M",
                            vrijednost = pojedeno.masti,
                            cilj = cilj?.masti,
                            color = FatPurple
                        )
                    }
                }

                // SAVJET DANA
                item {
                    SavjetDanaCard(
                        savjet = pregled.savjet,
                        onJeloClick = { detaljJeloId = it }
                    )
                }
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

                    Surface(
                        onClick = { navController.navigate(RUTA_SLIKA_ILI_OPIS) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = "Slika ili opis",
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
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

            item {
                Text(
                    text = "Vrijednosti su okvirne · Odakle su kalorije?",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate("odakle_su_kalorije") }
                        .padding(vertical = 8.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // Detalj jela iz savjeta dana: samo pregled, bez dodavanja u obrok
    detaljJeloId?.let { id ->
        JeloDetaljSheet(
            idJelo = id,
            onOdakleSuKalorije = {
                detaljJeloId = null
                navController.navigate("odakle_su_kalorije")
            },
            onDismiss = { detaljJeloId = null }
        )
    }
}

@Composable
private fun MacroMiniCard(
    modifier: Modifier,
    naziv: String,
    kratko: String,
    vrijednost: Double,
    cilj: Double?,
    color: Color
) {
    val progress =
        if (cilj != null && cilj > 0) {
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = "${formatMakro(vrijednost)}g",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )

            // Bez cilja nema ni "/ Xg" ni trake napretka
            if (cilj != null) {
                Text(
                    text = "/ %.0fg".format(cilj),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            } else {
                Text(
                    text = "bez cilja",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun SavjetDanaCard(
    savjet: Savjet,
    onJeloClick: (Long) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                        text = "Savjet dana",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )

                    Text(
                        text = savjet.naslov,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = savjet.tekst,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )

            savjet.jela.take(3).forEach { jelo ->
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    onClick = { onJeloClick(jelo.idJelo) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = jelo.naziv,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = "${formatKcal(jelo.kalorije)} kcal · ${formatMakro(jelo.proteini)} g proteina",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PregledNedostupanCard(onPonovi: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Pregled dana trenutno nije dostupan",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = onPonovi) {
                Text("Pokušaj ponovno", color = MacroGreen)
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
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Restaurant,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = label,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface,
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
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Restaurant,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(27.dp)
                )
            }

            Spacer(modifier = Modifier.height(13.dp))

            Text(
                text = "Još nema obroka",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Dodaj prvi današnji obrok i počni pratiti svoj unos.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = subtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        color = MaterialTheme.colorScheme.onSurface,
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
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
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
                        text = "Evidentirani obrok",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = formatKcal(stavka.kalorije),
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
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = 0.12f
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MacroValue(
                    label = "Proteini",
                    value = "${formatMakro(stavka.proteini)} g",
                    color = ProteinBlue
                )

                MacroValue(
                    label = "UH",
                    value = "${formatMakro(stavka.ugljikohidrati)} g",
                    color = CarbsOrange
                )

                MacroValue(
                    label = "Masti",
                    value = "${formatMakro(stavka.masti)} g",
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
    }
}