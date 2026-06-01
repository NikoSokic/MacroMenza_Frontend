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
import com.niko.macromenza.viewmodel.ProfilViewModel
import androidx.compose.ui.platform.LocalContext
import com.niko.macromenza.session.UserSessionManager
@Composable
fun UrediProfilScreen(
    navController: NavController,
    viewModel: ProfilViewModel = viewModel()
) {
    val korisnik by viewModel.korisnik.collectAsState()
    val profil by viewModel.profil.collectAsState()
    val poruka by viewModel.poruka.collectAsState()
    val context = LocalContext.current

    val sessionManager = remember {
        UserSessionManager(context)
    }

    val prijavljeniKorisnikId by sessionManager.korisnikId.collectAsState(initial = null)


    var ime by remember { mutableStateOf("") }
    var prezime by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var spol by remember { mutableStateOf("M") }

    LaunchedEffect(prijavljeniKorisnikId) {
        prijavljeniKorisnikId?.let { id ->
            viewModel.ucitajProfil(id)
        }
    }

    LaunchedEffect(korisnik, profil) {
        korisnik?.let {
            ime = it.ime
            prezime = it.prezime
            email = it.email
        }

        profil?.let {
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
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
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
                    idKorisnik = prijavljeniKorisnikId ?: return@Button,
                    ime = ime,
                    prezime = prezime,
                    email = email,
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