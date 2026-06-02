package com.niko.macromenza.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niko.macromenza.api.RetrofitInstance
import com.niko.macromenza.api.SupabaseAuthInstance
import com.niko.macromenza.model.Korisnik
import com.niko.macromenza.model.SupabaseAuthRequest
import com.niko.macromenza.session.UserSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val sessionManager: UserSessionManager
) : ViewModel() {

    private val _poruka = MutableStateFlow<String?>(null)
    val poruka = _poruka.asStateFlow()

    private val _ucitavanje = MutableStateFlow(false)
    val ucitavanje = _ucitavanje.asStateFlow()

    private val _supabaseUid = MutableStateFlow<String?>(null)
    val supabaseUid = _supabaseUid.asStateFlow()

    private val _korisnikId = MutableStateFlow<Long?>(null)
    val korisnikId = _korisnikId.asStateFlow()

    private val _onboardingZavrsen = MutableStateFlow<Boolean?>(null)
    val onboardingZavrsen = _onboardingZavrsen.asStateFlow()

    init {
        ucitajSesiju()
    }

    private fun ucitajSesiju() {
        viewModelScope.launch {
            sessionManager.supabaseUid.collect { uid ->
                _supabaseUid.value = uid
            }
        }

        viewModelScope.launch {
            sessionManager.korisnikId.collect { id ->
                _korisnikId.value = id
            }
        }
    }

    fun registracija(
        email: String,
        lozinka: String
    ) {
        viewModelScope.launch {
            _ucitavanje.value = true
            _poruka.value = null

            try {
                SupabaseAuthInstance.api.registracija(
                    apiKey = SupabaseAuthInstance.SUPABASE_ANON_KEY,
                    request = SupabaseAuthRequest(
                        email = email,
                        password = lozinka
                    )
                )

                _poruka.value = "Registracija uspješna. Provjeri email i potvrdi račun."

            } catch (e: Exception) {
                _poruka.value = e.message ?: "Greška pri registraciji"
            } finally {
                _ucitavanje.value = false
            }
        }
    }

    fun prijava(
        email: String,
        lozinka: String
    ) {
        viewModelScope.launch {
            _ucitavanje.value = true
            _poruka.value = null

            try {
                val response = SupabaseAuthInstance.api.prijava(
                    apiKey = SupabaseAuthInstance.SUPABASE_ANON_KEY,
                    request = SupabaseAuthRequest(
                        email = email,
                        password = lozinka
                    )
                )

                val uid = response.user?.id

                if (uid == null) {
                    _poruka.value = "Prijava nije uspjela."
                    return@launch
                }

                _supabaseUid.value = uid

                val backendKorisnik = poveziKorisnikaSBackendom(
                    supabaseUid = uid,
                    email = email
                )

                _korisnikId.value = backendKorisnik.id

                val status = RetrofitInstance.api.dohvatiOnboardingStatus(
                    backendKorisnik.id ?: 0L
                )

                _onboardingZavrsen.value = status.onboardingZavrsen

                if (status.onboardingZavrsen) {
                    sessionManager.spremiSesiju(
                        korisnikId = backendKorisnik.id ?: 0L,
                        supabaseUid = uid
                    )
                }

                _poruka.value = "Prijava uspješna"

            } catch (e: Exception) {
                _poruka.value = e.message ?: "Greška pri prijavi"
            } finally {
                _ucitavanje.value = false
            }
        }
    }

    fun odjava() {
        viewModelScope.launch {
            sessionManager.obrisiSesiju()

            _supabaseUid.value = null
            _korisnikId.value = null
            _onboardingZavrsen.value = null
            _poruka.value = "Odjavljen korisnik"
        }
    }

    private suspend fun poveziKorisnikaSBackendom(
        supabaseUid: String,
        email: String
    ): Korisnik {
        val korisnik = Korisnik(
            supabaseUid = supabaseUid,
            ime = "",
            prezime = "",
            email = email,
            lozinka_hash = ""
        )

        return RetrofitInstance.api.pronadiIliDodajKorisnikaPrekoSupabase(korisnik)
    }
}