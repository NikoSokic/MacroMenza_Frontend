package com.niko.macromenza.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niko.macromenza.api.RetrofitInstance
import com.niko.macromenza.model.Korisnik
import com.niko.macromenza.model.Mjerenje
import com.niko.macromenza.model.ProfilPodaci
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class ProfilViewModel : ViewModel() {

    private val _korisnik = MutableStateFlow<Korisnik?>(null)
    val korisnik = _korisnik.asStateFlow()

    private val _profil = MutableStateFlow<ProfilPodaci?>(null)
    val profil = _profil.asStateFlow()

    private val _poruka = MutableStateFlow<String?>(null)
    val poruka = _poruka.asStateFlow()

    fun ucitajProfil(idKorisnik: Long = 1) {
        viewModelScope.launch {
            try {
                _korisnik.value = RetrofitInstance.api.dohvatiKorisnika(idKorisnik)
                _profil.value = RetrofitInstance.api.dohvatiProfilPoKorisniku(idKorisnik)
            } catch (e: Exception) {
                _poruka.value = e.message
            }
        }
    }

    fun spremiProfil(
        idKorisnik: Long = 1,
        ime: String,
        prezime: String,
        visina: Int,
        masa: Double,
        dob: Int,
        spol: String
    ) {
        viewModelScope.launch {
            try {
                val stariKorisnik = _korisnik.value

                val azuriraniKorisnik = Korisnik(
                    id = idKorisnik,
                    ime = ime,
                    prezime = prezime,
                    email = stariKorisnik?.email ?: "test@test.com",
                    lozinka_hash = stariKorisnik?.lozinka_hash ?: "test"
                )

                RetrofitInstance.api.azurirajKorisnika(
                    id = idKorisnik,
                    korisnik = azuriraniKorisnik
                )

                val stariProfil = _profil.value

                if (stariProfil != null && stariProfil.id != null) {
                    val azuriraniProfil = ProfilPodaci(
                        id = stariProfil.id,
                        idKorisnik = idKorisnik,
                        visina = visina,
                        spol = spol,
                        dob = dob
                    )

                    RetrofitInstance.api.azurirajProfil(
                        id = stariProfil.id,
                        profil = azuriraniProfil
                    )
                }

                val novoMjerenje = Mjerenje(
                    idKorisnik = idKorisnik,
                    masa = masa,
                    razinaAktivnosti = "umjerena",
                    tipCilja = "odrzavanje",
                    datum = LocalDate.now().toString()
                )

                RetrofitInstance.api.dodajMjerenje(novoMjerenje)

                _poruka.value = "Profil spremljen"
                ucitajProfil(idKorisnik)

            } catch (e: Exception) {
                _poruka.value = e.message
            }
        }
    }
}