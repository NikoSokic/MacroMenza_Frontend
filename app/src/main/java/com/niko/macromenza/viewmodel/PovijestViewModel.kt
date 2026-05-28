package com.niko.macromenza.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niko.macromenza.api.RetrofitInstance
import com.niko.macromenza.model.PovijestDana
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.niko.macromenza.model.TjedniPregled
import kotlinx.coroutines.launch

class PovijestViewModel : ViewModel() {

    private val _dani = MutableStateFlow<List<String>>(emptyList())
    val dani = _dani.asStateFlow()

    private val _povijestDana = MutableStateFlow<PovijestDana?>(null)
    val povijestDana = _povijestDana.asStateFlow()

    private val _tjedniPregled = MutableStateFlow<TjedniPregled?>(null)
    val tjedniPregled = _tjedniPregled.asStateFlow()


    fun ucitajDane() {
        viewModelScope.launch {
            try {
                _dani.value = RetrofitInstance.api.dohvatiDanePovijesti()
            } catch (_: Exception) {

            }
        }
    }

    fun ucitajDan(datum: String) {
        viewModelScope.launch {
            try {
                _povijestDana.value =
                    RetrofitInstance.api.dohvatiPovijestZaDan(datum)
            } catch (_: Exception) {

            }
        }
    }
    fun ucitajTjedniPregled() {
        viewModelScope.launch {
            try {
                _tjedniPregled.value = RetrofitInstance.api.dohvatiTjedniPregled()
            } catch (_: Exception) {
            }
        }
    }

}