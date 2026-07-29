package com.niko.macromenza.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.niko.macromenza.ui.theme.MacroGreen
import com.niko.macromenza.ui.theme.MacroLightGreen
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
    var prikaziLozinku by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        viewModel.ocistiPoruku()

        onDispose {
            viewModel.ocistiPoruku()
        }
    }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        // Dekoracija gore lijevo
        Box(
            modifier = Modifier
                .size(210.dp)
                .offset(x = (-110).dp, y = (-95).dp)
                .clip(CircleShape)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                )
        )

        // Dekoracija dolje desno
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 120.dp, y = 110.dp)
                .clip(CircleShape)
                .background(
                    MacroGreen.copy(alpha = 0.08f)
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp)
                .padding(top = 90.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.Start
        ) {

            // Ikona
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Login,
                    contentDescription = null,
                    tint = MacroGreen,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Dobrodošao natrag",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Prijavi se u svoj MacroMenza račun.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(36.dp))

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                },
                label = {
                    Text("Email")
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MacroGreen,
                    focusedLabelColor = MacroGreen,
                    cursorColor = MacroGreen
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = lozinka,
                onValueChange = {
                    lozinka = it
                },
                label = {
                    Text("Lozinka")
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                visualTransformation =
                    if (prikaziLozinku) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            prikaziLozinku = !prikaziLozinku
                        }
                    ) {
                        Icon(
                            imageVector =
                                if (prikaziLozinku) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                            contentDescription =
                                if (prikaziLozinku) {
                                    "Sakrij lozinku"
                                } else {
                                    "Prikaži lozinku"
                                }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MacroGreen,
                    focusedLabelColor = MacroGreen,
                    cursorColor = MacroGreen
                )
            )

            poruka?.let {
                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    viewModel.prijava(
                        email = email.trim(),
                        lozinka = lozinka
                    )
                },
                enabled =
                    email.isNotBlank() &&
                            lozinka.isNotBlank() &&
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
                        text = "Prijavi se",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "Nemaš račun?",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TextButton(
                    onClick = {
                        viewModel.ocistiPoruku()
                        navController.navigate("register")
                    }
                ) {
                    Text(
                        text = "Registriraj se",
                        color = MacroGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}