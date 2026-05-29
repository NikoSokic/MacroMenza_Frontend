package com.niko.macromenza.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.niko.macromenza.viewmodel.CiljeviViewModel

@Composable
fun PostavkeCiljevaScreen(
    navController: NavController,
    viewModel: CiljeviViewModel = viewModel()
) {
    val poruka by viewModel.poruka.collectAsState()

    var visina by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var masa by remember { mutableStateOf("") }

    var tipCilja by remember { mutableStateOf("odrzavanje") }
    var aktivnost by remember { mutableStateOf("umjerena") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .padding(bottom = 80.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = {
                    navController.popBackStack()
                }
            ) {
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

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Podaci za preporuku",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = visina,
                    onValueChange = { visina = it },
                    label = { Text("Visina (cm)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = dob,
                    onValueChange = { dob = it },
                    label = { Text("Dob") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = masa,
                    onValueChange = { masa = it },
                    label = { Text("Masa (kg)") },
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

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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

        if (poruka != null) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = poruka ?: "",
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                viewModel.spremiCilj(
                    idKorisnik = 1,
                    visina = visina.toIntOrNull() ?: 0,
                    dob = dob.toIntOrNull() ?: 0,
                    masa = masa.toDoubleOrNull() ?: 0.0,
                    razinaAktivnosti = aktivnost,
                    tipCilja = tipCilja
                )
            },
            enabled =
                visina.toIntOrNull() != null &&
                        dob.toIntOrNull() != null &&
                        masa.toDoubleOrNull() != null &&
                        visina.toIntOrNull()!! > 0 &&
                        dob.toIntOrNull()!! > 0 &&
                        masa.toDoubleOrNull()!! > 0,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Text("Spremi cilj i izračunaj preporuku")
        }
    }
}