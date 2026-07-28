package com.niko.macromenza

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import com.niko.macromenza.navigation.AppNavigation
import com.niko.macromenza.session.ThemeManager
import com.niko.macromenza.ui.theme.AppTheme
import com.niko.macromenza.ui.theme.MacroMenzaTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(
            window,
            false
        )

        setContent {

            val themeManager = remember {
                ThemeManager(applicationContext)
            }

            val selectedTheme by
            themeManager.theme.collectAsState(
                initial = AppTheme.SYSTEM
            )

            val systemDark =
                isSystemInDarkTheme()

            val useDarkTheme =
                when (selectedTheme) {

                    AppTheme.SYSTEM ->
                        systemDark

                    AppTheme.LIGHT ->
                        false

                    AppTheme.DARK ->
                        true
                }

            MacroMenzaTheme(
                darkTheme = useDarkTheme
            ) {
                AppNavigation()
            }
        }
    }
}