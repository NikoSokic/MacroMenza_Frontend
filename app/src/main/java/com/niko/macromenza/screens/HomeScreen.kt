package com.niko.macromenza.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.niko.macromenza.model.StavkaObroka
import com.niko.macromenza.session.UserSessionManager
import com.niko.macromenza.viewmodel.HomeViewModel

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

    val prijavljeniKorisnikId by sessionManager.korisnikId.collectAsState(initial = null)

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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 16.dp,
            end = 16.dp,
            bottom = 100.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Danas",
                    style = MaterialTheme.typography.headlineLarge
                )

                Text(
                    text = "Pregled dnevnog unosa",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        if (preporuka == null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Text(
                            text = "Preporuka nije postavljena.",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Otvori Profil → Postavke ciljeva kako bi se izračunali tvoji dnevni ciljevi.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Uneseno kalorija",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = "%.0f / %.0f kcal".format(kalorije, ciljKalorije),
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = {
                            (kalorije / ciljKalorije).toFloat().coerceIn(0f, 1f)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Preostalo: %.0f kcal".format(
                            (ciljKalorije - kalorije).coerceAtLeast(0.0)
                        )
                    )
                }
            }
        }

        item {
            Text(
                text = "Makronutrijenti",
                style = MaterialTheme.typography.titleLarge
            )
        }

        item {
            MacroProgress("Proteini", proteini, ciljProteini, "g")
        }

        item {
            MacroProgress("Ugljikohidrati", uh, ciljUh, "g")
        }

        item {
            MacroProgress("Masti", masti, ciljMasti, "g")
        }

        item {
            Text(
                text = "Brzo dodaj obrok",
                style = MaterialTheme.typography.titleLarge
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                QuickMealButton(
                    "Doručak",
                    "DORUCAK",
                    navController,
                    Modifier.weight(1f)
                )

                QuickMealButton(
                    "Ručak",
                    "RUCAK",
                    navController,
                    Modifier.weight(1f)
                )

                QuickMealButton(
                    "Večera",
                    "VECERA",
                    navController,
                    Modifier.weight(1f)
                )

                QuickMealButton(
                    "Užina",
                    "UZINA",
                    navController,
                    Modifier.weight(1f)
                )
            }
        }

        item {
            Text(
                text = "Današnji obroci",
                style = MaterialTheme.typography.titleLarge
            )
        }

        val imaObroka = danasnjiObroci?.obroci?.any { (_, stavke) ->
            stavke.isNotEmpty()
        } == true

        if (!imaObroka) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Nema današnjih obroka.",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Dodaj doručak, ručak, večeru ili užinu.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            danasnjiObroci?.obroci?.forEach { (tip, stavke) ->
                if (stavke.isNotEmpty()) {
                    item {
                        Text(
                            text = tip,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    items(stavke) { stavka ->
                        DanasnjiObrokItem(stavka)
                    }
                }
            }
        }
    }
}

@Composable
fun MacroProgress(
    naziv: String,
    vrijednost: Double,
    cilj: Double,
    jedinica: String
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = naziv,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "%.0f / %.0f %s".format(vrijednost, cilj, jedinica)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = {
                    (vrijednost / cilj).toFloat().coerceIn(0f, 1f)
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun QuickMealButton(
    label: String,
    tipObroka: String,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = {
            navController.navigate("unos_obroka/$tipObroka")
        },
        modifier = modifier.height(42.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun DanasnjiObrokItem(stavka: StavkaObroka) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = stavka.nazivJela,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "%.0f kcal | P %.0fg | UH %.0fg | M %.0fg".format(
                    stavka.kalorije,
                    stavka.proteini,
                    stavka.ugljikohidrati,
                    stavka.masti
                ),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}