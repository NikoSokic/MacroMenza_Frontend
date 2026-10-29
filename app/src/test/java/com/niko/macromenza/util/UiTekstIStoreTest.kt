package com.niko.macromenza.util

import com.niko.macromenza.model.PrijedlogObroka
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class UiTekstTest {

    @Test
    fun resursJeJednakPoIdIArgumentima() {
        // StateFlow ne emitira novu vrijednost ako je jednaka, pa jednakost mora gledati sadržaj niza.
        assertEquals(UiTekst.Resurs(1, "a", 2), UiTekst.Resurs(1, "a", 2))
        assertEquals(UiTekst.Resurs(1, "a", 2).hashCode(), UiTekst.Resurs(1, "a", 2).hashCode())
        assertEquals(UiTekst.Resurs(5), UiTekst.Resurs(5))
    }

    @Test
    fun resursSRazlicitimIdomIliArgumentimaNijeJednak() {
        assertNotEquals(UiTekst.Resurs(1, "a"), UiTekst.Resurs(2, "a"))
        assertNotEquals(UiTekst.Resurs(1, "a"), UiTekst.Resurs(1, "b"))
        assertNotEquals(UiTekst.Resurs(1), UiTekst.Resurs(1, 0))
        val resurs: UiTekst = UiTekst.Resurs(1)
        val dinamicki: UiTekst = UiTekst.Dinamicki("1")
        assertNotEquals(resurs as Any, dinamicki as Any)
    }

    @Test
    fun dinamickiTekstJeJednakPoSadrzaju() {
        assertEquals(UiTekst.Dinamicki("Masa mora biti između 20 i 400 kg."), UiTekst.Dinamicki("Masa mora biti između 20 i 400 kg."))
        assertNotEquals(UiTekst.Dinamicki("a"), UiTekst.Dinamicki("b"))
    }
}

class PrijedlogObrokaStoreTest {

    @After
    fun ocisti() {
        PrijedlogObrokaStore.preuzmi()
    }

    @Test
    fun prijedlogSePreuzimaSamoJednom() {
        val prijedlog = PrijedlogObroka(napomena = "Okvirna procjena.")

        PrijedlogObrokaStore.postavi(prijedlog, "SLIKA")
        val prvi = PrijedlogObrokaStore.preuzmi()

        assertSame(prijedlog, prvi!!.prijedlog)
        assertEquals("SLIKA", prvi.izvor)
        assertNull(PrijedlogObrokaStore.preuzmi())
    }

    @Test
    fun noviPrijedlogPrepisujeNepreuzeti() {
        PrijedlogObrokaStore.postavi(PrijedlogObroka(napomena = "stari"), "OPIS")
        PrijedlogObrokaStore.postavi(PrijedlogObroka(napomena = "novi"), "SLIKA")

        val c = PrijedlogObrokaStore.preuzmi()!!
        assertEquals("novi", c.prijedlog.napomena)
        assertEquals("SLIKA", c.izvor)
    }

    @Test
    fun bezPostavljanjaNemaNicega() {
        assertNull(PrijedlogObrokaStore.preuzmi())
    }
}
