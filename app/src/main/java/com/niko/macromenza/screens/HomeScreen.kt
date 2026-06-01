package com.niko.macromenza.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.niko.macromenza.viewmodel.HomeViewModel
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.niko.macromenza.model.StavkaObroka
import androidx.compose.ui.platform.LocalContext
import com.niko.macromenza.session.UserSessionManager


@Composable
fun HomeScreen(
    navController: NavController,
    refreshKey: Int = 0,
    viewModel: HomeViewModel = viewModel()
) {
    val ukupniUnos by viewModel.ukupniUnos.collectAsState()
    val danasnjiObroci by viewModel.danasnjiObroci.collectAsState()
    val preporuka by viewModel.preporuka.collectAsState()

    val ciljKalorije = preporuka?.kalorije ?: 2400.0
    val ciljProteini = preporuka?.proteini ?: 160.0
    val ciljUh = preporuka?.ugljikohidrati ?: 300.0
    val ciljMasti = preporuka?.masti ?: 80.0

    val context = LocalContext.current

    val sessionManager = remember {
        UserSessionManager(context)
    }

    val prijavljeniKorisnikId by sessionManager.korisnikId.collectAsState(initial = null)

    if (preporuka == null) {
        Spacer(modifier = Modifier.height(12.dp))

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

    LaunchedEffect(refreshKey, prijavljeniKorisnikId) {
        prijavljeniKorisnikId?.let { id ->
            viewModel.ucitajUkupniUnos(id)
            viewModel.ucitajDanasnjeObroke(id)
            viewModel.ucitajPreporuku(id)
        }
    }

    val kalorije = ukupniUnos?.kalorije ?: 0.0
    val proteini = ukupniUnos?.proteini ?: 0.0
    val uh = ukupniUnos?.ugljikohidrati ?: 0.0
    val masti = ukupniUnos?.masti ?: 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Danas", style = MaterialTheme.typography.headlineLarge)
        Text("Pregled dnevnog unosa")

        Spacer(modifier = Modifier.height(12.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Uneseno kalorija", style = MaterialTheme.typography.titleMedium)

                Text(
                    "%.0f / %.0f kcal".format(kalorije, ciljKalorije),
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
                    "Preostalo: %.0f kcal".format(
                        (ciljKalorije - kalorije).coerceAtLeast(0.0)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text("Makronutrijenti", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(10.dp))

        MacroProgress("Proteini", proteini, ciljProteini, "g")
        MacroProgress("Ugljikohidrati", uh, ciljUh, "g")
        MacroProgress("Masti", masti, ciljMasti, "g")

        Spacer(modifier = Modifier.height(12.dp))

        Text("Brzo dodaj obrok", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(10.dp))

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

        Spacer(modifier = Modifier.height(12.dp))

        Text("Današnji obroci", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
        ) {
            LazyColumn(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val imaObroka = danasnjiObroci?.obroci?.any { (_, stavke) ->
                    stavke.isNotEmpty()
                } == true

                if (!imaObroka) {
                    item {
                        Text(
                            text = "Nema današnjih obroka.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    item {
                        Text(
                            text = "Dodaj doručak, ručak, večeru ili užinu.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(naziv, style = MaterialTheme.typography.titleMedium)
                Text("%.0f / %.0f %s".format(vrijednost, cilj, jedinica))
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
            Text(stavka.nazivJela)
            Text(
                "%.0f kcal | P %.0fg | UH %.0fg | M %.0fg".format(
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
