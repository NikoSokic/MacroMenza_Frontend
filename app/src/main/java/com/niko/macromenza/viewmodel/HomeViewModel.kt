package com.niko.macromenza.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niko.macromenza.api.RetrofitInstance
import com.niko.macromenza.model.PovijestDana
import com.niko.macromenza.model.Preporuka
import com.niko.macromenza.model.UkupniUnos
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class HomeViewModel : ViewModel() {

    private val _ukupniUnos = MutableStateFlow<UkupniUnos?>(null)
    val ukupniUnos = _ukupniUnos.asStateFlow()

    private val _danasnjiObroci = MutableStateFlow<PovijestDana?>(null)
    val danasnjiObroci = _danasnjiObroci.asStateFlow()

    private val _preporuka = MutableStateFlow<Preporuka?>(null)
    val preporuka = _preporuka.asStateFlow()

    fun ucitajUkupniUnos(idKorisnik: Long) {
        viewModelScope.launch {
            try {
                _ukupniUnos.value =
                    RetrofitInstance.api.dohvatiUkupniUnos(idKorisnik)
            } catch (_: Exception) {
            }
        }
    }

    fun ucitajDanasnjeObroke(idKorisnik: Long) {
        viewModelScope.launch {
            try {
                val danas = LocalDate.now().toString()

                _danasnjiObroci.value =
                    RetrofitInstance.api.dohvatiPovijestZaDan(
                        idKorisnik = idKorisnik,
                        datum = danas
                    )
            } catch (_: Exception) {
            }
        }
    }

    fun ucitajPreporuku(idKorisnik: Long) {
        viewModelScope.launch {
            try {
                val preporuke =
                    RetrofitInstance.api.dohvatiPreporukeZaKorisnika(idKorisnik)

                _preporuka.value = preporuke.maxByOrNull { it.id ?: 0 }
            } catch (_: Exception) {
            }
        }
    }
}