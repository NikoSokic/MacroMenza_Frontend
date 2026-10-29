package com.niko.macromenza.snimke

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import sergio.sastre.composable.preview.scanner.android.AndroidComposablePreviewScanner
import sergio.sastre.composable.preview.scanner.android.AndroidPreviewInfo
import sergio.sastre.composable.preview.scanner.core.preview.ComposablePreview

private const val OKVIR = "preview-okvir"

/** Previewi bez zadane širine dobivaju širinu malog mobitela, kao i ostali previewi aplikacije. */
private const val ZADANA_SIRINA_DP = 360

/** Animacije se zaustave ovoliko nakon prvog okvira (ulazni pokreti su kraći; kostur učitavanja je beskonačan). */
private const val VRIJEME_SNIMKE_MS = 2_000L

/**
 * Snimka svakog @Preview u aplikaciji, kako je definiran (širina, visina, fontScale; tamna tema je u tijelu
 * previewa). `recordRoborazziDebug` sprema referentne slike u src/test/snapshots, a `verifyRoborazziDebug` pada
 * na svaku nenamjernu vizualnu promjenu. Običan `testDebugUnitTest` samo iscrtava (hvata rušenja), bez usporedbe.
 *
 * Sat kompozicije je ručni: beskonačne animacije (kostur učitavanja) inače nikad ne puste ekran u mirovanje.
 * LocalInspectionMode je uključen kao u Android Studiju, pa ekrani biraju isti put kao u previewu.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w420dp-h2500dp-xhdpi")
class PreviewSnimkeTest(
    private val ime: String,
    private val preview: ComposablePreview<AndroidPreviewInfo>
) {

    companion object {
        /** Ime snimke: Datoteka_Funkcija, a funkcija s više @Preview anotacija dobiva _1, _2... */
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun previewi(): List<Array<Any>> {
            val svi = AndroidComposablePreviewScanner()
                .scanPackageTrees("com.niko.macromenza")
                .includePrivatePreviews()
                .getPreviews()
            fun osnova(p: ComposablePreview<AndroidPreviewInfo>) =
                p.declaringClass.substringAfterLast('.').removeSuffix("Kt") + "_" + p.methodName
            return svi.groupBy(::osnova).flatMap { (osnova, istaFunkcija) ->
                istaFunkcija.mapIndexed { i, p ->
                    arrayOf<Any>(if (istaFunkcija.size == 1) osnova else "${osnova}_${i + 1}", p)
                }
            }
        }
    }

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun snimka() {
        val info = preview.previewInfo
        val sirina = if (info.widthDp > 0) info.widthDp else ZADANA_SIRINA_DP

        compose.mainClock.autoAdvance = false
        compose.setContent {
            val gustoca = LocalDensity.current
            CompositionLocalProvider(
                LocalInspectionMode provides true,
                LocalDensity provides Density(gustoca.density, if (info.fontScale > 0f) info.fontScale else 1f)
            ) {
                var okvir = Modifier.testTag(OKVIR).width(sirina.dp)
                if (info.heightDp > 0) okvir = okvir.height(info.heightDp.dp)
                if (info.showBackground) okvir = okvir.background(Color.White)
                Box(okvir) { preview() }
            }
        }
        compose.mainClock.advanceTimeBy(VRIJEME_SNIMKE_MS)

        compose.onNodeWithTag(OKVIR).captureRoboImage("src/test/snapshots/$ime.png")
    }
}
