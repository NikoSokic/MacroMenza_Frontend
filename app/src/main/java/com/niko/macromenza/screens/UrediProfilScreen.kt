package com.niko.macromenza.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.niko.macromenza.viewmodel.ProfilViewModel

@Composable
fun UrediProfilScreen(
    navController: NavController,
    viewModel: ProfilViewModel = viewModel()
) {
    val korisnik by viewModel.korisnik.collectAsState()
    val profil by viewModel.profil.collectAsState()
    val poruka by viewModel.poruka.collectAsState()

    var ime by remember { mutableStateOf("") }
    var prezime by remember { mutableStateOf("") }
    var visina by remember { mutableStateOf("") }
    var masa by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var spol by remember { mutableStateOf("M") }

    LaunchedEffect(Unit) {
        viewModel.ucitajProfil(1)
    }

    LaunchedEffect(korisnik, profil) {
        korisnik?.let {
            ime = it.ime
            prezime = it.prezime
        }

        profil?.let {
            visina = it.visina.toString()
            dob = it.dob.toString()
            spol = it.spol
        }
    }

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
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Nazad"
                )
            }

            Text(
                text = "Uredi profil",
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
                    text = "Osobni podaci",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = ime,
                    onValueChange = { ime = it },
                    label = { Text("Ime") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = prezime,
                    onValueChange = { prezime = it },
                    label = { Text("Prezime") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = visina,
                    onValueChange = { visina = it },
                    label = { Text("Visina (cm)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = masa,
                    onValueChange = { masa = it },
                    label = { Text("Masa (kg)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = dob,
                    onValueChange = { dob = it },
                    label = { Text("Dob") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Spol",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = spol == "M",
                onClick = { spol = "M" },
                label = { Text("Muško") }
            )

            FilterChip(
                selected = spol == "Z",
                onClick = { spol = "Z" },
                label = { Text("Žensko") }
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
                viewModel.spremiProfil(
                    idKorisnik = 1,
                    ime = ime,
                    prezime = prezime,
                    visina = visina.toIntOrNull() ?: 0,
                    masa = masa.toDoubleOrNull() ?: 0.0,
                    dob = dob.toIntOrNull() ?: 0,
                    spol = spol
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Text("Spremi promjene")
        }
    }
}