package com.niko.macromenza.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun KonzumacijaScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text("Što jedeš?", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(20.dp))

        ObrokButton("Doručak", "DORUCAK", navController)
        ObrokButton("Ručak", "RUCAK", navController)
        ObrokButton("Večera", "VECERA", navController)
        ObrokButton("Užina", "UZINA", navController)
    }
}

@Composable
fun ObrokButton(
    label: String,
    tipObroka: String,
    navController: NavController
) {
    Button(
        onClick = {
            navController.navigate("unos_obroka/$tipObroka")
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .height(60.dp)
    ) {
        Text(label)
    }
}