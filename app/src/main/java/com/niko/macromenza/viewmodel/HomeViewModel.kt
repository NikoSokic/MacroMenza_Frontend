package com.niko.macromenza.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niko.macromenza.api.RetrofitInstance
import com.niko.macromenza.model.UkupniUnos
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.niko.macromenza.model.PovijestDana
import java.time.LocalDate


class HomeViewModel : ViewModel() {

    private val _ukupniUnos = MutableStateFlow<UkupniUnos?>(null)
    val ukupniUnos = _ukupniUnos.asStateFlow()
    private val _danasnjiObroci = MutableStateFlow<PovijestDana?>(null)
    val danasnjiObroci = _danasnjiObroci.asStateFlow()


    fun ucitajUkupniUnos() {
        viewModelScope.launch {
            try {
                _ukupniUnos.value = RetrofitInstance.api.dohvatiUkupniUnos()
            } catch (_: Exception) {
            }
        }
    }
    fun ucitajDanasnjeObroke() {
        viewModelScope.launch {
            try {
                val danas = LocalDate.now().toString()
                _danasnjiObroci.value =
                    RetrofitInstance.api.dohvatiPovijestZaDan(danas)
            } catch (_: Exception) {
            }
        }
    }

}