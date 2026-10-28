package com.niko.macromenza.ui.theme

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/** Ritam razmaka 4/8 dp. */
object MacroRazmaci {
    val xxs = 2.dp
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 40.dp

    val rubEkrana = 16.dp
    val iznadSekcije = 32.dp
    val ispodNaslovaSekcije = 12.dp
}

/** Veličine komponenti; ekrani ne smiju imati vlastite dp vrijednosti. */
object MacroDimenzije {
    val dodirniCilj = 48.dp
    val ikona = 24.dp
    val ikonaMala = 20.dp
    val ikonaSitna = 16.dp
    val linija = 1.dp
    val obrubIsprekidan = 1.5.dp

    // Luk (hero)
    val lukSirina = 196.dp
    val lukVisina = 170.dp
    val lukDebljina = 14.dp
    val prekoracenjeRub = 3.dp
    val heroUnutarnji = 12.dp
    val lukUnutarnjiPromjer = 150.dp

    // Trake makronutrijenata
    val trakaVisina = 6.dp
    val oznakaCiljaSirina = 2.dp
    val oznakaCiljaVisina = 14.dp

    // Pladanj i tanjuri
    val pladanjOkvir = 9.dp
    val pladanjUnutarnjiVrh = 2.dp
    val utorVisina = 100.dp
    val tanjurMin = 52.dp
    val tanjurMax = 92.dp
    val tanjurPrazan = 64.dp
    val tanjurUzinaMin = 40.dp
    val tanjurUzinaMax = 56.dp
    val sjenaTanjura = 3.dp
    val legendaTocka = 8.dp
    val xxsRub = 2.dp
    val iskraPolumjer = 2.5.dp
    val iskraPomak = 18.dp
    val utorPolumjer = 16.dp
    val zrnoTanko = 1.dp
    val zrnoDebelo = 2.dp
    val zrnoNjihanje = 6.dp
    val crtica = 5.dp

    // Povijest: tanjur dana u redu tjedna (7 stupaca od ≥ 48 dp; s prstenom preko cilja okvir je max + 2 × 5 dp)
    val tanjurTjedanMax = 36.dp
    val tanjurTjedanMin = 18.dp
    val tanjurTjedanPrazan = 22.dp
    /** Crtica ispod slova današnjeg dana. */
    val oznakaDanas = DpSize(12.dp, 2.dp)
    /** Vodoravno povlačenje dulje od ovoga mijenja tjedan. */
    val pragListanja = 80.dp

    // Profil: krug s inicijalima u zaglavlju korisnika
    val avatarProfila = 64.dp

    // Forme: radio oznaka u IzborOpcije (prsten i točka odabira)
    val radioOznaka = 20.dp
    val radioObrub = 2.dp
    val radioTocka = 10.dp

    // Moje mjere: graf mase (linija i točke u lukIspuna)
    val grafMaseVisina = 190.dp
    val grafLinija = 2.dp
    val grafTocka = 4.dp
    val grafTockaOdabrana = 6.dp
    val grafTockaRub = 2.dp

    // Redovi obroka i glif porcije
    val ikonaObroka = 40.dp
    val glifPorcije = 22.dp
    val pragBrisanja = 96.dp

    // Primarna akcija
    val fabVisina = 56.dp
    val prostorZaFab = 96.dp

    // Unos obroka: krug "+" u redu jela i kostur reda
    val gumbDodaj = 32.dp
    val kosturNaziv = 14.dp
    val kosturMakro = 10.dp
}

/** Omjeri za crteže u Canvasu (dio promjera ili širine). */
object MacroOmjeri {
    /** Luk od 260° s otvorom dolje: počinje na 140° (0° = desno, u smjeru kazaljke). */
    const val lukPocetak = 140f
    const val lukRaspon = 260f

    /** Okomiti položaj sadržaja u luku (BiasAlignment): središte kružnice je malo ispod sredine okvira. */
    const val lukSredisteBias = 0.12f

    /** Ljestvica traka ide do 125 % cilja, pa je oznaka cilja na 80 % širine. */
    const val trakaMaksimum = 1.25f

    const val rubTanjura = 0.08f
    const val razmakSegmenata = 0.025f
    const val udubljenjeTanjura = 0.70f
    const val kcalTanjura = 0.21f

    /** Širine traka u kosturu reda jela (dio širine teksta). */
    /** Neprozirnost oznake pogotka pretrage (boja teksta preko pozadine). */
    const val oznakaPogotka = 0.14f

    /** Neprozirnost označenog teksta u polju unosa (onSurface preko pozadine, ne paprika). */
    const val oznakaTeksta = 0.24f

    const val kosturNaziv = 0.6f
    const val kosturMakro = 0.4f

    /** Okvir pregleda slike (Slika ili opis): širina / visina = 16 : 10. */
    const val pregledSlike = 1.6f

    /** Disanje praznog tanjura i pulsiranje kostura: alfa između ovih vrijednosti (kao prazni utori na Home). */
    const val disanjeMin = 0.45f
    const val disanjeMaks = 0.8f
}
