package com.niko.macromenza.util

import com.niko.macromenza.model.PrijedlogObroka

/**
 * Predaja prijedloga iz ekrana "Slika ili opis" u unos obroka.
 * Unos obroka ga preuzima (i briše) kad se otvori ili vrati u prvi plan.
 */
object PrijedlogObrokaStore {

    class Cekajuci(val prijedlog: PrijedlogObroka, val izvor: String)

    @Volatile
    private var cekajuci: Cekajuci? = null

    fun postavi(prijedlog: PrijedlogObroka, izvor: String) {
        cekajuci = Cekajuci(prijedlog, izvor)
    }

    fun preuzmi(): Cekajuci? {
        val c = cekajuci
        cekajuci = null
        return c
    }
}
