package com.niko.macromenza.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niko.macromenza.api.RetrofitInstance
import com.niko.macromenza.model.Obrok
import com.niko.macromenza.model.ObrokRequest
import com.niko.macromenza.model.PovijestDana
import com.niko.macromenza.model.StavkaObrokaRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.niko.macromenza.model.TjedniPregled
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException

class PovijestViewModel : ViewModel() {

    private val _povijestDana = MutableStateFlow<PovijestDana?>(null)
    val povijestDana = _povijestDana.asStateFlow()

    private val _obrociDana = MutableStateFlow<List<Obrok>>(emptyList())
    val obrociDana = _obrociDana.asStateFlow()

    private val _tjedniPregled = MutableStateFlow<TjedniPregled?>(null)
    val tjedniPregled = _tjedniPregled.asStateFlow()

    /** Obrok koji je upravo obrisan; ekran na to nudi "Poništi" i čisti događaj. */
    private val _obrisaniObrok = MutableStateFlow<Obrok?>(null)
    val obrisaniObrok = _obrisaniObrok.asStateFlow()

    private val _greskaBrisanja = MutableStateFlow<String?>(null)
    val greskaBrisanja = _greskaBrisanja.asStateFlow()

    private var brisanjeUTijeku = false


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
    fun ucitajTjedniPregled(
        idKorisnik: Long
    ) {
        viewModelScope.launch {
            try {
                _tjedniPregled.value =
                    RetrofitInstance.api.dohvatiTjedniPregled(idKorisnik)
            } catch (_: Exception) {
            }
        }
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

    fun ocistiRezultatBrisanja() {
        _obrisaniObrok.value = null
        _greskaBrisanja.value = null
    }

    private fun osvjezi(idKorisnik: Long, datum: String) {
        ucitajDan(idKorisnik, datum)
        ucitajTjedniPregled(idKorisnik)
    }

    private fun poruka(e: HttpException): String? =
        try {
            e.response()?.errorBody()?.string()
                ?.let { JSONObject(it).optString("poruka") }
                ?.takeIf { it.isNotBlank() }
        } catch (ex: Exception) {
            null
        }

}
