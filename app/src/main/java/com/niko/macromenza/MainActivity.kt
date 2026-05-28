package com.niko.macromenza

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.niko.macromenza.screens.JelaScreen
import com.niko.macromenza.ui.theme.MacroMenzaTheme
import com.niko.macromenza.navigation.AppNavigation


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MacroMenzaTheme {
                AppNavigation()
            }
        }
    }
}