package com.niko.macromenza.screens.povijest

import com.niko.macromenza.model.NutritivneVrijednosti
import com.niko.macromenza.model.Tjedan
import com.niko.macromenza.model.TjedanDan
import com.niko.macromenza.ui.components.Makro
import java.time.LocalDate

/*
 * ⚠ LAŽNI PODACI, SAMO ZA @Preview. Ne koristiti u aplikaciji.
 * Dani su zbroj stvarnih jela iz menze; vrijednosti su okvirne i ručno upisane
 * (u aplikaciji tjedan dolazi s GET api/obroci/tjedan).
 */

/** Petak u tjednu 26. 10. – 1. 11. 2026.: prošli dani, danas i dva dana u budućnosti. */
val DANAS_PRIMJERA: LocalDate = LocalDate.of(2026, 10, 30)

private val POCETAK_PRIMJERA: LocalDate = LocalDate.of(2026, 10, 26)

private val CILJ_PRIMJERA = Makro(kcal = 2400.0, proteini = 160.0, masti = 67.0, ugljikohidrati = 290.0)

private class JeloPrimjera(val naziv: String, val makro: Makro)

private fun jelo(naziv: String, kcal: Double, p: Double, m: Double, u: Double) = JeloPrimjera(naziv, Makro(kcal, p, m, u))

private val JOGURT = jelo("Jogurt", 140.0, 7.0, 7.0, 11.0)
private val KRUH_PEKMEZ = jelo("Kruh s maslacem i pekmezom", 330.0, 7.0, 12.0, 48.0)
private val GRAH = jelo("Grah varivo", 310.0, 16.0, 9.0, 40.0)
private val POHANA_PILETINA = jelo("Pohana piletina", 420.0, 32.0, 22.0, 24.0)
private val PIRE = jelo("Pire krumpir (0,5)", 95.0, 2.0, 4.0, 13.0)
private val SALATA = jelo("Zelena salata", 45.0, 1.0, 3.0, 4.0)
private val KRUH = jelo("Kruh (1,5)", 170.0, 6.0, 1.0, 34.0)
private val PIZZA = jelo("Pizza mala", 380.0, 15.0, 14.0, 48.0)
private val PURETINA = jelo("Puretina u umaku od gljiva s njokima (1,5)", 560.0, 48.0, 18.0, 52.0)
private val SIR_VRHNJE = jelo("Sir s vrhnjem", 150.0, 8.0, 12.0, 3.0)
private val KRUH_POLA = jelo("Kruh (0,5)", 57.0, 2.0, 0.0, 11.0)

private val DORUCAK = listOf(JOGURT, KRUH_PEKMEZ)
private val RUCAK = listOf(GRAH, POHANA_PILETINA, PIRE, SALATA, KRUH)
private val VECERA = listOf(PURETINA, SIR_VRHNJE, KRUH_POLA)

private fun dan(indeks: Int, jela: List<JeloPrimjera>): TjedanDan {
    val m = jela.fold(Makro.NULA) { a, j -> a + j.makro }
    val imaUnos = jela.isNotEmpty()
    return TjedanDan(
        datum = POCETAK_PRIMJERA.plusDays(indeks.toLong()).toString(),
        kalorije = m.kcal,
        proteini = m.proteini,
        ugljikohidrati = m.ugljikohidrati,
        masti = m.masti,
        ciljKalorije = CILJ_PRIMJERA.kcal,
        imaUnos = imaUnos,
        uCilju = imaUnos && m.kcal / CILJ_PRIMJERA.kcal in 0.9..1.1
    )
}

private fun Makro.nv() = NutritivneVrijednosti(kalorije = kcal, proteini = proteini, ugljikohidrati = ugljikohidrati, masti = masti)

private fun tjedan(dani: List<TjedanDan>, imaPrimjer: Boolean = false): Tjedan {
    val sUnosom = dani.filter { it.imaUnos }
    val prosjek = if (sUnosom.isEmpty()) null else Makro(
        sUnosom.sumOf { it.kalorije } / sUnosom.size,
        sUnosom.sumOf { it.proteini } / sUnosom.size,
        sUnosom.sumOf { it.masti } / sUnosom.size,
        sUnosom.sumOf { it.ugljikohidrati } / sUnosom.size
    )
    return Tjedan(
        pocetak = POCETAK_PRIMJERA.toString(),
        kraj = POCETAK_PRIMJERA.plusDays(6).toString(),
        dani = dani,
        daniSUnosom = sUnosom.size,
        daniUCilju = dani.count { it.uCilju },
        prosjek = prosjek?.nv(),
        prosjekCilj = prosjek?.let { CILJ_PRIMJERA.nv() },
        imaPrethodni = true,
        imaSljedeci = false,
        imaPrimjer = imaPrimjer
    )
}

/**
 * Pon u cilju (2277 kcal), Uto preko cilja (2657, s pizzom za užinu), Sri bez unosa,
 * Čet u cilju (2327), Pet (danas) ispod cilja (1510), Sub i Ned u budućnosti.
 */
private val DANI_NORMALNO = listOf(
    dan(0, DORUCAK + RUCAK + VECERA),
    dan(1, DORUCAK + RUCAK + PIZZA + VECERA),
    dan(2, emptyList()),
    dan(3, listOf(JOGURT) + RUCAK + PIZZA + VECERA),
    dan(4, DORUCAK + RUCAK),
    dan(5, emptyList()),
    dan(6, emptyList())
)

val PRIMJER_TJEDAN_NORMALNO: Tjedan = tjedan(DANI_NORMALNO)

val PRIMJER_TJEDAN_PRAZNO: Tjedan = tjedan((0..6).map { dan(it, emptyList()) })

val PRIMJER_TJEDAN_PRIMJER: Tjedan = tjedan(DANI_NORMALNO, imaPrimjer = true)
