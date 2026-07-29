package com.niko.macromenza.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.niko.macromenza.session.UserSessionManager
import com.niko.macromenza.ui.components.screenBottomPadding
import com.niko.macromenza.ui.components.screenTopPadding
import com.niko.macromenza.ui.theme.MacroGreen
import com.niko.macromenza.ui.theme.MacroLightGreen
import com.niko.macromenza.ui.theme.MacroText
import com.niko.macromenza.ui.theme.MacroTextSecondary
import com.niko.macromenza.viewmodel.ProfilViewModel

@Composable
fun UrediProfilScreen(
    navController: NavController,
    viewModel: ProfilViewModel = viewModel()
) {
    val korisnik by viewModel.korisnik.collectAsState()
    val profil by viewModel.profil.collectAsState()
    val poruka by viewModel.poruka.collectAsState()

    val context = LocalContext.current

    val sessionManager = remember {
        UserSessionManager(context)
    }

    val prijavljeniKorisnikId by
    sessionManager.korisnikId.collectAsState(initial = null)

    var ime by remember { mutableStateOf("") }
    var prezime by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var spol by remember { mutableStateOf("M") }

    LaunchedEffect(prijavljeniKorisnikId) {
        prijavljeniKorisnikId?.let { id ->
            viewModel.ucitajProfil(id)
        }
    }

    LaunchedEffect(korisnik, profil) {
        korisnik?.let {
            ime = it.ime
            prezime = it.prezime
            email = it.email
        }

        profil?.let {
            spol = it.spol
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        Box(
            modifier = Modifier
                .size(230.dp)
                .offset(
                    x = 190.dp,
                    y = (-110).dp
                )
                .clip(CircleShape)
                .background(
                    MacroLightGreen.copy(alpha = 0.10f)
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(horizontal = 22.dp)
                .padding(
                    top = screenTopPadding(),
                    bottom = screenBottomPadding()
                )
        ) {

            // HEADER
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Surface(
                    onClick = {
                        navController.popBackStack()
                    },
                    modifier = Modifier.size(46.dp),
                    shape = CircleShape,
                    color = MacroLightGreen.copy(
                        alpha = 0.16f
                    )
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector =
                                Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Nazad",
                            tint = MacroGreen
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.width(14.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "Uredi profil",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MacroText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "Ažuriraj svoje osobne podatke",
                        color = MacroTextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(28.dp)
            )

            // OSOBNI PODACI
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp
            ) {

                Column(
                    modifier = Modifier.padding(18.dp)
                ) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = MacroLightGreen.copy(
                                alpha = 0.18f
                            )
                        ) {
                            Box(
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MacroGreen,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(
                            modifier = Modifier.width(12.dp)
                        )

                        Text(
                            text = "Osobni podaci",
                            modifier = Modifier.weight(1f),
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MacroText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(20.dp)
                    )

                    OutlinedTextField(
                        value = ime,
                        onValueChange = {
                            ime = it
                        },
                        label = {
                            Text("Ime")
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = editProfileFieldColors()
                    )

                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )

                    OutlinedTextField(
                        value = prezime,
                        onValueChange = {
                            prezime = it
                        },
                        label = {
                            Text("Prezime")
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = editProfileFieldColors()
                    )

                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                        },
                        label = {
                            Text("Email")
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = editProfileFieldColors()
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            // SPOL
            Text(
                text = "Spol",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MacroText
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                GenderOption(
                    title = "Muško",
                    selected = spol == "M",
                    onClick = {
                        spol = "M"
                    },
                    modifier = Modifier.weight(1f)
                )

                GenderOption(
                    title = "Žensko",
                    selected = spol == "Z",
                    onClick = {
                        spol = "Z"
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // PORUKA
            poruka?.let {

                Spacer(
                    modifier = Modifier.height(18.dp)
                )

                val uspjesno =
                    it.contains(
                        "usp",
                        ignoreCase = true
                    )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color =
                        if (uspjesno) {
                            MacroLightGreen.copy(
                                alpha = 0.18f
                            )
                        } else {
                            MaterialTheme.colorScheme.error
                                .copy(alpha = 0.08f)
                        }
                ) {
                    Text(
                        text = it,
                        modifier = Modifier.padding(14.dp),
                        color =
                            if (uspjesno) {
                                MacroGreen
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(30.dp)
            )

            // SPREMI
            Button(
                onClick = {
                    viewModel.spremiProfil(
                        idKorisnik =
                            prijavljeniKorisnikId
                                ?: return@Button,
                        ime = ime.trim(),
                        prezime = prezime.trim(),
                        email = email.trim(),
                        spol = spol
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MacroGreen,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(
                    horizontal = 18.dp,
                    vertical = 14.dp
                )
            ) {
                Text(
                    text = "Spremi promjene",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun GenderOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(
            min = 56.dp
        ),
        shape = RoundedCornerShape(17.dp),
        color =
            if (selected) {
                MacroLightGreen.copy(
                    alpha = 0.22f
                )
            } else {
                MaterialTheme.colorScheme.surface
            },
        border =
            if (selected) {
                androidx.compose.foundation.BorderStroke(
                    width = 1.5.dp,
                    color = MacroGreen
                )
            } else {
                androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color =
                        MacroTextSecondary.copy(
                            alpha = 0.20f
                        )
                )
            }
    ) {

        Box(
            modifier = Modifier.padding(
                horizontal = 10.dp,
                vertical = 14.dp
            ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                fontWeight =
                    if (selected) {
                        FontWeight.SemiBold
                    } else {
                        FontWeight.Normal
                    },
                color =
                    if (selected) {
                        MacroGreen
                    } else {
                        MacroText
                    },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun editProfileFieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MacroGreen,
        focusedLabelColor = MacroGreen,
        cursorColor = MacroGreen
    )