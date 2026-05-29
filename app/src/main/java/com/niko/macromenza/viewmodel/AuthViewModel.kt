package com.niko.macromenza.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niko.macromenza.api.RetrofitInstance
import com.niko.macromenza.model.Korisnik
import com.niko.macromenza.supabase.SupabaseClientInstance
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val _poruka = MutableStateFlow<String?>(null)
    val poruka = _poruka.asStateFlow()

    private val _ucitavanje = MutableStateFlow(false)
    val ucitavanje = _ucitavanje.asStateFlow()

    private val _supabaseUid = MutableStateFlow<String?>(null)
    val supabaseUid = _supabaseUid.asStateFlow()

    private val _korisnikId = MutableStateFlow<Long?>(null)
    val korisnikId = _korisnikId.asStateFlow()

    fun registracija(
        email: String,
        lozinka: String
    ) {
        viewModelScope.launch {
            _ucitavanje.value = true
            _poruka.value = null

            try {
                SupabaseClientInstance.client.auth.signUpWith(Email) {
                    this.email = email
                    this.password = lozinka
                }

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
                SupabaseClientInstance.client.auth.signInWith(Email) {
                    this.email = email
                    this.password = lozinka
                }

                val user =
                    SupabaseClientInstance.client.auth.retrieveUserForCurrentSession(
                        updateSession = true
                    )

                val uid = user.id.toString()

                _supabaseUid.value = uid

                poveziKorisnikaSBackendom(
                    supabaseUid = uid,
                    email = email
                )

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
            try {
                SupabaseClientInstance.client.auth.signOut()

                _supabaseUid.value = null
                _korisnikId.value = null
                _poruka.value = "Odjavljen korisnik"

            } catch (e: Exception) {
                _poruka.value = e.message ?: "Greška pri odjavi"
            }
        }
    }

    private suspend fun poveziKorisnikaSBackendom(
        supabaseUid: String,
        email: String
    ) {
        val korisnik = Korisnik(
            supabaseUid = supabaseUid,
            ime = "",
            prezime = "",
            email = email,
            lozinka_hash = ""
        )

        val backendKorisnik =
            RetrofitInstance.api.pronadiIliDodajKorisnikaPrekoSupabase(korisnik)

        _korisnikId.value = backendKorisnik.id
    }
}