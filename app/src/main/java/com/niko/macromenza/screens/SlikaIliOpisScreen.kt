package com.niko.macromenza.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.niko.macromenza.model.PrijedlogObroka
import com.niko.macromenza.session.UserSessionManager
import com.niko.macromenza.ui.components.screenBottomPadding
import com.niko.macromenza.ui.components.screenTopPadding
import com.niko.macromenza.ui.theme.MacroGreen
import com.niko.macromenza.util.PrijedlogObrokaStore
import com.niko.macromenza.util.formatKcal
import com.niko.macromenza.util.formatKolicina
import com.niko.macromenza.util.ucitajPregled
import com.niko.macromenza.viewmodel.NAJVISE_ZNAKOVA_OPISA
import com.niko.macromenza.viewmodel.SlikaIliOpisViewModel
import java.time.LocalTime
import java.time.ZoneId

const val RUTA_SLIKA_ILI_OPIS = "slika_ili_opis"

private val ZAGREB = ZoneId.of("Europe/Zagreb")

@Composable
fun SlikaIliOpisScreen(
    navController: NavController,
    // Slika podijeljena iz druge aplikacije; ekran je preuzme i javi da je obrađena.
    dijeljenaSlika: Uri? = null,
    onDijeljenaPrimljena: () -> Unit = {},
    viewModel: SlikaIliOpisViewModel = viewModel()
) {
    val opis by viewModel.opis.collectAsState()
    val slika by viewModel.slika.collectAsState()
    val ucitava by viewModel.ucitava.collectAsState()
    val rezultat by viewModel.rezultat.collectAsState()
    val izvor by viewModel.izvor.collectAsState()
    val greska by viewModel.greska.collectAsState()

    val context = LocalContext.current
    val sessionManager = remember { UserSessionManager(context) }
    val korisnikId by sessionManager.korisnikId.collectAsState(initial = null)

    LaunchedEffect(dijeljenaSlika) {
        val uri = dijeljenaSlika ?: return@LaunchedEffect
        viewModel.postaviSliku(uri)
        onDijeljenaPrimljena()
    }

    var kameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val kamera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { uspjeh ->
        viewModel.kameraGotova(uspjeh, kameraUri)
        kameraUri = null
    }

    val galerija = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.postaviSliku(uri)
    }

    // Prvi dio: pregled je gotov (uspješno ili ne); drugi: sama slika.
    val pregled by produceState(initialValue = false to (null as ImageBitmap?), slika) {
        val uri = slika
        value = false to null
        value = true to uri?.let { ucitajPregled(context, it)?.asImageBitmap() }
    }

    val mozePrepoznati = !ucitava && korisnikId != null && (slika != null || opis.isNotBlank())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        Box(
            modifier = Modifier
                .size(230.dp)
                .offset(x = 190.dp, y = (-110).dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(top = screenTopPadding(), bottom = screenBottomPadding())
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.size(46.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Nazad",
                            tint = MacroGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Slika ili opis",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Prepoznaj obrok iz slike ili teksta",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val uri = viewModel.pripremiKameru()
                        kameraUri = uri
                        kamera.launch(uri)
                    },
                    enabled = !ucitava,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = MacroGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Fotografiraj", color = MaterialTheme.colorScheme.onSurface)
                }

                OutlinedButton(
                    onClick = {
                        galerija.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    enabled = !ucitava,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = MacroGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Iz galerije", color = MaterialTheme.colorScheme.onSurface)
                }
            }

            if (slika != null) {
                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 260.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    val (pregledGotov, bitmapa) = pregled
                    if (bitmapa != null) {
                        Image(
                            bitmap = bitmapa,
                            contentDescription = "Odabrana slika",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 260.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else if (pregledGotov) {
                        Text(
                            text = "Pregled slike nije dostupan",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        CircularProgressIndicator(color = MacroGreen, modifier = Modifier.size(28.dp))
                    }

                    Surface(
                        onClick = { viewModel.ukloniSliku() },
                        enabled = !ucitava,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(36.dp),
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.55f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Ukloni sliku",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            OutlinedTextField(
                value = opis,
                onValueChange = { viewModel.postaviOpis(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Opiši obrok") },
                placeholder = { Text("npr. pileći file s rižom i salata") },
                minLines = 3,
                maxLines = 6,
                enabled = !ucitava,
                shape = RoundedCornerShape(16.dp),
                supportingText = {
                    Text("${opis.length}/$NAJVISE_ZNAKOVA_OPISA")
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { korisnikId?.let { viewModel.prepoznaj(it) } },
                enabled = mozePrepoznati,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MacroGreen,
                    contentColor = Color.White
                )
            ) {
                if (ucitava) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Prepoznajem…", fontWeight = FontWeight.Bold)
                } else {
                    Text("Prepoznaj", fontWeight = FontWeight.Bold)
                }
            }

            greska?.let {
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            rezultat?.let { prijedlog ->
                Spacer(modifier = Modifier.height(20.dp))

                RezultatKartica(
                    prijedlog = prijedlog,
                    onKoristi = {
                        PrijedlogObrokaStore.postavi(prijedlog, izvor)

                        val prethodni = navController.previousBackStackEntry
                        val izUnosa = prethodni?.destination?.route?.startsWith("unos_obroka") == true &&
                            (prethodni.arguments?.getLong("idObrok", -1L) ?: -1L) <= 0L

                        if (izUnosa) {
                            // Unos obroka preuzima prijedlog čim se vrati u prvi plan.
                            navController.popBackStack()
                        } else {
                            val tip = zadaniTipZaVrijeme(LocalTime.now(ZAGREB))
                            navController.navigate("unos_obroka/$tip") {
                                popUpTo(RUTA_SLIKA_ILI_OPIS) { inclusive = true }
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Slika i opis šalju se Google Gemini servisu radi prepoznavanja. " +
                    "Prijedlog je okviran, provjeri ga prije spremanja.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RezultatKartica(
    prijedlog: PrijedlogObroka,
    onKoristi: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {

            Text(
                text = "Prijedlog obroka",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (prijedlog.stavke.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Nijedno jelo iz menze nije prepoznato.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            prijedlog.stavke.forEach { s ->
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = s.naziv,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "${formatKolicina(s.kolicina)} porc. · ${formatKcal(s.kalorije)} kcal",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (s.sigurnost != "VISOKA") {
                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = "provjeri",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            if (prijedlog.neprepoznato.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Neprepoznato (nije u bazi menze)",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                prijedlog.neprepoznato.forEach {
                    Text(
                        text = "• $it",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            prijedlog.napomena?.takeIf { it.isNotBlank() }?.let {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onKoristi,
                enabled = prijedlog.stavke.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MacroGreen,
                    contentColor = Color.White
                )
            ) {
                Text("Koristi prijedlog", fontWeight = FontWeight.Bold)
            }
        }
    }
}
