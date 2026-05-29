package com.niko.macromenza.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niko.macromenza.api.RetrofitInstance
import com.niko.macromenza.model.Mjerenje
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MjereViewModel : ViewModel() {

    private val _mjerenja = MutableStateFlow<List<Mjerenje>>(emptyList())
    val mjerenja = _mjerenja.asStateFlow()

    private val _greska = MutableStateFlow<String?>(null)
    val greska = _greska.asStateFlow()

    fun ucitajMjerenja(idKorisnik: Long = 1) {
        viewModelScope.launch {
            try {
                _mjerenja.value =
                    RetrofitInstance.api.dohvatiMjerenjaZaKorisnika(idKorisnik)
            } catch (e: Exception) {
                _greska.value = e.message
            }
        }
    }
}