package com.niko.macromenza.screens.slikailiopis

import com.niko.macromenza.model.PrijedlogObroka
import com.niko.macromenza.model.PrijedlogStavka
import com.niko.macromenza.ui.components.Makro

/** Vrijednosti stavke prijedloga su već za cijelu količinu (backend ih množi), pa se ne množe ponovno. */
fun PrijedlogStavka.uMakro() = Makro(kalorije, proteini, masti, ugljikohidrati)

/** Zbroj svih stavki prijedloga (prazan prijedlog daje [Makro.NULA]). */
fun PrijedlogObroka.ukupno(): Makro = stavke.fold(Makro.NULA) { zbroj, stavka -> zbroj + stavka.uMakro() }

/** Sve osim VISOKE sigurnosti traži da korisnik provjeri količinu i jelo. */
fun PrijedlogStavka.trebaProvjeriti(): Boolean = sigurnost != "VISOKA"
