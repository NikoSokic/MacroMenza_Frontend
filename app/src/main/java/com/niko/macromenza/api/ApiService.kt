package com.niko.macromenza.api

import com.niko.macromenza.model.Jelo
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
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {

    @GET("api/jela")
    suspend fun getJela(
        @Query("search") search: String? = null,
        @Query("tipObroka") tipObroka: String? = null
    ): List<Jelo>

    @POST("api/konzumacije")
    suspend fun spremiKonzumaciju(
        @Body request: KonzumacijaRequest
    )

    @GET("api/jela/filter")
    suspend fun filtrirajJela(
        @Query("minKalorije") minKalorije: Int? = null,
        @Query("maxKalorije") maxKalorije: Int? = null,
        @Query("minProteini") minProteini: Int? = null,
        @Query("maxProteini") maxProteini: Int? = null,
        @Query("minUgljikohidrati") minUgljikohidrati: Int? = null,
        @Query("maxUgljikohidrati") maxUgljikohidrati: Int? = null,
        @Query("minMasti") minMasti: Int? = null,
        @Query("maxMasti") maxMasti: Int? = null
    ): List<Jelo>

    @GET("api/konzumacije/povijest/dani/1")
    suspend fun dohvatiDanePovijesti(): List<String>

    @GET("api/konzumacije/povijest/1")
    suspend fun dohvatiPovijestZaDan(
        @Query("datum") datum: String
    ): PovijestDana

    @GET("api/konzumacije/tjedni-pregled/1")
    suspend fun dohvatiTjedniPregled(): TjedniPregled

    @GET("api/konzumacije/ukupno/1")
    suspend fun dohvatiUkupniUnos(): UkupniUnos

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



}