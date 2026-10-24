package com.niko.macromenza.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niko.macromenza.api.RetrofitInstance
import com.niko.macromenza.model.Jelo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.niko.macromenza.model.Obrok
import com.niko.macromenza.model.ObrokRequest
import com.niko.macromenza.model.StavkaObrokaRequest
import org.json.JSONObject
import retrofit2.HttpException


class JelaViewModel : ViewModel() {

    private val _jela = MutableStateFlow<List<Jelo>>(emptyList())
    val jela: StateFlow<List<Jelo>> = _jela

    private val _greska = MutableStateFlow<String?>(null)
    val greska: StateFlow<String?> = _greska

    private val _spremanjeUTijeku = MutableStateFlow(false)
    val spremanjeUTijeku: StateFlow<Boolean> = _spremanjeUTijeku

    private val _spremljeniObrok = MutableStateFlow<Obrok?>(null)
    val spremljeniObrok: StateFlow<Obrok?> = _spremljeniObrok

    private val _greskaSpremanja = MutableStateFlow<String?>(null)
    val greskaSpremanja: StateFlow<String?> = _greskaSpremanja

    fun ucitajJela(
        search: String? = null,
        tipObroka: String? = null
    ) {
        viewModelScope.launch {
            try {
                _jela.value =
                    RetrofitInstance.api.getJela(
                        search = search,
                        tipObroka = tipObroka
                    )

            } catch (e: Exception) {
                _greska.value = e.message
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

    fun filtrirajJela(
        minKalorije: Double?,
        maxKalorije: Double?,
        minProteini: Double?,
        maxProteini: Double?,
        minUgljikohidrati: Double?,
        maxUgljikohidrati: Double?,
        minMasti: Double?,
        maxMasti: Double?
    ) {
        viewModelScope.launch {
            try {
                _jela.value = RetrofitInstance.api.filtrirajJela(
                    minKalorije = minKalorije,
                    maxKalorije = maxKalorije,
                    minProteini = minProteini,
                    maxProteini = maxProteini,
                    minUgljikohidrati = minUgljikohidrati,
                    maxUgljikohidrati = maxUgljikohidrati,
                    minMasti = minMasti,
                    maxMasti = maxMasti
                )
            } catch (e: Exception) {
                _greska.value = e.message
            }
        }
    }




}