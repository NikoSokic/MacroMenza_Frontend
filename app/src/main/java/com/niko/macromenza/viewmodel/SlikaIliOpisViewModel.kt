package com.niko.macromenza.viewmodel

import android.app.Application
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.niko.macromenza.api.RetrofitInstance
import com.niko.macromenza.model.PrijedlogObroka
import com.niko.macromenza.util.pripremiSliku
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.HttpException
import java.io.File
import java.io.IOException
import java.io.InterruptedIOException
import java.util.UUID

const val NAJVISE_ZNAKOVA_OPISA = 1000

class SlikaIliOpisViewModel(application: Application) : AndroidViewModel(application) {

    private val _opis = MutableStateFlow("")
    val opis: StateFlow<String> = _opis

    private val _slika = MutableStateFlow<Uri?>(null)
    val slika: StateFlow<Uri?> = _slika

    private val _ucitava = MutableStateFlow(false)
    val ucitava: StateFlow<Boolean> = _ucitava

    private val _rezultat = MutableStateFlow<PrijedlogObroka?>(null)
    val rezultat: StateFlow<PrijedlogObroka?> = _rezultat

    // SLIKA ako je uz zahtjev poslana slika, inače OPIS; vrijedi za trenutni rezultat.
    private val _izvor = MutableStateFlow("OPIS")
    val izvor: StateFlow<String> = _izvor

    private val _greska = MutableStateFlow<String?>(null)
    val greska: StateFlow<String?> = _greska

    // Datoteka koju kamera upravo puni, ili već snimljena fotografija; brišemo je nakon slanja.
    private var kameraDatoteka: File? = null

    fun postaviOpis(tekst: String) {
        _opis.value = tekst.take(NAJVISE_ZNAKOVA_OPISA)
    }

    fun postaviSliku(uri: Uri) {
        brisiKameru()
        _slika.value = uri
        _greska.value = null
    }

    fun ukloniSliku() {
        brisiKameru()
        _slika.value = null
    }

    /** URI u koji će kamera spremiti fotografiju. */
    fun pripremiKameru(): Uri {
        brisiKameru()
        val kontekst = getApplication<Application>()
        val mapa = File(kontekst.cacheDir, "kamera").apply { mkdirs() }
        val datoteka = File(mapa, "foto_${UUID.randomUUID()}.jpg")
        kameraDatoteka = datoteka
        return FileProvider.getUriForFile(kontekst, "${kontekst.packageName}.fileprovider", datoteka)
    }

    fun kameraGotova(uspjeh: Boolean, uri: Uri?) {
        if (uspjeh && uri != null) {
            _slika.value = uri
            _greska.value = null
        } else {
            brisiKameru()
        }
    }

    fun prepoznaj(idKorisnik: Long) {
        if (_ucitava.value) return
        val slikaUri = _slika.value
        val opisTeksta = _opis.value.trim().ifEmpty { null }
        if (slikaUri == null && opisTeksta == null) return

        viewModelScope.launch {
            _ucitava.value = true
            _greska.value = null
            _rezultat.value = null

            var pripremljena: File? = null
            try {
                pripremljena = if (slikaUri != null) {
                    try {
                        pripremiSliku(getApplication(), slikaUri)
                    } catch (e: IOException) {
                        _greska.value = e.message ?: "Slika se ne može pročitati."
                        return@launch
                    } catch (e: SecurityException) {
                        _greska.value = "Aplikacija nema pristup toj slici. Odaberi je ponovno."
                        return@launch
                    } catch (e: OutOfMemoryError) {
                        _greska.value = "Slika je prevelika za obradu."
                        return@launch
                    }
                } else {
                    null
                }

                val dio = pripremljena?.let {
                    MultipartBody.Part.createFormData(
                        "slika",
                        "slika.jpg",
                        it.asRequestBody("image/jpeg".toMediaType())
                    )
                }

                val odgovor = RetrofitInstance.prepoznavanjeApi.prepoznaj(
                    slika = dio,
                    opis = opisTeksta?.toRequestBody("text/plain; charset=utf-8".toMediaType()),
                    idKorisnik = idKorisnik.toString().toRequestBody("text/plain".toMediaType())
                )
                _izvor.value = if (slikaUri != null) "SLIKA" else "OPIS"
                _rezultat.value = odgovor
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                _greska.value = porukaGreske(e)
            } catch (e: InterruptedIOException) {
                _greska.value = "Poslužitelj nije odgovorio na vrijeme. Pokušaj ponovno."
            } catch (e: IOException) {
                _greska.value = "Nema veze s poslužiteljem. Provjeri vezu i pokušaj ponovno."
            } catch (e: Exception) {
                _greska.value = "Prepoznavanje nije uspjelo."
            } finally {
                pripremljena?.delete()
                _ucitava.value = false
            }
        }
    }

    private fun brisiKameru() {
        kameraDatoteka?.delete()
        kameraDatoteka = null
    }

    override fun onCleared() {
        brisiKameru()
        super.onCleared()
    }

    private fun porukaGreske(e: HttpException): String {
        val poruka = try {
            e.response()?.errorBody()?.string()?.let { JSONObject(it).optString("poruka") }
        } catch (ex: Exception) {
            null
        }

        return if (poruka.isNullOrBlank()) {
            "Prepoznavanje nije uspjelo (greška ${e.code()})."
        } else {
            poruka
        }
    }
}
