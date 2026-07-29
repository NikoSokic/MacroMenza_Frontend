package com.niko.macromenza.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.niko.macromenza.api.RetrofitInstance
import com.niko.macromenza.model.RegistracijaProfilRequest
import com.niko.macromenza.session.UserSessionManager
import com.niko.macromenza.ui.theme.MacroGreen
import com.niko.macromenza.ui.theme.MacroLightGreen
import kotlinx.coroutines.launch

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        Box(
            modifier = Modifier
                .size(220.dp)
                .offset(x = (-120).dp, y = (-90).dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
        )

        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 120.dp, y = 120.dp)
                .clip(CircleShape)
                .background(MacroGreen.copy(alpha = 0.08f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 56.dp, bottom = 36.dp)
        ) {

            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MacroGreen,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Još samo par detalja",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Ovi podaci služe za izračun tvoje dnevne preporuke.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Postavljanje profila",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Još malo",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MacroGreen
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { 0.75f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50)),
                color = MacroGreen,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
            )

            Spacer(modifier = Modifier.height(30.dp))

            SectionCard(
                title = "Osnovni podaci",
                icon = Icons.Default.Person
            ) {
                OutlinedTextField(
                    value = ime,
                    onValueChange = { ime = it },
                    label = { Text("Ime") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = onboardingFieldColors()
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = prezime,
                    onValueChange = { prezime = it },
                    label = { Text("Prezime") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = onboardingFieldColors()
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Spol",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = spol == "M",
                        onClick = { spol = "M" },
                        label = { Text("Muško") },
                        colors = onboardingChipColors()
                    )

                    FilterChip(
                        selected = spol == "Z",
                        onClick = { spol = "Z" },
                        label = { Text("Žensko") },
                        colors = onboardingChipColors()
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            SectionCard(
                title = "Tjelesne mjere",
                icon = Icons.Default.Straighten
            ) {
                OutlinedTextField(
                    value = visina,
                    onValueChange = { noviUnos ->
                        if (noviUnos.all { it.isDigit() }) {
                            visina = noviUnos
                        }
                    },
                    label = { Text("Visina (cm)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = onboardingFieldColors()
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = dob,
                    onValueChange = { noviUnos ->
                        if (noviUnos.all { it.isDigit() }) {
                            dob = noviUnos
                        }
                    },
                    label = { Text("Dob") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = onboardingFieldColors()
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = masa,
                    onValueChange = { noviUnos ->
                        if (noviUnos.all { it.isDigit() || it == '.' || it == ',' }) {
                            masa = noviUnos.replace(",", ".")
                        }
                    },
                    label = { Text("Masa (kg)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = onboardingFieldColors()
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            SectionCard(
                title = "Razina aktivnosti",
                icon = Icons.Default.FitnessCenter
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AktivnostChip(
                        tekst = "Sjedilačka",
                        vrijednost = "sjedilacka",
                        odabrano = aktivnost,
                        onClick = { aktivnost = "sjedilacka" }
                    )

                    AktivnostChip(
                        tekst = "Lagana",
                        vrijednost = "lagana",
                        odabrano = aktivnost,
                        onClick = { aktivnost = "lagana" }
                    )

                    AktivnostChip(
                        tekst = "Umjerena",
                        vrijednost = "umjerena",
                        odabrano = aktivnost,
                        onClick = { aktivnost = "umjerena" }
                    )

                    AktivnostChip(
                        tekst = "Visoka",
                        vrijednost = "visoka",
                        odabrano = aktivnost,
                        onClick = { aktivnost = "visoka" }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            SectionCard(
                title = "Tvoj cilj",
                icon = Icons.Default.TrackChanges
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GoalOption(
                        title = "Mršavljenje",
                        description = "Želim smanjiti tjelesnu masu.",
                        selected = cilj == "mrsavljenje",
                        onClick = { cilj = "mrsavljenje" }
                    )

                    GoalOption(
                        title = "Održavanje",
                        description = "Želim zadržati trenutnu tjelesnu masu.",
                        selected = cilj == "odrzavanje",
                        onClick = { cilj = "odrzavanje" }
                    )

                    GoalOption(
                        title = "Dobivanje mase",
                        description = "Želim povećati tjelesnu masu.",
                        selected = cilj == "dobivanje_mase",
                        onClick = { cilj = "dobivanje_mase" }
                    )
                }
            }

            poruka?.let {
                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            Button(
                onClick = {
                    val visinaBroj = visina.toIntOrNull()
                    val dobBroj = dob.toIntOrNull()
                    val masaBroj = masa.toDoubleOrNull()

                    if (visinaBroj == null || visinaBroj !in 100..250) {
                        poruka = "Provjeri visinu. Očekujemo vrijednost između 100 i 250 cm."
                        return@Button
                    }

                    if (dobBroj == null || dobBroj !in 15..100) {
                        poruka = "Provjeri dob. Očekujemo vrijednost između 15 i 100 godina."
                        return@Button
                    }

                    if (masaBroj == null || masaBroj !in 30.0..300.0) {
                        poruka = "Provjeri masu. Očekujemo vrijednost između 30 i 300 kg."
                        return@Button
                    }

                    scope.launch {
                        ucitavanje = true
                        poruka = null

                        try {
                            val korisnik =
                                RetrofitInstance.api.dohvatiKorisnika(korisnikId)

                            RetrofitInstance.api.registracijaProfil(
                                RegistracijaProfilRequest(
                                    supabaseUid = korisnik.supabaseUid ?: "",
                                    email = korisnik.email,
                                    ime = ime.trim(),
                                    prezime = prezime.trim(),
                                    spol = spol,
                                    visina = visinaBroj,
                                    dob = dobBroj,
                                    masa = masaBroj,
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
                            poruka =
                                "Nismo uspjeli spremiti podatke. Pokušaj ponovno."
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
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MacroGreen,
                    contentColor = Color.White,
                    disabledContainerColor = MacroGreen.copy(alpha = 0.30f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
            ) {
                if (ucitavanje) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "Završi postavljanje",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MacroGreen,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            content()
        }
    }
}

@Composable
private fun AktivnostChip(
    tekst: String,
    vrijednost: String,
    odabrano: String,
    onClick: () -> Unit
) {
    FilterChip(
        selected = odabrano == vrijednost,
        onClick = onClick,
        label = {
            Text(tekst)
        },
        colors = onboardingChipColors()
    )
}

@Composable
private fun GoalOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color =
            if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
            } else {
                MaterialTheme.colorScheme.background
            },
        border =
            if (selected) {
                androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    MacroGreen
                )
            } else {
                androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.20f)
                )
            }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MacroGreen
                )
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun onboardingFieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MacroGreen,
        focusedLabelColor = MacroGreen,
        cursorColor = MacroGreen
    )

@Composable
private fun onboardingChipColors() =
    FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
        selectedLabelColor = MacroGreen
    )