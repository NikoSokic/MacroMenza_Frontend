package com.niko.macromenza.testutil

import com.niko.macromenza.model.DanPregled
import com.niko.macromenza.model.Jelo
import com.niko.macromenza.model.KalorijeTipa
import com.niko.macromenza.model.MeniDTO
import com.niko.macromenza.model.MeniStavkaDTO
import com.niko.macromenza.model.Mjerenje
import com.niko.macromenza.model.NutritivneVrijednosti
import com.niko.macromenza.model.Obrok
import com.niko.macromenza.model.ObrokStavka
import com.niko.macromenza.model.PovijestDana
import com.niko.macromenza.model.Preporuka
import com.niko.macromenza.model.ProfilPodaci
import com.niko.macromenza.model.Savjet
import com.niko.macromenza.model.Tjedan
import com.niko.macromenza.model.TjedanDan

/** Jelo s vrijednostima po porciji. */
fun jelo(
    id: Long = 1,
    naziv: String = "Grah varivo",
    kcal: Double = 320.0,
    proteini: Double = 18.0,
    ugljikohidrati: Double = 40.0,
    masti: Double = 9.0,
    kategorija: String? = "Variva"
) = Jelo(id, naziv, null, kcal, proteini, ugljikohidrati, masti, null, kategorija, null, null)

/** Stavka obroka; vrijednosti su za cijelu količinu (kao u odgovoru backenda). */
fun stavka(
    idJelo: Long = 1,
    naziv: String = "Grah varivo",
    kolicina: Double = 1.0,
    kcal: Double = 320.0,
    proteini: Double = 18.0,
    ugljikohidrati: Double = 40.0,
    masti: Double = 9.0
) = ObrokStavka(idJelo, naziv, kolicina, kcal, proteini, ugljikohidrati, masti)

fun obrok(
    id: Long = 100,
    datum: String = "2026-10-28",
    vrijeme: String = "12:30",
    tip: String = "RUCAK",
    izvor: String = "RUCNO",
    stavke: List<ObrokStavka> = listOf(stavka()),
) = Obrok(
    id = id, datum = datum, vrijeme = vrijeme, tipObroka = tip, izvor = izvor, stavke = stavke,
    ukupnoKalorije = stavke.sumOf { it.kalorije },
    ukupnoProteini = stavke.sumOf { it.proteini },
    ukupnoUgljikohidrati = stavke.sumOf { it.ugljikohidrati },
    ukupnoMasti = stavke.sumOf { it.masti }
)

fun preporuka(id: Long = 1, kcal: Double = 2400.0) =
    Preporuka(id, 7, kcal, 160.0, 290.0, 67.0, "2026-10-01")

fun mjerenje(id: Long? = 1, masa: Double = 80.0, aktivnost: String = "umjerena", cilj: String = "odrzavanje") =
    Mjerenje(id = id, idKorisnik = 7, masa = masa, razinaAktivnosti = aktivnost, tipCilja = cilj, datum = "2026-10-01")

fun profil(id: Long? = 5, spol: String = "M", visina: Int = 180, dob: Int = 25) =
    ProfilPodaci(id = id, idKorisnik = 7, visina = visina, spol = spol, dob = dob)

fun vrijednosti(kcal: Double, p: Double = 0.0, u: Double = 0.0, m: Double = 0.0) =
    NutritivneVrijednosti(kcal, p, u, m)

fun danPregled(
    cilj: NutritivneVrijednosti? = vrijednosti(2400.0, 160.0, 290.0, 67.0),
    pojedeno: NutritivneVrijednosti = vrijednosti(1000.0, 50.0, 120.0, 30.0)
) = DanPregled(
    cilj, pojedeno,
    listOf(
        KalorijeTipa("DORUCAK", 400.0, 1), KalorijeTipa("RUCAK", 600.0, 1),
        KalorijeTipa("UZINA", 0.0, 0), KalorijeTipa("VECERA", 0.0, 0)
    ),
    Savjet("Savjet za menzu", "Juha prije glavnog jela brzo zasiti.", emptyList())
)

fun povijestDana(datum: String = "2026-10-28", kcal: Double = 1000.0) =
    PovijestDana(datum, kcal, 50.0, 120.0, 30.0, emptyMap())

fun tjedanDan(
    datum: String,
    kcal: Double = 0.0,
    cilj: Double? = 2400.0,
    imaUnos: Boolean = kcal > 0,
    uCilju: Boolean = false
) = TjedanDan(datum, kcal, kcal / 20, kcal / 8, kcal / 36, cilj, imaUnos, uCilju)

/** Tjedan od ponedjeljka [pocetak] (ISO); dani nemaju unos osim ako se zadaju. */
fun tjedan(
    pocetak: String = "2026-10-26",
    kraj: String = java.time.LocalDate.parse(pocetak).plusDays(6).toString(),
    dani: List<TjedanDan> = (0..6).map { tjedanDan(java.time.LocalDate.parse(pocetak).plusDays(it.toLong()).toString()) },
    imaPrethodni: Boolean = true,
    imaSljedeci: Boolean = false,
    imaPrimjer: Boolean = false,
    prosjek: NutritivneVrijednosti? = null,
    prosjekCilj: NutritivneVrijednosti? = vrijednosti(2400.0, 160.0, 290.0, 67.0)
) = Tjedan(
    pocetak, kraj, dani, dani.count { it.imaUnos }, dani.count { it.uCilju },
    prosjek, prosjekCilj, imaPrethodni, imaSljedeci, imaPrimjer
)

fun meniStavka(idJelo: Long, naziv: String, kolicina: Double, kcal: Double, p: Double = 0.0, u: Double = 0.0, m: Double = 0.0) =
    MeniStavkaDTO(idJelo, naziv, naziv, kolicina, false, kcal, p, u, m)

fun meni(id: Long = 1, tip: String = "RUCAK", vegetarijanski: Boolean = false, vararg stavke: MeniStavkaDTO) =
    MeniDTO(
        id, "R$id", tip, vegetarijanski, id.toInt(),
        stavke.sumOf { it.kalorije }, stavke.sumOf { it.proteini },
        stavke.sumOf { it.ugljikohidrati }, stavke.sumOf { it.masti }, stavke.toList()
    )
