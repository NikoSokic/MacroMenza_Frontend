package com.niko.macromenza.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.niko.macromenza.viewmodel.AuthViewModel

@Composable
fun RegisterScreen(
    navController: NavController,
    viewModel: AuthViewModel
) {
    val poruka by viewModel.poruka.collectAsState()
    val ucitavanje by viewModel.ucitavanje.collectAsState()

    var ime by remember { mutableStateOf("") }
    var prezime by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var lozinka by remember { mutableStateOf("") }
    var ponovljenaLozinka by remember { mutableStateOf("") }

    var spol by remember { mutableStateOf("M") }
    var visina by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var masa by remember { mutableStateOf("") }

    var aktivnost by remember { mutableStateOf("umjerena") }
    var cilj by remember { mutableStateOf("odrzavanje") }

    val lozinkeSePoklapaju =
        lozinka.isNotBlank() && lozinka == ponovljenaLozinka

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Icon(
            imageVector = Icons.Default.PersonAdd,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Registracija", style = MaterialTheme.typography.headlineLarge)
        Text("Unesi podatke za MacroMenza račun")

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = ime,
            onValueChange = { ime = it },
            label = { Text("Ime") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = prezime,
            onValueChange = { prezime = it },
            label = { Text("Prezime") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = lozinka,
            onValueChange = { lozinka = it },
            label = { Text("Lozinka") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = ponovljenaLozinka,
            onValueChange = { ponovljenaLozinka = it },
            label = { Text("Ponovi lozinku") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            isError = ponovljenaLozinka.isNotBlank() && !lozinkeSePoklapaju
        )

        if (ponovljenaLozinka.isNotBlank() && !lozinkeSePoklapaju) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Lozinke se ne poklapaju.",
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text("Spol", style = MaterialTheme.typography.titleMedium)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = visina,
            onValueChange = { visina = it },
            label = { Text("Visina (cm)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = dob,
            onValueChange = { dob = it },
            label = { Text("Dob") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = masa,
            onValueChange = { masa = it },
            label = { Text("Masa (kg)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text("Razina aktivnosti", style = MaterialTheme.typography.titleMedium)

        Column {
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

        Spacer(modifier = Modifier.height(20.dp))

        Text("Cilj", style = MaterialTheme.typography.titleMedium)

        Column {
            FilterChip(
                selected = cilj == "mrsavljenje",
                onClick = { cilj = "mrsavljenje" },
                label = { Text("Mršavljenje") }
            )

            FilterChip(
                selected = cilj == "odrzavanje",
                onClick = { cilj = "odrzavanje" },
                label = { Text("Održavanje") }
            )

            FilterChip(
                selected = cilj == "dobivanje_mase",
                onClick = { cilj = "dobivanje_mase" },
                label = { Text("Dobivanje mase") }
            )
        }

        if (poruka != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = poruka ?: "",
                color = if (poruka?.contains("uspješna", ignoreCase = true) == true)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                viewModel.registracija(
                    email = email.trim(),
                    lozinka = lozinka,
                    ime = ime.trim(),
                    prezime = prezime.trim(),
                    spol = spol,
                    visina = visina.toInt(),
                    dob = dob.toInt(),
                    masa = masa.toDouble(),
                    razinaAktivnosti = aktivnost,
                    tipCilja = cilj
                )
            },
            enabled =
                ime.isNotBlank() &&
                        prezime.isNotBlank() &&
                        email.isNotBlank() &&
                        lozinkeSePoklapaju &&
                        visina.toIntOrNull() != null &&
                        dob.toIntOrNull() != null &&
                        masa.toDoubleOrNull() != null &&
                        !ucitavanje,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            if (ucitavanje) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Registriraj se")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(
            onClick = {
                navController.navigate("login")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Već imaš račun? Prijavi se")
        }
    }
}