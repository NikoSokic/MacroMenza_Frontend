package com.niko.macromenza.api

import com.niko.macromenza.model.SupabaseOsvjeziRequest
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.HttpException

/** Svaki zahtjev backendu nosi Supabase access token (backend ga provjerava i iz njega čita korisnika). */
class TokenInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = TokenStore.accessToken ?: return chain.proceed(chain.request())
        return chain.proceed(chain.request().withBearer(token))
    }
}

/**
 * Backend je vratio 401 (access token istekao, traje oko sat vremena): osvježi ga refresh tokenom i ponovi zahtjev.
 * Ako ni refresh token ne vrijedi, sesija se briše pa korisnik ponovno prolazi prijavu.
 */
class TokenAuthenticator : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        val poslani = response.request.header("Authorization")?.removePrefix("Bearer ") ?: return null
        if (response.priorResponse != null) return null

        synchronized(TokenStore) {
            // Drugi zahtjev je u međuvremenu već osvježio token.
            val trenutni = TokenStore.accessToken
            if (trenutni != null && trenutni != poslani) {
                return response.request.withBearer(trenutni)
            }

            val refresh = TokenStore.refreshToken ?: return null
            val noviTokeni = try {
                runBlocking {
                    SupabaseAuthInstance.api.osvjezi(
                        apiKey = SupabaseAuthInstance.SUPABASE_ANON_KEY,
                        request = SupabaseOsvjeziRequest(refresh)
                    )
                }
            } catch (e: HttpException) {
                // Refresh token odbijen: sesija je gotova. Ostale greške (mreža) ne smiju odjaviti korisnika.
                if (e.code() in 400..403) runBlocking { TokenStore.obrisi() }
                return null
            } catch (e: Exception) {
                return null
            }

            val access = noviTokeni.access_token ?: return null
            runBlocking { TokenStore.postavi(access, noviTokeni.refresh_token ?: refresh) }
            return response.request.withBearer(access)
        }
    }
}

private fun Request.withBearer(token: String): Request =
    newBuilder().header("Authorization", "Bearer $token").build()
