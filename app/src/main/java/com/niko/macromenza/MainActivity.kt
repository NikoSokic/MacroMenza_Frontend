package com.niko.macromenza

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.niko.macromenza.navigation.AppNavigation
import com.niko.macromenza.session.ThemeManager
import com.niko.macromenza.ui.theme.AppTheme
import com.niko.macromenza.ui.theme.MacroMenzaTheme
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.ui.graphics.toArgb
import com.niko.macromenza.ui.theme.MacroBackground
import com.niko.macromenza.ui.theme.MacroDark

class MainActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = MacroBackground.toArgb(),
                darkScrim = MacroDark.toArgb()
            )
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