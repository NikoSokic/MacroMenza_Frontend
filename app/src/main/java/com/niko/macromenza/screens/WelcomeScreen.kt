package com.niko.macromenza.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun WelcomeScreen(
    navController: NavController
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "Dobrodošao u MacroMenza",
            style = MaterialTheme.typography.headlineLarge
        )

        Text(
            text = "Prati prehranu i makronutrijente u menzi",
            style = MaterialTheme.typography.bodyLarge
        )

        Button(
            onClick = {
                navController.navigate("register")
            },
            modifier = Modifier.padding(top = 32.dp)
        ) {
            Text("Sign Up")
        }

        TextButton(
            onClick = {
                navController.navigate("login")
            }
        ) {
            Text("Log In")
        }
    }
}