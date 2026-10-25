package com.niko.macromenza.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niko.macromenza.api.RetrofitInstance
import com.niko.macromenza.model.CestoJedenoJelo
import com.niko.macromenza.model.Jelo
import com.niko.macromenza.model.KategorijaJela
import com.niko.macromenza.model.MeniDTO
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.niko.macromenza.model.Obrok
import com.niko.macromenza.model.ObrokRequest
import com.niko.macromenza.model.PovijestDana
import com.niko.macromenza.model.Preporuka
import com.niko.macromenza.model.StavkaObrokaRequest
import org.json.JSONObject
import retrofit2.HttpException
import java.time.LocalDate
import java.time.ZoneId


/** Primijenjeni rasponi makronutrijenata (po porciji); null znači bez ograničenja. */
data class MakroFilter(
    val minKalorije: Double? = null,
    val maxKalorije: Double? = null,
    val minProteini: Double? = null,
    val maxProteini: Double? = null,
    val minUgljikohidrati: Double? = null,
    val maxUgljikohidrati: Double? = null,
    val minMasti: Double? = null,
    val maxMasti: Double? = null
) {
    val aktivan: Boolean
        get() = listOf(
            minKalorije, maxKalorije, minProteini, maxProteini,
            minUgljikohidrati, maxUgljikohidrati, minMasti, maxMasti
        ).any { it != null }

    /** Isto pravilo kao na backendu: granice su uključive. */
    fun propusta(jelo: Jelo): Boolean =
        uOpsegu(jelo.kalorije, minKalorije, maxKalorije) &&
            uOpsegu(jelo.proteini, minProteini, maxProteini) &&
            uOpsegu(jelo.ugljikohidrati, minUgljikohidrati, maxUgljikohidrati) &&
            uOpsegu(jelo.masti, minMasti, maxMasti)

    private fun uOpsegu(vrijednost: Double, min: Double?, max: Double?): Boolean =
        (min == null || vrijednost >= min) && (max == null || vrijednost <= max)
}

/** Jelo u obroku koji se slaže; vrijednosti jela su po porciji. */
data class OdabranaStavka(
    val jelo: Jelo,
    val kolicina: Double
)

/** Odabir ostaje i kad pretraga ili kategorija makne jelo s liste; redoslijed je redoslijed dodavanja. */
data class OdabirObroka(
    val stavke: Map<Long, OdabranaStavka> = emptyMap(),
    // Jela koja su došla iz menija; dok ih ima, izvor obroka je MENI.
    val jelaIzMenija: Set<Long> = emptySet()
)

data class SazetakObroka(
    val brojStavki: Int = 0,
    val kalorije: Double = 0.0,
    val proteini: Double = 0.0,
    val ugljikohidrati: Double = 0.0,
    val masti: Double = 0.0,
    /** Pojedeno za odabrani datum; null dok se ne učita ili ako učitavanje ne uspije. */
    val pojedenoKalorije: Double? = null,
    /** null ako korisnik nema preporuku. */
    val ciljKalorije: Double? = null
) {
    val nakonObroka: Double?
        get() = pojedenoKalorije?.plus(kalorije)
}

private const val ODGODA_PRETRAGE_MS = 300L

private val ZAGREB = ZoneId.of("Europe/Zagreb")

enum class KarticaUnosa { JELA, MENIJI }

class JelaViewModel : ViewModel() {

    private var idKorisnik: Long? = null

    private val _odabir = MutableStateFlow(OdabirObroka())
    val odabir: StateFlow<OdabirObroka> = _odabir

    private val _datum = MutableStateFlow(LocalDate.now(ZAGREB))
    val datum: StateFlow<LocalDate> = _datum

    private val _pojedeno = MutableStateFlow<PovijestDana?>(null)

    private val _cilj = MutableStateFlow<Preporuka?>(null)

    private var ucitavanjePojedenog: Job? = null

    val sazetak: StateFlow<SazetakObroka> = combine(_odabir, _pojedeno, _cilj) { odabir, pojedeno, cilj ->
        val stavke = odabir.stavke.values
        SazetakObroka(
            brojStavki = stavke.size,
            kalorije = stavke.sumOf { it.jelo.kalorije * it.kolicina },
            proteini = stavke.sumOf { it.jelo.proteini * it.kolicina },
            ugljikohidrati = stavke.sumOf { it.jelo.ugljikohidrati * it.kolicina },
            masti = stavke.sumOf { it.jelo.masti * it.kolicina },
            pojedenoKalorije = pojedeno?.ukupnoKalorije,
            ciljKalorije = cilj?.kalorije
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SazetakObroka())

    private val _kartica = MutableStateFlow(KarticaUnosa.JELA)
    val kartica: StateFlow<KarticaUnosa> = _kartica

    private val _meniji = MutableStateFlow<List<MeniDTO>>(emptyList())
    val meniji: StateFlow<List<MeniDTO>> = _meniji

    private val _menijiUcitavanje = MutableStateFlow(false)
    val menijiUcitavanje: StateFlow<Boolean> = _menijiUcitavanje

    private val _greskaMenija = MutableStateFlow<String?>(null)
    val greskaMenija: StateFlow<String?> = _greskaMenija

    /** Tip menija (RUCAK / VECERA) kad tip obroka ne određuje filter; null = svi. */
    private val _tipMenija = MutableStateFlow<String?>(null)
    val tipMenija: StateFlow<String?> = _tipMenija

    private val _samoVegetarijanski = MutableStateFlow(false)
    val samoVegetarijanski: StateFlow<Boolean> = _samoVegetarijanski

    private var menijiUcitani = false

    private val _jela = MutableStateFlow<List<Jelo>>(emptyList())
    val jela: StateFlow<List<Jelo>> = _jela

    private val _upit = MutableStateFlow("")
    val upit: StateFlow<String> = _upit

    private val _kategorija = MutableStateFlow<String?>(null)
    val kategorija: StateFlow<String?> = _kategorija

    private val _makroFilter = MutableStateFlow(MakroFilter())
    val makroFilter: StateFlow<MakroFilter> = _makroFilter

    private val _kategorije = MutableStateFlow<List<KategorijaJela>>(emptyList())
    val kategorije: StateFlow<List<KategorijaJela>> = _kategorije

    private val _cestoJedeno = MutableStateFlow<List<CestoJedenoJelo>>(emptyList())
    val cestoJedeno: StateFlow<List<CestoJedenoJelo>> = _cestoJedeno

    private val _greska = MutableStateFlow<String?>(null)
    val greska: StateFlow<String?> = _greska

    private var ucitavanjeJela: Job? = null

    private val _spremanjeUTijeku = MutableStateFlow(false)
    val spremanjeUTijeku: StateFlow<Boolean> = _spremanjeUTijeku

    private val _spremljeniObrok = MutableStateFlow<Obrok?>(null)
    val spremljeniObrok: StateFlow<Obrok?> = _spremljeniObrok

    private val _greskaSpremanja = MutableStateFlow<String?>(null)
    val greskaSpremanja: StateFlow<String?> = _greskaSpremanja

    /**
     * Učitava jela prema upitu, kategoriji i makro filteru zajedno. Novi poziv otkazuje prethodni,
     * pa stariji (sporiji) odgovor ne može pregaziti noviji.
     */
    fun ucitajJela(odgodaMs: Long = 0L) {
        ucitavanjeJela?.cancel()
        ucitavanjeJela = viewModelScope.launch {
            if (odgodaMs > 0) delay(odgodaMs)

            val filter = _makroFilter.value

            try {
                _jela.value = RetrofitInstance.api.getJela(
                    search = _upit.value.trim().ifBlank { null },
                    kategorija = _kategorija.value,
                    minKalorije = filter.minKalorije,
                    maxKalorije = filter.maxKalorije,
                    minProteini = filter.minProteini,
                    maxProteini = filter.maxProteini,
                    minUgljikohidrati = filter.minUgljikohidrati,
                    maxUgljikohidrati = filter.maxUgljikohidrati,
                    minMasti = filter.minMasti,
                    maxMasti = filter.maxMasti
                )
                _greska.value = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _greska.value = e.message
            }
        }
    }

    fun postaviUpit(upit: String) {
        _upit.value = upit
        ucitajJela(ODGODA_PRETRAGE_MS)
    }

    fun postaviKategoriju(kategorija: String?) {
        if (_kategorija.value == kategorija) return
        _kategorija.value = kategorija
        ucitajJela()
    }

    fun primijeniMakroFilter(filter: MakroFilter) {
        _makroFilter.value = filter
        ucitajJela()
    }

    fun resetirajMakroFilter() {
        _makroFilter.value = MakroFilter()
        ucitajJela()
    }

    fun postaviKarticu(kartica: KarticaUnosa) {
        _kartica.value = kartica
        if (kartica == KarticaUnosa.MENIJI) ucitajMenije()
    }

    fun postaviTipMenija(tip: String?) {
        _tipMenija.value = tip
    }

    fun postaviSamoVegetarijanski(samo: Boolean) {
        _samoVegetarijanski.value = samo
    }

    /** Meniji se učitavaju jednom (svih 84), a filtriraju se lokalno. */
    fun ucitajMenije() {
        if (menijiUcitani || _menijiUcitavanje.value) return

        viewModelScope.launch {
            _menijiUcitavanje.value = true
            _greskaMenija.value = null
            try {
                _meniji.value = RetrofitInstance.api.getMeniji()
                menijiUcitani = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _greskaMenija.value = "Meniji se nisu učitali. Provjeri vezu s poslužiteljem."
            } finally {
                _menijiUcitavanje.value = false
            }
        }
    }

    fun ucitajKategorije() {
        viewModelScope.launch {
            try {
                _kategorije.value = RetrofitInstance.api.getKategorijeJela()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _greska.value = e.message
            }
        }
    }

    fun ucitajCestoJedeno(idKorisnik: Long) {
        viewModelScope.launch {
            try {
                _cestoJedeno.value = RetrofitInstance.api.getCestoJedeno(idKorisnik)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // "Često jedeš" je dodatak; ako ne uspije, sekcija se samo ne prikazuje.
                _cestoJedeno.value = emptyList()
            }
        }
    }

    fun postaviKorisnika(id: Long) {
        idKorisnik = id
        ucitajCestoJedeno(id)
        ucitajCilj(id)
        ucitajPojedeno()
    }

    fun postaviDatum(noviDatum: LocalDate) {
        if (_datum.value == noviDatum) return
        _datum.value = noviDatum
        ucitajPojedeno()
    }

    /** Pojedeno za odabrani datum; novi poziv otkazuje prethodni pa stariji datum ne može pregaziti noviji. */
    private fun ucitajPojedeno() {
        val id = idKorisnik ?: return
        val trazeniDatum = _datum.value

        ucitavanjePojedenog?.cancel()
        _pojedeno.value = null
        ucitavanjePojedenog = viewModelScope.launch {
            try {
                _pojedeno.value = RetrofitInstance.api.dohvatiPovijestZaDan(
                    idKorisnik = id,
                    datum = trazeniDatum.toString()
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Bez pojedenog se redak "Nakon ovog obroka" samo ne prikazuje.
                _pojedeno.value = null
            }
        }
    }

    private fun ucitajCilj(id: Long) {
        viewModelScope.launch {
            try {
                _cilj.value = RetrofitInstance.api.dohvatiPreporukeZaKorisnika(id)
                    .maxByOrNull { it.id ?: 0 }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _cilj.value = null
            }
        }
    }

    // Prvi + daje 1 porciju, dalje koraci po 0,5.
    fun povecaj(jelo: Jelo) = dodaj(jelo, if (kolicina(jelo.id) == 0.0) 1.0 else 0.5)

    // Iz detalja jela: odabrane porcije dodaju se na već odabranu količinu.
    fun dodaj(jelo: Jelo, porcije: Double) {
        val odabir = _odabir.value
        _odabir.value = odabir.copy(
            stavke = odabir.stavke + (jelo.id to OdabranaStavka(jelo, kolicina(jelo.id) + porcije))
        )
    }

    // − s 0,5 miče jelo iz obroka.
    fun smanji(idJelo: Long) {
        val trenutna = kolicina(idJelo)
        if (trenutna <= 0.0) return

        if (trenutna <= 0.5) {
            ukloni(idJelo)
        } else {
            val odabir = _odabir.value
            val stavka = odabir.stavke.getValue(idJelo)
            _odabir.value = odabir.copy(
                stavke = odabir.stavke + (idJelo to stavka.copy(kolicina = trenutna - 0.5))
            )
        }
    }

    fun ukloni(idJelo: Long) {
        val odabir = _odabir.value
        _odabir.value = odabir.copy(
            stavke = odabir.stavke - idJelo,
            jelaIzMenija = odabir.jelaIzMenija - idJelo
        )
    }

    /** Vraća odabir prije dodavanja, za "Poništi". */
    fun dodajMeni(meni: MeniDTO): OdabirObroka {
        val staro = _odabir.value
        val stavke = LinkedHashMap(staro.stavke)

        meni.stavke.forEach { s ->
            val postojeca = stavke[s.idJelo]
            // Vrijednosti stavke menija su za cijelu količinu, a odabir računa po porciji.
            val jelo = postojeca?.jelo ?: Jelo(
                id = s.idJelo,
                naziv = s.nazivJela,
                opis = null,
                kalorije = s.kalorije / s.kolicina,
                proteini = s.proteini / s.kolicina,
                ugljikohidrati = s.ugljikohidrati / s.kolicina,
                masti = s.masti / s.kolicina,
                sifra = null,
                kategorija = null,
                cijenaEur = null,
                napomena = null
            )
            stavke[s.idJelo] = OdabranaStavka(jelo, (postojeca?.kolicina ?: 0.0) + s.kolicina)
        }

        _odabir.value = OdabirObroka(
            stavke = stavke,
            jelaIzMenija = staro.jelaIzMenija + meni.stavke.map { it.idJelo }
        )

        return staro
    }

    fun vratiOdabir(staro: OdabirObroka) {
        _odabir.value = staro
    }

    private fun kolicina(idJelo: Long): Double = _odabir.value.stavke[idJelo]?.kolicina ?: 0.0

    /** Sprema cijeli obrok jednim pozivom; backend sprema sve stavke ili nijednu. */
    fun spremiObrok(vrijeme: String, tipObroka: String) {
        val id = idKorisnik ?: return
        val odabir = _odabir.value
        if (odabir.stavke.isEmpty() || _spremanjeUTijeku.value) return

        viewModelScope.launch {
            _spremanjeUTijeku.value = true
            try {
                _spremljeniObrok.value = RetrofitInstance.api.spremiObrok(
                    ObrokRequest(
                        idKorisnik = id,
                        datum = _datum.value.toString(),
                        vrijeme = vrijeme,
                        tipObroka = tipObroka,
                        izvor = if (odabir.jelaIzMenija.isNotEmpty()) "MENI" else "RUCNO",
                        stavke = odabir.stavke.values.map {
                            StavkaObrokaRequest(idJelo = it.jelo.id, kolicina = it.kolicina)
                        }
                    )
                )
                // Obrok je spremljen: prazan odabir sprječava dvostruko spremanje nakon povratka na ekran.
                _odabir.value = OdabirObroka()
                ucitajPojedeno()
                ucitajCestoJedeno(id)
            } catch (e: HttpException) {
                _greskaSpremanja.value = porukaGreske(e)
            } catch (e: Exception) {
                _greskaSpremanja.value = "Spremanje nije uspjelo. Provjeri vezu s poslužiteljem."
            } finally {
                _spremanjeUTijeku.value = false
            }
        }
    }

    fun ocistiRezultatSpremanja() {
        _spremljeniObrok.value = null
        _greskaSpremanja.value = null
    }

    private fun porukaGreske(e: HttpException): String {
        val poruka = try {
            e.response()?.errorBody()?.string()?.let { JSONObject(it).optString("poruka") }
        } catch (ex: Exception) {
            null
        }

        return if (poruka.isNullOrBlank()) {
            "Spremanje nije uspjelo (greška ${e.code()})."
        } else {
            poruka
        }
    }

}