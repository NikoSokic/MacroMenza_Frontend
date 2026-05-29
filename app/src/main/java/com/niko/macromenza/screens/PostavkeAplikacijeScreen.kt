package com.niko.macromenza.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun PostavkeAplikacijeScreen(
    navController: NavController
) {
    var tema by remember { mutableStateOf("SISTEMSKA") }

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
                text = "Postavke aplikacije",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        SettingsSectionCard(
            title = "Tema aplikacije",
            icon = {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = tema == "SISTEMSKA",
                    onClick = { tema = "SISTEMSKA" },
                    label = { Text("Sistemska") }
                )

                FilterChip(
                    selected = tema == "SVIJETLA",
                    onClick = { tema = "SVIJETLA" },
                    label = { Text("Svijetla") }
                )

                FilterChip(
                    selected = tema == "TAMNA",
                    onClick = { tema = "TAMNA" },
                    label = { Text("Tamna") }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsSectionCard(
            title = "Backend status",
            icon = {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            Text("Način rada: Development")
            Text("Backend: lokalni Spring Boot")
            Text("Baza: Supabase PostgreSQL")
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsSectionCard(
            title = "O aplikaciji",
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            Text("MacroMenza")
            Text("Aplikacija za praćenje prehrane u menzi")
            Text("Verzija: 1.0")
            Text("Završni rad")
        }
    }
}

@Composable
fun SettingsSectionCard(
    title: String,
    icon: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                icon()

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            content()
        }
    }
}