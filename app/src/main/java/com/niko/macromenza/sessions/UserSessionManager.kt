package com.niko.macromenza.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userSessionDataStore by preferencesDataStore(
    name = "user_session"
)

class UserSessionManager(
    private val context: Context
) {
    companion object {
        private val KORISNIK_ID = longPreferencesKey("korisnik_id")
        private val SUPABASE_UID = stringPreferencesKey("supabase_uid")
    }

    val korisnikId: Flow<Long?> =
        context.userSessionDataStore.data.map { preferences ->
            preferences[KORISNIK_ID]
        }

    val supabaseUid: Flow<String?> =
        context.userSessionDataStore.data.map { preferences ->
            preferences[SUPABASE_UID]
        }

    val isLoggedIn: Flow<Boolean> =
        context.userSessionDataStore.data.map { preferences ->
            val korisnikId = preferences[KORISNIK_ID]
            val supabaseUid = preferences[SUPABASE_UID]

            korisnikId != null &&
                    korisnikId > 0 &&
                    !supabaseUid.isNullOrBlank()
        }

    suspend fun spremiSesiju(
        korisnikId: Long,
        supabaseUid: String
    ) {
        if (korisnikId <= 0 || supabaseUid.isBlank()) {
            return
        }

        context.userSessionDataStore.edit { preferences ->
            preferences[KORISNIK_ID] = korisnikId
            preferences[SUPABASE_UID] = supabaseUid
        }
    }

    suspend fun obrisiSesiju() {
        context.userSessionDataStore.edit { preferences ->
            preferences.clear()
        }
    }
}