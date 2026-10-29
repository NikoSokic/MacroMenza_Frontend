package com.niko.macromenza

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.IntentCompat
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

        // Do prvog kadra trake prate sustav (kao početni prozor); zatim temu aplikacije iz Composea.
        val sustavTamni = (resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        postaviSustavneTrake(sustavTamni, getColor(R.color.pozadina_prozora))

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
                val pozadina = MaterialTheme.colorScheme.background.toArgb()

                LaunchedEffect(useDarkTheme, pozadina) {
                    postaviSustavneTrake(useDarkTheme, pozadina)
                }

                AppNavigation(
                    dijeljenaSlika = dijeljenaSlika,
                    onDijeljenaObradena = { dijeljenaSlika = null }
                )
            }
        }
    }

    // Statusna traka prozirna; navigacijska sa scrimom u boji pozadine (papir ili noć).
    // Svijetla tema daje tamne ikone na obje trake, tamna svijetle.
    private fun postaviSustavneTrake(tamna: Boolean, pozadina: Int) {
        val prozirno = android.graphics.Color.TRANSPARENT
        enableEdgeToEdge(
            statusBarStyle = if (tamna) {
                SystemBarStyle.dark(prozirno)
            } else {
                SystemBarStyle.light(prozirno, prozirno)
            },
            navigationBarStyle = if (tamna) {
                SystemBarStyle.dark(pozadina)
            } else {
                SystemBarStyle.light(pozadina, pozadina)
            }
        )
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