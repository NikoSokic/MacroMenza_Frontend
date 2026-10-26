package com.niko.macromenza.screens.dnevnipregled

import androidx.compose.runtime.Immutable
import com.niko.macromenza.model.DanPregled
import com.niko.macromenza.model.NutritivneVrijednosti
import com.niko.macromenza.model.Obrok
import com.niko.macromenza.model.Savjet
import java.time.LocalDate

/** Tipovi obroka koji imaju utor na pladnju (menza: doručak, ručak, večera). */
val TIPOVI_UTORA = listOf("DORUCAK", "RUCAK", "VECERA")

/** Užina nema utor; kad je unesena, na pladnju se pojavi kao tanjurić. */
const val TIP_UZINA = "UZINA"

private val REDOSLIJED_TIPOVA = listOf("DORUCAK", "RUCAK", "UZINA", "VECERA")

@Immutable
data class Makro(
    val kcal: Double,
    val proteini: Double,
    val masti: Double,
    val ugljikohidrati: Double
) {
    operator fun plus(o: Makro) = Makro(kcal + o.kcal, proteini + o.proteini, masti + o.masti, ugljikohidrati + o.ugljikohidrati)

    /** Udjeli kcal iz P (×4), M (×9) i U (×4) za rub tanjura; zbroj je 1 ili su svi 0. */
    fun udjeli(): Triple<Float, Float, Float> {
        val p = proteini * 4
        val m = masti * 9
        val u = ugljikohidrati * 4
        val ukupno = p + m + u
        if (ukupno <= 0.0) return Triple(0f, 0f, 0f)
        return Triple((p / ukupno).toFloat(), (m / ukupno).toFloat(), (u / ukupno).toFloat())
    }

    companion object {
        val NULA = Makro(0.0, 0.0, 0.0, 0.0)
    }
}

fun NutritivneVrijednosti.uMakro() = Makro(kalorije, proteini, masti, ugljikohidrati)

@Immutable
data class JeloUi(
    val naziv: String,
    val kolicina: Double,
    val makro: Makro
)

/**
 * Jedan spremljeni obrok. [vrijeme] je "HH:mm" ili null (lažni podaci bez vremena).
 * [izvor] je izvorni objekt s backenda (za brisanje, poništavanje i uređivanje); u previewima je null.
 */
@Immutable
data class ObrokUi(
    val id: Long,
    val tip: String,
    val vrijeme: String?,
    val makro: Makro,
    val jela: List<JeloUi>,
    val izvor: Obrok?
)

/** Utor na pladnju; [makro] je null kad je utor prazan. [vrijeme] je vrijeme prvog obroka tog tipa. */
@Immutable
data class TanjurUi(
    val tip: String,
    val makro: Makro?,
    val vrijeme: String? = null,
    val jeUzina: Boolean = false
)

@Immutable
data class DnevniPregledUi(
    val datum: LocalDate,
    val cilj: Makro?,
    val pojedeno: Makro,
    val obroci: List<ObrokUi>,
    val savjet: Savjet?
) {
    val tanjuri: List<TanjurUi> by lazy {
        val utori = TIPOVI_UTORA.map { tip -> TanjurUi(tip, zbrojTipa(tip), prvoVrijeme(tip)) }
        val uzina = zbrojTipa(TIP_UZINA)?.let { TanjurUi(TIP_UZINA, it, prvoVrijeme(TIP_UZINA), jeUzina = true) }
        // Užina stoji između ručka i večere, kao u danu
        if (uzina != null) utori.take(2) + uzina + utori.drop(2) else utori
    }

    val popunjeniUtori: Int get() = tanjuri.count { !it.jeUzina && it.makro != null }

    val jePrazan: Boolean get() = obroci.isEmpty()

    private fun prvoVrijeme(tip: String): String? = obroci.firstOrNull { it.tip == tip }?.vrijeme

    private fun zbrojTipa(tip: String): Makro? =
        obroci.filter { it.tip == tip }.takeIf { it.isNotEmpty() }?.fold(Makro.NULA) { a, o -> a + o.makro }
}

sealed interface DnevniPregledStanje {
    data object Ucitavanje : DnevniPregledStanje
    data object Greska : DnevniPregledStanje

    @Immutable
    data class Sadrzaj(val pregled: DnevniPregledUi) : DnevniPregledStanje
}

fun Obrok.uUi() = ObrokUi(
    id = id,
    tip = tipObroka.uppercase(),
    vrijeme = vrijeme.take(5),
    makro = Makro(ukupnoKalorije, ukupnoProteini, ukupnoMasti, ukupnoUgljikohidrati),
    jela = stavke.map { JeloUi(it.naziv, it.kolicina, Makro(it.kalorije, it.proteini, it.masti, it.ugljikohidrati)) },
    izvor = this
)

/**
 * Spaja dan-pregled (cilj, savjet) s obrocima dana (vrijeme, stavke, id za brisanje).
 * Pojedeno se računa iz obroka (iste vrijednosti koje backend zbraja), pa luk odmah prati brisanje i poništavanje;
 * dok obroci nisu stigli ([koristiObroke] = false i lista je prazna), koristi se zbroj iz dan-pregleda.
 */
fun spojiDnevniPregled(
    datum: LocalDate,
    danPregled: DanPregled,
    obroci: List<Obrok>,
    skriveniId: Set<Long>,
    koristiObroke: Boolean
): DnevniPregledUi {
    val vidljivi = obroci
        .filter { it.id !in skriveniId }
        .map { it.uUi() }
        .sortedWith(compareBy<ObrokUi>({ it.vrijeme ?: "" }, { REDOSLIJED_TIPOVA.indexOf(it.tip) }))

    val pojedeno = if (koristiObroke || obroci.isNotEmpty()) vidljivi.fold(Makro.NULA) { a, o -> a + o.makro }
    else danPregled.pojedeno.uMakro()

    return DnevniPregledUi(
        datum = datum,
        cilj = danPregled.cilj?.uMakro(),
        pojedeno = pojedeno,
        obroci = vidljivi,
        savjet = danPregled.savjet
    )
}
