package com.niko.macromenza.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.niko.macromenza.screens.slikailiopis.PregledSlike
import com.niko.macromenza.screens.slikailiopis.SlikaIliOpisAkcije
import com.niko.macromenza.screens.slikailiopis.SlikaIliOpisSadrzaj
import com.niko.macromenza.screens.slikailiopis.SlikaIliOpisUi
import com.niko.macromenza.session.UserSessionManager
import com.niko.macromenza.ui.theme.UlazUSesiji
import com.niko.macromenza.util.PrijedlogObrokaStore
import com.niko.macromenza.util.ucitajPregled
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

    // Puni ulazni slijed samo pri prvom otvaranju u sesiji; poslije samo prijelaz ekrana.
    val puniUlaz = remember { UlazUSesiji.jePrvi(UlazUSesiji.SLIKA_ILI_OPIS) }
    SideEffect { UlazUSesiji.oznaci(UlazUSesiji.SLIKA_ILI_OPIS) }

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

    val ui = SlikaIliOpisUi(
        opis = opis,
        pregled = when {
            slika == null -> PregledSlike.Nema
            !pregled.first -> PregledSlike.Ucitava
            else -> PregledSlike.Gotov(pregled.second)
        },
        ucitava = ucitava,
        greska = greska,
        rezultat = rezultat,
        korisnikSpreman = korisnikId != null
    )

    SlikaIliOpisSadrzaj(
        ui = ui,
        puniUlaz = puniUlaz,
        akcije = SlikaIliOpisAkcije(
            onNatrag = { navController.popBackStack() },
            onFotografiraj = {
                val uri = viewModel.pripremiKameru()
                kameraUri = uri
                kamera.launch(uri)
            },
            onGalerija = {
                galerija.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onUkloniSliku = { viewModel.ukloniSliku() },
            onOpis = { viewModel.postaviOpis(it) },
            onPrepoznaj = { korisnikId?.let { viewModel.prepoznaj(it) } },
            onKoristiPrijedlog = koristi@{
                val prijedlog = rezultat ?: return@koristi
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
    )
}
