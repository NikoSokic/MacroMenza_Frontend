package com.niko.macromenza.screens.profil

import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.niko.macromenza.R
import com.niko.macromenza.model.Korisnik
import com.niko.macromenza.model.Mjerenje
import com.niko.macromenza.model.Preporuka
import com.niko.macromenza.ui.components.DijalogPotvrde
import com.niko.macromenza.ui.components.Makro
import com.niko.macromenza.ui.components.TablicniBroj
import com.niko.macromenza.ui.components.makroKratko
import com.niko.macromenza.ui.components.screenBottomPadding
import com.niko.macromenza.ui.components.screenTopPadding
import com.niko.macromenza.ui.components.skalaPritiska
import com.niko.macromenza.ui.theme.Kasnjenje
import com.niko.macromenza.ui.theme.MacroDimenzije
import com.niko.macromenza.ui.theme.MacroMenzaTheme
import com.niko.macromenza.ui.theme.MacroOblici
import com.niko.macromenza.ui.theme.MacroOmjeri
import com.niko.macromenza.ui.theme.MacroRazmaci
import com.niko.macromenza.ui.theme.Opruge
import com.niko.macromenza.ui.theme.POMAK_ULAZA_DP
import com.niko.macromenza.ui.theme.Trajanje
import com.niko.macromenza.ui.theme.Ublazavanje
import com.niko.macromenza.ui.theme.macroBoje
import com.niko.macromenza.ui.theme.macroTipografija
import com.niko.macromenza.ui.theme.opruga
import com.niko.macromenza.ui.theme.rememberSmanjeniPokret
import com.niko.macromenza.ui.theme.ulazniTween
import com.niko.macromenza.util.formatKcal
import com.niko.macromenza.util.formatMakro
import com.niko.macromenza.util.formatMasa
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

/** Nakon ovoliko ms od dolaska sadržaja ulazni slijed je odigran (cijeli slijed traje 890 ms). */
private const val TRAJANJE_ULAZA = 1000L

/** Redoslijed u ulaznom slijedu: zadnji (odjava) kreće na 140 + 8 × 50 = 540 ms i završava na 860 ms. */
private const val ULAZ_NASLOV = 0
private const val ULAZ_ZAGLAVLJE = 1
private const val ULAZ_CILJ = 2
private const val ULAZ_POSTAVKE = 3
private const val ULAZ_PRVI_RED = 4
private const val ULAZ_ODJAVA = 8

/** Podaci glavnog Profila; ime i prezime su nullable jer ih Gson može ostaviti praznima. */
@Immutable
data class ProfilUi(
    val ime: String?,
    val prezime: String?,
    val tipCilja: String?,
    val preporuka: Makro?,
    val masa: Double?
)

sealed interface ProfilStanje {
    data object Ucitavanje : ProfilStanje
    data object Greska : ProfilStanje
    data class Sadrzaj(val profil: ProfilUi) : ProfilStanje
}

/** Greška je samo kad korisnik nije stigao; dok je korisnik učitan, osvježavanje ne skriva sadržaj. */
fun profilStanje(
    korisnik: Korisnik?,
    zadnjeMjerenje: Mjerenje?,
    zadnjaPreporuka: Preporuka?,
    poruka: String?
): ProfilStanje = when {
    korisnik != null -> ProfilStanje.Sadrzaj(
        ProfilUi(
            ime = korisnik.ime,
            prezime = korisnik.prezime,
            tipCilja = zadnjeMjerenje?.tipCilja,
            preporuka = zadnjaPreporuka?.let { Makro(it.kalorije, it.proteini, it.masti, it.ugljikohidrati) },
            masa = zadnjeMjerenje?.masa
        )
    )
    poruka != null -> ProfilStanje.Greska
    else -> ProfilStanje.Ucitavanje
}

/** Prvo slovo imena i prezimena; null kad nema ni jednog (tada se crta ikona osobe). */
fun inicijali(ime: String?, prezime: String?): String? =
    listOfNotNull(ime?.trim()?.firstOrNull(), prezime?.trim()?.firstOrNull())
        .joinToString("")
        .uppercase()
        .ifEmpty { null }

@Composable
fun nazivCilja(tipCilja: String?): String? = when (tipCilja) {
    "mrsavljenje" -> stringResource(R.string.prof_cilj_mrsavljenje)
    "odrzavanje" -> stringResource(R.string.prof_cilj_odrzavanje)
    "dobivanje_mase" -> stringResource(R.string.prof_cilj_dobivanje_mase)
    else -> null
}

/** Radnje ekrana; ProfilScreen ih spaja na ViewModel i navigaciju, previewi ostavljaju prazne. */
@Immutable
class ProfilAkcije(
    val onUrediProfil: () -> Unit = {},
    val onPostavkeCiljeva: () -> Unit = {},
    val onMojeMjere: () -> Unit = {},
    val onOdakleSuKalorije: () -> Unit = {},
    val onPostavkeAplikacije: () -> Unit = {},
    val onPonovi: () -> Unit = {},
    /** Potvrđena odjava (dijalog je u sadržaju). */
    val onOdjava: () -> Unit = {}
)

/**
 * Tab "Profil" bez ViewModela (design-system/macromenza/pages/profil.md).
 * [puniUlaz] je istina samo pri prvom otvaranju u sesiji: zaglavlje, cilj i redovi ulaze stupnjevano.
 */
@OptIn(ExperimentalAnimationApi::class) // Transition.Crossfade s contentKey
@Composable
fun ProfilSadrzaj(
    stanje: ProfilStanje,
    akcije: ProfilAkcije,
    modifier: Modifier = Modifier,
    puniUlaz: Boolean = true
) {
    val smanjeno = rememberSmanjeniPokret()
    val pregledAlata = LocalInspectionMode.current

    // Odbrojava se od dolaska sadržaja, pa se slijed odigra i kad profil stigne nakon kostura.
    val imaSadrzaj = stanje is ProfilStanje.Sadrzaj
    var ulazOdigran by rememberSaveable { mutableStateOf(false) }
    val animirajUlaz = puniUlaz && !ulazOdigran && !smanjeno && !pregledAlata
    LaunchedEffect(imaSadrzaj) {
        if (imaSadrzaj) {
            delay(TRAJANJE_ULAZA)
            ulazOdigran = true
        }
    }

    var prikaziOdjavu by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MacroRazmaci.rubEkrana)
                .padding(top = screenTopPadding(), bottom = screenBottomPadding())
        ) {
            Text(
                text = stringResource(R.string.prof_naslov),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .semantics { heading() }
                    .ulaz(ULAZ_NASLOV, animirajUlaz)
            )

            // Ključ je vrsta stanja: osvježavanje istog sadržaja ne pokreće prijelaz.
            updateTransition(targetState = stanje, label = "stanjeProfila").Crossfade(
                contentKey = { it::class },
                animationSpec = if (smanjeno) snap() else tween(Trajanje.kratko)
            ) { s ->
                Column {
                    when (s) {
                        ProfilStanje.Ucitavanje -> Kostur(smanjeno)
                        ProfilStanje.Greska -> Greska(akcije.onPonovi)
                        is ProfilStanje.Sadrzaj -> {
                            ZaglavljeKorisnika(
                                profil = s.profil,
                                onUrediProfil = akcije.onUrediProfil,
                                modifier = Modifier.ulaz(ULAZ_ZAGLAVLJE, animirajUlaz)
                            )
                            TvojCilj(
                                profil = s.profil,
                                animirajUlaz = animirajUlaz,
                                smanjeno = smanjeno,
                                onPostaviCilj = akcije.onPostavkeCiljeva,
                                modifier = Modifier.ulaz(ULAZ_CILJ, animirajUlaz)
                            )
                            Postavke(akcije, animirajUlaz)
                        }
                    }

                    if (s != ProfilStanje.Ucitavanje) {
                        Spacer(Modifier.height(MacroRazmaci.iznadSekcije))
                        RedPostavke(
                            ikona = Icons.AutoMirrored.Rounded.Logout,
                            naslov = stringResource(R.string.prof_odjava),
                            onClick = { prikaziOdjavu = true },
                            opasno = true,
                            modifier = Modifier.ulaz(ULAZ_ODJAVA, animirajUlaz)
                        )
                    }
                }
            }
        }
    }

    if (prikaziOdjavu) {
        DijalogPotvrde(
            naslov = stringResource(R.string.prof_odjava_naslov),
            tekst = stringResource(R.string.prof_odjava_tekst),
            oznakaPotvrde = stringResource(R.string.prof_odjava),
            destruktivna = true,
            onPotvrdi = {
                prikaziOdjavu = false
                akcije.onOdjava()
            },
            onOdustani = { prikaziOdjavu = false }
        )
    }
}

@Composable
private fun ZaglavljeKorisnika(
    profil: ProfilUi,
    onUrediProfil: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shema = MaterialTheme.colorScheme
    val slova = inicijali(profil.ime, profil.prezime)
    val punoIme = listOfNotNull(profil.ime?.trim(), profil.prezime?.trim())
        .filter { it.isNotEmpty() }
        .joinToString(" ")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = MacroRazmaci.xl),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(MacroDimenzije.avatarProfila)
                .background(shema.surface, CircleShape)
                .border(MacroDimenzije.linija, shema.outlineVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (slova != null) {
                Text(
                    text = slova,
                    style = MaterialTheme.typography.headlineSmall,
                    color = shema.onSurface,
                    maxLines = 1,
                    modifier = Modifier.clearAndSetSemantics { }
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    tint = shema.onSurfaceVariant,
                    modifier = Modifier.size(MacroDimenzije.ikona)
                )
            }
        }

        Spacer(Modifier.width(MacroRazmaci.l))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = punoIme.ifEmpty { stringResource(R.string.prof_bez_imena) },
                style = MaterialTheme.typography.titleMedium,
                color = if (punoIme.isEmpty()) shema.onSurfaceVariant else shema.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Link(
                tekst = stringResource(R.string.prof_uredi_profil),
                ikona = Icons.Rounded.Edit,
                onClick = onUrediProfil
            )
        }
    }
}

@Composable
private fun TvojCilj(
    profil: ProfilUi,
    animirajUlaz: Boolean,
    smanjeno: Boolean,
    onPostaviCilj: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shema = MaterialTheme.colorScheme
    val tip = MaterialTheme.typography
    val preporuka = profil.preporuka

    Column(modifier = modifier.fillMaxWidth()) {
        NaslovSekcije(stringResource(R.string.prof_tvoj_cilj))

        nazivCilja(profil.tipCilja)?.let { naziv ->
            Text(
                text = naziv,
                style = tip.bodyLarge,
                color = shema.onSurface,
                modifier = Modifier.padding(bottom = MacroRazmaci.s)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MacroRazmaci.l)
        ) {
            if (preporuka != null) {
                val kcal by animiranaVrijednost(
                    cilj = preporuka.kcal.toFloat(),
                    animirajUlaz = animirajUlaz,
                    kasnjenje = Kasnjenje.nakonPrijelaza + ULAZ_CILJ * Kasnjenje.korak,
                    smanjeno = smanjeno
                )
                val opis = stringResource(R.string.prof_cilj_opis, formatKcal(preporuka.kcal))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clearAndSetSemantics { contentDescription = opis }
                ) {
                    TablicniBroj(
                        tekst = { kcal.roundToInt().toString() },
                        stil = MaterialTheme.macroTipografija.brojSrednji,
                        boja = shema.onSurface
                    )
                    Text(
                        text = stringResource(R.string.prof_kcal_dnevno),
                        style = tip.labelMedium,
                        color = shema.onSurfaceVariant
                    )
                }
            } else {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.prof_cilj_nije_postavljen),
                        style = tip.bodyMedium,
                        color = shema.onSurface
                    )
                    Link(
                        tekst = stringResource(R.string.prof_postavi_cilj),
                        onClick = onPostaviCilj
                    )
                }
            }

            profil.masa?.let { masa ->
                val opis = stringResource(R.string.prof_masa_opis, formatMasa(masa))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clearAndSetSemantics { contentDescription = opis }
                ) {
                    TablicniBroj(
                        // Isti zapis kao formatMasa, bez jedinice (jedinica je u oznaci ispod).
                        tekst = { formatMakro(masa) },
                        stil = MaterialTheme.macroTipografija.brojSrednji,
                        boja = shema.onSurface
                    )
                    Text(
                        text = stringResource(R.string.prof_kg_zadnja_masa),
                        style = tip.labelMedium,
                        color = shema.onSurfaceVariant
                    )
                }
            } ?: Spacer(Modifier.weight(1f))
        }

        if (preporuka != null) {
            Text(
                text = stringResource(R.string.prof_preporuka, makroKratko(preporuka)),
                style = tip.bodySmall,
                color = shema.onSurfaceVariant,
                modifier = Modifier.padding(top = MacroRazmaci.m)
            )
        }
    }
}

@Composable
private fun Postavke(akcije: ProfilAkcije, animirajUlaz: Boolean) {
    val redovi = listOf(
        Triple(Icons.Rounded.Flag, R.string.prof_postavke_ciljeva to R.string.prof_postavke_ciljeva_opis, akcije.onPostavkeCiljeva),
        Triple(Icons.Rounded.MonitorWeight, R.string.prof_moje_mjere to R.string.prof_moje_mjere_opis, akcije.onMojeMjere),
        Triple(Icons.Rounded.Info, R.string.prof_odakle to R.string.prof_odakle_opis, akcije.onOdakleSuKalorije),
        Triple(Icons.Rounded.Settings, R.string.prof_postavke_aplikacije to R.string.prof_postavke_aplikacije_opis, akcije.onPostavkeAplikacije)
    )

    NaslovSekcije(
        tekst = stringResource(R.string.prof_postavke),
        modifier = Modifier.ulaz(ULAZ_POSTAVKE, animirajUlaz)
    )
    redovi.forEachIndexed { i, (ikona, tekstovi, onClick) ->
        Column(modifier = Modifier.ulaz(ULAZ_PRVI_RED + i, animirajUlaz)) {
            if (i > 0) {
                HorizontalDivider(thickness = MacroDimenzije.linija, color = MaterialTheme.colorScheme.outlineVariant)
            }
            RedPostavke(
                ikona = ikona,
                naslov = stringResource(tekstovi.first),
                opis = stringResource(tekstovi.second),
                onClick = onClick
            )
        }
    }
}

@Composable
private fun NaslovSekcije(tekst: String, modifier: Modifier = Modifier) {
    Text(
        text = tekst,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier
            .padding(top = MacroRazmaci.iznadSekcije, bottom = MacroRazmaci.ispodNaslovaSekcije)
            .semantics { heading() }
    )
}

/** Red postavke (pages/profil.md § Zajednički elementi); [opasno] je odjava: ikona i tekst u error, bez ›. */
@Composable
private fun RedPostavke(
    ikona: ImageVector,
    naslov: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    opis: String? = null,
    opasno: Boolean = false
) {
    val shema = MaterialTheme.colorScheme
    val izvor = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MacroDimenzije.dodirniCilj)
            .skalaPritiska(izvor)
            .clickable(interactionSource = izvor, indication = null, role = Role.Button, onClick = onClick)
            .padding(vertical = MacroRazmaci.m),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = ikona,
            contentDescription = null,
            tint = if (opasno) shema.error else shema.onSurfaceVariant,
            modifier = Modifier.size(MacroDimenzije.ikona)
        )
        Spacer(Modifier.width(MacroRazmaci.l))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = naslov,
                style = MaterialTheme.typography.titleSmall,
                color = if (opasno) shema.error else shema.onSurface
            )
            opis?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = shema.onSurfaceVariant
                )
            }
        }
        if (!opasno) {
            Spacer(Modifier.width(MacroRazmaci.s))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = shema.onSurfaceVariant,
                modifier = Modifier.size(MacroDimenzije.ikonaMala)
            )
        }
    }
}

/** Link u primary (paprika je dopuštena za linkove), dodirni cilj 48 dp. */
@Composable
private fun Link(tekst: String, onClick: () -> Unit, ikona: ImageVector? = null) {
    val izvor = remember { MutableInteractionSource() }
    val boja = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .heightIn(min = MacroDimenzije.dodirniCilj)
            .skalaPritiska(izvor)
            .clickable(interactionSource = izvor, indication = null, role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ikona?.let {
            Icon(it, contentDescription = null, tint = boja, modifier = Modifier.size(MacroDimenzije.ikonaSitna))
            Spacer(Modifier.width(MacroRazmaci.s))
        }
        Text(text = tekst, style = MaterialTheme.typography.labelLarge, color = boja)
    }
}

/** Učitavanje: kostur zaglavlja, cilja i četiri reda (bez spinnera), s blagim pulsiranjem. */
@Composable
private fun Kostur(smanjeno: Boolean) {
    val boja = MaterialTheme.macroBoje.kostur
    val opis = stringResource(R.string.prof_ucitavanje)
    val puls = if (smanjeno) null else rememberInfiniteTransition(label = "kostur").animateFloat(
        initialValue = 0.55f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(Trajanje.disanje / 2), RepeatMode.Reverse),
        label = "puls"
    )

    @Composable
    fun Traka(udio: Float, visina: Dp, modifier: Modifier = Modifier) = Box(
        modifier
            .fillMaxWidth(udio)
            .height(visina)
            .background(boja, MacroOblici.traka)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = puls?.value ?: 1f }
            .semantics { contentDescription = opis }
    ) {
        Row(
            modifier = Modifier.padding(top = MacroRazmaci.xl),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(MacroDimenzije.avatarProfila).background(boja, CircleShape))
            Spacer(Modifier.width(MacroRazmaci.l))
            Column {
                Traka(MacroOmjeri.kosturNaziv, MacroDimenzije.kosturNaziv)
                Spacer(Modifier.height(MacroRazmaci.m))
                Traka(MacroOmjeri.kosturMakro, MacroDimenzije.kosturMakro)
            }
        }

        Traka(MacroOmjeri.kosturMakro, MacroDimenzije.kosturNaziv, Modifier.padding(top = MacroRazmaci.iznadSekcije))
        Row(
            modifier = Modifier.padding(top = MacroRazmaci.l),
            horizontalArrangement = Arrangement.spacedBy(MacroRazmaci.l)
        ) {
            repeat(2) {
                Column(Modifier.weight(1f)) {
                    Traka(MacroOmjeri.kosturNaziv, MacroDimenzije.ikona)
                    Spacer(Modifier.height(MacroRazmaci.s))
                    Traka(MacroOmjeri.kosturNaziv, MacroDimenzije.kosturMakro)
                }
            }
        }

        Traka(MacroOmjeri.kosturMakro, MacroDimenzije.kosturNaziv, Modifier.padding(top = MacroRazmaci.iznadSekcije))
        repeat(4) {
            Row(
                modifier = Modifier
                    .padding(top = MacroRazmaci.l)
                    .heightIn(min = MacroDimenzije.dodirniCilj),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(MacroDimenzije.ikona).background(boja, CircleShape))
                Spacer(Modifier.width(MacroRazmaci.l))
                Column {
                    Traka(MacroOmjeri.kosturNaziv, MacroDimenzije.kosturNaziv)
                    Spacer(Modifier.height(MacroRazmaci.xs))
                    Traka(MacroOmjeri.kosturMakro, MacroDimenzije.kosturMakro)
                }
            }
        }
    }
}

@Composable
private fun Greska(onPonovi: () -> Unit) {
    val shema = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = MacroRazmaci.xl)
            .background(shema.surface, MacroOblici.pladanj)
            .border(MacroDimenzije.linija, shema.outlineVariant, MacroOblici.pladanj)
            .padding(MacroRazmaci.xl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Rounded.CloudOff, contentDescription = null, tint = shema.onSurfaceVariant, modifier = Modifier.size(MacroDimenzije.ikona))
        Spacer(Modifier.height(MacroRazmaci.m))
        Text(
            stringResource(R.string.prof_greska_naslov),
            style = MaterialTheme.typography.titleMedium,
            color = shema.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(MacroRazmaci.xs))
        Text(
            stringResource(R.string.prof_greska_tekst),
            style = MaterialTheme.typography.bodyMedium,
            color = shema.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(MacroRazmaci.l))
        val izvor = remember { MutableInteractionSource() }
        OutlinedButton(
            onClick = onPonovi,
            interactionSource = izvor,
            shape = MacroOblici.gumb,
            modifier = Modifier
                .heightIn(min = MacroDimenzije.dodirniCilj)
                .skalaPritiska(izvor)
        ) {
            Text(stringResource(R.string.prof_pokusaj_ponovno), style = MaterialTheme.typography.labelLarge, color = shema.onSurface)
        }
    }
}

/** Prvi ulaz u sesiji: izblijeđivanje + pomak 12 dp nakon prijelaza ekrana, stupnjevano (samo graphicsLayer). */
private fun Modifier.ulaz(indeks: Int, aktivno: Boolean): Modifier = composed {
    val napredak = remember { Animatable(if (aktivno) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (napredak.value < 1f) {
            delay((Kasnjenje.nakonPrijelaza + indeks * Kasnjenje.korak).toLong())
            napredak.animateTo(1f, tween(Trajanje.srednje, easing = Ublazavanje.usporavanje))
        }
    }
    graphicsLayer {
        alpha = napredak.value
        translationY = (1f - napredak.value) * POMAK_ULAZA_DP.dp.toPx()
    }
}

/** Broji od nule samo pri prvom ulazu; kasnije promjene prelaze oprugom od trenutne vrijednosti. */
@Composable
private fun animiranaVrijednost(cilj: Float, animirajUlaz: Boolean, kasnjenje: Int, smanjeno: Boolean): State<Float> {
    val vrijednost = remember { Animatable(if (animirajUlaz) 0f else cilj) }
    var prviPut by remember { mutableStateOf(true) }
    LaunchedEffect(cilj) {
        val spec = if (prviPut && animirajUlaz) ulazniTween<Float>(Trajanje.ulaz, kasnjenje, smanjeno)
        else opruga(Opruge.blaga(), smanjeno)
        prviPut = false
        vrijednost.animateTo(cilj, spec)
    }
    return vrijednost.asState()
}

// ---------- Previewi (lažni podaci iz ProfilPrimjeri.kt) ----------

@Composable
private fun PreviewOkvir(tamna: Boolean, stanje: ProfilStanje) {
    MacroMenzaTheme(darkTheme = tamna) {
        ProfilSadrzaj(stanje = stanje, akcije = ProfilAkcije(), puniUlaz = false)
    }
}

private const val VISINA_PREVIEWA = 900

@Preview(name = "Svijetla · normalno", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewSvijetlaNormalno() = PreviewOkvir(false, ProfilStanje.Sadrzaj(PRIMJER_PROFIL_NORMALNO))

@Preview(name = "Tamna · normalno", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewTamnaNormalno() = PreviewOkvir(true, ProfilStanje.Sadrzaj(PRIMJER_PROFIL_NORMALNO))

@Preview(name = "Svijetla · bez cilja", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewSvijetlaBezCilja() = PreviewOkvir(false, ProfilStanje.Sadrzaj(PRIMJER_PROFIL_BEZ_CILJA))

@Preview(name = "Tamna · bez cilja", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewTamnaBezCilja() = PreviewOkvir(true, ProfilStanje.Sadrzaj(PRIMJER_PROFIL_BEZ_CILJA))

@Preview(name = "Svijetla · bez imena", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewSvijetlaBezImena() = PreviewOkvir(false, ProfilStanje.Sadrzaj(PRIMJER_PROFIL_BEZ_IMENA))

@Preview(name = "Tamna · bez imena", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewTamnaBezImena() = PreviewOkvir(true, ProfilStanje.Sadrzaj(PRIMJER_PROFIL_BEZ_IMENA))

@Preview(name = "Svijetla · učitavanje", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewSvijetlaUcitavanje() = PreviewOkvir(false, ProfilStanje.Ucitavanje)

@Preview(name = "Tamna · učitavanje", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewTamnaUcitavanje() = PreviewOkvir(true, ProfilStanje.Ucitavanje)

@Preview(name = "Svijetla · greška", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewSvijetlaGreska() = PreviewOkvir(false, ProfilStanje.Greska)

@Preview(name = "Tamna · greška", widthDp = 360, heightDp = VISINA_PREVIEWA)
@Composable
private fun PreviewTamnaGreska() = PreviewOkvir(true, ProfilStanje.Greska)

@Preview(name = "Svijetla · dugo ime · fontScale 1,3", widthDp = 360, heightDp = 1200, fontScale = 1.3f)
@Composable
private fun PreviewVelikiFontNormalno() = PreviewOkvir(false, ProfilStanje.Sadrzaj(PRIMJER_PROFIL_DUGO_IME))

@Preview(name = "Tamna · bez cilja · fontScale 1,3", widthDp = 360, heightDp = 1200, fontScale = 1.3f)
@Composable
private fun PreviewVelikiFontBezCilja() = PreviewOkvir(true, ProfilStanje.Sadrzaj(PRIMJER_PROFIL_BEZ_CILJA))

@Preview(name = "Svijetla · greška · fontScale 1,3", widthDp = 360, heightDp = 1200, fontScale = 1.3f)
@Composable
private fun PreviewVelikiFontGreska() = PreviewOkvir(false, ProfilStanje.Greska)
