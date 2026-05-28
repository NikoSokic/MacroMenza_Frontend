package com.niko.macromenza.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niko.macromenza.api.RetrofitInstance
import com.niko.macromenza.model.Mjerenje
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
class CiljeviViewModel : ViewModel() {

    private val _poruka = MutableStateFlow<String?>(null)
    val poruka = _poruka.asStateFlow()

    fun spremiCilj(
        idKorisnik: Long = 1,
        masa: Double,
        razinaAktivnosti: String,
        tipCilja: String
    ) {
        viewModelScope.launch {
            try {
                val mjerenje = Mjerenje(
                    idKorisnik = idKorisnik,
                    masa = masa,
                    razinaAktivnosti = razinaAktivnosti,
                    tipCilja = tipCilja,
                    datum = LocalDate.now().toString()
                )

                RetrofitInstance.api.dodajMjerenje(mjerenje)

                _poruka.value = "Cilj spremljen"
            } catch (e: Exception) {
                _poruka.value = e.message
            }
        }
    }
}