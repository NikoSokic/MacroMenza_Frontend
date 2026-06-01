package com.niko.macromenza.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.niko.macromenza.viewmodel.PovijestViewModel
import java.time.LocalDate
import androidx.compose.ui.platform.LocalContext
import com.niko.macromenza.session.UserSessionManager

enum class PovijestTab {
    TJEDNI,
    DNEVNI
}

@Composable
fun PovijestScreen(
    viewModel: PovijestViewModel = viewModel()
) {
    val tjedniPregled by viewModel.tjedniPregled.collectAsState()
    val povijestDana by viewModel.povijestDana.collectAsState()

    var odabraniTab by remember { mutableStateOf(PovijestTab.TJEDNI) }
    var odabraniDatum by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    val sessionManager = remember {
        UserSessionManager(context)
    }

    val prijavljeniKorisnikId by sessionManager.korisnikId.collectAsState(initial = null)

    LaunchedEffect(prijavljeniKorisnikId) {
        prijavljeniKorisnikId?.let { id ->
            viewModel.ucitajTjedniPregled(id)
        }
    }

    LaunchedEffect(odabraniDatum) {
        odabraniDatum?.let { datum ->
            prijavljeniKorisnikId?.let { id ->
                viewModel.ucitajDan(id, datum)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Povijest", style = MaterialTheme.typography.headlineMedium)
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = odabraniTab == PovijestTab.TJEDNI,
                    onClick = { odabraniTab = PovijestTab.TJEDNI },
                    label = { Text("Tjedni pregled") },
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = odabraniTab == PovijestTab.DNEVNI,
                    onClick = {
                        odabraniDatum = tjedniPregled?.dani?.lastOrNull()?.datum
                        odabraniTab = PovijestTab.DNEVNI
                    },
                    label = { Text("Dnevni pregled") },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (odabraniTab == PovijestTab.TJEDNI) {
            tjedniPregled?.let { pregled ->
                val imaTjednihUnosa = pregled.dani.any { it.kalorije > 0 }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = {}) {
                            Icon(
                                Icons.Default.ChevronLeft,
                                contentDescription = "Prethodni tjedan"
                            )
                        }

                        Column {
                            Text(
                                text = "${pregled.datumOd} - ${pregled.datumDo}",
                                style = MaterialTheme.typography.titleLarge
                            )

                            Text(
                                text = "Pritisni datum za odabir tjedna",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        IconButton(onClick = {}) {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = "Sljedeći tjedan"
                            )
                        }
                    }
                }

                if (!imaTjednihUnosa) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Nema unosa za ovaj tjedan.",
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "Dodaj obrok kako bi se prikazao tjedni pregled.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SummaryCard(
                                naslov = "Prosjek kalorija",
                                vrijednost = "%.0f".format(pregled.prosjekKalorija),
                                jedinica = "kcal",
                                modifier = Modifier.weight(1f)
                            )

                            SummaryCard(
                                naslov = "Prosjek proteina",
                                vrijednost = "%.0f".format(pregled.ukupnoProteini / 7.0),
                                jedinica = "g",
                                modifier = Modifier.weight(1f)
                            )

                            SummaryCard(
                                naslov = "Uspješnost",
                                vrijednost = "85",
                                jedinica = "%",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Kalorije po danima",
                                    style = MaterialTheme.typography.titleLarge
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                val daniUTjednu =
                                    listOf("Pon", "Uto", "Sri", "Čet", "Pet", "Sub", "Ned")
                                val maxKalorije =
                                    pregled.dani.maxOfOrNull { it.kalorije } ?: 1.0

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(210.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    pregled.dani.forEachIndexed { index, dan ->
                                        val visina = if (maxKalorije > 0) {
                                            (dan.kalorije / maxKalorije).toFloat()
                                        } else {
                                            0f
                                        }

                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    odabraniDatum = dan.datum
                                                    odabraniTab = PovijestTab.DNEVNI
                                                },
                                            verticalArrangement = Arrangement.Bottom
                                        ) {
                                            Text(
                                                text = "%.0f".format(dan.kalorije),
                                                style = MaterialTheme.typography.bodySmall
                                            )

                                            Spacer(modifier = Modifier.height(6.dp))

                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .fillMaxHeight(visina.coerceAtLeast(0.05f)),
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = MaterialTheme.shapes.medium
                                            ) {}

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Text(
                                                text = daniUTjednu.getOrElse(index) { "" },
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        val najboljiDan = pregled.dani.maxByOrNull { it.kalorije }
                        val najviseProteina = pregled.dani.maxByOrNull { it.proteini }

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Najbolji dan",
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Text(
                                    text = if (najboljiDan != null)
                                        "${najboljiDan.datum} – %.0f kcal"
                                            .format(najboljiDan.kalorije)
                                    else
                                        "Nema podataka"
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "Najveći unos proteina",
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Text(
                                    text = if (najviseProteina != null)
                                        "${najviseProteina.datum} – %.0f g"
                                            .format(najviseProteina.proteini)
                                    else
                                        "Nema podataka"
                                )
                            }
                        }
                    }
                }
            }
        } else {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = {
                            odabraniDatum?.let {
                                odabraniDatum = LocalDate.parse(it).minusDays(1).toString()
                            }
                        }
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Prethodni dan")
                    }

                    Text(
                        text = odabraniDatum ?: "Nije odabran datum",
                        style = MaterialTheme.typography.titleMedium
                    )

                    IconButton(
                        onClick = {
                            odabraniDatum?.let {
                                odabraniDatum = LocalDate.parse(it).plusDays(1).toString()
                            }
                        }
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Sljedeći dan")
                    }
                }
            }

            povijestDana?.let { dan ->
                val imaObroka = dan.obroci.any { (_, stavke) ->
                    stavke.isNotEmpty()
                }

                if (!imaObroka) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Nema podataka za ovaj dan.",
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "Dodaj obrok kroz Konzumacija tab.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Ukupno", style = MaterialTheme.typography.titleLarge)
                                Text("%.0f kcal".format(dan.ukupnoKalorije))
                                Text("P: %.0f g".format(dan.ukupnoProteini))
                                Text("UH: %.0f g".format(dan.ukupnoUgljikohidrati))
                                Text("M: %.0f g".format(dan.ukupnoMasti))
                            }
                        }
                    }

                    dan.obroci.forEach { (tip, stavke) ->
                        if (stavke.isNotEmpty()) {
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
        }
    }
}

@Composable
fun SummaryCard(
    naslov: String,
    vrijednost: String,
    jedinica: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(115.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = naslov,
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = vrijednost,
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = jedinica,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}