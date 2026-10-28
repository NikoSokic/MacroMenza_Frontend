package com.niko.macromenza.screens.povijest

import androidx.compose.runtime.Immutable
import com.niko.macromenza.model.Obrok
import com.niko.macromenza.model.PovijestDana
import com.niko.macromenza.model.Tjedan
import com.niko.macromenza.model.TjedanDan
import com.niko.macromenza.ui.components.Makro
import com.niko.macromenza.ui.components.ObrokUi
import com.niko.macromenza.ui.components.uUi
import com.niko.macromenza.ui.components.uMakro
import java.time.LocalDate

enum class PovijestTab {
    TJEDNI,
    DNEVNI
}

/** Stanje tanjura dana u redu tjedna (pages/povijest.md, tablica stanja). Danas je zaseban podatak ([DanTjednaUi.jeDanas]). */
enum class StanjeDana {
    ISPOD_CILJA,
    U_CILJU,
    PREKO_CILJA,
    /** Dan s unosom za koji nema preporuke: tanjur bez kruga cilja i bez statusa. */
    BEZ_CILJA,
    BEZ_UNOSA,
    BUDUCNOST
}

/** Dan u tjednu; [makro] je [Makro.NULA] kad dan nema unosa, [ciljKcal] null kad za dan nema preporuke. */
@Immutable
data class DanTjednaUi(
    val datum: LocalDate,
    val makro: Makro,
    val ciljKcal: Double?,
    val imaUnos: Boolean,
    val uCilju: Boolean,
    val uBuducnosti: Boolean,
    val jeDanas: Boolean
) {
    /** "U cilju" dolazi s backenda (±10 %); preko cilja je svaki drugi dan iznad cilja. */
    val stanje: StanjeDana get() = when {
        uBuducnosti -> StanjeDana.BUDUCNOST
        !imaUnos -> StanjeDana.BEZ_UNOSA
        ciljKcal == null || ciljKcal <= 0.0 -> StanjeDana.BEZ_CILJA
        uCilju -> StanjeDana.U_CILJU
        makro.kcal > ciljKcal -> StanjeDana.PREKO_CILJA
        else -> StanjeDana.ISPOD_CILJA
    }
}

@Immutable
data class TjedanUi(
    val pocetak: LocalDate,
    val kraj: LocalDate,
    val dani: List<DanTjednaUi>,
    val daniSUnosom: Int,
    val daniUCilju: Int,
    val prosjek: Makro?,
    val prosjekCilj: Makro?,
    val imaPrethodni: Boolean,
    val imaSljedeci: Boolean,
    val imaPrimjer: Boolean,
    /** Primjer se stvara za zadnjih 7 dana (bez danas), pa tjedan mora imati barem jedan takav dan. */
    val mozePrimjer: Boolean
) {
    /** Na tekućem tjednu nema sljedećeg (backend ne pušta listanje u budućnost). */
    val jeTekuci: Boolean get() = !imaSljedeci

    val jePrazan: Boolean get() = daniSUnosom == 0

    val raspon: String get() = formatRasponTjedna(pocetak, kraj)
}

sealed interface TjedanStanje {
    data object Ucitavanje : TjedanStanje
    data object Greska : TjedanStanje
    data class Sadrzaj(val tjedan: TjedanUi) : TjedanStanje
}

/** Greška ima prednost: zatraženi tjedan nije stigao, pa se stari ne prikazuje kao da jest. */
fun tjedanStanje(tjedan: Tjedan?, greska: Boolean, danas: LocalDate): TjedanStanje = when {
    greska -> TjedanStanje.Greska
    tjedan == null -> TjedanStanje.Ucitavanje
    else -> TjedanStanje.Sadrzaj(tjedan.uUi(danas))
}

fun Tjedan.uUi(danas: LocalDate): TjedanUi {
    val od = LocalDate.parse(pocetak)
    val doDana = LocalDate.parse(kraj)
    return TjedanUi(
        pocetak = od,
        kraj = doDana,
        dani = dani.map { it.uUi(danas) },
        daniSUnosom = daniSUnosom,
        daniUCilju = daniUCilju,
        prosjek = prosjek?.uMakro(),
        prosjekCilj = prosjekCilj?.uMakro(),
        imaPrethodni = imaPrethodni,
        imaSljedeci = imaSljedeci,
        imaPrimjer = imaPrimjer,
        mozePrimjer = !od.isAfter(danas.minusDays(1)) && !doDana.isBefore(danas.minusDays(7))
    )
}

private fun TjedanDan.uUi(danas: LocalDate): DanTjednaUi {
    val dan = LocalDate.parse(datum)
    return DanTjednaUi(
        datum = dan,
        makro = if (imaUnos) Makro(kalorije, proteini, masti, ugljikohidrati) else Makro.NULA,
        ciljKcal = ciljKalorije,
        imaUnos = imaUnos,
        uCilju = uCilju,
        uBuducnosti = dan.isAfter(danas),
        jeDanas = dan == danas
    )
}

/** "20. – 26. 10. 2026.", preko mjeseca "27. 10. – 2. 11. 2026.", preko godine s obje godine. */
fun formatRasponTjedna(od: LocalDate, doDana: LocalDate): String = when {
    od.year != doDana.year ->
        "${od.dayOfMonth}. ${od.monthValue}. ${od.year}. – ${doDana.dayOfMonth}. ${doDana.monthValue}. ${doDana.year}."
    od.monthValue != doDana.monthValue ->
        "${od.dayOfMonth}. ${od.monthValue}. – ${doDana.dayOfMonth}. ${doDana.monthValue}. ${doDana.year}."
    else ->
        "${od.dayOfMonth}. – ${doDana.dayOfMonth}. ${doDana.monthValue}. ${doDana.year}."
}

// ---------- Dan (kartica Dan u Povijesti i Detalj dana) ----------

/** Dan je preko cilja iznad ovog dijela cilja (isti prag kao "u cilju" ±10 %). */
private const val PREKO_CILJA_DANA = 1.1

/**
 * Jedan dan: zbroj iz obroka (pa prati brisanje i "Poništi" odmah), cilj kcal dana iz tjedna i cilj P/M/U
 * izveden iz tjednog prosjeka ([ciljDana]). Obroci su poredani po vremenu.
 */
@Immutable
data class DanUi(
    val datum: LocalDate,
    val ukupno: Makro,
    val ciljKcal: Double?,
    val ciljMakro: Makro?,
    val obroci: List<ObrokUi>
) {
    val jePrazan: Boolean get() = obroci.isEmpty()

    val prekoCilja: Boolean get() = ciljKcal != null && ciljKcal > 0 && ukupno.kcal > ciljKcal * PREKO_CILJA_DANA
}

sealed interface DanStanje {
    data object Ucitavanje : DanStanje
    data class Sadrzaj(val dan: DanUi) : DanStanje
}

/**
 * Cilj P/M/U za jedan dan. API daje samo kcal cilj po danu ([ciljKcal]), pa se tjedni prosječni cilj
 * skalira omjerom ciljKcal / prosjekCilj.kcal (omjer P/M/U ostaje isti). Bez kcal cilja ili prosjeka: null.
 */
fun ciljDana(ciljKcal: Double?, prosjekCilj: Makro?): Makro? {
    if (ciljKcal == null || ciljKcal <= 0.0) return null
    if (prosjekCilj == null || prosjekCilj.kcal <= 0.0) return null
    val omjer = ciljKcal / prosjekCilj.kcal
    return Makro(
        kcal = ciljKcal,
        proteini = prosjekCilj.proteini * omjer,
        masti = prosjekCilj.masti * omjer,
        ugljikohidrati = prosjekCilj.ugljikohidrati * omjer
    )
}

/**
 * Stanje dana iz podataka PovijestViewModela. Dan je učitan kad povijest dana stigne za taj datum i (ako ima kcal)
 * stignu i obroci. [istekaoRok]: ViewModel ne izlaže grešku dana, pa se nakon roka prikazuje prazan dan.
 * [skriveniId] su obroci obrisani optimistično (nestaju odmah, vraćaju se ako brisanje ne uspije).
 */
fun danStanje(
    datum: LocalDate,
    povijestDana: PovijestDana?,
    obroci: List<Obrok>,
    skriveniId: Set<Long>,
    tjedan: Tjedan?,
    istekaoRok: Boolean
): DanStanje {
    val iso = datum.toString()
    val obrociDana = obroci.filter { it.datum == iso }
    val vidljivi = obrociDana.filter { it.id !in skriveniId }.map { it.uUi() }.sortedBy { it.vrijeme.orEmpty() }
    val danStigao = povijestDana?.datum == iso
    val cekaObroke = danStigao && povijestDana!!.ukupnoKalorije > 0 && obrociDana.isEmpty() && skriveniId.isEmpty()
    if ((!danStigao || cekaObroke) && !istekaoRok) return DanStanje.Ucitavanje

    val ciljKcal = tjedan?.dani?.firstOrNull { it.datum == iso }?.ciljKalorije
    return DanStanje.Sadrzaj(
        DanUi(
            datum = datum,
            ukupno = vidljivi.fold(Makro.NULA) { a, o -> a + o.makro },
            ciljKcal = ciljKcal,
            ciljMakro = ciljDana(ciljKcal, tjedan?.prosjekCilj?.uMakro()),
            obroci = vidljivi
        )
    )
}
