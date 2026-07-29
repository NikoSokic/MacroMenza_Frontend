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
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Straighten
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
import com.niko.macromenza.viewmodel.CiljeviViewModel

@Composable
fun PostavkeCiljevaScreen(
    navController: NavController,
    viewModel: CiljeviViewModel = viewModel()
) {
    val poruka by viewModel.poruka.collectAsState()

    val context = LocalContext.current

    val sessionManager = remember {
        UserSessionManager(context)
    }

    val prijavljeniKorisnikId by
    sessionManager.korisnikId.collectAsState(initial = null)

    var visina by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var masa by remember { mutableStateOf("") }

    var tipCilja by remember {
        mutableStateOf("odrzavanje")
    }

    var aktivnost by remember {
        mutableStateOf("umjerena")
    }

    var greska by remember {
        mutableStateOf<String?>(null)
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
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
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
                    color = MaterialTheme.colorScheme.primary.copy(
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
                        text = "Postavke ciljeva",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "Prilagodi dnevnu preporuku",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(28.dp)
            )

            // PODACI ZA PREPORUKU
            SettingsSectionCard(
                title = "Podaci za preporuku",
                icon = Icons.Default.Straighten
            ) {

                OutlinedTextField(
                    value = visina,
                    onValueChange = { noviUnos ->
                        if (noviUnos.all { it.isDigit() }) {
                            visina = noviUnos
                        }
                    },
                    label = {
                        Text("Visina (cm)")
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = goalFieldColors()
                )

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                OutlinedTextField(
                    value = dob,
                    onValueChange = { noviUnos ->
                        if (noviUnos.all { it.isDigit() }) {
                            dob = noviUnos
                        }
                    },
                    label = {
                        Text("Dob")
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = goalFieldColors()
                )

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                OutlinedTextField(
                    value = masa,
                    onValueChange = { noviUnos ->
                        if (
                            noviUnos.all {
                                it.isDigit() ||
                                        it == '.' ||
                                        it == ','
                            }
                        ) {
                            masa = noviUnos.replace(",", ".")
                        }
                    },
                    label = {
                        Text("Masa (kg)")
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = goalFieldColors()
                )
            }

            Spacer(
                modifier = Modifier.height(22.dp)
            )

            // CILJ
            Text(
                text = "Tvoj cilj",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = "Odaberi što želiš postići.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            GoalChoiceCard(
                title = "Mršavljenje",
                subtitle = "Postupno smanjenje tjelesne mase",
                selected = tipCilja == "mrsavljenje",
                onClick = {
                    tipCilja = "mrsavljenje"
                }
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            GoalChoiceCard(
                title = "Održavanje težine",
                subtitle = "Zadrži trenutnu tjelesnu masu",
                selected = tipCilja == "odrzavanje",
                onClick = {
                    tipCilja = "odrzavanje"
                }
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            GoalChoiceCard(
                title = "Dobivanje mase",
                subtitle = "Postupno povećanje tjelesne mase",
                selected = tipCilja == "dobivanje_mase",
                onClick = {
                    tipCilja = "dobivanje_mase"
                }
            )

            Spacer(
                modifier = Modifier.height(26.dp)
            )

            // AKTIVNOST
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(
                            RoundedCornerShape(13.dp)
                        )
                        .background(
                            MaterialTheme.colorScheme.primary.copy(
                                alpha = 0.18f
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = MacroGreen,
                        modifier = Modifier.size(21.dp)
                    )
                }

                Spacer(
                    modifier = Modifier.width(10.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "Razina aktivnosti",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "Koliko si aktivan tijekom tjedna?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                ActivityChoice(
                    title = "Sjedilačka",
                    selected =
                        aktivnost == "sjedilacka",
                    onClick = {
                        aktivnost = "sjedilacka"
                    },
                    modifier = Modifier.weight(1f)
                )

                ActivityChoice(
                    title = "Lagana",
                    selected =
                        aktivnost == "lagana",
                    onClick = {
                        aktivnost = "lagana"
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                ActivityChoice(
                    title = "Umjerena",
                    selected =
                        aktivnost == "umjerena",
                    onClick = {
                        aktivnost = "umjerena"
                    },
                    modifier = Modifier.weight(1f)
                )

                ActivityChoice(
                    title = "Visoka",
                    selected =
                        aktivnost == "visoka",
                    onClick = {
                        aktivnost = "visoka"
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            greska?.let {

                Spacer(
                    modifier = Modifier.height(18.dp)
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color =
                        MaterialTheme.colorScheme.error
                            .copy(alpha = 0.08f)
                ) {

                    Text(
                        text = it,
                        color =
                            MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(14.dp),
                        style =
                            MaterialTheme.typography.bodyMedium
                    )
                }
            }

            poruka?.let {

                Spacer(
                    modifier = Modifier.height(18.dp)
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color =
                        MaterialTheme.colorScheme.primary.copy(
                            alpha = 0.18f
                        )
                ) {

                    Text(
                        text = it,
                        color = MacroGreen,
                        modifier = Modifier.padding(14.dp),
                        style =
                            MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(28.dp)
            )

            Button(
                onClick = {

                    val visinaBroj =
                        visina.toIntOrNull()

                    val dobBroj =
                        dob.toIntOrNull()

                    val masaBroj =
                        masa.toDoubleOrNull()

                    if (
                        prijavljeniKorisnikId == null
                    ) {
                        greska =
                            "Korisnik nije prijavljen."
                        return@Button
                    }

                    if (
                        visinaBroj == null ||
                        visinaBroj !in 100..250
                    ) {
                        greska =
                            "Provjeri visinu. Vrijednost treba biti između 100 i 250 cm."
                        return@Button
                    }

                    if (
                        dobBroj == null ||
                        dobBroj !in 15..100
                    ) {
                        greska =
                            "Provjeri dob. Vrijednost treba biti između 15 i 100 godina."
                        return@Button
                    }

                    if (
                        masaBroj == null ||
                        masaBroj !in 30.0..300.0
                    ) {
                        greska =
                            "Provjeri masu. Vrijednost treba biti između 30 i 300 kg."
                        return@Button
                    }

                    greska = null

                    viewModel.spremiCilj(
                        idKorisnik =
                            prijavljeniKorisnikId!!,
                        visina = visinaBroj,
                        dob = dobBroj,
                        masa = masaBroj,
                        razinaAktivnosti =
                            aktivnost,
                        tipCilja =
                            tipCilja
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 58.dp),
                shape = RoundedCornerShape(18.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MacroGreen,
                        contentColor = Color.White
                    ),
                contentPadding = PaddingValues(
                    horizontal = 16.dp,
                    vertical = 14.dp
                )
            ) {

                Text(
                    text =
                        "Izračunaj novu preporuku",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {

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

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(
                            RoundedCornerShape(14.dp)
                        )
                        .background(
                            MaterialTheme.colorScheme.primary.copy(
                                alpha = 0.18f
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MacroGreen,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(
                    modifier = Modifier.width(12.dp)
                )

                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(
                modifier = Modifier.height(18.dp)
            )

            content()
        }
    }
}

@Composable
private fun GoalChoiceCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color =
            if (selected) {
                MaterialTheme.colorScheme.primary.copy(
                    alpha = 0.18f
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
                null
            },
        tonalElevation =
            if (selected) {
                0.dp
            } else {
                1.dp
            }
    ) {

        Row(
            modifier = Modifier.padding(
                horizontal = 14.dp,
                vertical = 14.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            RadioButton(
                selected = selected,
                onClick = onClick,
                colors =
                    RadioButtonDefaults.colors(
                        selectedColor = MacroGreen
                    )
            )

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    color =
                        if (selected) {
                            MacroGreen
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(
                    modifier = Modifier.height(2.dp)
                )

                Text(
                    text = subtitle,
                    style =
                        MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ActivityChoice(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(
            min = 58.dp
        ),
        shape = RoundedCornerShape(17.dp),
        color =
            if (selected) {
                MaterialTheme.colorScheme.primary.copy(
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
                null
            },
        tonalElevation =
            if (selected) {
                0.dp
            } else {
                1.dp
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
                fontWeight = FontWeight.SemiBold,
                color =
                    if (selected) {
                        MacroGreen
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun goalFieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MacroGreen,
        focusedLabelColor = MacroGreen,
        cursorColor = MacroGreen
    )