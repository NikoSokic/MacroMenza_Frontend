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

    var email by remember { mutableStateOf("") }
    var lozinka by remember { mutableStateOf("") }
    var ponovljenaLozinka by remember { mutableStateOf("") }

    val lozinkeSePoklapaju =
        lozinka.isNotBlank() && lozinka == ponovljenaLozinka

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.PersonAdd,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Registracija",
            style = MaterialTheme.typography.headlineLarge
        )

        Text(
            text = "Napravi MacroMenza račun",
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
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
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
                    lozinka = lozinka
                )
            },
            enabled =
                email.isNotBlank() &&
                        lozinkeSePoklapaju &&
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