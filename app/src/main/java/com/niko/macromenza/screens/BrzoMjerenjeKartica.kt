package com.niko.macromenza.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niko.macromenza.ui.theme.MacroGreen
import com.niko.macromenza.util.filtrirajUnosMase
import com.niko.macromenza.util.formatMasa
import com.niko.macromenza.util.parsirajMasu
import com.niko.macromenza.viewmodel.PorukaMjerenja
import kotlin.math.roundToInt

/** Raspon mase u desetinkama kilograma (20–400 kg), isto kao CHECK u bazi. */
private val RASPON_DESETINKI = 200..4000
private const val ZADANE_DESETINKE = 700
private const val KASNJENJE_PONAVLJANJA_MS = 400L
private const val RAZMAK_PONAVLJANJA_MS = 80L

/**
 * Brzi unos današnje mase: ±0,1 kg (držanje ponavlja), tap na broj otvara ručni unos.
 * Masa se drži u desetinkama (Int), da ±0,1 ne nakuplja grešku Doublea.
 */
@Composable
fun BrzoMjerenjeKartica(
    zadnjaMasa: Double?,
    danasnjaMasa: Double?,
    sprema: Boolean,
    poruka: PorukaMjerenja?,
    onSpremi: (desetinke: Int) -> Unit
) {
    val pocetne = zadnjaMasa?.let { (it * 10).roundToInt() } ?: ZADANE_DESETINKE

    var desetinke by remember(pocetne) {
        mutableIntStateOf(pocetne.coerceIn(RASPON_DESETINKI))
    }

    var rucniUnos by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "Brzo mjerenje",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                KorakGumb(
                    tekst = "−0,1",
                    opis = "Smanji masu za 0,1 kg",
                    onKorak = {
                        desetinke = (desetinke - 1).coerceIn(RASPON_DESETINKI)
                    }
                )

                Text(
                    text = formatMasa(desetinke / 10.0),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClickLabel = "Upiši masu ručno") {
                            rucniUnos = true
                        }
                        .padding(vertical = 8.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )

                KorakGumb(
                    tekst = "+0,1",
                    opis = "Povećaj masu za 0,1 kg",
                    onKorak = {
                        desetinke = (desetinke + 1).coerceIn(RASPON_DESETINKI)
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onSpremi(desetinke) },
                enabled = !sprema,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MacroGreen,
                    contentColor = Color.White,
                    disabledContainerColor = MacroGreen.copy(alpha = 0.30f)
                )
            ) {
                if (sprema) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "Spremi",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (danasnjaMasa != null) {
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Danas već upisano: ${formatMasa(danasnjaMasa)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            poruka?.let {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = it.tekst,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (it.uspjeh) MacroGreen else MaterialTheme.colorScheme.error
                )
            }
        }
    }

    if (rucniUnos) {
        RucniUnosMaseDialog(
            pocetno = desetinke,
            onPotvrdi = {
                desetinke = it
                rucniUnos = false
            },
            onOdustani = { rucniUnos = false }
        )
    }
}

/** Okrugli gumb koji na tap napravi jedan korak, a dok se drži ponavlja ga. */
@Composable
private fun KorakGumb(
    tekst: String,
    opis: String,
    onKorak: () -> Unit
) {
    val trenutniKorak by rememberUpdatedState(onKorak)

    Box(
        modifier = Modifier
            .size(58.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
            .semantics {
                role = Role.Button
                contentDescription = opis
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    trenutniKorak()

                    // null = istekao rok, a prst je još dolje, pa se korak ponavlja.
                    val pusten = withTimeoutOrNull(KASNJENJE_PONAVLJANJA_MS) {
                        waitForUpOrCancellation()
                        true
                    }

                    if (pusten == null) {
                        do {
                            trenutniKorak()
                        } while (
                            withTimeoutOrNull(RAZMAK_PONAVLJANJA_MS) {
                                waitForUpOrCancellation()
                                true
                            } == null
                        )
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = tekst,
            color = MacroGreen,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun RucniUnosMaseDialog(
    pocetno: Int,
    onPotvrdi: (desetinke: Int) -> Unit,
    onOdustani: () -> Unit
) {
    // Tekst je na otvaranju označen, pa ga upisivanje odmah zamjenjuje.
    var polje by remember {
        val pocetniTekst = formatMasa(pocetno / 10.0).removeSuffix(" kg")
        mutableStateOf(TextFieldValue(pocetniTekst, selection = TextRange(0, pocetniTekst.length)))
    }
    val tekst = polje.text

    val fokus = remember { FocusRequester() }
    LaunchedEffect(Unit) { fokus.requestFocus() }

    val masa = parsirajMasu(tekst)
    val ispravno = masa != null && (masa * 10).roundToInt() in RASPON_DESETINKI

    AlertDialog(
        onDismissRequest = onOdustani,
        title = { Text("Današnja masa") },
        text = {
            OutlinedTextField(
                value = polje,
                onValueChange = { novo ->
                    if (filtrirajUnosMase(novo.text) != null) polje = novo
                },
                modifier = Modifier.focusRequester(fokus),
                label = { Text("Masa (kg)") },
                singleLine = true,
                isError = tekst.isNotEmpty() && !ispravno,
                supportingText = {
                    Text("Između 20 i 400 kg, najviše jedna decimala.")
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal
                ),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MacroGreen,
                    focusedLabelColor = MacroGreen,
                    cursorColor = MacroGreen
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = { masa?.let { onPotvrdi((it * 10).roundToInt()) } },
                enabled = ispravno
            ) {
                Text("U redu", color = if (ispravno) MacroGreen else Color.Unspecified)
            }
        },
        dismissButton = {
            TextButton(onClick = onOdustani) {
                Text("Odustani")
            }
        }
    )
}
