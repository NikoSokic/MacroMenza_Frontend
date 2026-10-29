package com.niko.macromenza.testutil

import com.niko.macromenza.api.ApiService
import com.niko.macromenza.api.RetrofitInstance
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import retrofit2.HttpException
import retrofit2.Response

/** viewModelScope koristi Dispatchers.Main, pa ga u testu zamjenjuje kontrolirani dispatcher (virtualno vrijeme). */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val dispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}

/**
 * Zamjenjuje RetrofitInstance.api i prepoznavanjeApi lažnim ApiService-om, pa ViewModeli ne dodiruju mrežu.
 * Nepodešen poziv baca iznimku (mockk nije "relaxed"), što ViewModel vidi kao grešku veze.
 */
class FakeApiRule : TestWatcher() {
    val api: ApiService = mockk()

    override fun starting(description: Description) {
        mockkObject(RetrofitInstance)
        every { RetrofitInstance.api } returns api
        every { RetrofitInstance.prepoznavanjeApi } returns api
    }

    override fun finished(description: Description) {
        unmockkObject(RetrofitInstance)
    }
}

private val JSON = "application/json".toMediaType()

/** HttpException kakvu baca Retrofit; [poruka] ide u tijelo kao {"poruka": "..."} (format backenda). */
fun httpGreska(kod: Int, poruka: String? = null): HttpException =
    HttpException(Response.error<Any>(kod, tijeloGreske(poruka)))

/** Neuspješan odgovor za metode koje vraćaju Response<Unit> (brisanje, primjer tjedna). */
fun neuspjeh(kod: Int, poruka: String? = null): Response<Unit> = Response.error(kod, tijeloGreske(poruka))

fun uspjeh(): Response<Unit> = Response.success(Unit)

private fun tijeloGreske(poruka: String?) =
    (poruka?.let { """{"poruka":"$it"}""" } ?: "").toResponseBody(JSON)

/** Tekst datoteke iz src/test/resources/odgovori (stvarni odgovori backenda). */
fun odgovor(ime: String): String =
    requireNotNull(object {}.javaClass.getResourceAsStream("/odgovori/$ime.json")) { "Nema odgovori/$ime.json" }
        .bufferedReader(Charsets.UTF_8).use { it.readText() }
