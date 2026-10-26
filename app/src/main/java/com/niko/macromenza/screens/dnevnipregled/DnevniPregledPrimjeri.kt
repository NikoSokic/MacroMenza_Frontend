package com.niko.macromenza.screens.dnevnipregled

import com.niko.macromenza.model.Savjet
import com.niko.macromenza.model.SavjetJelo
import com.niko.macromenza.ui.components.Makro
import java.time.LocalDate

/*
 * ⚠ LAŽNI PODACI, SAMO ZA @Preview. Ne koristiti u aplikaciji.
 * Vrijednosti jela su okvirne i ručno upisane (u aplikaciji dolaze iz v_jelo_nutritivno preko /api/obroci).
 */

private val DATUM_PRIMJERA: LocalDate = LocalDate.of(2026, 10, 26)

private val CILJ_PRIMJERA = Makro(kcal = 2400.0, proteini = 160.0, masti = 67.0, ugljikohidrati = 290.0)

private fun jelo(naziv: String, kolicina: Double, kcal: Double, p: Double, m: Double, u: Double) =
    JeloUi(naziv, kolicina, Makro(kcal, p, m, u))

private fun obrok(id: Long, tip: String, vrijeme: String, vararg jela: JeloUi) =
    ObrokUi(id, tip, vrijeme, jela.fold(Makro.NULA) { a, j -> a + j.makro }, jela.toList(), izvor = null)

private val SAVJET_PRIMJERA = Savjet(
    naslov = "Fali ti proteina",
    tekst = "Do cilja ti nedostaje oko 90 g proteina. Za večeru biraj jelo s mesom ili mahunarkama.",
    jela = listOf(
        SavjetJelo(idJelo = 1, naziv = "Pileći file na žaru", kalorije = 240.0, proteini = 44.0),
        SavjetJelo(idJelo = 2, naziv = "Grah varivo", kalorije = 310.0, proteini = 16.0)
    )
)

private val DORUCAK = obrok(
    1, "DORUCAK", "08:10",
    jelo("Jogurt", 1.0, 140.0, 7.0, 7.0, 11.0),
    jelo("Kruh s maslacem i pekmezom", 1.0, 330.0, 7.0, 12.0, 48.0)
)

private val RUCAK = obrok(
    2, "RUCAK", "12:40",
    jelo("Grah varivo", 1.0, 310.0, 16.0, 9.0, 40.0),
    jelo("Pohana piletina", 1.0, 420.0, 32.0, 22.0, 24.0),
    jelo("Pire krumpir", 0.5, 95.0, 2.0, 4.0, 13.0),
    jelo("Zelena salata", 1.0, 45.0, 1.0, 3.0, 4.0),
    jelo("Kruh", 1.5, 170.0, 6.0, 1.0, 34.0)
)

private val UZINA = obrok(
    3, "UZINA", "16:20",
    jelo("Pizza mala", 1.0, 380.0, 15.0, 14.0, 48.0)
)

private val VECERA = obrok(
    4, "VECERA", "19:05",
    jelo("Puretina u umaku od gljiva s njokima", 1.5, 560.0, 48.0, 18.0, 52.0),
    jelo("Sir s vrhnjem", 1.0, 150.0, 8.0, 12.0, 3.0),
    jelo("Kruh", 0.5, 57.0, 2.0, 0.0, 11.0)
)

private fun pregled(vararg obroci: ObrokUi) = DnevniPregledUi(
    datum = DATUM_PRIMJERA,
    cilj = CILJ_PRIMJERA,
    pojedeno = obroci.fold(Makro.NULA) { a, o -> a + o.makro },
    obroci = obroci.toList(),
    savjet = SAVJET_PRIMJERA
)

/** Prazan dan: nijedan obrok. */
val PRIMJER_PRAZNO = pregled()

/** Uobičajen dan: doručak i ručak, 1510 kcal od 2400. */
val PRIMJER_NORMALNO = pregled(DORUCAK, RUCAK)

/** Preko cilja: sva četiri obroka, 2657 kcal od 2400 (užina kao tanjurić). */
val PRIMJER_PREKO_CILJA = pregled(DORUCAK, RUCAK, UZINA, VECERA)
