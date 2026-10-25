package com.niko.macromenza.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niko.macromenza.api.RetrofitInstance
import com.niko.macromenza.model.CestoJedenoJelo
import com.niko.macromenza.model.Jelo
import com.niko.macromenza.model.KategorijaJela
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.niko.macromenza.model.Obrok
import com.niko.macromenza.model.ObrokRequest
import com.niko.macromenza.model.StavkaObrokaRequest
import org.json.JSONObject
import retrofit2.HttpException


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

private const val ODGODA_PRETRAGE_MS = 300L

class JelaViewModel : ViewModel() {

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

    /** Sprema cijeli obrok jednim pozivom; backend sprema sve stavke ili nijednu. */
    fun spremiObrok(
        idKorisnik: Long,
        datum: String,
        vrijeme: String,
        tipObroka: String,
        stavke: List<StavkaObrokaRequest>
    ) {
        if (_spremanjeUTijeku.value) return

        viewModelScope.launch {
            _spremanjeUTijeku.value = true
            try {
                _spremljeniObrok.value = RetrofitInstance.api.spremiObrok(
                    ObrokRequest(
                        idKorisnik = idKorisnik,
                        datum = datum,
                        vrijeme = vrijeme,
                        tipObroka = tipObroka,
                        stavke = stavke
                    )
                )
                ucitajCestoJedeno(idKorisnik)
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