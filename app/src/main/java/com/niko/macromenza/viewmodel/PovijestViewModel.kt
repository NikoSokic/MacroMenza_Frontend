package com.niko.macromenza.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niko.macromenza.api.RetrofitInstance
import com.niko.macromenza.model.Obrok
import com.niko.macromenza.model.ObrokRequest
import com.niko.macromenza.model.PovijestDana
import com.niko.macromenza.model.StavkaObrokaRequest
import com.niko.macromenza.model.Tjedan
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
import java.time.LocalDate

class PovijestViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _povijestDana = MutableStateFlow<PovijestDana?>(null)
    val povijestDana = _povijestDana.asStateFlow()

    private val _obrociDana = MutableStateFlow<List<Obrok>>(emptyList())
    val obrociDana = _obrociDana.asStateFlow()

    private val _tjedan = MutableStateFlow<Tjedan?>(null)
    val tjedan = _tjedan.asStateFlow()

    private val _greskaTjedna = MutableStateFlow(false)
    val greskaTjedna = _greskaTjedna.asStateFlow()

    /** Ponedjeljak odabranog tjedna (YYYY-MM-DD); null je tekući tjedan. Preživi i gašenje procesa. */
    private var odabraniPocetak: String?
        get() = savedStateHandle[KLJUC_POCETAK_TJEDNA]
        set(value) {
            savedStateHandle[KLJUC_POCETAK_TJEDNA] = value
        }

    private var ucitavanjeTjedna: Job? = null

    /** Obrok koji je upravo obrisan; ekran na to nudi "Poništi" i čisti događaj. */
    private val _obrisaniObrok = MutableStateFlow<Obrok?>(null)
    val obrisaniObrok = _obrisaniObrok.asStateFlow()

    private val _greskaBrisanja = MutableStateFlow<String?>(null)
    val greskaBrisanja = _greskaBrisanja.asStateFlow()

    private var brisanjeUTijeku = false

    private val _primjerUTijeku = MutableStateFlow(false)
    val primjerUTijeku = _primjerUTijeku.asStateFlow()


    fun ucitajDan(
        idKorisnik: Long,
        datum: String
    ) {
        viewModelScope.launch {
            try {
                _povijestDana.value =
                    RetrofitInstance.api.dohvatiPovijestZaDan(
                        idKorisnik = idKorisnik,
                        datum = datum
                    )
            } catch (_: Exception) {

            }
        }
        viewModelScope.launch {
            try {
                _obrociDana.value =
                    RetrofitInstance.api.getObroci(idKorisnik, datum).sortedBy { it.vrijeme }
            } catch (_: Exception) {
                _obrociDana.value = emptyList()
            }
        }
    }
    /** Učitava odabrani tjedan; novi zahtjev prekida prethodni pa brzo listanje ne prikaže krivi tjedan. */
    fun ucitajTjedan(idKorisnik: Long) {
        ucitavanjeTjedna?.cancel()
        ucitavanjeTjedna = viewModelScope.launch {
            try {
                _tjedan.value = RetrofitInstance.api.getTjedan(idKorisnik, odabraniPocetak)
                _greskaTjedna.value = false
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _greskaTjedna.value = true
            }
        }
    }

    fun prethodniTjedan(idKorisnik: Long) {
        val tjedan = _tjedan.value ?: return
        if (!tjedan.imaPrethodni) return
        odabraniPocetak = LocalDate.parse(tjedan.pocetak).minusWeeks(1).toString()
        ucitajTjedan(idKorisnik)
    }

    fun sljedeciTjedan(idKorisnik: Long) {
        val tjedan = _tjedan.value ?: return
        if (!tjedan.imaSljedeci) return
        odabraniPocetak = LocalDate.parse(tjedan.pocetak).plusWeeks(1).toString()
        ucitajTjedan(idKorisnik)
    }

    fun ovajTjedan(idKorisnik: Long) {
        odabraniPocetak = null
        ucitajTjedan(idKorisnik)
    }

    /** Briše obrok; lista se mijenja tek kad backend potvrdi, a greška ostavlja obrok na mjestu. */
    fun obrisiObrok(idKorisnik: Long, obrok: Obrok) {
        if (brisanjeUTijeku) return

        viewModelScope.launch {
            brisanjeUTijeku = true
            try {
                val odgovor = RetrofitInstance.api.obrisiObrok(obrok.id, idKorisnik)

                if (odgovor.isSuccessful) {
                    _obrociDana.value = _obrociDana.value.filter { it.id != obrok.id }
                    osvjezi(idKorisnik, obrok.datum)
                    _obrisaniObrok.value = obrok
                } else if (odgovor.code() == 404) {
                    _greskaBrisanja.value = "Obrok je već obrisan."
                    osvjezi(idKorisnik, obrok.datum)
                } else {
                    _greskaBrisanja.value = "Brisanje nije uspjelo (greška ${odgovor.code()})."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _greskaBrisanja.value = "Brisanje nije uspjelo. Provjeri vezu s poslužiteljem."
            } finally {
                brisanjeUTijeku = false
            }
        }
    }

    /** "Poništi": isti datum, vrijeme, tip, izvor i stavke spremaju se kao novi obrok (novi id). */
    fun vratiObrok(idKorisnik: Long, obrok: Obrok) {
        viewModelScope.launch {
            try {
                RetrofitInstance.api.spremiObrok(
                    ObrokRequest(
                        idKorisnik = idKorisnik,
                        datum = obrok.datum,
                        vrijeme = obrok.vrijeme,
                        tipObroka = obrok.tipObroka,
                        izvor = obrok.izvor,
                        stavke = obrok.stavke.map {
                            StavkaObrokaRequest(idJelo = it.idJelo, kolicina = it.kolicina)
                        }
                    )
                )
                osvjezi(idKorisnik, obrok.datum)
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                _greskaBrisanja.value = "Vraćanje nije uspjelo: " + (poruka(e) ?: "greška ${e.code()}.")
            } catch (e: Exception) {
                _greskaBrisanja.value = "Vraćanje nije uspjelo. Provjeri vezu s poslužiteljem."
            }
        }
    }

    /** Stvara primjer tjedna (obroci s izvorom PRIMJER) za dane bez unosa; greška ide u snackbar. */
    fun ucitajPrimjerTjedna(idKorisnik: Long) = primjerTjedna(
        idKorisnik,
        greska = "Primjer tjedna nije učitan."
    ) { RetrofitInstance.api.ucitajPrimjerTjedna(idKorisnik) }

    /** Briše samo obroke s izvorom PRIMJER. */
    fun ukloniPrimjerTjedna(idKorisnik: Long) = primjerTjedna(
        idKorisnik,
        greska = "Primjer tjedna nije uklonjen."
    ) { RetrofitInstance.api.ukloniPrimjerTjedna(idKorisnik) }

    private fun primjerTjedna(
        idKorisnik: Long,
        greska: String,
        poziv: suspend () -> retrofit2.Response<Unit>
    ) {
        if (_primjerUTijeku.value) return

        viewModelScope.launch {
            _primjerUTijeku.value = true
            try {
                val odgovor = poziv()
                if (!odgovor.isSuccessful) {
                    val poruka = try {
                        odgovor.errorBody()?.string()
                            ?.let { JSONObject(it).optString("poruka") }
                            ?.takeIf { it.isNotBlank() }
                    } catch (_: Exception) {
                        null
                    }
                    _greskaBrisanja.value = "$greska ${poruka ?: "(greška ${odgovor.code()})"}"
                }
                ucitajTjedan(idKorisnik)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _greskaBrisanja.value = "$greska Provjeri vezu s poslužiteljem."
            } finally {
                _primjerUTijeku.value = false
            }
        }
    }

    fun ocistiRezultatBrisanja() {
        _obrisaniObrok.value = null
        _greskaBrisanja.value = null
    }

    private fun osvjezi(idKorisnik: Long, datum: String) {
        ucitajDan(idKorisnik, datum)
        ucitajTjedan(idKorisnik)
    }

    private fun poruka(e: HttpException): String? =
        try {
            e.response()?.errorBody()?.string()
                ?.let { JSONObject(it).optString("poruka") }
                ?.takeIf { it.isNotBlank() }
        } catch (ex: Exception) {
            null
        }

    private companion object {
        const val KLJUC_POCETAK_TJEDNA = "pocetakTjedna"
    }
}
