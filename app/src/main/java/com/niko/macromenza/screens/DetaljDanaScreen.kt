package com.niko.macromenza.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.niko.macromenza.model.StavkaObroka
import com.niko.macromenza.viewmodel.PovijestViewModel
import androidx.compose.ui.platform.LocalContext
import com.niko.macromenza.session.UserSessionManager
@Composable
fun DetaljDanaScreen(
    datum: String,
    viewModel: PovijestViewModel = viewModel()
) {
    val povijestDana by viewModel.povijestDana.collectAsState()

    val context = LocalContext.current

    val sessionManager = remember {
        UserSessionManager(context)
    }

    val prijavljeniKorisnikId by sessionManager.korisnikId.collectAsState(initial = null)

    LaunchedEffect(datum, prijavljeniKorisnikId) {
        prijavljeniKorisnikId?.let { id ->
            viewModel.ucitajDan(id, datum)
        }
    }


    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = datum,
                style = MaterialTheme.typography.headlineMedium
            )
        }

        povijestDana?.let { dan ->
            item {
                Text("Ukupno: ${dan.ukupnoKalorije} kcal")
            }

            dan.obroci.forEach { (tip, stavke) ->
                item {
                    SekcijaObroka(
                        naslov = tip,
                        stavke = stavke
                    )
                }
            }
        }
    }
}

@Composable
fun SekcijaObroka(
    naslov: String,
    stavke: List<StavkaObroka>
) {
    if (stavke.isEmpty()) return

    Column {
        Text(
            text = naslov,
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        stavke.forEach { stavka ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp)
                ) {
                    Text(
                        stavka.nazivJela,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        "${stavka.kalorije} kcal | " +
                                "P ${stavka.proteini}g | " +
                                "UH ${stavka.ugljikohidrati}g | " +
                                "M ${stavka.masti}g"
                    )
                }
            }
        }
    }
}