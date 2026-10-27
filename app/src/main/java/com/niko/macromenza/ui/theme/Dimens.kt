package com.niko.macromenza.ui.theme

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

    const val kosturNaziv = 0.6f
    const val kosturMakro = 0.4f
}
