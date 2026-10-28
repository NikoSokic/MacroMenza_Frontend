package com.niko.macromenza.screens.unosobroka

import androidx.compose.runtime.Immutable
import com.niko.macromenza.model.CestoJedenoJelo
import com.niko.macromenza.model.Jelo
import com.niko.macromenza.model.KategorijaJela
import com.niko.macromenza.model.MeniDTO
import com.niko.macromenza.ui.components.Makro
import com.niko.macromenza.viewmodel.MakroFilter
import java.text.Normalizer
import java.time.LocalDate
import java.time.LocalTime

sealed interface StanjeJela {
    data object Ucitavanje : StanjeJela
    data object Greska : StanjeJela

    @Immutable
    data class Sadrzaj(val jela: List<Jelo>) : StanjeJela
}

sealed interface StanjeMenija {
    data object Ucitavanje : StanjeMenija

    @Immutable
    data class Greska(val poruka: String) : StanjeMenija

    @Immutable
    data class Sadrzaj(val meniji: List<MeniDTO>) : StanjeMenija
}

/** Sve što prvi dio unosa obroka prikazuje; UnosObrokaScreen ga slaže iz JelaViewModela, previewi iz primjera. */
@Immutable
data class UnosObrokaUi(
    val tip: String,
    val uredivanje: Boolean,
    val ucitavanjeObroka: Boolean,
    val mozeBrisati: Boolean,
    val spremanjeUTijeku: Boolean,
    val datum: LocalDate,
    val danas: LocalDate,
    val vrijeme: LocalTime,
    /** Pojedeno na odabrani datum zajedno s ovim obrokom; null dok se ne učita. */
    val nakonObroka: Double?,
    val ciljKalorije: Double?,
    val upit: String,
    val kategorija: String?,
    val kategorije: List<KategorijaJela>,
    val makroFilter: MakroFilter,
    val jela: StanjeJela,
    val cesto: List<CestoJedenoJelo>,
    val meniji: StanjeMenija,
    val tipMenija: String?,
    val samoVegetarijanski: Boolean,
    /** Količina po id-u jela za jela koja su već u obroku. */
    val kolicine: Map<Long, Double>
)

class UnosObrokaAkcije(
    val onNatrag: () -> Unit = {},
    val onTip: (String) -> Unit = {},
    val onDatum: () -> Unit = {},
    val onVrijeme: () -> Unit = {},
    val onObrisi: () -> Unit = {},
    val onSlikaIliOpis: () -> Unit = {},
    val onUpit: (String) -> Unit = {},
    val onKategorija: (String?) -> Unit = {},
    val onMakroFilter: (MakroFilter) -> Unit = {},
    val onFilteri: () -> Unit = {},
    val onDodaj: (Jelo) -> Unit = {},
    val onDetalj: (Jelo) -> Unit = {},
    val onDodajMeni: (MeniDTO) -> Unit = {},
    val onTipMenija: (String?) -> Unit = {},
    val onVegetarijanski: (Boolean) -> Unit = {},
    val onPonoviJela: () -> Unit = {},
    val onPonoviMenije: () -> Unit = {}
)

fun Jelo.uMakro() = Makro(kalorije, proteini, masti, ugljikohidrati)

/** Skupina jela pod jednim naslovom; [kategorija] null znači jela bez kategorije ("Ostalo"). */
@Immutable
data class SkupinaJela(val kategorija: String?, val jela: List<Jelo>)

/** Grupira jela po kategoriji, redoslijedom kategorija s backenda; nepoznate idu na kraj, a jela bez kategorije zadnja. */
fun grupirajPoKategoriji(jela: List<Jelo>, kategorije: List<KategorijaJela>): List<SkupinaJela> {
    val redoslijed = kategorije.mapIndexed { i, k -> k.naziv to i }.toMap()
    return jela
        .groupBy { it.kategorija }
        .entries
        .sortedWith(compareBy<Map.Entry<String?, List<Jelo>>>({ it.key == null }, { redoslijed[it.key] ?: Int.MAX_VALUE }, { it.key }))
        .map { SkupinaJela(it.key, it.value) }
}

/** Slovo bez dijakritike i malim slovom, da "cevapi" pronađe "Ćevapi"; duljina naziva ostaje ista. */
private fun osnovnoSlovo(c: Char): Char = when (c) {
    'đ', 'Đ' -> 'd'
    else -> Normalizer.normalize(c.toString(), Normalizer.Form.NFD).first().lowercaseChar()
}

/** Dio naziva koji odgovara upitu (za oznaku pogotka), ili null. */
fun pogodakUpita(naziv: String, upit: String): IntRange? {
    val trazeno = upit.trim()
    if (trazeno.isEmpty()) return null
    val n = String(CharArray(naziv.length) { osnovnoSlovo(naziv[it]) })
    val u = String(CharArray(trazeno.length) { osnovnoSlovo(trazeno[it]) })
    // Isti pogodak kao rang: najprije početak riječi, inače prvi bilo gdje.
    var i = n.indexOf(u)
    var prvi = i
    while (i > 0 && n[i - 1].isLetterOrDigit()) i = n.indexOf(u, i + 1)
    if (i >= 0) prvi = i
    return if (prvi < 0) null else prvi until prvi + u.length
}

/** Rang pogotka u nazivu (manji je bolji): naziv počinje upitom, neka riječ počinje upitom, naziv sadrži upit. */
private const val RANG_POCETAK = 0
private const val RANG_RIJEC = 1
private const val RANG_SADRZI = 2
private const val RANG_BEZ_POGOTKA = 3

private fun presloziNaziv(tekst: String): String = String(CharArray(tekst.length) { osnovnoSlovo(tekst[it]) })

fun rangPogotka(naziv: String, upit: String): Int {
    val u = presloziNaziv(upit.trim())
    if (u.isEmpty()) return RANG_BEZ_POGOTKA
    val n = presloziNaziv(naziv)
    if (n.startsWith(u)) return RANG_POCETAK
    var i = n.indexOf(u)
    if (i < 0) return RANG_BEZ_POGOTKA
    while (i >= 0) {
        if (i == 0 || !n[i - 1].isLetterOrDigit()) return RANG_RIJEC
        i = n.indexOf(u, i + 1)
    }
    return RANG_SADRZI
}

/**
 * Rezultati pretrage kao jedna lista po relevantnosti: početak naziva, pa početak riječi, pa bilo gdje
 * u nazivu (backend može vratiti i jela koja se poklapaju drugdje; ona idu na kraj). Unutar istog ranga
 * prednost imaju jela iz "Često jedeš" (češća prva), a zatim ostaje redoslijed s backenda.
 */
fun rangirajPoUpitu(jela: List<Jelo>, upit: String, cesto: List<CestoJedenoJelo>): List<Jelo> {
    val puta = cesto.associate { it.jelo.id to it.brojPuta }
    return jela
        .withIndex()
        .sortedWith(
            compareBy<IndexedValue<Jelo>>(
                { rangPogotka(it.value.naziv, upit) },
                { -(puta[it.value.id] ?: 0) },
                { it.index }
            )
        )
        .map { it.value }
}

/** Broj aktivnih filtera: svaka vrijednost (kcal, P, U, M) s donjom ili gornjom granicom broji se jednom. */
fun brojAktivnihFiltera(f: MakroFilter): Int = listOf(
    f.minKalorije to f.maxKalorije,
    f.minProteini to f.maxProteini,
    f.minUgljikohidrati to f.maxUgljikohidrati,
    f.minMasti to f.maxMasti
).count { (min, max) -> min != null || max != null }
