package com.niko.macromenza.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niko.macromenza.api.RetrofitInstance
import com.niko.macromenza.model.JeloDetalj
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

sealed interface DetaljStanje {
    data object Ucitavanje : DetaljStanje
    data class Greska(val poruka: String) : DetaljStanje
    data class Podaci(val detalj: JeloDetalj) : DetaljStanje
}

class JeloDetaljViewModel : ViewModel() {

    private val _stanje = MutableStateFlow<DetaljStanje>(DetaljStanje.Ucitavanje)
    val stanje: StateFlow<DetaljStanje> = _stanje

    private var ucitavanje: Job? = null

    /** Novi poziv otkazuje prethodni, pa se ne može prikazati detalj krivog jela. */
    fun ucitaj(idJelo: Long) {
        ucitavanje?.cancel()
        _stanje.value = DetaljStanje.Ucitavanje
        ucitavanje = viewModelScope.launch {
            _stanje.value = try {
                DetaljStanje.Podaci(RetrofitInstance.api.getJeloDetalj(idJelo))
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                DetaljStanje.Greska(
                    if (e.code() == 404) "Jelo nije pronađeno."
                    else "Detalj nije učitan (greška ${e.code()})."
                )
            } catch (e: Exception) {
                DetaljStanje.Greska("Detalj nije učitan. Provjeri vezu s poslužiteljem.")
            }
        }
    }
}
