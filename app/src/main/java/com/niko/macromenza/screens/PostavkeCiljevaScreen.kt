package com.niko.macromenza.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.niko.macromenza.session.UserSessionManager
import com.niko.macromenza.viewmodel.CiljeviViewModel

@Composable
fun PostavkeCiljevaScreen(
    navController: NavController,
    viewModel: CiljeviViewModel = viewModel()
) {
    val poruka by viewModel.poruka.collectAsState()
    val context = LocalContext.current

    val sessionManager = remember {
        UserSessionManager(context)
    }

    val prijavljeniKorisnikId by sessionManager.korisnikId.collectAsState(initial = null)

    var visina by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var masa by remember { mutableStateOf("") }

    var tipCilja by remember { mutableStateOf("odrzavanje") }
    var aktivnost by remember { mutableStateOf("umjerena") }

    var greska by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .padding(bottom = 80.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Nazad"
                )
            }

            Text(
                text = "Postavke ciljeva",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Podaci za preporuku",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = visina,
                    onValueChange = { noviUnos ->
                        if (noviUnos.all { it.isDigit() }) {
                            visina = noviUnos
                        }
                    },
                    label = { Text("Visina (cm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = dob,
                    onValueChange = { noviUnos ->
                        if (noviUnos.all { it.isDigit() }) {
                            dob = noviUnos
                        }
                    },
                    label = { Text("Dob") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = masa,
                    onValueChange = { noviUnos ->
                        if (noviUnos.all { it.isDigit() || it == '.' || it == ',' }) {
                            masa = noviUnos.replace(",", ".")
                        }
                    },
                    label = { Text("Masa (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Tip cilja",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = tipCilja == "mrsavljenje",
                onClick = { tipCilja = "mrsavljenje" },
                label = { Text("Mršavljenje") }
            )

            FilterChip(
                selected = tipCilja == "odrzavanje",
                onClick = { tipCilja = "odrzavanje" },
                label = { Text("Održavanje težine") }
            )

            FilterChip(
                selected = tipCilja == "dobivanje_mase",
                onClick = { tipCilja = "dobivanje_mase" },
                label = { Text("Dobivanje mase") }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Razina aktivnosti",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = aktivnost == "sjedilacka",
                onClick = { aktivnost = "sjedilacka" },
                label = { Text("Sjedilačka") }
            )

            FilterChip(
                selected = aktivnost == "lagana",
                onClick = { aktivnost = "lagana" },
                label = { Text("Lagana") }
            )

            FilterChip(
                selected = aktivnost == "umjerena",
                onClick = { aktivnost = "umjerena" },
                label = { Text("Umjerena") }
            )

            FilterChip(
                selected = aktivnost == "visoka",
                onClick = { aktivnost = "visoka" },
                label = { Text("Visoka") }
            )
        }

        greska?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error
            )
        }

        poruka?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val visinaBroj = visina.toIntOrNull()
                val dobBroj = dob.toIntOrNull()
                val masaBroj = masa.toDoubleOrNull()

                if (prijavljeniKorisnikId == null) {
                    greska = "Korisnik nije prijavljen."
                    return@Button
                }

                if (visinaBroj == null) {
                    greska = "Visina mora biti broj."
                    return@Button
                }

                if (dobBroj == null) {
                    greska = "Dob mora biti broj."
                    return@Button
                }

                if (masaBroj == null) {
                    greska = "Masa mora biti broj."
                    return@Button
                }

                if (visinaBroj < 100 || visinaBroj > 250) {
                    greska = "Visina mora biti između 100 i 250 cm."
                    return@Button
                }

                if (dobBroj < 15 || dobBroj > 100) {
                    greska = "Dob mora biti između 15 i 100 godina."
                    return@Button
                }

                if (masaBroj < 30 || masaBroj > 300) {
                    greska = "Masa mora biti između 30 i 300 kg."
                    return@Button
                }

                greska = null

                viewModel.spremiCilj(
                    idKorisnik = prijavljeniKorisnikId!!,
                    visina = visinaBroj,
                    dob = dobBroj,
                    masa = masaBroj,
                    razinaAktivnosti = aktivnost,
                    tipCilja = tipCilja
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Text("Spremi cilj i izračunaj preporuku")
        }
    }
}