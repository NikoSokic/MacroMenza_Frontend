package com.niko.macromenza.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niko.macromenza.R
import com.niko.macromenza.api.RetrofitInstance
import com.niko.macromenza.model.BrzoMjerenjeRequest
import com.niko.macromenza.model.Mjerenje
import com.niko.macromenza.model.MjerenjeMase
import com.niko.macromenza.model.Preporuka
import com.niko.macromenza.util.UiTekst
import com.niko.macromenza.util.formatKcal
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException

data class PorukaMjerenja(val tekst: UiTekst, val uspjeh: Boolean)

class MjereViewModel : ViewModel() {

    private val _mjerenja = MutableStateFlow<List<Mjerenje>>(emptyList())
    val mjerenja = _mjerenja.asStateFlow()

    /** Točke grafa: zadnja masa po danu, po datumu uzlazno. */
    private val _mase = MutableStateFlow<List<MjerenjeMase>>(emptyList())
    val mase = _mase.asStateFlow()

    private val _preporuke = MutableStateFlow<List<Preporuka>>(emptyList())
    val preporuke = _preporuke.asStateFlow()

    private val _greska = MutableStateFlow<String?>(null)
    val greska = _greska.asStateFlow()

    private val _sprema = MutableStateFlow(false)
    val sprema = _sprema.asStateFlow()

    /** Rezultat brzog mjerenja (uspjeh ili greška) ispod gumba "Spremi". */
    private val _poruka = MutableStateFlow<PorukaMjerenja?>(null)
    val poruka = _poruka.asStateFlow()

    fun ucitajMjerenja(idKorisnik: Long = 1) {
        viewModelScope.launch {
            try {
                _mjerenja.value =
                    RetrofitInstance.api.dohvatiMjerenjaZaKorisnika(idKorisnik)
                _mase.value = RetrofitInstance.api.getMase(idKorisnik)
                _preporuke.value = RetrofitInstance.api.dohvatiPreporukeZaKorisnika(idKorisnik)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _greska.value = e.message
            }
        }
    }

    /** Sprema današnju masu; [desetinke] su masa × 10 (npr. 724), da se izbjegne 72.30000000000001. */
    fun spremiBrzo(idKorisnik: Long, desetinke: Int) {
        if (_sprema.value) return

        viewModelScope.launch {
            _sprema.value = true
            _poruka.value = null
            try {
                val odgovor = RetrofitInstance.api.brzoMjerenje(
                    BrzoMjerenjeRequest(idKorisnik = idKorisnik, masa = desetinke / 10.0)
                )
                _poruka.value = PorukaMjerenja(
                    UiTekst.Resurs(R.string.mj_brzo_spremljeno, formatKcal(odgovor.preporuka.kalorije)),
                    uspjeh = true
                )
                ucitajMjerenja(idKorisnik)
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                _poruka.value = PorukaMjerenja(
                    porukaGreske(e)?.let { UiTekst.Resurs(R.string.mj_brzo_greska_poruka, it) }
                        ?: UiTekst.Resurs(R.string.mj_brzo_greska_http, e.code()),
                    uspjeh = false
                )
            } catch (e: Exception) {
                _poruka.value = PorukaMjerenja(
                    UiTekst.Resurs(R.string.kom_spremanje_greska_veza),
                    uspjeh = false
                )
            } finally {
                _sprema.value = false
            }
        }
    }

    private fun porukaGreske(e: HttpException): String? =
        try {
            e.response()?.errorBody()?.string()
                ?.let { JSONObject(it).optString("poruka") }
                ?.takeIf { it.isNotBlank() }
        } catch (ex: Exception) {
            null
        }
}
