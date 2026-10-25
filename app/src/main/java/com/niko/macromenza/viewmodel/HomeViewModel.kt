package com.niko.macromenza.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niko.macromenza.api.RetrofitInstance
import com.niko.macromenza.model.DanPregled
import com.niko.macromenza.model.PovijestDana
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class HomeViewModel : ViewModel() {

    private val _danPregled = MutableStateFlow<DanPregled?>(null)
    val danPregled = _danPregled.asStateFlow()

    private val _danasnjiObroci = MutableStateFlow<PovijestDana?>(null)
    val danasnjiObroci = _danasnjiObroci.asStateFlow()

    private val _ucitava = MutableStateFlow(false)
    val ucitava = _ucitava.asStateFlow()

    /** Pri prvom učitavanju ekran je prazan pa se prikazuje splash; kasnija osvježavanja idu tiho u pozadini. */
    fun ucitajHome(idKorisnik: Long) {
        viewModelScope.launch {
            _ucitava.value = _danPregled.value == null

            try {
                val danas = LocalDate.now(ZoneId.of("Europe/Zagreb")).toString()

                _danPregled.value =
                    RetrofitInstance.api.getDanPregled(idKorisnik = idKorisnik, datum = danas)

                _danasnjiObroci.value =
                    RetrofitInstance.api.dohvatiPovijestZaDan(
                        idKorisnik = idKorisnik,
                        datum = danas
                    )

            } catch (_: Exception) {
            } finally {
                _ucitava.value = false
            }
        }
    }
}
