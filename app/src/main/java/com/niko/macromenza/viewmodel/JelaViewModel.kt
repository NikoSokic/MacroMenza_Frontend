package com.niko.macromenza.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niko.macromenza.api.RetrofitInstance
import com.niko.macromenza.model.Jelo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.niko.macromenza.model.KonzumacijaRequest


class JelaViewModel : ViewModel() {

    private val _jela = MutableStateFlow<List<Jelo>>(emptyList())
    val jela: StateFlow<List<Jelo>> = _jela

    private val _greska = MutableStateFlow<String?>(null)
    val greska: StateFlow<String?> = _greska

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

    fun spremiKonzumaciju(idJelo: Long, kolicina: Double, tipObroka: String) {
        viewModelScope.launch {
            try {
                val request = KonzumacijaRequest(
                    idJelo = idJelo,
                    kolicina = kolicina.toInt(),
                    tipObroka = tipObroka
                )

                RetrofitInstance.api.spremiKonzumaciju(request)
            } catch (e: Exception) {
                _greska.value = e.message
            }
        }
    }

    fun filtrirajJela(
        minKalorije: Int?,
        maxKalorije: Int?,
        minProteini: Int?,
        maxProteini: Int?,
        minUgljikohidrati: Int?,
        maxUgljikohidrati: Int?,
        minMasti: Int?,
        maxMasti: Int?
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