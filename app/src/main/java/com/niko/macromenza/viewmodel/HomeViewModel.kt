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


    private val _ucitava = MutableStateFlow(false)
    val ucitava = _ucitava.asStateFlow()

    fun ucitajHome(idKorisnik: Long) {
        viewModelScope.launch {
            _ucitava.value = true

            try {
                val danas = LocalDate.now().toString()

                _ukupniUnos.value =
                    RetrofitInstance.api.dohvatiUkupniUnos(idKorisnik)

                _danasnjiObroci.value =
                    RetrofitInstance.api.dohvatiPovijestZaDan(
                        idKorisnik = idKorisnik,
                        datum = danas
                    )

                val preporuke =
                    RetrofitInstance.api.dohvatiPreporukeZaKorisnika(idKorisnik)

                _preporuka.value =
                    preporuke.maxByOrNull { it.id ?: 0 }

            } catch (_: Exception) {
            } finally {
                _ucitava.value = false
            }
        }
    }
}