package com.niko.macromenza.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Login
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.niko.macromenza.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: AuthViewModel
) {
    val poruka by viewModel.poruka.collectAsState()
    val ucitavanje by viewModel.ucitavanje.collectAsState()
    val korisnikId by viewModel.korisnikId.collectAsState()
    val onboardingZavrsen by viewModel.onboardingZavrsen.collectAsState()

    var email by remember { mutableStateOf("") }
    var lozinka by remember { mutableStateOf("") }

    LaunchedEffect(korisnikId, onboardingZavrsen) {
        if (korisnikId != null && onboardingZavrsen != null) {
            if (onboardingZavrsen == true) {
                navController.navigate("home") {
                    popUpTo("login") {
                        inclusive = true
                    }
                }
            } else {
                navController.navigate("onboarding/$korisnikId") {
                    popUpTo("login") {
                        inclusive = true
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Login,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Prijava",
            style = MaterialTheme.typography.headlineLarge
        )

        Text(
            text = "Prijavi se u MacroMenza račun",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

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

        if (poruka != null) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = poruka ?: "",
                color = if (poruka == "Prijava uspješna")
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                viewModel.prijava(
                    email = email.trim(),
                    lozinka = lozinka
                )
            },
            enabled = email.isNotBlank() && lozinka.isNotBlank() && !ucitavanje,
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
                Text("Prijavi se")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(
            onClick = {
                navController.navigate("register")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Nemaš račun? Registriraj se")
        }
    }
}