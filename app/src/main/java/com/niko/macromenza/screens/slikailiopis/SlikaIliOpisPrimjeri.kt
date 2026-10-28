package com.niko.macromenza.screens.slikailiopis

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import com.niko.macromenza.model.PrijedlogObroka
import com.niko.macromenza.model.PrijedlogStavka

/*
 * ⚠ LAŽNI PODACI, SAMO ZA @Preview. Ne koristiti u aplikaciji.
 * Vrijednosti jela su okvirne i ručno upisane (u aplikaciji dolaze s POST /api/prepoznavanje).
 */

private const val OPIS_PRIMJERA = "Pohana svinjetina, pire krumpir i zelena salata"

/** Sve stavke sigurne: ručak iz menze, 1 + 1 + 0,5 porcije. */
private val PRIJEDLOG_NORMALNO = PrijedlogObroka(
    stavke = listOf(
        PrijedlogStavka(21, "Pohana svinjetina", 1.0, "VISOKA", 434.0, 31.0, 22.0, 27.0),
        PrijedlogStavka(133, "Pire krumpir", 1.0, "VISOKA", 302.0, 6.0, 40.0, 13.0),
        PrijedlogStavka(192, "Zelena salata", 0.5, "VISOKA", 53.0, 1.0, 2.0, 4.0)
    )
)

/** Nesigurne stavke, dugi naziv jela, neprepoznato i napomena modela. */
private val PRIJEDLOG_PROVJERI = PrijedlogObroka(
    stavke = listOf(
        PrijedlogStavka(21, "Pohana svinjetina", 1.0, "VISOKA", 434.0, 31.0, 22.0, 27.0),
        PrijedlogStavka(133, "Pire krumpir", 1.5, "SREDNJA", 453.0, 9.0, 60.0, 20.0),
        PrijedlogStavka(77, "Puretina u umaku od gljiva s njokima", 1.0, "NISKA", 373.0, 32.0, 35.0, 12.0)
    ),
    neprepoznato = listOf("Sok od naranče"),
    napomena = "Količina pirea procijenjena je sa slike."
)

/** Ništa iz menze nije prepoznato. */
private val PRIJEDLOG_PRAZAN = PrijedlogObroka(
    neprepoznato = listOf("Sok od naranče"),
    napomena = "Na slici se vidi samo čaša."
)

/** Prazno: nema ni slike ni opisa, "Prepoznaj" je onemogućen. */
val PRIMJER_PRAZNO = SlikaIliOpisUi()

/** Slika i opis, spremno za prepoznavanje (bitmapa se crta u previewu, vidi [primjerSlike]). */
fun primjerSlikaIOpis(slika: ImageBitmap) = SlikaIliOpisUi(
    opis = OPIS_PRIMJERA,
    pregled = PregledSlike.Gotov(slika)
)

/** Pregled slike se još učitava (kostur). */
val PRIMJER_PREGLED_UCITAVA = SlikaIliOpisUi(opis = OPIS_PRIMJERA, pregled = PregledSlike.Ucitava)

/** Analiza u tijeku: prazan tanjur diše, kontrole su onemogućene. */
val PRIMJER_ANALIZA = SlikaIliOpisUi(opis = OPIS_PRIMJERA, ucitava = true)

/** Greška mreže s "Pokušaj ponovno". */
val PRIMJER_GRESKA = SlikaIliOpisUi(
    opis = OPIS_PRIMJERA,
    greska = "Nema veze s poslužiteljem. Provjeri vezu i pokušaj ponovno."
)

/** Rezultat: sve stavke sigurne. */
val PRIMJER_REZULTAT = SlikaIliOpisUi(opis = OPIS_PRIMJERA, rezultat = PRIJEDLOG_NORMALNO)

/** Rezultat sa stavkama "Provjeri" (i dugim nazivom jela) te neprepoznatim. */
val PRIMJER_REZULTAT_PROVJERI = SlikaIliOpisUi(opis = OPIS_PRIMJERA, rezultat = PRIJEDLOG_PROVJERI)

/** Prazan rezultat: nema stavki, nema "Koristi prijedlog". */
val PRIMJER_REZULTAT_PRAZAN = SlikaIliOpisUi(opis = "Čaša soka", rezultat = PRIJEDLOG_PRAZAN)

/** Tanjur odozgo na stolu, umjesto prave fotografije (previewi nemaju pristup galeriji). */
fun primjerSlike(stol: Color, tanjur: Color, hrana: Color): ImageBitmap {
    val slika = ImageBitmap(width = 640, height = 400)
    val platno = Canvas(slika)
    val boja = Paint()
    boja.color = stol
    platno.drawRect(0f, 0f, 640f, 400f, boja)
    boja.color = tanjur
    platno.drawCircle(Offset(320f, 200f), 170f, boja)
    boja.color = hrana
    platno.drawCircle(Offset(260f, 170f), 70f, boja)
    platno.drawCircle(Offset(390f, 240f), 55f, boja)
    return slika
}
