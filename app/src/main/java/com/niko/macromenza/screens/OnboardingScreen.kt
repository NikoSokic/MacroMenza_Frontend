package com.niko.macromenza.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.niko.macromenza.api.RetrofitInstance
import com.niko.macromenza.model.RegistracijaProfilRequest
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import com.niko.macromenza.session.UserSessionManager

@Composable
fun OnboardingScreen(
    navController: NavController,
    korisnikId: Long
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val sessionManager = remember {
        UserSessionManager(context)
    }

    var ime by remember { mutableStateOf("") }
    var prezime by remember { mutableStateOf("") }
    var spol by remember { mutableStateOf("M") }
    var visina by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var masa by remember { mutableStateOf("") }
    var aktivnost by remember { mutableStateOf("umjerena") }
    var cilj by remember { mutableStateOf("odrzavanje") }

    var ucitavanje by remember { mutableStateOf(false) }
    var poruka by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            text = "Dovrši profil",
            style = MaterialTheme.typography.headlineLarge
        )

        Text(
            text = "Unesi podatke za izračun dnevne preporuke.",
            style = MaterialTheme.typography.bodyMedium
        )

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

        Spacer(modifier = Modifier.height(16.dp))

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
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                scope.launch {
                    ucitavanje = true
                    poruka = null

                    try {
                        val korisnik = RetrofitInstance.api.dohvatiKorisnika(korisnikId)

                        RetrofitInstance.api.registracijaProfil(
                            RegistracijaProfilRequest(
                                supabaseUid = korisnik.supabaseUid ?: "",
                                email = korisnik.email,
                                ime = ime.trim(),
                                prezime = prezime.trim(),
                                spol = spol,
                                visina = visina.toInt(),
                                dob = dob.toInt(),
                                masa = masa.toDouble(),
                                razinaAktivnosti = aktivnost,
                                tipCilja = cilj
                            )
                        )

                        sessionManager.spremiSesiju(
                            korisnikId = korisnikId,
                            supabaseUid = korisnik.supabaseUid ?: ""
                        )

                        navController.navigate("home") {
                            popUpTo("onboarding/$korisnikId") {
                                inclusive = true
                            }
                        }

                    } catch (e: Exception) {
                        poruka = e.message ?: "Greška pri spremanju podataka"
                    } finally {
                        ucitavanje = false
                    }
                }
            },
            enabled =
                ime.isNotBlank() &&
                        prezime.isNotBlank() &&
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
                Text("Završi")
            }
        }
    }
}