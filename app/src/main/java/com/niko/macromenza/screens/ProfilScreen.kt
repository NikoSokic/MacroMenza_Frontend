package com.niko.macromenza.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.niko.macromenza.viewmodel.ProfilViewModel
import androidx.compose.ui.platform.LocalContext
import com.niko.macromenza.session.UserSessionManager
import com.niko.macromenza.viewmodel.AuthViewModel
import com.niko.macromenza.viewmodel.AuthViewModelFactory
import androidx.compose.ui.platform.LocalContext


@Composable
fun ProfilScreen(
    navController: NavController,
    refreshKey: Int = 0,
    viewModel: ProfilViewModel = viewModel()
) {
    val korisnik by viewModel.korisnik.collectAsState()
    val zadnjeMjerenje by viewModel.zadnjeMjerenje.collectAsState()
    val zadnjaPreporuka by viewModel.zadnjaPreporuka.collectAsState()
    var prikaziOdjavaDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val sessionManager = remember {
        UserSessionManager(context)
    }

    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(
            UserSessionManager(context)
        )
    )

    val prijavljeniKorisnikId by sessionManager.korisnikId.collectAsState(initial = null)

    LaunchedEffect(refreshKey, prijavljeniKorisnikId) {
        prijavljeniKorisnikId?.let { id ->
            viewModel.ucitajProfil(id)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .padding(bottom = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Profil",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        ProfileAvatar(
            initials = buildString {
                append(korisnik?.ime?.firstOrNull() ?: 'K')
                append(korisnik?.prezime?.firstOrNull() ?: 'R')
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "${korisnik?.ime ?: ""} ${korisnik?.prezime ?: ""}".ifBlank { "Korisnik" },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        TextButton(
            onClick = {
                navController.navigate("uredi_profil")
            }
        ) {
            Text("uredi profil")
        }

        Spacer(modifier = Modifier.height(18.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Ciljevi",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            GoalCard(
                title = prikaziCilj(zadnjeMjerenje?.tipCilja),
                subtitle = if (zadnjaPreporuka != null) {
                    "${zadnjaPreporuka?.kalorije?.toInt()} kcal dnevno"
                } else {
                    "Nema preporuke"
                }
            )

            Spacer(modifier = Modifier.height(18.dp))

            ProfileMenuItem(
                title = "Postavke ciljeva",
                iconType = ProfileIconType.Goal,
                onClick = {
                    navController.navigate("postavke_ciljeva")
                }
            )

            ProfileMenuItem(
                title = "Moje mjere",
                iconType = ProfileIconType.Measurements,
                onClick = {
                    navController.navigate("moje_mjere")
                }
            )

            ProfileMenuItem(
                title = "Postavke aplikacije",
                iconType = ProfileIconType.Settings,
                onClick = {
                    navController.navigate("postavke_aplikacije")
                }
            )
            ProfileMenuItem(
                title = "Odjava",
                iconType = ProfileIconType.Logout,
                onClick = {
                    prikaziOdjavaDialog = true
                }
            )
        }
        if (prikaziOdjavaDialog) {
            AlertDialog(
                onDismissRequest = {
                    prikaziOdjavaDialog = false
                },
                title = {
                    Text("Odjava")
                },
                text = {
                    Text("Jeste li sigurni da se želite odjaviti?")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            prikaziOdjavaDialog = false

                            authViewModel.odjava()

                            navController.navigate("login") {
                                popUpTo(0)
                            }
                        }
                    ) {
                        Text("Odjava")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            prikaziOdjavaDialog = false
                        }
                    ) {
                        Text("Odustani")
                    }
                }
            )
        }
    }
}

@Composable
fun ProfileAvatar(
    initials: String
) {
    Surface(
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun GoalCard(
    title: String,
    subtitle: String
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

enum class ProfileIconType {
    Goal,
    Measurements,
    Settings,
    Logout
}

@Composable
fun ProfileMenuItem(
    title: String,
    iconType: ProfileIconType,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when (iconType) {
                            ProfileIconType.Goal -> Icons.Default.Flag
                            ProfileIconType.Measurements -> Icons.Default.Straighten
                            ProfileIconType.Settings -> Icons.Default.Settings
                            ProfileIconType.Logout -> Icons.AutoMirrored.Filled.ExitToApp
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun prikaziCilj(tipCilja: String?): String {
    return when (tipCilja) {
        "mrsavljenje" -> "Mršavljenje"
        "odrzavanje" -> "Održavanje težine"
        "dobivanje_mase" -> "Dobivanje mase"
        else -> "Cilj nije postavljen"
    }
}