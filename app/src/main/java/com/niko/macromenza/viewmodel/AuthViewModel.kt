package com.niko.macromenza.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niko.macromenza.R
import com.niko.macromenza.api.RetrofitInstance
import com.niko.macromenza.api.SupabaseAuthInstance
import com.niko.macromenza.model.Korisnik
import com.niko.macromenza.model.SupabaseAuthRequest
import com.niko.macromenza.session.UserSessionManager
import com.niko.macromenza.util.UiTekst
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val sessionManager: UserSessionManager
) : ViewModel() {

    private val _poruka = MutableStateFlow<UiTekst?>(null)
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
                _poruka.value = UiTekst.Resurs(R.string.auth_unesi_email)
                _ucitavanje.value = false
                return@launch
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailTrim).matches()) {
                _poruka.value = UiTekst.Resurs(R.string.auth_reg_email_neispravan)
                _ucitavanje.value = false
                return@launch
            }

            if (lozinkaTrim.isBlank()) {
                _poruka.value = UiTekst.Resurs(R.string.auth_unesi_lozinku)
                _ucitavanje.value = false
                return@launch
            }

            if (lozinkaTrim.length < 6) {
                _poruka.value = UiTekst.Resurs(R.string.auth_reg_lozinka_kratka)
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

                _poruka.value = UiTekst.Resurs(R.string.auth_reg_email_poslan)

            } catch (e: retrofit2.HttpException) {
                _poruka.value = when (e.code()) {
                    400 -> UiTekst.Resurs(R.string.auth_reg_greska_400)
                    401 -> UiTekst.Resurs(R.string.auth_reg_greska_401)
                    409 -> UiTekst.Resurs(R.string.auth_reg_greska_409)
                    422 -> UiTekst.Resurs(R.string.auth_reg_greska_422)
                    429 -> UiTekst.Resurs(R.string.auth_reg_greska_429)
                    else -> UiTekst.Resurs(R.string.auth_greska_http, e.code())
                }
            } catch (e: java.net.UnknownHostException) {
                _poruka.value = UiTekst.Resurs(R.string.auth_reg_nema_interneta)
            } catch (e: java.net.SocketTimeoutException) {
                _poruka.value = UiTekst.Resurs(R.string.auth_reg_timeout)
            } catch (e: Exception) {
                _poruka.value = UiTekst.Resurs(R.string.auth_reg_greska)
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
                _poruka.value = UiTekst.Resurs(R.string.auth_unesi_email)
                _ucitavanje.value = false
                return@launch
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailTrim).matches()) {
                _poruka.value = UiTekst.Resurs(R.string.auth_prij_email_neispravan)
                _ucitavanje.value = false
                return@launch
            }

            if (lozinkaTrim.isBlank()) {
                _poruka.value = UiTekst.Resurs(R.string.auth_unesi_lozinku)
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
                    _poruka.value = UiTekst.Resurs(R.string.auth_prij_neuspjela)
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
                    400 -> UiTekst.Resurs(R.string.auth_prij_greska_400)
                    401 -> UiTekst.Resurs(R.string.auth_prij_pogresni_podaci)
                    403 -> UiTekst.Resurs(R.string.auth_prij_greska_403)
                    422 -> UiTekst.Resurs(R.string.auth_prij_greska_422)
                    429 -> UiTekst.Resurs(R.string.auth_prij_greska_429)
                    else -> UiTekst.Resurs(R.string.auth_greska_http, e.code())
                }

            } catch (e: java.net.UnknownHostException) {

                _poruka.value = UiTekst.Resurs(R.string.auth_prij_nema_interneta)

            } catch (e: java.net.SocketTimeoutException) {

                _poruka.value = UiTekst.Resurs(R.string.auth_prij_timeout)

            } catch (e: Exception) {

                val poruka = e.message?.lowercase() ?: ""

                _poruka.value = when {
                    "email not confirmed" in poruka ->
                        UiTekst.Resurs(R.string.auth_prij_nije_potvrden)

                    "invalid login credentials" in poruka ->
                        UiTekst.Resurs(R.string.auth_prij_pogresni_podaci)

                    else ->
                        UiTekst.Resurs(R.string.auth_prij_neuspjela)
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
            _poruka.value = UiTekst.Resurs(R.string.auth_odjavljen)
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