package com.niko.macromenza.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.niko.macromenza.model.Jelo
import com.niko.macromenza.viewmodel.JelaViewModel
import kotlinx.coroutines.delay
import androidx.compose.material.icons.filled.FilterList


@Composable
fun UnosObrokaScreen(
    tipObroka: String,
    navController: NavController,
    viewModel: JelaViewModel = viewModel()
) {
    val jela by viewModel.jela.collectAsState()
    val greska by viewModel.greska.collectAsState()

    var search by remember { mutableStateOf("") }
    var showFilterDialog by remember { mutableStateOf(false) }

    var minKalorije by remember { mutableStateOf("") }
    var maxKalorije by remember { mutableStateOf("") }
    var minProteini by remember { mutableStateOf("") }
    var maxProteini by remember { mutableStateOf("") }

    val kolicine = remember { mutableStateMapOf<Long, Int>() }

    LaunchedEffect(search) {
        delay(300)
        viewModel.ucitajJela(search = search.ifBlank { null })
    }

    val odabranaJela = jela.filter { (kolicine[it.id] ?: 0) > 0 }

    val ukupnoKalorija = odabranaJela.sumOf { it.kalorije * (kolicine[it.id] ?: 0) }
    val ukupnoProteina = odabranaJela.sumOf { it.proteini * (kolicine[it.id] ?: 0) }
    val ukupnoUh = odabranaJela.sumOf { it.ugljikohidrati * (kolicine[it.id] ?: 0) }
    val ukupnoMasti = odabranaJela.sumOf { it.masti * (kolicine[it.id] ?: 0) }
    var prikaziOstalo by remember { mutableStateOf(false) }
    var minUgljikohidrati by remember { mutableStateOf("") }
    var maxUgljikohidrati by remember { mutableStateOf("") }
    var minMasti by remember { mutableStateOf("") }
    var maxMasti by remember { mutableStateOf("") }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(tipObroka, style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text("Pretraži jelo") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = { showFilterDialog = true },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(Icons.Default.FilterList, contentDescription = "Filter")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (showFilterDialog) {
            AlertDialog(
                onDismissRequest = { showFilterDialog = false },
                title = {
                    Text("Filter jela")
                },
                text = {
                    Column {
                        OutlinedTextField(
                            value = minKalorije,
                            onValueChange = { minKalorije = it },
                            label = { Text("Min kalorije") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = maxKalorije,
                            onValueChange = { maxKalorije = it },
                            label = { Text("Max kalorije") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = minProteini,
                            onValueChange = { minProteini = it },
                            label = { Text("Min proteini") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = maxProteini,
                            onValueChange = { maxProteini = it },
                            label = { Text("Max proteini") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        TextButton(
                            onClick = { prikaziOstalo = !prikaziOstalo }
                        ) {
                            Text(if (prikaziOstalo) "Sakrij ostalo" else "Prikaži ostalo")
                        }

                        if (prikaziOstalo) {
                            OutlinedTextField(
                                value = minUgljikohidrati,
                                onValueChange = { minUgljikohidrati = it },
                                label = { Text("Min ugljikohidrati") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = maxUgljikohidrati,
                                onValueChange = { maxUgljikohidrati = it },
                                label = { Text("Max ugljikohidrati") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = minMasti,
                                onValueChange = { minMasti = it },
                                label = { Text("Min masti") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = maxMasti,
                                onValueChange = { maxMasti = it },
                                label = { Text("Max masti") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.filtrirajJela(
                                minKalorije = minKalorije.toIntOrNull(),
                                maxKalorije = maxKalorije.toIntOrNull(),
                                minProteini = minProteini.toIntOrNull(),
                                maxProteini = maxProteini.toIntOrNull(),
                                minUgljikohidrati = minUgljikohidrati.toIntOrNull(),
                                maxUgljikohidrati = maxUgljikohidrati.toIntOrNull(),
                                minMasti = minMasti.toIntOrNull(),
                                maxMasti = maxMasti.toIntOrNull()
                            )

                            showFilterDialog = false
                        }
                    ) {
                        Text("Primijeni")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            minKalorije = ""
                            maxKalorije = ""
                            minProteini = ""
                            maxProteini = ""
                            minUgljikohidrati = ""
                            maxUgljikohidrati = ""
                            minMasti = ""
                            maxMasti = ""
                            prikaziOstalo = false

                            viewModel.ucitajJela(search = search.ifBlank { null })

                            showFilterDialog = false
                        }
                    ) {
                        Text("Resetiraj")
                    }
                }
            )
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Ukupno", style = MaterialTheme.typography.titleMedium)
                Text("%.1f kcal".format(ukupnoKalorija))
                Text(
                    "P: %.1f g | UH: %.1f g | M: %.1f g".format(
                        ukupnoProteina,
                        ukupnoUh,
                        ukupnoMasti
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        odabranaJela.forEach { jelo ->
                            viewModel.spremiKonzumaciju(
                                idJelo = jelo.id,
                                kolicina = (kolicine[jelo.id] ?: 0).toDouble(),
                                tipObroka = tipObroka
                            )
                        }

                        navController.navigate("obrok_spremljen/$tipObroka")
                    },
                    enabled = odabranaJela.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Spremi obrok")
                }
            }
        }

        if (greska != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Greška: $greska", color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (jela.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Nema pronađenih jela.",
                                style = MaterialTheme.typography.titleMedium
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Pokušaj promijeniti pretragu ili filter.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(jela) { jelo ->
                    JeloKolicinaItem(
                        jelo = jelo,
                        kolicina = kolicine[jelo.id] ?: 0,
                        onPlus = {
                            kolicine[jelo.id] = (kolicine[jelo.id] ?: 0) + 1
                        },
                        onMinus = {
                            val trenutna = kolicine[jelo.id] ?: 0
                            if (trenutna > 0) {
                                kolicine[jelo.id] = trenutna - 1
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun JeloKolicinaItem(
    jelo: Jelo,
    kolicina: Int,
    onPlus: () -> Unit,
    onMinus: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(jelo.naziv, style = MaterialTheme.typography.titleMedium)
                Text(
                    "%.0f kcal | P %.1fg | UH %.1fg | M %.1fg".format(
                        jelo.kalorije,
                        jelo.proteini,
                        jelo.ugljikohidrati,
                        jelo.masti
                    )
                )
            }

            Row {
                IconButton(onClick = onMinus) {
                    Icon(Icons.Default.Remove, contentDescription = "Smanji")
                }

                Text(
                    text = kolicina.toString(),
                    modifier = Modifier.padding(top = 12.dp)
                )

                IconButton(onClick = onPlus) {
                    Icon(Icons.Default.Add, contentDescription = "Povećaj")
                }
            }
        }
    }
}