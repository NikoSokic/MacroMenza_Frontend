package com.niko.macromenza.screens.povijest

import com.niko.macromenza.model.NutritivneVrijednosti
import com.niko.macromenza.model.Tjedan
import com.niko.macromenza.model.TjedanDan
import com.niko.macromenza.ui.components.JeloUi
import com.niko.macromenza.ui.components.Makro
import com.niko.macromenza.ui.components.ObrokUi
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

/** [makro] je za [kolicina] porcija (već pomnoženo). */
private class JeloPrimjera(val naziv: String, val kolicina: Double, val makro: Makro)

private fun jelo(naziv: String, kcal: Double, p: Double, m: Double, u: Double, kolicina: Double = 1.0) =
    JeloPrimjera(naziv, kolicina, Makro(kcal, p, m, u))

private val JOGURT = jelo("Jogurt", 140.0, 7.0, 7.0, 11.0)
private val KRUH_PEKMEZ = jelo("Kruh s maslacem i pekmezom", 330.0, 7.0, 12.0, 48.0)
private val GRAH = jelo("Grah varivo", 310.0, 16.0, 9.0, 40.0)
private val POHANA_PILETINA = jelo("Pohana piletina", 420.0, 32.0, 22.0, 24.0)
private val PIRE = jelo("Pire krumpir", 95.0, 2.0, 4.0, 13.0, kolicina = 0.5)
private val SALATA = jelo("Zelena salata", 45.0, 1.0, 3.0, 4.0)
private val KRUH = jelo("Kruh", 170.0, 6.0, 1.0, 34.0, kolicina = 1.5)
private val PIZZA = jelo("Pizza mala", 380.0, 15.0, 14.0, 48.0)
private val PURETINA = jelo("Puretina u umaku od gljiva s njokima", 560.0, 48.0, 18.0, 52.0, kolicina = 1.5)
private val SIR_VRHNJE = jelo("Sir s vrhnjem", 150.0, 8.0, 12.0, 3.0)
private val KRUH_POLA = jelo("Kruh", 57.0, 2.0, 0.0, 11.0, kolicina = 0.5)

private val DORUCAK = listOf(JOGURT, KRUH_PEKMEZ)
private val RUCAK = listOf(GRAH, POHANA_PILETINA, PIRE, SALATA, KRUH)
private val VECERA = listOf(PURETINA, SIR_VRHNJE, KRUH_POLA)

private fun dan(indeks: Int, jela: List<JeloPrimjera>, cilj: Makro? = CILJ_PRIMJERA): TjedanDan {
    val m = jela.fold(Makro.NULA) { a, j -> a + j.makro }
    val imaUnos = jela.isNotEmpty()
    return TjedanDan(
        datum = POCETAK_PRIMJERA.plusDays(indeks.toLong()).toString(),
        kalorije = m.kcal,
        proteini = m.proteini,
        ugljikohidrati = m.ugljikohidrati,
        masti = m.masti,
        ciljKalorije = cilj?.kcal,
        imaUnos = imaUnos,
        // Na backendu se računa isto (±10 %); frontend u aplikaciji uzima uCilju s backenda.
        uCilju = imaUnos && cilj != null && m.kcal / cilj.kcal in 0.9..1.1
    )
}

private fun Makro.nv() = NutritivneVrijednosti(kalorije = kcal, proteini = proteini, ugljikohidrati = ugljikohidrati, masti = masti)

private fun tjedan(dani: List<TjedanDan>, imaPrimjer: Boolean = false, cilj: Makro? = CILJ_PRIMJERA): Tjedan {
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
        prosjekCilj = if (prosjek != null) cilj?.nv() else null,
        imaPrethodni = true,
        imaSljedeci = false,
        imaPrimjer = imaPrimjer
    )
}

/**
 * Sva stanja dana: Pon u cilju (2277 kcal), Uto preko cilja (2657, s pizzom za užinu), Sri bez unosa,
 * Čet ispod cilja (1807, ručak i večera), Pet (danas) ispod cilja (1510), Sub i Ned u budućnosti.
 */
private val DANI_NORMALNO = listOf(
    dan(0, DORUCAK + RUCAK + VECERA),
    dan(1, DORUCAK + RUCAK + PIZZA + VECERA),
    dan(2, emptyList()),
    dan(3, RUCAK + VECERA),
    dan(4, DORUCAK + RUCAK),
    dan(5, emptyList()),
    dan(6, emptyList())
)

val PRIMJER_TJEDAN_NORMALNO: Tjedan = tjedan(DANI_NORMALNO)

val PRIMJER_TJEDAN_PRAZNO: Tjedan = tjedan((0..6).map { dan(it, emptyList()) })

val PRIMJER_TJEDAN_PRIMJER: Tjedan = tjedan(DANI_NORMALNO, imaPrimjer = true)

/** Isti dani bez preporuke (korisnik nije postavio cilj): tanjuri bez kruga cilja, bez "u cilju" i "preko cilja". */
val PRIMJER_TJEDAN_BEZ_CILJA: Tjedan = tjedan(
    listOf(
        dan(0, DORUCAK + RUCAK + VECERA, cilj = null),
        dan(1, DORUCAK + RUCAK + PIZZA + VECERA, cilj = null),
        dan(2, emptyList(), cilj = null),
        dan(3, RUCAK + VECERA, cilj = null),
        dan(4, DORUCAK + RUCAK, cilj = null),
        dan(5, emptyList(), cilj = null),
        dan(6, emptyList(), cilj = null)
    ),
    cilj = null
)

/** Indeks utorka (preko cilja) za preview s odabranim danom. */
const val ODABRANI_DAN_PRIMJERA = 1

/** Prosjek u cilju: 3 od 4 dana s unosom u cilju (2277, 2327, 2277, 1807 kcal), prosjek 2172 od 2400. */
val PRIMJER_TJEDAN_U_CILJU: Tjedan = tjedan(
    listOf(
        dan(0, DORUCAK + RUCAK + VECERA),
        dan(1, listOf(JOGURT) + RUCAK + PIZZA + VECERA),
        dan(2, DORUCAK + RUCAK + VECERA),
        dan(3, RUCAK + VECERA),
        dan(4, emptyList()),
        dan(5, emptyList()),
        dan(6, emptyList())
    )
)

/** Prosjek preko cilja: nijedan dan u cilju (2657, 2797, 2657, 2797 kcal), prosjek 2727 od 2400 (113 %). */
val PRIMJER_TJEDAN_PREKO_CILJA: Tjedan = tjedan(
    listOf(
        dan(0, DORUCAK + RUCAK + PIZZA + VECERA),
        dan(1, DORUCAK + listOf(JOGURT) + RUCAK + PIZZA + VECERA),
        dan(2, DORUCAK + RUCAK + PIZZA + VECERA),
        dan(3, DORUCAK + listOf(JOGURT) + RUCAK + PIZZA + VECERA),
        dan(4, emptyList()),
        dan(5, emptyList()),
        dan(6, emptyList())
    )
)

// ---------- Dan (kartica Dan i Detalj dana) ----------

/** Ponedjeljak 26. 10. 2026. */
val DATUM_DANA_PRIMJERA: LocalDate = POCETAK_PRIMJERA

const val ID_RUCKA_PRIMJERA = 2L

private fun obrok(id: Long, tip: String, vrijeme: String, jela: List<JeloPrimjera>) = ObrokUi(
    id = id,
    tip = tip,
    vrijeme = vrijeme,
    makro = jela.fold(Makro.NULA) { a, j -> a + j.makro },
    jela = jela.map { JeloUi(it.naziv, it.kolicina, it.makro) },
    izvor = null
)

/**
 * Tjedni prosječni cilj 2200 kcal; dan s ciljem 2400 kcal dobiva P/M/U skaliran s 2400 / 2200
 * (pokriva [ciljDana]: 150 → 164 g P, 61 → 67 g M, 265 → 289 g U).
 */
private val PROSJEK_CILJA_PRIMJERA = Makro(kcal = 2200.0, proteini = 150.0, masti = 61.0, ugljikohidrati = 265.0)

private fun danPrimjera(datum: LocalDate, vararg obroci: ObrokUi) = DanUi(
    datum = datum,
    ukupno = obroci.fold(Makro.NULA) { a, o -> a + o.makro },
    ciljKcal = CILJ_PRIMJERA.kcal,
    ciljMakro = ciljDana(CILJ_PRIMJERA.kcal, PROSJEK_CILJA_PRIMJERA),
    obroci = obroci.toList()
)

/** Pon: doručak, ručak i večera, 2277 od 2400 kcal. */
val DAN_PRIMJER_U_CILJU: DanUi = danPrimjera(
    DATUM_DANA_PRIMJERA,
    obrok(1, "DORUCAK", "08:10", DORUCAK),
    obrok(ID_RUCKA_PRIMJERA, "RUCAK", "12:40", RUCAK),
    obrok(4, "VECERA", "19:05", VECERA)
)

/** Uto: uz to i pizza za užinu, 2657 od 2400 kcal (preko cilja 257). */
val DAN_PRIMJER_PREKO_CILJA: DanUi = danPrimjera(
    DATUM_DANA_PRIMJERA.plusDays(1),
    obrok(1, "DORUCAK", "08:10", DORUCAK),
    obrok(ID_RUCKA_PRIMJERA, "RUCAK", "12:40", RUCAK),
    obrok(3, "UZINA", "16:20", listOf(PIZZA)),
    obrok(4, "VECERA", "19:05", VECERA)
)

/** Sri: bez unosa. */
val DAN_PRIMJER_PRAZAN: DanUi = danPrimjera(DATUM_DANA_PRIMJERA.plusDays(2))
