package com.niko.macromenza.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.niko.macromenza.session.UserSessionManager
import com.niko.macromenza.ui.theme.MacroGreen
import com.niko.macromenza.ui.theme.MacroLightGreen
import com.niko.macromenza.ui.theme.MacroText
import com.niko.macromenza.ui.theme.MacroTextSecondary
import com.niko.macromenza.viewmodel.AuthViewModel
import com.niko.macromenza.viewmodel.AuthViewModelFactory
import com.niko.macromenza.viewmodel.ProfilViewModel

@Composable
fun ProfilScreen(
    navController: NavController,
    refreshKey: Int = 0,
    viewModel: ProfilViewModel = viewModel()
) {
    val korisnik by viewModel.korisnik.collectAsState()
    val zadnjeMjerenje by viewModel.zadnjeMjerenje.collectAsState()
    val zadnjaPreporuka by viewModel.zadnjaPreporuka.collectAsState()
    val ucitava by viewModel.ucitava.collectAsState()

    var prikaziOdjavaDialog by remember {
        mutableStateOf(false)
    }

    val context = LocalContext.current

    val sessionManager = remember {
        UserSessionManager(context)
    }

    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(
            UserSessionManager(context)
        )
    )

    val prijavljeniKorisnikId by
    sessionManager.korisnikId.collectAsState(initial = null)

    LaunchedEffect(
        refreshKey,
        prijavljeniKorisnikId
    ) {
        prijavljeniKorisnikId?.let { id ->
            viewModel.ucitajProfil(id)
        }
    }

    if (ucitava) {
        SplashScreen()
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        // Dekorativni krug
        Box(
            modifier = Modifier
                .size(230.dp)
                .offset(
                    x = 190.dp,
                    y = (-100).dp
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
                .padding(
                    horizontal = 24.dp
                )
                .padding(
                    top = 28.dp,
                    bottom = 110.dp
                )
        ) {

            Text(
                text = "Profil",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = MacroText
            )

            Text(
                text = "Tvoj račun i postavke",
                fontSize = 17.sp,
                color = MacroTextSecondary
            )

            Spacer(
                modifier = Modifier.height(28.dp)
            )

            // USER HEADER
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                ProfileAvatar(
                    initials = buildString {
                        append(
                            korisnik?.ime
                                ?.firstOrNull()
                                ?: 'K'
                        )

                        append(
                            korisnik?.prezime
                                ?.firstOrNull()
                                ?: 'R'
                        )
                    }
                )

                Spacer(
                    modifier = Modifier.width(16.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            "${korisnik?.ime ?: ""} ${korisnik?.prezime ?: ""}"
                                .ifBlank {
                                    "Korisnik"
                                },
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold,
                        color = MacroText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(
                        modifier = Modifier.height(5.dp)
                    )

                    Row(
                        modifier = Modifier
                            .clip(
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                navController.navigate(
                                    "uredi_profil"
                                )
                            }
                            .padding(
                                vertical = 5.dp,
                                horizontal = 2.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = MacroGreen,
                            modifier = Modifier.size(16.dp)
                        )

                        Spacer(
                            modifier = Modifier.width(6.dp)
                        )

                        Text(
                            text = "Uredi profil",
                            color = MacroGreen,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(32.dp)
            )

            Text(
                text = "Tvoj cilj",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MacroText
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            ProfileGoalCard(
                title = prikaziCilj(
                    zadnjeMjerenje?.tipCilja
                ),
                calories =
                    zadnjaPreporuka
                        ?.kalorije
                        ?.toInt()
            )

            Spacer(
                modifier = Modifier.height(30.dp)
            )

            Text(
                text = "Postavke",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MacroText
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            ProfileMenuItem(
                title = "Postavke ciljeva",
                subtitle = "Prilagodi svoj dnevni cilj",
                iconType = ProfileIconType.Goal,
                onClick = {
                    navController.navigate(
                        "postavke_ciljeva"
                    )
                }
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            ProfileMenuItem(
                title = "Moje mjere",
                subtitle = "Prati težinu i tjelesne podatke",
                iconType = ProfileIconType.Measurements,
                onClick = {
                    navController.navigate(
                        "moje_mjere"
                    )
                }
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            ProfileMenuItem(
                title = "Postavke aplikacije",
                subtitle = "Prilagodi MacroMenzu",
                iconType = ProfileIconType.Settings,
                onClick = {
                    navController.navigate(
                        "postavke_aplikacije"
                    )
                }
            )

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            LogoutItem(
                onClick = {
                    prikaziOdjavaDialog = true
                }
            )
        }
    }

    if (prikaziOdjavaDialog) {

        AlertDialog(
            onDismissRequest = {
                prikaziOdjavaDialog = false
            },
            shape = RoundedCornerShape(26.dp),

            title = {
                Text(
                    text = "Odjava",
                    fontWeight = FontWeight.Bold
                )
            },

            text = {
                Text(
                    text =
                        "Jesi li siguran da se želiš odjaviti iz MacroMenze?"
                )
            },

            confirmButton = {
                Button(
                    onClick = {
                        prikaziOdjavaDialog = false

                        authViewModel.odjava()

                        navController.navigate("login") {
                            popUpTo(0)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor =
                            MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Odjavi se")
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

@Composable
fun ProfileAvatar(
    initials: String
) {
    Surface(
        modifier = Modifier.size(80.dp),
        shape = CircleShape,
        color = MacroLightGreen.copy(
            alpha = 0.20f
        )
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials.uppercase(),
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                color = MacroGreen,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ProfileGoalCard(
    title: String,
    calories: Int?
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MacroLightGreen.copy(
            alpha = 0.16f
        )
    ) {

        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Surface(
                modifier = Modifier.size(50.dp),
                shape = RoundedCornerShape(16.dp),
                color = MacroLightGreen.copy(
                    alpha = 0.28f
                )
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        tint = MacroGreen,
                        modifier = Modifier.size(25.dp)
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
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MacroText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(
                    modifier = Modifier.height(3.dp)
                )

                Text(
                    text =
                        if (calories != null) {
                            "$calories kcal dnevno"
                        } else {
                            "Dnevna preporuka nije postavljena"
                        },
                    color = MacroTextSecondary,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

enum class ProfileIconType {
    Goal,
    Measurements,
    Settings
}

@Composable
fun ProfileMenuItem(
    title: String,
    subtitle: String,
    iconType: ProfileIconType,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(22.dp)
            )
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {

        Row(
            modifier = Modifier.padding(
                horizontal = 15.dp,
                vertical = 16.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Surface(
                modifier = Modifier.size(46.dp),
                shape = RoundedCornerShape(14.dp),
                color = MacroLightGreen.copy(
                    alpha = 0.18f
                )
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            when (iconType) {

                                ProfileIconType.Goal ->
                                    Icons.Default.Flag

                                ProfileIconType.Measurements ->
                                    Icons.Default.Straighten

                                ProfileIconType.Settings ->
                                    Icons.Default.Settings
                            },
                        contentDescription = null,
                        tint = MacroGreen,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(
                modifier = Modifier.width(13.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MacroText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(
                    modifier = Modifier.height(2.dp)
                )

                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = MacroTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            Icon(
                imageVector =
                    Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MacroTextSecondary,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@Composable
private fun LogoutItem(
    onClick: () -> Unit
) {
    val errorColor =
        MaterialTheme.colorScheme.error

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(22.dp)
            )
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(22.dp),
        color = errorColor.copy(
            alpha = 0.07f
        )
    ) {

        Row(
            modifier = Modifier.padding(
                horizontal = 15.dp,
                vertical = 16.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Surface(
                modifier = Modifier.size(46.dp),
                shape = RoundedCornerShape(14.dp),
                color = errorColor.copy(
                    alpha = 0.10f
                )
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector =
                            Icons.AutoMirrored
                                .Filled
                                .ExitToApp,
                        contentDescription = null,
                        tint = errorColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(
                modifier = Modifier.width(13.dp)
            )

            Text(
                text = "Odjavi se",
                modifier = Modifier.weight(1f),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = errorColor,
                maxLines = 1
            )

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            Icon(
                imageVector =
                    Icons.AutoMirrored
                        .Filled
                        .ArrowForwardIos,
                contentDescription = null,
                tint = errorColor.copy(
                    alpha = 0.7f
                ),
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

fun prikaziCilj(
    tipCilja: String?
): String {
    return when (tipCilja) {

        "mrsavljenje" ->
            "Mršavljenje"

        "odrzavanje" ->
            "Održavanje težine"

        "dobivanje_mase" ->
            "Dobivanje mase"

        else ->
            "Cilj nije postavljen"
    }
}