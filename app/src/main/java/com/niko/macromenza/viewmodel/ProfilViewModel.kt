package com.niko.macromenza.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niko.macromenza.api.RetrofitInstance
import com.niko.macromenza.model.Korisnik
import com.niko.macromenza.model.Mjerenje
import com.niko.macromenza.model.ProfilPodaci
import com.niko.macromenza.model.Preporuka
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfilViewModel : ViewModel() {

    private val _korisnik = MutableStateFlow<Korisnik?>(null)
    val korisnik = _korisnik.asStateFlow()

    private val _profil = MutableStateFlow<ProfilPodaci?>(null)
    val profil = _profil.asStateFlow()

    private val _poruka = MutableStateFlow<String?>(null)
    val poruka = _poruka.asStateFlow()

    private val _zadnjeMjerenje = MutableStateFlow<Mjerenje?>(null)
    val zadnjeMjerenje = _zadnjeMjerenje.asStateFlow()

    private val _zadnjaPreporuka = MutableStateFlow<Preporuka?>(null)
    val zadnjaPreporuka = _zadnjaPreporuka.asStateFlow()

    private val _ucitava = MutableStateFlow(false)
    val ucitava = _ucitava.asStateFlow()

    fun ucitajProfil(idKorisnik: Long = 1) {
        viewModelScope.launch {
            _ucitava.value = true
            _poruka.value = null

            try {
                _korisnik.value = RetrofitInstance.api.dohvatiKorisnika(idKorisnik)

                try {
                    _profil.value = RetrofitInstance.api.dohvatiProfilPoKorisniku(idKorisnik)
                } catch (_: Exception) {
                    _profil.value = null
                }

                try {
                    val mjerenja = RetrofitInstance.api.dohvatiMjerenjaZaKorisnika(idKorisnik)
                    _zadnjeMjerenje.value = mjerenja.maxByOrNull { it.id ?: 0 }
                } catch (_: Exception) {
                    _zadnjeMjerenje.value = null
                }

                try {
                    val preporuke = RetrofitInstance.api.dohvatiPreporukeZaKorisnika(idKorisnik)
                    _zadnjaPreporuka.value = preporuke.maxByOrNull { it.id ?: 0 }
                } catch (_: Exception) {
                    _zadnjaPreporuka.value = null
                }

            } catch (e: Exception) {
                _poruka.value = e.message
            } finally {
                _ucitava.value = false
            }
        }
    }

    fun spremiProfil(
        idKorisnik: Long = 1,
        ime: String,
        prezime: String,
        email: String,
        spol: String
    ) {
        viewModelScope.launch {
            try {
                val stariKorisnik = _korisnik.value

                val azuriraniKorisnik = Korisnik(
                    id = idKorisnik,
                    ime = ime,
                    prezime = prezime,
                    email = email,
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
                        visina = stariProfil.visina,
                        spol = spol,
                        dob = stariProfil.dob
                    )

                    RetrofitInstance.api.azurirajProfil(
                        id = stariProfil.id,
                        profil = azuriraniProfil
                    )
                }

                _poruka.value = "Profil spremljen"
                ucitajProfil(idKorisnik)

            } catch (e: Exception) {
                _poruka.value = e.message
            }
        }
    }
}