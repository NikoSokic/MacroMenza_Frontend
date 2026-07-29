    package com.niko.macromenza.screens
    
    import androidx.compose.foundation.background
    import androidx.compose.foundation.clickable
    import androidx.compose.foundation.layout.*
    import androidx.compose.foundation.shape.CircleShape
    import androidx.compose.foundation.shape.RoundedCornerShape
    import androidx.compose.material.icons.Icons
    import androidx.compose.material.icons.filled.ChevronLeft
    import androidx.compose.material.icons.filled.ChevronRight
    import androidx.compose.material.icons.filled.LocalFireDepartment
    import androidx.compose.material.icons.filled.Star
    import androidx.compose.material3.*
    import androidx.compose.runtime.*
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.draw.clip
    import androidx.compose.ui.graphics.Color
    import androidx.compose.ui.text.font.FontWeight
    import androidx.compose.ui.unit.dp
    import androidx.lifecycle.viewmodel.compose.viewModel
    import com.niko.macromenza.session.UserSessionManager
    import com.niko.macromenza.ui.theme.MacroGreen
    import com.niko.macromenza.ui.theme.MacroLightGreen
    import com.niko.macromenza.ui.theme.MacroText
    import com.niko.macromenza.ui.theme.MacroTextSecondary
    import com.niko.macromenza.viewmodel.PovijestViewModel
    import java.time.LocalDate
    import androidx.compose.ui.platform.LocalContext
    import com.niko.macromenza.model.StavkaObroka
    import androidx.compose.ui.text.style.TextAlign
    import androidx.compose.ui.text.style.TextOverflow
    import com.niko.macromenza.ui.components.screenBottomPadding
    import com.niko.macromenza.ui.components.screenTopPadding

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
        val sessionManager = remember { UserSessionManager(context) }
    
        val prijavljeniKorisnikId by
        sessionManager.korisnikId.collectAsState(initial = null)
    
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
    
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
    
            // Dekorativni krug
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .offset(x = 190.dp, y = (-100).dp)
                    .clip(CircleShape)
                    .background(MacroLightGreen.copy(alpha = 0.10f))
            )
    
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 22.dp,
                    end = 22.dp,
                    top = screenTopPadding(),
                    bottom = screenBottomPadding()
                ),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
    
                item {
                    Column {
                        Text(
                            text = "Povijest",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MacroText
                        )
    
                        Text(
                            text = "Tvoj napredak kroz vrijeme",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MacroTextSecondary
                        )
                    }
                }
    
                item {
                    PovijestTabSelector(
                        selected = odabraniTab,
                        onSelected = { tab ->
    
                            if (tab == PovijestTab.DNEVNI) {
                                odabraniDatum =
                                    tjedniPregled?.dani?.lastOrNull()?.datum
                            }
    
                            odabraniTab = tab
                        }
                    )
                }
    
                if (odabraniTab == PovijestTab.TJEDNI) {
    
                    tjedniPregled?.let { pregled ->
    
                        val imaUnosa =
                            pregled.dani.any { it.kalorije > 0 }
    
                        item {
                            WeekSelector(
                                datumOd = pregled.datumOd,
                                datumDo = pregled.datumDo
                            )
                        }
    
                        if (!imaUnosa) {
    
                            item {
                                EmptyHistoryCard()
                            }
    
                        } else {
    
                            val ukupnoKalorija =
                                pregled.dani.sumOf { it.kalorije }
    
                            val ukupnoProteina =
                                pregled.dani.sumOf { it.proteini }
    
                            val aktivniDani =
                                pregled.dani.count { it.kalorije > 0 }
    
                            item {
    
                                WeeklyOverviewCard(
                                    prosjekKalorija = pregled.prosjekKalorija,
                                    ukupnoKalorija = ukupnoKalorija,
                                    aktivniDani = aktivniDani
                                )
                            }
    
                            item {
    
                                Text(
                                    text = "Tjedni pregled",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
    
                            item {
    
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement =
                                        Arrangement.spacedBy(10.dp)
                                ) {
    
                                    HistoryStatCard(
                                        title = "Kalorije",
                                        value = "%.0f".format(
                                            pregled.prosjekKalorija
                                        ),
                                        unit = "prosjek",
                                        modifier = Modifier.weight(1f)
                                    )
    
                                    HistoryStatCard(
                                        title = "Proteini",
                                        value = "%.0f".format(
                                            ukupnoProteina / 7.0
                                        ),
                                        unit = "g / dan",
                                        modifier = Modifier.weight(1f)
                                    )
    
                                    HistoryStatCard(
                                        title = "Aktivnost",
                                        value = "$aktivniDani",
                                        unit = "/ 7 dana",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
    
                            item {
    
                                WeeklyChart(
                                    pregled = pregled,
                                    onDayClick = { datum ->
                                        odabraniDatum = datum
                                        odabraniTab = PovijestTab.DNEVNI
                                    }
                                )
                            }
    
                            item {
    
                                val najboljiDan =
                                    pregled.dani.maxByOrNull {
                                        it.kalorije
                                    }
    
                                val najviseProteina =
                                    pregled.dani.maxByOrNull {
                                        it.proteini
                                    }
    
                                Column {
    
                                    Text(
                                        text = "Istaknuto",
                                        style =
                                            MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
    
                                    Spacer(
                                        modifier = Modifier.height(12.dp)
                                    )
    
                                    HighlightCard(
                                        icon = {
                                            Icon(
                                                imageVector =
                                                    Icons.Default
                                                        .LocalFireDepartment,
                                                contentDescription = null,
                                                tint = MacroGreen
                                            )
                                        },
                                        title = "Najveći unos",
                                        value = najboljiDan?.let {
                                            "%.0f kcal".format(
                                                it.kalorije
                                            )
                                        } ?: "—",
                                        subtitle =
                                            najboljiDan?.datum ?: ""
                                    )
    
                                    Spacer(
                                        modifier = Modifier.height(10.dp)
                                    )
    
                                    HighlightCard(
                                        icon = {
                                            Icon(
                                                imageVector =
                                                    Icons.Default.Star,
                                                contentDescription = null,
                                                tint = MacroGreen
                                            )
                                        },
                                        title = "Najviše proteina",
                                        value = najviseProteina?.let {
                                            "%.0f g".format(
                                                it.proteini
                                            )
                                        } ?: "—",
                                        subtitle =
                                            najviseProteina?.datum ?: ""
                                    )
                                }
                            }
                        }
                    }
    
                } else {
    
                    item {
    
                        DaySelector(
                            datum = odabraniDatum,
                            onPrevious = {
    
                                odabraniDatum?.let {
                                    odabraniDatum =
                                        LocalDate.parse(it)
                                            .minusDays(1)
                                            .toString()
                                }
                            },
                            onNext = {
    
                                odabraniDatum?.let {
                                    odabraniDatum =
                                        LocalDate.parse(it)
                                            .plusDays(1)
                                            .toString()
                                }
                            }
                        )
                    }
    
                    povijestDana?.let { dan ->
    
                        val imaObroka =
                            dan.obroci.any { (_, stavke) ->
                                stavke.isNotEmpty()
                            }
    
                        if (!imaObroka) {
    
                            item {
                                EmptyDayCard()
                            }
    
                        } else {
    
                            item {
    
                                DailySummaryCard(
                                    kalorije = dan.ukupnoKalorije,
                                    proteini = dan.ukupnoProteini,
                                    uh = dan.ukupnoUgljikohidrati,
                                    masti = dan.ukupnoMasti
                                )
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
    }
    
    @Composable
    private fun PovijestTabSelector(
        selected: PovijestTab,
        onSelected: (PovijestTab) -> Unit
    ) {
    
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(18.dp),
            color = MacroLightGreen.copy(alpha = 0.12f)
        ) {
    
            Row(
                modifier = Modifier.padding(5.dp)
            ) {
    
                HistoryTab(
                    text = "Tjedan",
                    selected = selected == PovijestTab.TJEDNI,
                    onClick = {
                        onSelected(PovijestTab.TJEDNI)
                    },
                    modifier = Modifier.weight(1f)
                )
    
                HistoryTab(
                    text = "Dan",
                    selected = selected == PovijestTab.DNEVNI,
                    onClick = {
                        onSelected(PovijestTab.DNEVNI)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
    
    @Composable
    private fun HistoryTab(
        text: String,
        selected: Boolean,
        onClick: () -> Unit,
        modifier: Modifier
    ) {
    
        Surface(
            modifier = modifier
                .fillMaxHeight()
                .clickable { onClick() },
            shape = RoundedCornerShape(14.dp),
            color = if (selected)
                MacroGreen
            else
                Color.Transparent
        ) {
    
            Box(
                contentAlignment = Alignment.Center
            ) {
    
                Text(
                    text = text,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected)
                        Color.White
                    else
                        MacroTextSecondary
                )
            }
        }
    }

    @Composable
    private fun WeekSelector(
        datumOd: String,
        datumDo: String
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Surface(
                shape = CircleShape,
                color = MacroLightGreen.copy(alpha = 0.18f)
            ) {
                IconButton(
                    onClick = {}
                ) {
                    Icon(
                        Icons.Default.ChevronLeft,
                        contentDescription = "Prethodni tjedan",
                        tint = MacroGreen
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$datumOd  –  $datumDo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "Ovaj tjedan",
                    style = MaterialTheme.typography.bodySmall,
                    color = MacroTextSecondary
                )
            }

            Surface(
                shape = CircleShape,
                color = MacroLightGreen.copy(alpha = 0.18f)
            ) {
                IconButton(
                    onClick = {}
                ) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Sljedeći tjedan",
                        tint = MacroGreen
                    )
                }
            }
        }
    }
    @Composable
    private fun WeeklyOverviewCard(
        prosjekKalorija: Double,
        ukupnoKalorija: Double,
        aktivniDani: Int
    ) {
    
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MacroGreen
        ) {
    
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
    
                Text(
                    text = "Prosječni dnevni unos",
                    color = Color.White.copy(alpha = 0.80f),
                    style = MaterialTheme.typography.bodyMedium
                )
    
                Spacer(modifier = Modifier.height(4.dp))
    
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
    
                    Text(
                        text = "%.0f".format(prosjekKalorija),
                        color = Color.White,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
    
                    Spacer(modifier = Modifier.width(6.dp))
    
                    Text(
                        text = "kcal",
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 5.dp)
                    )
                }
    
                Spacer(modifier = Modifier.height(20.dp))
    
                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.20f)
                )
    
                Spacer(modifier = Modifier.height(16.dp))
    
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ) {
    
                    Column {
    
                        Text(
                            "Ukupno",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall
                        )
    
                        Text(
                            "%.0f kcal".format(ukupnoKalorija),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
    
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
    
                        Text(
                            "Evidentirano",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall
                        )
    
                        Text(
                            "$aktivniDani / 7 dana",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
    
    @Composable
    private fun HistoryStatCard(
        title: String,
        value: String,
        unit: String,
        modifier: Modifier
    ) {
    
        Surface(
            modifier = modifier.height(118.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
    
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.Center
            ) {
    
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MacroTextSecondary
                )
    
                Spacer(modifier = Modifier.height(6.dp))
    
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MacroText
                )
    
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodySmall,
                    color = MacroTextSecondary
                )
            }
        }
    }
    
    @Composable
    private fun WeeklyChart(
        pregled: com.niko.macromenza.model.TjedniPregled,
        onDayClick: (String) -> Unit
    ) {
    
        val dani =
            listOf("Pon", "Uto", "Sri", "Čet", "Pet", "Sub", "Ned")
    
        val maxKalorije =
            pregled.dani.maxOfOrNull {
                it.kalorije
            }?.coerceAtLeast(1.0) ?: 1.0
    
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
    
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
    
                Text(
                    text = "Kalorije po danima",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
    
                Text(
                    text = "Dnevni unos tijekom tjedna",
                    style = MaterialTheme.typography.bodySmall,
                    color = MacroTextSecondary
                )
    
                Spacer(modifier = Modifier.height(22.dp))
    
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    horizontalArrangement =
                        Arrangement.spacedBy(9.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
    
                    pregled.dani.forEachIndexed { index, dan ->
    
                        val progress =
                            (dan.kalorije / maxKalorije)
                                .toFloat()
                                .coerceIn(0f, 1f)
    
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable {
                                    onDayClick(dan.datum)
                                },
                            horizontalAlignment =
                                Alignment.CenterHorizontally,
                            verticalArrangement =
                                Arrangement.Bottom
                        ) {
    
                            if (dan.kalorije > 0) {
    
                                Text(
                                    text = "%.0f".format(dan.kalorije),
                                    style =
                                        MaterialTheme.typography.labelSmall,
                                    color = MacroTextSecondary
                                )
    
                                Spacer(
                                    modifier = Modifier.height(6.dp)
                                )
                            }
    
                            Box(
                                modifier = Modifier
                                    .width(30.dp)
                                    .height(
                                        if (dan.kalorije == 0.0)
                                            6.dp
                                        else
                                            (120 * progress)
                                                .coerceAtLeast(18f)
                                                .dp
                                    )
                                    .clip(
                                        RoundedCornerShape(12.dp)
                                    )
                                    .background(
                                        if (dan.kalorije > 0)
                                            MacroGreen
                                        else
                                            MacroLightGreen.copy(
                                                alpha = 0.25f
                                            )
                                    )
                            )
    
                            Spacer(
                                modifier = Modifier.height(8.dp)
                            )
    
                            Text(
                                text =
                                    dani.getOrElse(index) { "" },
                                style =
                                    MaterialTheme.typography.bodySmall,
                                color = MacroTextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
    
    @Composable
    private fun HighlightCard(
        icon: @Composable () -> Unit,
        title: String,
        value: String,
        subtitle: String
    ) {
    
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = MacroLightGreen.copy(alpha = 0.12f)
        ) {
    
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
    
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MacroLightGreen.copy(alpha = 0.25f)
                ) {
    
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        icon()
                    }
                }
    
                Spacer(modifier = Modifier.width(14.dp))
    
                Column(
                    modifier = Modifier.weight(1f)
                ) {
    
                    Text(
                        text = title,
                        fontWeight = FontWeight.SemiBold
                    )
    
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MacroTextSecondary
                    )
                }
    
                Text(
                    text = value,
                    color = MacroGreen,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
    
    @Composable
    private fun DaySelector(
        datum: String?,
        onPrevious: () -> Unit,
        onNext: () -> Unit
    ) {
    
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {
    
            IconButton(onClick = onPrevious) {
    
                Icon(
                    Icons.Default.ChevronLeft,
                    contentDescription = null
                )
            }
    
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
    
                Text(
                    text = datum ?: "Odaberi datum",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
    
                Text(
                    text = "Dnevni pregled",
                    style = MaterialTheme.typography.bodySmall,
                    color = MacroTextSecondary
                )
            }
    
            IconButton(onClick = onNext) {
    
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null
                )
            }
        }
    }
    
    @Composable
    private fun DailySummaryCard(
        kalorije: Double,
        proteini: Double,
        uh: Double,
        masti: Double
    ) {
    
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MacroGreen
        ) {
    
            Column(
                modifier = Modifier.padding(22.dp)
            ) {
    
                Text(
                    text = "Ukupni dnevni unos",
                    color = Color.White.copy(alpha = 0.8f)
                )
    
                Text(
                    text = "%.0f kcal".format(kalorije),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
    
                Spacer(modifier = Modifier.height(20.dp))
    
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ) {
    
                    DailyMacro("Proteini", proteini)
                    DailyMacro("UH", uh)
                    DailyMacro("Masti", masti)
                }
            }
        }
    }
    
    @Composable
    private fun DailyMacro(
        title: String,
        value: Double
    ) {
    
        Column {
    
            Text(
                text = "%.0f g".format(value),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
    
            Text(
                text = title,
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
    
    @Composable
    private fun EmptyHistoryCard() {
    
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MacroLightGreen.copy(alpha = 0.12f)
        ) {
    
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
    
                Text(
                    text = "Još nema aktivnosti",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
    
                Spacer(modifier = Modifier.height(4.dp))
    
                Text(
                    text = "Dodaj obroke kako bi ovdje mogao pratiti svoj tjedni napredak.",
                    color = MacroTextSecondary
                )
            }
        }
    }
    
    @Composable
    private fun EmptyDayCard() {
    
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MacroLightGreen.copy(alpha = 0.12f)
        ) {
    
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
    
                Text(
                    text = "Nema unosa za ovaj dan",
                    fontWeight = FontWeight.Bold
                )
    
                Spacer(modifier = Modifier.height(4.dp))
    
                Text(
                    text = "Za odabrani datum nije evidentiran nijedan obrok.",
                    color = MacroTextSecondary
                )
            }
        }
    }
    
    @Composable
    fun SekcijaObroka(
        naslov: String,
        stavke: List<StavkaObroka>
    ) {
        if (stavke.isEmpty()) return
    
        SekcijaObrokaDetalj(
            naslov = naslov,
            stavke = stavke
        )
    }