package com.niko.macromenza.api

import com.niko.macromenza.model.CestoJedenoJelo
import com.niko.macromenza.model.Jelo
import com.niko.macromenza.model.JeloDetalj
import com.niko.macromenza.model.KategorijaJela
import com.niko.macromenza.model.PovijestDana
import retrofit2.http.GET
import com.niko.macromenza.model.KonzumacijaRequest
import com.niko.macromenza.model.TjedniPregled
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import com.niko.macromenza.model.UkupniUnos
import com.niko.macromenza.model.Korisnik
import com.niko.macromenza.model.ProfilPodaci
import com.niko.macromenza.model.Mjerenje
import com.niko.macromenza.model.Preporuka
import com.niko.macromenza.model.RegistracijaProfilRequest
import com.niko.macromenza.model.OnboardingStatusResponse
import com.niko.macromenza.model.MeniDTO
import com.niko.macromenza.model.Obrok
import com.niko.macromenza.model.ObrokRequest

import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {

    @GET("api/jela")
    suspend fun getJela(
        @Query("search") search: String? = null,
        @Query("tipObroka") tipObroka: String? = null,
        @Query("kategorija") kategorija: String? = null,
        @Query("minKalorije") minKalorije: Double? = null,
        @Query("maxKalorije") maxKalorije: Double? = null,
        @Query("minProteini") minProteini: Double? = null,
        @Query("maxProteini") maxProteini: Double? = null,
        @Query("minUgljikohidrati") minUgljikohidrati: Double? = null,
        @Query("maxUgljikohidrati") maxUgljikohidrati: Double? = null,
        @Query("minMasti") minMasti: Double? = null,
        @Query("maxMasti") maxMasti: Double? = null
    ): List<Jelo>

    @GET("api/jela/kategorije")
    suspend fun getKategorijeJela(): List<KategorijaJela>

    @GET("api/jela/{id}/detalj")
    suspend fun getJeloDetalj(
        @Path("id") id: Long
    ): JeloDetalj

    @GET("api/jela/cesto")
    suspend fun getCestoJedeno(
        @Query("idKorisnik") idKorisnik: Long,
        @Query("limit") limit: Int = 10
    ): List<CestoJedenoJelo>

    @POST("api/konzumacije")
    suspend fun spremiKonzumaciju(
        @Body request: KonzumacijaRequest
    )

    @POST("api/obroci")
    suspend fun spremiObrok(
        @Body request: ObrokRequest
    ): Obrok

    @GET("api/obroci")
    suspend fun getObroci(
        @Query("idKorisnik") idKorisnik: Long,
        @Query("datum") datum: String
    ): List<Obrok>

    @GET("api/obroci/{id}")
    suspend fun getObrok(
        @Path("id") id: Long,
        @Query("idKorisnik") idKorisnik: Long
    ): Obrok

    @PUT("api/obroci/{id}")
    suspend fun azurirajObrok(
        @Path("id") id: Long,
        @Query("idKorisnik") idKorisnik: Long,
        @Body request: ObrokRequest
    ): Obrok

    @DELETE("api/obroci/{id}")
    suspend fun obrisiObrok(
        @Path("id") id: Long,
        @Query("idKorisnik") idKorisnik: Long
    ): Response<Unit>

    @GET("api/jela/filter")
    suspend fun filtrirajJela(
        @Query("minKalorije") minKalorije: Double? = null,
        @Query("maxKalorije") maxKalorije: Double? = null,
        @Query("minProteini") minProteini: Double? = null,
        @Query("maxProteini") maxProteini: Double? = null,
        @Query("minUgljikohidrati") minUgljikohidrati: Double? = null,
        @Query("maxUgljikohidrati") maxUgljikohidrati: Double? = null,
        @Query("minMasti") minMasti: Double? = null,
        @Query("maxMasti") maxMasti: Double? = null
    ): List<Jelo>

    @GET("api/meniji")
    suspend fun getMeniji(
        @Query("tip") tip: String? = null,
        @Query("vegetarijanski") vegetarijanski: Boolean? = null
    ): List<MeniDTO>

    @GET("api/meniji/{id}")
    suspend fun getMeni(
        @Path("id") id: Long
    ): MeniDTO

    @GET("api/konzumacije/povijest/{idKorisnik}")
    suspend fun dohvatiPovijestZaDan(
        @Path("idKorisnik") idKorisnik: Long,
        @Query("datum") datum: String
    ): PovijestDana

    @GET("api/konzumacije/tjedni-pregled/{idKorisnik}")
    suspend fun dohvatiTjedniPregled(
        @Path("idKorisnik") idKorisnik: Long
    ): TjedniPregled

    @GET("api/konzumacije/ukupno/{idKorisnik}")
    suspend fun dohvatiUkupniUnos(
        @Path("idKorisnik") idKorisnik: Long
    ): UkupniUnos

    @GET("korisnici/{id}")
    suspend fun dohvatiKorisnika(
        @Path("id") id: Long
    ): Korisnik

    @PUT("korisnici/{id}")
    suspend fun azurirajKorisnika(
        @Path("id") id: Long,
        @Body korisnik: Korisnik
    ): Korisnik

    @GET("profil/korisnik/{idKorisnik}")
    suspend fun dohvatiProfilPoKorisniku(
        @Path("idKorisnik") idKorisnik: Long
    ): ProfilPodaci

    @PUT("profil/{id}")
    suspend fun azurirajProfil(
        @Path("id") id: Long,
        @Body profil: ProfilPodaci
    ): ProfilPodaci

    @POST("mjerenja")
    suspend fun dodajMjerenje(
        @Body mjerenje: Mjerenje
    ): Mjerenje

    @GET("mjerenja/korisnik/{idKorisnik}")
    suspend fun dohvatiMjerenjaZaKorisnika(
        @Path("idKorisnik") idKorisnik: Long
    ): List<Mjerenje>

    @GET("preporuke/korisnik/{idKorisnik}")
    suspend fun dohvatiPreporukeZaKorisnika(
        @Path("idKorisnik") idKorisnik: Long
    ): List<Preporuka>

    @POST("preporuke/izracunaj/{idKorisnik}")
    suspend fun izracunajPreporuku(
        @Path("idKorisnik") idKorisnik: Long
    ): Preporuka

    @POST("korisnici/supabase")
    suspend fun pronadiIliDodajKorisnikaPrekoSupabase(
        @Body korisnik: Korisnik
    ): Korisnik

    @POST("profil")
    suspend fun dodajProfil(
        @Body profil: ProfilPodaci
    ): ProfilPodaci

    @POST("korisnici/registracija-profil")
    suspend fun registracijaProfil(
        @Body request: RegistracijaProfilRequest
    ): Korisnik

    @GET("korisnici/{id}/onboarding-status")
    suspend fun dohvatiOnboardingStatus(
        @Path("id") id: Long
    ): OnboardingStatusResponse

}