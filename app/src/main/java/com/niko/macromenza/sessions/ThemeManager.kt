package com.niko.macromenza.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.niko.macromenza.ui.theme.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore by preferencesDataStore(
    name = "theme_preferences"
)

class ThemeManager(
    private val context: Context
) {

    companion object {
        private val THEME_KEY =
            stringPreferencesKey("app_theme")
    }

    val theme: Flow<AppTheme> =
        context.themeDataStore.data.map { preferences ->

            when (preferences[THEME_KEY]) {
                AppTheme.LIGHT.name -> AppTheme.LIGHT
                AppTheme.DARK.name -> AppTheme.DARK
                else -> AppTheme.SYSTEM
            }
        }

    suspend fun saveTheme(
        theme: AppTheme
    ) {
        context.themeDataStore.edit { preferences ->
            preferences[THEME_KEY] = theme.name
        }
    }
}