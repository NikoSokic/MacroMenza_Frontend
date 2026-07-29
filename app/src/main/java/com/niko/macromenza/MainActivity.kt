package com.niko.macromenza

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
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

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = android.graphics.Color.WHITE,
                darkScrim = android.graphics.Color.BLACK
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

            SideEffect {

                val controller =
                    WindowCompat.getInsetsController(
                        window,
                        window.decorView
                    )

                controller.isAppearanceLightStatusBars =
                    !useDarkTheme

                controller.isAppearanceLightNavigationBars =
                    !useDarkTheme
            }

            MacroMenzaTheme(
                darkTheme = useDarkTheme
            ) {
                AppNavigation()
            }
        }
    }
}