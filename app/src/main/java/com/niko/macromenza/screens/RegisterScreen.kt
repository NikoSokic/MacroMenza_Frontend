package com.niko.macromenza.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
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
import com.niko.macromenza.ui.theme.MacroText
import com.niko.macromenza.ui.theme.MacroTextSecondary
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

    var prikaziLozinku by remember { mutableStateOf(false) }
    var prikaziPonovljenuLozinku by remember { mutableStateOf(false) }

    val lozinkeSePoklapaju =
        lozinka.isNotBlank() && lozinka == ponovljenaLozinka

    DisposableEffect(Unit) {
        viewModel.ocistiPoruku()

        onDispose {
            viewModel.ocistiPoruku()
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
                    MacroLightGreen.copy(alpha = 0.12f)
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
                .padding(top = 76.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.Start
        ) {

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        MacroLightGreen.copy(alpha = 0.18f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = MacroGreen,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            Text(
                text = "Krenimo",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MacroText
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Napravi svoj MacroMenza račun.",
                style = MaterialTheme.typography.bodyLarge,
                color = MacroTextSecondary
            )

            Spacer(modifier = Modifier.height(32.dp))

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

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = ponovljenaLozinka,
                onValueChange = {
                    ponovljenaLozinka = it
                },
                label = {
                    Text("Ponovi lozinku")
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                visualTransformation =
                    if (prikaziPonovljenuLozinku) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            prikaziPonovljenuLozinku =
                                !prikaziPonovljenuLozinku
                        }
                    ) {
                        Icon(
                            imageVector =
                                if (prikaziPonovljenuLozinku) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                            contentDescription =
                                if (prikaziPonovljenuLozinku) {
                                    "Sakrij lozinku"
                                } else {
                                    "Prikaži lozinku"
                                }
                        )
                    }
                },
                isError =
                    ponovljenaLozinka.isNotBlank() &&
                            !lozinkeSePoklapaju,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MacroGreen,
                    focusedLabelColor = MacroGreen,
                    cursorColor = MacroGreen
                )
            )

            if (
                ponovljenaLozinka.isNotBlank() &&
                !lozinkeSePoklapaju
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Lozinke se ne poklapaju.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            poruka?.let {
                Spacer(modifier = Modifier.height(14.dp))

                val uspjesnaPoruka =
                    it.contains(
                        "Poslali smo ti email",
                        ignoreCase = true
                    )

                Surface(
                    color =
                        if (uspjesnaPoruka) {
                            MacroGreen.copy(alpha = 0.10f)
                        } else {
                            MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
                        },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = it,
                        color =
                            if (uspjesnaPoruka) {
                                MacroGreen
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

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
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MacroGreen,
                    contentColor = Color.White,
                    disabledContainerColor =
                        MacroGreen.copy(alpha = 0.30f)
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
                        text = "Registriraj se",
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
                    text = "Već imaš račun?",
                    color = MacroTextSecondary
                )

                TextButton(
                    onClick = {
                        viewModel.ocistiPoruku()
                        navController.navigate("login")
                    }
                ) {
                    Text(
                        text = "Prijavi se",
                        color = MacroGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}