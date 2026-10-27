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
    val onRaspon: () -> Unit = {},
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

/** Dio naziva koji odgovara upitu (za podebljanje), ili null. */
fun pogodakUpita(naziv: String, upit: String): IntRange? {
    val trazeno = upit.trim()
    if (trazeno.isEmpty()) return null
    val n = String(CharArray(naziv.length) { osnovnoSlovo(naziv[it]) })
    val u = String(CharArray(trazeno.length) { osnovnoSlovo(trazeno[it]) })
    val i = n.indexOf(u)
    return if (i < 0) null else i until i + u.length
}
