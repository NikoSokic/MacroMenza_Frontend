package com.niko.macromenza.screens.povijest

import androidx.compose.runtime.Immutable
import com.niko.macromenza.model.Tjedan
import com.niko.macromenza.model.TjedanDan
import com.niko.macromenza.ui.components.Makro
import com.niko.macromenza.ui.components.uMakro
import java.time.LocalDate

enum class PovijestTab {
    TJEDNI,
    DNEVNI
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
)

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
