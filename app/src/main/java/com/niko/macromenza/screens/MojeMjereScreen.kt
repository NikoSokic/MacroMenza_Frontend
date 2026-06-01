package com.niko.macromenza.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.niko.macromenza.model.Mjerenje
import com.niko.macromenza.viewmodel.MjereViewModel
import androidx.compose.ui.platform.LocalContext
import com.niko.macromenza.session.UserSessionManager

@Composable
fun MojeMjereScreen(
    navController: NavController,
    viewModel: MjereViewModel = viewModel()
) {
    val mjerenja by viewModel.mjerenja.collectAsState()
    val greska by viewModel.greska.collectAsState()

    val context = LocalContext.current

    val sessionManager = remember {
        UserSessionManager(context)
    }

    val prijavljeniKorisnikId by sessionManager.korisnikId.collectAsState(initial = null)

    LaunchedEffect(prijavljeniKorisnikId) {
        prijavljeniKorisnikId?.let { id ->
            viewModel.ucitajMjerenja(id)
        }
    }

    val zadnjeMjerenje = mjerenja.maxByOrNull { it.datum ?: "" }

    Column(
        modifier = Modifier
            .fillMaxSize()
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
                text = "Moje mjere",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Scale,
                    contentDescription = null,
                    modifier = Modifier.size(42.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Zadnja masa",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = if (zadnjeMjerenje != null)
                            "%.1f kg".format(zadnjeMjerenje.masa)
                        else
                            "Nema mjerenja",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }
        }

        if (greska != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Greška: $greska",
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Povijest mjerenja",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (mjerenja.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Još nema spremljenih mjerenja.",
                                style = MaterialTheme.typography.titleMedium
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Postavi cilj kako bi se spremilo prvo mjerenje.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(mjerenja.sortedByDescending { it.id ?: 0 }) { mjerenje ->
                    MjerenjeItem(mjerenje)
                }
            }
        }
    }
}

@Composable
fun MjerenjeItem(
    mjerenje: Mjerenje
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = mjerenje.datum ?: "Bez datuma",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text("Masa: %.1f kg".format(mjerenje.masa))
            Text("Aktivnost: ${mjerenje.razinaAktivnosti}")
            Text("Cilj: ${mjerenje.tipCilja}")
        }
    }
}