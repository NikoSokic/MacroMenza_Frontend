package com.niko.macromenza.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.niko.macromenza.viewmodel.JelaViewModel

@Composable
fun JelaScreen(viewModel: JelaViewModel = viewModel()) {
    val jela by viewModel.jela.collectAsState()
    val greska by viewModel.greska.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.ucitajJela()
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Popis jela", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        if (greska != null) {
            Text("Greška: $greska")
        }

        LazyColumn {
            items(jela) { jelo ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(jelo.naziv, style = MaterialTheme.typography.titleMedium)
                        Text("Kalorije: ${jelo.kalorije}")
                        Text("Proteini: ${jelo.proteini} g")
                        Text("UH: ${jelo.ugljikohidrati} g")
                        Text("Masti: ${jelo.masti} g")
                    }
                }
            }
        }
    }
}