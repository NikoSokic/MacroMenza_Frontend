package com.niko.macromenza

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.IntentCompat
import androidx.core.view.WindowCompat
import com.niko.macromenza.navigation.AppNavigation
import com.niko.macromenza.session.ThemeManager
import com.niko.macromenza.ui.theme.AppTheme
import com.niko.macromenza.ui.theme.MacroMenzaTheme

class MainActivity : ComponentActivity() {

    // Slika podijeljena iz druge aplikacije (ACTION_SEND); čeka dok je NavGraph ne preuzme.
    private var dijeljenaSlika by mutableStateOf<Uri?>(null)

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        // Nakon rotacije ili povratka iz nedavnih aplikacija isti intent se ne obrađuje ponovno.
        if (savedInstanceState == null) {
            obradiDijeljenje(intent)
        }

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
                AppNavigation(
                    dijeljenaSlika = dijeljenaSlika,
                    onDijeljenaObradena = { dijeljenaSlika = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        obradiDijeljenje(intent)
    }

    private fun obradiDijeljenje(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return
        if (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0) return
        if (intent.type?.startsWith("image/") != true) return

        IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)?.let {
            dijeljenaSlika = it
        }
    }
}