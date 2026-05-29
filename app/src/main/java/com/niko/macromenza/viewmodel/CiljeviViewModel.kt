package com.niko.macromenza.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niko.macromenza.api.RetrofitInstance
import com.niko.macromenza.model.Mjerenje
import com.niko.macromenza.model.ProfilPodaci
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class CiljeviViewModel : ViewModel() {

    private val _poruka = MutableStateFlow<String?>(null)
    val poruka = _poruka.asStateFlow()

    fun spremiCilj(
        idKorisnik: Long = 1,
        visina: Int,
        dob: Int,
        masa: Double,
        razinaAktivnosti: String,
        tipCilja: String
    ) {
        viewModelScope.launch {
            try {
                val stariProfil =
                    RetrofitInstance.api.dohvatiProfilPoKorisniku(idKorisnik)

                if (stariProfil.id != null) {
                    val azuriraniProfil = ProfilPodaci(
                        id = stariProfil.id,
                        idKorisnik = idKorisnik,
                        visina = visina,
                        spol = stariProfil.spol,
                        dob = dob
                    )

                    RetrofitInstance.api.azurirajProfil(
                        id = stariProfil.id,
                        profil = azuriraniProfil
                    )
                }

                val mjerenje = Mjerenje(
                    idKorisnik = idKorisnik,
                    masa = masa,
                    razinaAktivnosti = razinaAktivnosti,
                    tipCilja = tipCilja,
                    datum = LocalDate.now().toString()
                )

                RetrofitInstance.api.dodajMjerenje(mjerenje)

                RetrofitInstance.api.izracunajPreporuku(idKorisnik)

                _poruka.value = "Cilj i preporuka spremljeni"

            } catch (e: Exception) {
                _poruka.value = e.message
            }
        }
    }
}