package com.niko.macromenza.api

import android.content.Context
import com.niko.macromenza.session.UserSessionManager
import kotlinx.coroutines.runBlocking

/**
 * Supabase tokeni za pozive backendu. OkHttp interceptor ih čita sinkrono, pa su u memoriji;
 * trajno se čuvaju u DataStoreu (UserSessionManager), a učitavaju se pri pokretanju aplikacije.
 */
object TokenStore {

    @Volatile
    var accessToken: String? = null
        private set

    @Volatile
    var refreshToken: String? = null
        private set

    @Volatile
    private var sesija: UserSessionManager? = null

    fun inicijaliziraj(context: Context) {
        val upravitelj = UserSessionManager(context.applicationContext)
        sesija = upravitelj
        val (access, refresh) = runBlocking { upravitelj.ucitajTokene() }
        accessToken = access
        refreshToken = refresh
    }

    suspend fun postavi(access: String, refresh: String) {
        accessToken = access
        refreshToken = refresh
        sesija?.spremiTokene(access, refresh)
    }

    /** Odjava ili istekla sesija: brišu se tokeni i cijela spremljena sesija. */
    suspend fun obrisi() {
        accessToken = null
        refreshToken = null
        sesija?.obrisiSesiju()
    }
}
