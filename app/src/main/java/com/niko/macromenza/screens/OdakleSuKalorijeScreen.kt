package com.niko.macromenza.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.niko.macromenza.api.RetrofitInstance
import com.niko.macromenza.model.IzvoriPodataka
import com.niko.macromenza.ui.components.screenBottomPadding
import com.niko.macromenza.ui.components.screenTopPadding
import com.niko.macromenza.ui.theme.MacroGreen

@Composable
fun OdakleSuKalorijeScreen(
    navController: NavController
) {
    var izvori by remember { mutableStateOf<IzvoriPodataka?>(null) }

    LaunchedEffect(Unit) {
        try {
            izvori = RetrofitInstance.api.getIzvoriPodataka()
        } catch (_: Exception) {
            // Brojevi su dodatak; ekran radi i bez njih.
        }
    }

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
                        text = "Odakle su kalorije",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Kako računamo vrijednosti jela",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            izvori?.let {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Broj(it.brojJela.toString(), "jela")
                        Broj(it.brojNamirnica.toString(), "namirnica")
                        Broj(it.brojMenija.toString(), "menija")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            Objasnjenje(
                naslov = "Recepture",
                tekst = "Jela se računaju iz službenih receptura menze (${izvori?.izvorNormativa ?: "NN 120/2013"}). " +
                    "Za svaki sastojak uzima se količina u gramima iz recepture, a vrijednost se računa iz " +
                    "nutritivnih vrijednosti namirnice na 100 g (USDA i EU tablice): " +
                    "vrijednost na 100 g × grami ÷ 100. Zbroj svih sastojaka je vrijednost jedne porcije."
            )

            Objasnjenje(
                naslov = "\"Ili\" i komadi",
                tekst = "Kad receptura nudi alternativu (\"ili\"), uzima se prva opcija. Za namirnice koje se " +
                    "mjere u komadima (kom) koristi se prosječna masa jednog komada u gramima."
            )

            Objasnjenje(
                naslov = "Meniji",
                tekst = "Meni je zbroj jela koja ga čine. Stavka označena s \"≈\" nema vlastitu recepturu, pa je " +
                    "zamijenjena sličnim jelom iz baze; ta se vrijednost ubraja u ukupni zbroj menija."
            )

            Objasnjenje(
                naslov = "Zašto su vrijednosti okvirne",
                tekst = "Receptura je normativ, a ne vaga: stvarna porcija može biti veća ili manja, " +
                    "količina ulja i način pripreme se razlikuju, a tablice namirnica su prosjeci. " +
                    "Zato brojevi pokazuju red veličine i trend, a ne laboratorijsku točnost."
            )
        }
    }
}

@Composable
private fun Broj(vrijednost: String, oznaka: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = vrijednost,
            color = MacroGreen,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp
        )
        Text(
            text = oznaka,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun Objasnjenje(naslov: String, tekst: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = naslov,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = tekst,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
