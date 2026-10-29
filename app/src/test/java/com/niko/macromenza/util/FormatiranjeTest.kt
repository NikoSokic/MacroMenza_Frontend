package com.niko.macromenza.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Hrvatski brojevi: decimalni zarez, bez tisućica, kcal bez decimala. */
class FormatiranjeTest {

    @Test
    fun kalorijeBezDecimala() {
        assertEquals("1350", formatKcal(1350.25))
        assertEquals("2798", formatKcal(2797.75))
        assertEquals("0", formatKcal(0.0))
        assertEquals("12345", formatKcal(12345.0)) // bez razdjelnika tisućica
    }

    @Test
    fun makronutrijentiNaJednuDecimaluSZarezom() {
        assertEquals("36,0", formatMakro(36.04))
        assertEquals("0,0", formatMakro(0.0))
        assertEquals("18,6", formatMakro(18.55))
    }

    @Test
    fun gramiICijena() {
        assertEquals("68", formatGrami(67.5))
        assertEquals("1,99", formatCijena(1.99))
        assertEquals("0,39", formatCijena(0.39))
        assertEquals("3,00", formatCijena(3.0))
    }

    @Test
    fun kolicinaPorcijaCijeliBrojBezDecimala() {
        assertEquals("1", formatKolicina(1.0))
        assertEquals("2", formatKolicina(2.0))
        assertEquals("0,5", formatKolicina(0.5))
        assertEquals("1,5", formatKolicina(1.5))
        assertEquals("99,5", formatKolicina(99.5))
    }

    @Test
    fun masaSJedinicom() {
        assertEquals("72,4 kg", formatMasa(72.4))
        assertEquals("80,0 kg", formatMasa(80.0))
    }

    @Test
    fun promjenaMaseImaPredznakIPravuMinusZnaku() {
        assertEquals("−1,2 kg", formatPromjenaMase(-1.2)) // U+2212, ne crtica
        assertEquals("+0,3 kg", formatPromjenaMase(0.3))
        assertEquals("0,0 kg", formatPromjenaMase(0.0))
        // Šum pomičnog zareza (80,0 − 80,1) ne smije dati "-0,1000000000058"
        assertEquals("−0,1 kg", formatPromjenaMase(80.0 - 80.1))
        assertEquals("+0,1 kg", formatPromjenaMase(80.1 - 80.0))
        // Razlika koja se zaokružuje na nulu nema predznak
        assertEquals("0,0 kg", formatPromjenaMase(0.04))
        assertEquals("0,0 kg", formatPromjenaMase(-0.04))
    }

    @Test
    fun unosMaseDopustaZnamenkeIJedanSeparatorSJednomDecimalom() {
        listOf("", "7", "72", "72,", "72.", "72,4", "72.4", "123,4", "400", ".5", ",5").forEach {
            assertEquals("'$it' treba proći", it, filtrirajUnosMase(it))
        }
    }

    @Test
    fun unosMaseOdbijaNeispravno() {
        listOf("72,44", "72.45", "72,4,1", "72,4.1", "1234", "1234,5", "abc", "72 kg", "-72", "7e2").forEach {
            assertNull("'$it' treba odbiti", filtrirajUnosMase(it))
        }
    }

    @Test
    fun parsiranjeMase() {
        assertEquals(72.4, parsirajMasu("72,4")!!, 0.0)
        assertEquals(72.4, parsirajMasu("72.4")!!, 0.0)
        assertEquals(80.0, parsirajMasu(" 80 ")!!, 0.0)
        assertNull(parsirajMasu(""))
        assertNull(parsirajMasu("x"))
        assertNull(parsirajMasu("72,4,1"))
    }
}
