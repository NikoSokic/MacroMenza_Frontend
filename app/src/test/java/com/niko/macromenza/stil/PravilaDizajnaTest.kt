package com.niko.macromenza.stil

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Pravila dizajna "Pladanj" (CLAUDE.md, pravilo 8 i "Zabranjeno") provjerena nad izvornim kodom:
 * boje, dimenzije i fontovi samo iz ui/theme, bez gradijenata, zadane M3 ljubičaste i emojija.
 * Radni direktorij unit testova je modul app/.
 */
class PravilaDizajnaTest {

    private val izvor = File("src/main/java/com/niko/macromenza")
    private val tema = File(izvor, "ui/theme")
    private val stringovi = File("src/main/res/values/strings.xml")

    private val kotlin: List<File> = izvor.walkTopDown().filter { it.extension == "kt" }.toList()
    private val izvanTeme: List<File> = kotlin.filterNot { it.path.startsWith(tema.path) }

    private val emoji = Regex("[\\x{1F300}-\\x{1FAFF}\\x{2600}-\\x{27BF}\\x{1F000}-\\x{1F2FF}]")

    private fun kodBezKomentara(f: File): List<Pair<Int, String>> =
        f.readLines().mapIndexed { i, l -> i + 1 to l.substringBefore("//") }
            .filterNot { (_, l) -> l.trimStart().startsWith("*") || l.trimStart().startsWith("/*") }

    private fun nadi(datoteke: List<File>, uzorak: Regex, dopusteno: (String) -> Boolean = { false }): List<String> =
        datoteke.flatMap { f ->
            kodBezKomentara(f).filter { (_, l) -> uzorak.containsMatchIn(l) && !dopusteno(l) }
                .map { (n, l) -> "${f.path.substringAfter("macromenza${File.separator}")}:$n  ${l.trim()}" }
        }

    @Test
    fun izvorniKodJeDostupan() {
        assertTrue("nema ${izvor.absolutePath}", kotlin.size > 50)
        assertTrue(stringovi.exists())
    }

    @Test
    fun bojeSamoIzTeme() {
        assertEquals(emptyList<String>(), nadi(izvanTeme, Regex("""Color\(0x|Color\.(Red|Blue|Green|Yellow|Cyan|Magenta|Gray|LightGray|DarkGray)\b""")))
    }

    @Test
    fun dimenzijeITipografijaSamoIzTeme() {
        // 0.dp je početna vrijednost mjerenja, ne dizajnerska vrijednost.
        val brojDp = Regex("""\b\d+(\.\d+)?\.(dp|sp)\b""")
        assertEquals(emptyList<String>(), nadi(izvanTeme, brojDp) { l -> brojDp.findAll(l).all { it.value == "0.dp" } })
    }

    @Test
    fun nemaGradijenata() {
        assertEquals(emptyList<String>(), nadi(kotlin, Regex("""Brush\.(linear|radial|vertical|horizontal|sweep)Gradient""")))
    }

    @Test
    fun najviseDvaFontaIBezSistemskih() {
        val fontovi = File("src/main/res/font").listFiles().orEmpty().map { it.nameWithoutExtension }.sorted()
        assertEquals(listOf("fraunces", "hanken_grotesk"), fontovi)
        assertEquals(emptyList<String>(), nadi(kotlin, Regex("""FontFamily\.(Serif|SansSerif|Monospace|Cursive)""")))
    }

    @Test
    fun nemaZadaneMaterialLjubicaste() {
        assertEquals(emptyList<String>(), nadi(kotlin, Regex("""(?i)6750A4|D0BCFF|625B71|Purple40|Purple80|PurpleGrey""")))
    }

    @Test
    fun razmaciNaRubuStringaNeSmijuNestati() {
        // aapt2 briše razmake na početku i kraju neprotumačenog stringa, pa " · " na uređaju postaje "·".
        // Razmak se čuva navodnicima ("\" · \"") ili  .
        val rubniRazmak = Regex("""<string name="([^"]+)">(\s.*|.*\s)</string>""")
        val nadeno = stringovi.readLines().mapIndexedNotNull { i, l ->
            rubniRazmak.find(l)?.takeUnless { it.groupValues[2].trim().startsWith("\"") }
                ?.let { "strings.xml:${i + 1}  ${it.groupValues[1]} = \"${it.groupValues[2]}\"" }
        }
        assertEquals(emptyList<String>(), nadeno)
    }

    @Test
    fun nemaEmojijaUTekstovima() {
        val uStringovima = stringovi.readLines().mapIndexedNotNull { i, l ->
            if (emoji.containsMatchIn(l)) "strings.xml:${i + 1}  ${l.trim()}" else null
        }
        val uKodu = nadi(kotlin, Regex("\"[^\"]*" + emoji.pattern + "[^\"]*\""))

        assertEquals(
            "Emoji kao ikona nije dopušten (koristi Material Symbols); nađeno ${uStringovima.size + uKodu.size}",
            emptyList<String>(), uStringovima + uKodu
        )
    }
}
