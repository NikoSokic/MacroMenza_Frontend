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

            val emailTrim = email.trim()
            val lozinkaTrim = lozinka.trim()

            if (emailTrim.isBlank()) {
                _poruka.value = "📧 Unesi email adresu."
                _ucitavanje.value = false
                return@launch
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailTrim).matches()) {
                _poruka.value = "📧 Ovo ne izgleda kao pravi email. Probaj nešto tipa ime@email.com."
                _ucitavanje.value = false
                return@launch
            }

            if (lozinkaTrim.isBlank()) {
                _poruka.value = "🔒 Unesi lozinku."
                _ucitavanje.value = false
                return@launch
            }

            if (lozinkaTrim.length < 6) {
                _poruka.value = "🔒 Lozinka mora imati barem 6 znakova. Nije CIA, ali ipak treba malo sigurnosti 😄"
                _ucitavanje.value = false
                return@launch
            }

            try {
                SupabaseAuthInstance.api.registracija(
                    apiKey = SupabaseAuthInstance.SUPABASE_ANON_KEY,
                    request = SupabaseAuthRequest(
                        email = emailTrim,
                        password = lozinkaTrim
                    )
                )

                _poruka.value = "📩 Poslali smo ti email za potvrdu računa. Otvori mail i potvrdi registraciju."

            } catch (e: retrofit2.HttpException) {
                _poruka.value = when (e.code()) {
                    400 -> "Podaci nisu ispravni. Provjeri email i lozinku."
                    401 -> "Nemaš dozvolu za ovu akciju."
                    409 -> "Korisnik s ovom email adresom već postoji."
                    422 -> "📧 Email ili lozinka nisu ispravni. Provjeri format emaila i duljinu lozinke."
                    429 -> "Previše pokušaja. Pričekaj malo pa pokušaj ponovno."
                    else -> "Došlo je do greške (${e.code()}). Pokušaj ponovno."
                }
            } catch (e: java.net.UnknownHostException) {
                _poruka.value = "Nema internetske veze. Spoji se na internet pa pokušaj ponovno."
            } catch (e: java.net.SocketTimeoutException) {
                _poruka.value = "Server se malo uspavao. Pričekaj par sekundi pa pokušaj ponovno."
            } catch (e: Exception) {
                _poruka.value = "Greška pri registraciji. Pokušaj ponovno."
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

            val emailTrim = email.trim()
            val lozinkaTrim = lozinka.trim()

            if (emailTrim.isBlank()) {
                _poruka.value = "📧 Unesi email adresu."
                _ucitavanje.value = false
                return@launch
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailTrim).matches()) {
                _poruka.value = "📧 Ovo ne izgleda kao pravi email."
                _ucitavanje.value = false
                return@launch
            }

            if (lozinkaTrim.isBlank()) {
                _poruka.value = "🔒 Unesi lozinku."
                _ucitavanje.value = false
                return@launch
            }

            try {
                val response = SupabaseAuthInstance.api.prijava(
                    apiKey = SupabaseAuthInstance.SUPABASE_ANON_KEY,
                    request = SupabaseAuthRequest(
                        email = emailTrim,
                        password = lozinkaTrim
                    )
                )

                val uid = response.user?.id

                if (uid == null) {
                    _poruka.value = "Prijava nije uspjela. Pokušaj ponovno."
                    return@launch
                }

                _supabaseUid.value = uid

                val backendKorisnik = poveziKorisnikaSBackendom(
                    supabaseUid = uid,
                    email = emailTrim
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


            } catch (e: retrofit2.HttpException) {

                _poruka.value = when (e.code()) {
                    400 -> "📧 Email ili lozinka nisu ispravni."
                    401 -> "🔒 Pogrešan email ili lozinka."
                    403 -> "📩 Prvo potvrdi svoj račun putem emaila."
                    422 -> "📧 Provjeri email i lozinku."
                    429 -> "⏳ Previše pokušaja prijave. Pričekaj malo."
                    else -> "Došlo je do greške (${e.code()}). Pokušaj ponovno."
                }

            } catch (e: java.net.UnknownHostException) {

                _poruka.value = "📡 Nema internetske veze."

            } catch (e: java.net.SocketTimeoutException) {

                _poruka.value = "😴 Server se upravo budi. Pokušaj ponovno za nekoliko sekundi."

            } catch (e: Exception) {

                val poruka = e.message?.lowercase() ?: ""

                _poruka.value = when {
                    "email not confirmed" in poruka ->
                        "📩 Potvrdi račun putem emaila prije prijave."

                    "invalid login credentials" in poruka ->
                        "🔒 Pogrešan email ili lozinka."

                    else ->
                        "Prijava nije uspjela. Pokušaj ponovno."
                }
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

    fun ocistiPoruku() {
        _poruka.value = null
    }

}