package com.niko.macromenza.screens.unosobroka

import com.niko.macromenza.model.CestoJedenoJelo
import com.niko.macromenza.model.Jelo
import com.niko.macromenza.model.KategorijaJela
import com.niko.macromenza.model.MeniDTO
import com.niko.macromenza.model.MeniStavkaDTO
import com.niko.macromenza.viewmodel.MakroFilter
import java.time.LocalDate
import java.time.LocalTime

/*
 * ⚠ LAŽNI PODACI, SAMO ZA @Preview. Ne koristiti u aplikaciji.
 * Nazivi su prava jela iz menze (jela.csv, meni_stavke.csv); vrijednosti su okvirne i ručno upisane
 * (u aplikaciji dolaze iz v_jelo_nutritivno preko /api/jela i /api/meniji).
 */

private val DANAS_PRIMJERA: LocalDate = LocalDate.of(2026, 10, 26)

private fun jelo(id: Long, naziv: String, kategorija: String, kcal: Double, p: Double, m: Double, u: Double) =
    Jelo(id, naziv, null, kcal, p, u, m, null, kategorija, null, null)

private val JUHA_KRUMPIR = jelo(2, "Juha od krumpira", "Juhe", 172.0, 4.0, 7.0, 23.0)
private val JUHA_POVRCE = jelo(3, "Juha od povrća", "Juhe", 133.0, 3.0, 6.0, 17.0)
private val PECENA_PILETINA = jelo(40, "Pečena piletina", "Glavna jela", 488.0, 45.0, 32.0, 2.0)
private val PILECI_PAPRIKAS = jelo(41, "Pileći paprikaš", "Glavna jela", 531.0, 38.0, 27.0, 34.0)
private val PILECI_FILE = jelo(72, "Pileći file na žaru", "Glavna jela", 253.0, 44.0, 8.0, 1.0)
private val SARMA = jelo(37, "Sarma", "Glavna jela", 452.0, 22.0, 29.0, 26.0)
private val PUNJENA_PAPRIKA = jelo(36, "Punjena paprika", "Glavna jela", 512.0, 24.0, 26.0, 44.0)
private val GRAH = jelo(171, "Grah varivo", "Variva", 472.0, 24.0, 14.0, 62.0)
private val FINO_VARIVO = jelo(170, "Fino varivo", "Variva", 302.0, 9.0, 13.0, 37.0)
private val SALATA_KUPUS = jelo(181, "Salata od svj. kupusa", "Salate", 125.0, 2.0, 10.0, 7.0)
private val JOGURT = jelo(209, "Jogurt", "Mliječni proizvodi", 114.0, 6.0, 6.0, 9.0)
private val PANIRANI_FILE = jelo(232, "Panirani pileći file", "Jela po narudžbi", 522.0, 39.0, 27.0, 30.0)
private val SALATA_PILETINA = jelo(258, "Salata s piletinom i tjesteninom", "Brza jela i doručak", 626.0, 33.0, 31.0, 53.0)

private val KATEGORIJE_PRIMJERA = listOf(
    KategorijaJela("Juhe", 17),
    KategorijaJela("Glavna jela", 62),
    KategorijaJela("Variva", 14),
    KategorijaJela("Salate", 16),
    KategorijaJela("Mliječni proizvodi", 6),
    KategorijaJela("Jela po narudžbi", 12),
    KategorijaJela("Brza jela i doručak", 18)
)

private val SVA_JELA = listOf(
    JUHA_KRUMPIR, JUHA_POVRCE, PECENA_PILETINA, PILECI_PAPRIKAS, PILECI_FILE, SARMA, PUNJENA_PAPRIKA,
    GRAH, FINO_VARIVO, SALATA_KUPUS, JOGURT
)

private fun stavka(id: Long, oznaka: String, kolicina: Double = 1.0, priblizno: Boolean = false, kcal: Double) =
    MeniStavkaDTO(id, oznaka, oznaka, kolicina, priblizno, kcal, 0.0, 0.0, 0.0)

private val MENIJI_PRIMJERA = listOf(
    MeniDTO(
        12, "R12", "RUCAK", false, 12, 1298.0, 66.0, 160.0, 42.0,
        listOf(
            stavka(12, "Juha ragu pileća", priblizno = true, kcal = 254.0),
            stavka(40, "Pečena piletina", kcal = 488.0),
            stavka(128, "Mlinci", kcal = 286.0),
            stavka(190, "Salata od mrkvice", kcal = 90.0),
            stavka(205, "Kruh (3 šnite)/pecivo", kolicina = 3.0, kcal = 180.0)
        )
    ),
    MeniDTO(
        2, "R2", "RUCAK", false, 2, 1185.0, 49.0, 151.0, 38.0,
        listOf(
            stavka(2, "Juha od krumpira", kcal = 172.0),
            stavka(20, "Pečena svinjetina", kcal = 282.0),
            stavka(170, "Grah varivo s tjesteninom", kcal = 436.0),
            stavka(181, "Salata od svj. kupusa", kcal = 125.0),
            stavka(205, "Kruh (3 šnite)/pecivo", kolicina = 3.0, kcal = 170.0)
        )
    ),
    MeniDTO(
        21, "R21", "RUCAK", true, 21, 980.0, 31.0, 140.0, 30.0,
        listOf(
            stavka(3, "Juha od povrća", kcal = 133.0),
            stavka(160, "Musaka od šampinjona", kcal = 422.0),
            stavka(192, "Zelena salata", kcal = 60.0),
            stavka(205, "Kruh (3 šnite)/pecivo", kolicina = 3.0, kcal = 180.0)
        )
    ),
    MeniDTO(30, "R30", "RUCAK", false, 30, 1210.0, 58.0, 150.0, 40.0, listOf(stavka(37, "Sarma", kcal = 452.0))),
    MeniDTO(
        43, "V1", "VECERA", false, 1, 1094.0, 41.0, 128.0, 44.0,
        listOf(
            stavka(19, "Paprikaš svinjski", kcal = 313.0),
            stavka(146, "Slani krumpir", kcal = 284.0),
            stavka(192, "Zelena salata", kcal = 60.0),
            stavka(205, "Kruh (3 šnite)/pecivo", kolicina = 3.0, kcal = 180.0),
            stavka(209, "Jogurt", kcal = 114.0)
        )
    )
)

private val CESTO_PRIMJERA = listOf(
    CestoJedenoJelo(PILECI_FILE, 9, "2026-10-23"),
    CestoJedenoJelo(GRAH, 6, "2026-10-21"),
    CestoJedenoJelo(JOGURT, 5, "2026-10-25")
)

val PRIMJER_ZADANO = UnosObrokaUi(
    tip = "RUCAK",
    uredivanje = false,
    ucitavanjeObroka = false,
    mozeBrisati = false,
    spremanjeUTijeku = false,
    datum = DANAS_PRIMJERA,
    danas = DANAS_PRIMJERA,
    vrijeme = LocalTime.of(12, 40),
    nakonObroka = 1510.0,
    ciljKalorije = 2400.0,
    upit = "",
    kategorija = null,
    kategorije = KATEGORIJE_PRIMJERA,
    makroFilter = MakroFilter(),
    jela = StanjeJela.Sadrzaj(SVA_JELA),
    cesto = CESTO_PRIMJERA,
    meniji = StanjeMenija.Sadrzaj(MENIJI_PRIMJERA),
    tipMenija = null,
    samoVegetarijanski = false,
    kolicine = mapOf(PILECI_FILE.id to 1.5, SALATA_KUPUS.id to 1.0)
)

val PRIMJER_PRETRAGA = PRIMJER_ZADANO.copy(
    upit = "pile",
    makroFilter = MakroFilter(minProteini = 25.0),
    jela = StanjeJela.Sadrzaj(listOf(PECENA_PILETINA, PILECI_PAPRIKAS, PILECI_FILE, PANIRANI_FILE, SALATA_PILETINA))
)

val PRIMJER_BEZ_REZULTATA = PRIMJER_ZADANO.copy(
    upit = "ćevapi s kajmakom",
    jela = StanjeJela.Sadrzaj(emptyList())
)

val PRIMJER_UREDIVANJE = PRIMJER_ZADANO.copy(
    tip = "VECERA",
    uredivanje = true,
    mozeBrisati = true,
    datum = DANAS_PRIMJERA.minusDays(2),
    vrijeme = LocalTime.of(19, 15),
    nakonObroka = 2560.0,
    kolicine = mapOf(SARMA.id to 1.0, SALATA_KUPUS.id to 0.5, JOGURT.id to 2.0)
)

val PRIMJER_UCITAVANJE = PRIMJER_ZADANO.copy(
    tip = "DORUCAK",
    vrijeme = LocalTime.of(8, 10),
    nakonObroka = null,
    jela = StanjeJela.Ucitavanje,
    cesto = emptyList(),
    meniji = StanjeMenija.Ucitavanje,
    kolicine = emptyMap()
)

val PRIMJER_GRESKA = PRIMJER_ZADANO.copy(
    jela = StanjeJela.Greska,
    cesto = emptyList(),
    meniji = StanjeMenija.Greska("Meniji se nisu učitali. Provjeri vezu s poslužiteljem.")
)
