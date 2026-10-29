# Pregled frontenda: tema „Pladanj", dosljednost i višak

Svi koraci iz §7 riješeni (29. 10. 2026.)

> Datum: 28. 10. 2026. Pregledan je samo `frontend/` (bez backenda). Kod, resursi i postojeći dokumenti nisu mijenjani.
> Putanje su relativne prema `frontend/app/src/main/java/com/niko/macromenza/`, osim kad počinju s `app/`, `res/` ili `frontend/`.
> Metoda: čitanje `CLAUDE.md`, `design-system/macromenza/MASTER.md`, svih `pages/*.md`, `ui/theme/` i `ui/components/`; `rg`/`grep` po nazivima i vrijednostima; cijelo čitanje ekrana koji nisu na Pladnju; `gradlew assembleDebug`, `gradlew lint` i ponovno `compileDebugKotlin` radi upozorenja.
> **Nije provjereno:** aplikacija nije pokrenuta (emulator/mobitel), pa izgled pri fontScale 1,3, stvarni kontrasti i ponašanje sustavnih traka nisu viđeni uživo. Kod Pladanj ekrana dodirni ciljevi i `contentDescription` nisu pregledani redak po redak, nego uzorkom i grepom.

---

## 1. Sažetak

Aplikacija je u dvije jasno odvojene polovice: 11 glavnih ekrana (Home, kartica Unos, unos obroka, Povijest, Detalj dana, cijeli tab Profil, Slika ili opis) dosljedno je na Pladnju, bez ijedne heks, dp ili sp vrijednosti, sa stringovima u `strings.xml` i previewima u obje teme. Osam datoteka uopće nije prebačeno: Splash, Welcome, Login, Register, Onboarding, ObrokSpremljen, `SazetakObrokaBar` i `JeloDetaljSheet`; one i dalje koriste `MacroGreen`, `Color.White`, plavu/narančastu/ljubičastu za makronutrijente, ukrasne krugove, spinnere, kartice za grupiranje i tekst izravno u kodu. Najviše odskaču `SazetakObrokaBar` i `JeloDetaljSheet`, jer se vide usred inače prebačenog toka unosa obroka (zelena traka i „UH" ispod porculanskih redova jela) i na Home i kartici Unos. Drugi vidljivi problem je plutajuća navigacija (`MacroBottomBar`): animira širinu stavke s oprugom koja odskače, ima vlastite dp vrijednosti, sjenu i `Icons.Default` umjesto `Rounded`. Višak je mali i lako se briše: sedam starih boja u `Color.kt` (plus `MacroLightGreen` koji se samo uvozi), sve boje u `colors.xml`, jedan string, pet nekorištenih Retrofit metoda s dva modela i datoteke koje ne pripadaju repozitoriju (`app/release/` s APK-om od 12 MB, `.kotlin/errors/`, dijelovi `.idea/`).

---

## 2. Tablica ekrana

Stupci: **Pladanj** = boje, tipografija, oblici i razmaci iz `ui/theme/`; **spec** = postoji li `pages/*.md`; **stanja** = kostur, prazno, greška s „Pokušaj ponovno"; **tamna** = ima li nešto što u tamnoj temi ispada krivo; **pristupačnost** = prema kodu (nije provjereno na uređaju).

| Ekran | Pladanj | Page spec | Stanja | Tamna tema | Pristupačnost | Napomena |
|---|---|---|---|---|---|---|
| Splash (`screens/SplashScreen.kt`) | da (korak 8) | `prijava.md` § Splash | spinner (`:110`) | zelena ploha s `onPrimary` (`:70`, `:80`) | slogan u 65 % prigušene boje (`:123`), vjerojatno < 4,5:1 | prikazuje se dok se čita sesija (`navigation/NavGraph.kt:88`) |
| Welcome (`WelcomeScreen.kt`) | da (korak 8) | `prijava.md` § Welcome | — | krugovi i zelena kao u svijetloj | `fontSize` u kodu, ne skalira se po stilu | vlastiti „logo" s „M" (`:83-98`) |
| Login (`LoginScreen.kt`) | da (korak 7) | `prijava.md` | spinner u gumbu (`:255`) | `Color.White` na zelenoj (`:247`, `:258`) | lozinka ima `contentDescription` (`:195-200`) | — |
| Register (`RegisterScreen.kt`) | da (korak 7) | `prijava.md` | spinner u gumbu (`:324`) | isto kao Login | — | uspjeh u zelenoj (`:278-289`) |
| Onboarding (`OnboardingScreen.kt`) | da (korak 9) | `profil.md` § Onboarding | spinneri (`:443`, `:541`) | `tonalElevation` kartice (`:568`) | `RadioButton` u retku, ali red nije `selectable` (`:632-665`) | jedino `KakoSmoIzracunaliKartica` (`:390`) je na Pladnju |
| Home (`HomeScreen.kt`, `screens/dnevnipregled/`) | da | `daily-overview.md` | sva (kostur, prazno, greška, preko cilja) | ok | ok prema kodu | otvara stari `JeloDetaljSheet` (`HomeScreen.kt:166`) |
| Kartica Unos (`KonzumacijaScreen.kt`) | da | `meal-logging.md` | ok | ok | ok | otvara stari `JeloDetaljSheet` (`:189`); prefiks resursa `ko_` uz `uo_` |
| Unos obroka (`UnosObrokaScreen.kt`, `screens/unosobroka/`) | djelomično | `meal-logging.md` (paket 1) | ok | ok | ok | dno ekrana je stari `SazetakObrokaBar` (`:354`), dijalog odbacivanja na zadanom M3 (`:407-427`) |
| Sažetak obroka (`SazetakObrokaBar.kt`) | da (korak 3) | nema („paket 2 još nije prebačen", `meal-logging.md:4`) | — | plava/narančasta/ljubičasta iste u obje teme (`:42-44`) | gumbi 32 i 40 dp (`:327`, `:345`, `:356`), tekst 11 sp (`:281`) | animira visinu (`:90-91`) |
| Detalj jela (`JeloDetaljSheet.kt`) | da (korak 3) | nema (paket 2) | spinner (`:114`), greška s „Pokušaj ponovno" u zelenoj (`:102`) | narančasta podloga napomene (`:270`) | gumbi 40 dp (`:399`, `:424`), tekst 11 sp (`:157`, `:377`) | koriste ga Home, kartica Unos i unos obroka |
| Obrok spremljen (`ObrokSpremljenScreen.kt`) | ne | nema | — | `Color.White` na zelenom krugu (`:108`) | — | MASTER §8 traži `Tanjur` na potvrdi spremanja, ovdje ga nema |
| Povijest (`PovijestScreen.kt`, `screens/povijest/`) | da | `povijest.md` | sva | ok | ok | — |
| Detalj dana (`DetaljDanaScreen.kt`) | da | `povijest.md` § Detalj dana | ok | ok | ok | tekst snackbara u kodu (`:181-182`) |
| Profil (`ProfilScreen.kt`, `screens/profil/`) | da | `profil.md` | sva | ok | ok | — |
| Uredi profil (`UrediProfilScreen.kt`) | da | `profil.md` | ok | ok | ok | — |
| Postavke ciljeva (`PostavkeCiljevaScreen.kt`) | da | `profil.md` | ok | ok | ok | kcal s točkom tisućica (§3) ✅ korak 11 |
| Moje mjere (`MojeMjereScreen.kt`, `GrafMase.kt`, `BrzoMjerenjeKartica.kt`) | da | `profil.md` | ok | ok | ok | „u 1 dana" (§3) |
| Postavke aplikacije (`PostavkeAplikacijeScreen.kt`) | da | `profil.md` | ok | ok | ok | — |
| Odakle su kalorije (`OdakleSuKalorijeScreen.kt`) | da | `profil.md` | ok | ok | ok | — |
| Slika ili opis (`SlikaIliOpisScreen.kt`, `screens/slikailiopis/`) | da | `slika-ili-opis.md` | sva | ok | ok | — |
| Plutajuća navigacija (`navigation/MacroBottomBar.kt`) | da (korak 5) | MASTER §8 „Donja navigacija" | — | traka bez sjene, rub `outlineVariant` | `Role.Tab`, `selectableGroup`, ikona bez `contentDescription` (tekst je vidljiv) | previewi svijetli/tamni × 4 taba, 320 dp i fontScale 1,3 |

**Previewi:** svi prebačeni ekrani imaju svijetle i tamne previewe s više stanja, većinom i fontScale 1,3 (npr. `screens/povijest/PovijestSadrzaj.kt` 25, `screens/slikailiopis/SlikaIliOpisSadrzaj.kt` 21, `screens/profil/ProfilSadrzaj.kt` 13). Kartica Unos ima samo dva tamna (`KonzumacijaScreen.kt:405-421`). Osam starih datoteka i `MacroBottomBar` nemaju nijedan preview.

**Ukupno:** 13 ekrana „da", 2 „djelomično" (unos obroka, plutajuća navigacija), 6 „ne" (stanje nakon koraka 8).

---

## 3. Odstupanja od teme po ekranu

P1 = vidljivo korisniku, P2 = sitno.

### Splash (`screens/SplashScreen.kt`)
| Što ne valja | Redak | Prijedlog | Prioritet |
|---|---|---|---|
| Ukrasni krugovi u kutovima (paprika 10 % i zelena 6 %) | `:31-57` | Ukloniti; pozadina je samo `background`. | P1 |
| Logo je zelena ploha sa sjenom 8 dp i „M" od 46 sp bold | `:67-83` | Krug kao u Postavkama aplikacije (`avatarProfila`, `surface`, rub `linija`, „M" u Frauncesu). | P1 |
| Naslov `fontSize = 34.sp`, `FontWeight.Bold` | `:91-92` | `headlineSmall` (Fraunces). | P1 |
| `CircularProgressIndicator` u zelenoj | `:110-114` | Kostur ili prazan tanjur koji diše (kao Slika ili opis). | P1 |
| Slogan u `onSurfaceVariant` s alfom 0,65 | `:123-125` | Bez alfe (prigušeni tekst mora ostati ≥ 4,5:1). | P2 |
| Svi tekstovi u kodu | `:90`, `:101`, `:118` | U `strings.xml`. | P2 |

### Welcome (`screens/WelcomeScreen.kt`)
| Što ne valja | Redak | Prijedlog | Prioritet |
|---|---|---|---|
| Ukrasni krugovi | `:49-69` | Ukloniti. | P1 |
| „Menza" u zelenoj, zelena crtica ispod naslova | `:128`, `:141-147` | Naslov u `onBackground`, bez crtice. | P1 |
| Gumb „Registriraj se" zelen s `Color.White`, „Prijavi se" sa zelenim obrubom | `:195-196`, `:229-238` | `GumbSpremi` (paprika) + obrubljeni gumb u `onSurface`. | P1 |
| Ikone značajki u zelenoj na podlozi paprike 18 % (dvije naglasne boje zajedno) | `:258-271` | Ikone u `onSurfaceVariant` bez obojene podloge, `Icons.Rounded`. | P1 |
| Veličine ovisne o visini ekrana (`28/60.dp`, `36/42.sp`) i `fontSize` na svakom tekstu | `:41-46`, `:94`, `:108`, `:135`, `:204`, `:236` | Stilovi iz `Typography` i `MacroRazmaci`. | P2 |
| `Icons.Default.*` i `AutoMirrored.Filled` | `:165-175`, `:210` | `Icons.Rounded` / `AutoMirrored.Rounded`. | P2 |

### Login i Register (`screens/LoginScreen.kt`, `screens/RegisterScreen.kt`)
| Što ne valja | Redak | Prijedlog | Prioritet |
|---|---|---|---|
| Ukrasni krugovi | Login `:76-96`, Register `:62-82` | Ukloniti. | P1 |
| Ikona u kvadratu s paprikom 18 % i zelenom ikonom | Login `:108-123`, Register `:93-108` | Ukloniti ili krug `avatarProfila` bez boje. | P1 |
| Polja `OutlinedTextField` sa zelenim fokusom, oblik 16 dp, visina 64 dp | Login `:144-212`, Register `:129-249` | `PoljeUnosa` (lozinka treba `visualTransformation` i `trailingIcon`, kojih `PoljeUnosa` sada nema). | P1 |
| Gumb zelen s `Color.White` i spinnerom | Login `:233-267`, Register `:301-336` | `GumbSpremi` (`sprema = true` piše „Spremam…"; za prijavu tekst „Prijavljujem…"). | P1 |
| Link „Registriraj se"/„Prijavi se" u zelenoj | Login `:290`, Register `:359` | `primary` (linkovi smiju biti paprika). | P1 |
| Greška/uspjeh na obojenoj podlozi (error 8 %, zelena 10 %) | Login `:217-228`, Register `:275-296` | Tihi red s ikonom (`ErrorOutline` / `Check`) kao u Moje mjere. | P2 |
| Naslov `32.sp` Bold | Login `:129-130`, Register `:114-115` | `headlineSmall`. | P2 |
| `Icons.Default.Login` je zastario (upozorenje kompajlera) | Login `:118` | `Icons.AutoMirrored.Rounded.Login`. | P2 |
| Svi tekstovi u kodu | cijele datoteke | `strings.xml`, prefiks npr. `auth_`. | P2 |

### Onboarding (`screens/OnboardingScreen.kt`)
| Što ne valja | Redak | Prijedlog | Prioritet |
|---|---|---|---|
| Ukrasni krugovi | `:90-102` | Ukloniti. | P1 |
| Sekcije u karticama (`SectionCard`, `tonalElevation`, ikona na paprici 18 %) | `:181`, `:238`, `:298`, `:338`, `:559-606` | Naslovi sekcija `headlineSmall` + razmak i divideri, kao Postavke ciljeva. | P1 |
| Spol i aktivnost kao `FilterChip` sa zelenim tekstom na paprici 22 % | `:220-231`, `:609-623`, `:697-700` | `IzborOpcije` (kao Uredi profil i Postavke ciljeva). | P1 |
| Cilj kao kartice s `RadioButton` u zelenoj i obojenom podlogom odabranog | `:626-686` | `IzborOpcije` s opisima („−500 kcal od dnevne potrošnje"). | P1 |
| Polja sa zelenim fokusom | `:185-293`, `:689-694` | `PoljeUnosa` s pomoćnim tekstom raspona i greškom ispod polja. | P1 |
| Traka napretka u zelenoj na paprici 18 %, fiksno 0,75 | `:169-177` | Ukloniti ili `onSurface` na `surfaceVariant`. | P2 |
| Gumb „Završi postavljanje" zelen, spinneri u gumbima | `:529-551`, `:443`, `:541` | `GumbSpremi`; obrubljeni „Pokaži kako se računa moj cilj" u `onSurface`. | P1 |
| Poruka greške na obojenoj podlozi | `:371-381` | Tihi red ispod gumba (kao Postavke ciljeva). | P2 |
| Svi tekstovi u kodu (52 literala) | cijela datoteka | `strings.xml`, prefiks `onb_`. | P2 |
| Nema page speca | — | Spec nije nužan ako ekran slijedi `profil.md` § Postavke ciljeva; dovoljno to napisati u MASTER. | P2 |

### Unos obroka: `SazetakObrokaBar` (`screens/SazetakObrokaBar.kt`)
| Što ne valja | Redak | Prijedlog | Prioritet |
|---|---|---|---|
| Makronutrijenti u plavoj, narančastoj i ljubičastoj | `:42-44`, `:214-216` | `macroBoje.proteini/masti/ugljikohidrati` ili samo `kcalIMakroKratko`. | P1 |
| Oznaka „UH" i redoslijed P / UH / M | `:215` | P, M, U (MASTER §8: „nikad B ili UH"). | P1 |
| Zbroj kcal i kcal stavke u zelenoj, 24 sp bold | `:200-205`, `:309-314` | `TablicniBroj`/`brojSrednji` u `onSurface`. | P1 |
| Gumb „Spremi" zelen s `Color.White`; onemogućen gumb na M3 zadanom | `:221-236`, `:171-178` | Paprika (`primary`) iz `GumbSpremi` stila; ovo je jedina primarna akcija ekrana. | P1 |
| Prekoračenje „Nakon ovog obroka" u `error` | `:253-255` | `macroBoje.visnja` uz tekst „Preko cilja". | P1 |
| Koračnik porcija: ±32 dp, × 40 dp, zelene ikone, bez `PorcijaGlif` | `:319-363` | 48 dp ciljevi, `PorcijaGlif` uz količinu, tonski gumbi kao u Moje mjere. | P1 |
| Otvaranje liste animira visinu (`expandVertically`/`shrinkVertically`) | `:88-92` | `fadeIn`/`slideIn` kroz `graphicsLayer` (MASTER §5). | P2 |
| Vlastiti oblik 28 dp, sjena 8 dp, `tonalElevation` | `:82-85` | `MacroOblici.pladanj`, sjena samo ako spec to kaže. | P2 |
| „2 stavki" (krivi oblik za 2–4) | `:195` | `pluralStringResource(R.plurals.kom_stavki, …)` (već postoji). | P1 |
| „Spremam..." s tri točke | `:232` | „Spremam…" kao `GumbSpremi`. | P2 |
| Tekst 11 sp (ispod najmanjih 12) | `:281` | `labelSmall`. | P2 |

### Detalj jela: `JeloDetaljSheet` (`screens/JeloDetaljSheet.kt`)
| Što ne valja | Redak | Prijedlog | Prioritet |
|---|---|---|---|
| Makronutrijenti u plavoj, narančastoj, ljubičastoj; „UH" | `:36-38`, `:229-231` | Boje iz `macroBoje` i P/M/U, ili `Tanjur` s rubom P/M/U. | P1 |
| kcal u zelenoj 28 sp; kcal sastojka u zelenoj | `:210-215`, `:344-351` | `TablicniBroj`/`brojSrednji` u `onSurface`; kcal sastojka prigušeno. | P1 |
| Vrijednosti u kartici (`Surface` 22 dp, `tonalElevation`) | `:203-262` | Na pozadini sheeta, razmak i divideri. | P1 |
| Napomena na narančastoj podlozi s narančastom ikonom | `:267-291` | Tihi red: `Info` u `onSurfaceVariant`, tekst `bodyMedium`. | P2 |
| Učitavanje je zeleni spinner | `:107-116` | `KosturOblik` (naslov, broj, tri reda). | P1 |
| „Pokušaj ponovno" u zelenoj | `:101-103` | `primary` kao link (kao tihi redovi drugdje). | P2 |
| „Dodaj u obrok" zelen; koračnik s paprikom 16 % i zelenim krugom „+" 40 dp | `:133-148`, `:383-443` | Gumb u paprici, koračnik isti kao u `SazetakObrokaBar` nakon popravka, 48 dp. | P1 |
| Link „Odakle su kalorije?" u zelenoj, 12 sp, bez 48 dp cilja | `:161-172` | `TextButton` u `primary`, `heightIn(dodirniCilj)` kao na Home (`DnevniPregledSadrzaj.kt:520-526`). | P2 |
| Napomena okvirnosti 11 sp | `:154-159` | `bodySmall` bez `fontSize`. | P2 |
| Rub sheeta 22 dp umjesto `rubEkrana` | `:73` | `MacroRazmaci.rubEkrana`. | P2 |
| Svi tekstovi u kodu | cijela datoteka | `strings.xml`, prefiks `jd_`. | P2 |

### Obrok spremljen (`screens/ObrokSpremljenScreen.kt`)
| Što ne valja | Redak | Prijedlog | Prioritet |
|---|---|---|---|
| Ukrasni krugovi | `:39-59` | Ukloniti. | P1 |
| Zelena kvačica u dvostrukom krugu | `:89-112` | `Tanjur` spremljenog obroka (MASTER §8 navodi „potvrda spremanja"). | P1 |
| Kartica s porukom i podkartica na paprici 16 % | `:75-151` | Bez kartice; tekst na pozadini. | P1 |
| „Dodaj još" zelen, „Natrag na početnu" sa zelenim obrubom | `:155-204` | Paprika za jednu akciju, druga obrubljena u `onSurface`. | P1 |
| „Večera je uspješno dodan u današnji unos": krivi rod za Večeru i Užinu, i „današnji" kad je obrok za drugi datum | `:127` | Rečenica bez roda („Spremljeno: Večera, 12:40") iz `strings.xml`; datum iz obroka. | P1 |
| Vlastiti `formatTipObrokaSpremljen` | `:209-219` | `nazivTipa` iz `ui/components/Tekstovi.kt`. | P2 |
| Naslov 28 sp bold, gumbi 17 sp | `:119-120`, `:174`, `:201` | `headlineSmall`, `labelLarge`. | P2 |

### Unos obroka, prebačeni dio (`screens/UnosObrokaScreen.kt`)
| Što ne valja | Redak | Prijedlog | Prioritet |
|---|---|---|---|
| U uređivanju je plutajuća navigacija skrivena (`navigation/NavGraph.kt:62-71`), a sažetak, lista i snackbar i dalje ostavljaju mjesto za nju (`screenBottomPadding()` sa zadanim `hasBottomNavigation = true`) | `:351`, `:377`, `:388` | `screenBottomPadding(hasBottomNavigation = !uredivanje)`. **Provjeriti u aplikaciji** (iz koda slijedi praznina od oko 80 dp ispod sažetka). ✅ korak 11 | P1 |
| Dijalog „Odbaciti promjene?" na zadanom M3: bez `shape`/`containerColor`, „Nastavi" u zadanoj boji `TextButton`a (paprika) | `:407-427` | Isti oblik kao `PotvrdaBrisanjaObrokaDialog` (vidi §4). ✅ korak 6 | P2 |
| „U redu" u biraču datuma i vremena u zadanoj paprici, dok je u Moje mjere „U redu" u `onSurface` | `:465`, `:515` | Odlučiti jedno (§4). ✅ korak 6: „U redu" i „Odustani" u `onSurface` | P2 |

### Plutajuća navigacija (`navigation/MacroBottomBar.kt`, `navigation/BottomNav.kt`)
| Što ne valja | Redak | Prijedlog | Prioritet |
|---|---|---|---|
| Odabrana stavka animira `weight` (širinu layouta) oprugom `dampingRatio 0,75` (odskok) koja nije iz `Motion.kt` | `MacroBottomBar.kt:73-86` | Fiksne širine; indikator klizi kroz `graphicsLayer` uz `Opruge.blaga` (kao segment u Povijesti). | P1 |
| dp vrijednosti u kodu (16, 10, 72, 28, 8, 2, 52, 20, 24, 8) | `:32-48`, `:87-88`, `:107`, `:117` | Tokeni u `Dimens.kt`/`Shape.kt` (npr. `MacroOblici.pladanj`, `dodirniCilj`). | P2 |
| Sjena 8 dp i `tonalElevation` | `:42-43` | MASTER §3: plosnato; rub `outlineVariant` umjesto sjene ili token sjene. | P2 |
| `fontWeight` u kodu | `:123` | `labelLarge`. | P2 |
| `Icons.Default.*` (puni stil) | `BottomNav.kt:144-147` | `Icons.Rounded.*`. | P2 |
| Bez `skalaPritiska`, bez `Role.Tab`/`selected`; ikona ima `contentDescription` jednak vidljivom tekstu, pa čitač ponavlja naslov | `MacroBottomBar.kt:95-126` | `selectable(role = Role.Tab)`, ikona `contentDescription = null` kad je tekst vidljiv. | P2 |
| Naslovi tabova u kodu | `BottomNav.kt:144-147` | `strings.xml` (`nav_`). | P2 |

### Ostalo vidljivo izvan ekrana
| Što ne valja | Datoteka i redak | Prijedlog | Prioritet |
|---|---|---|---|
| Ikona aplikacije je zadana Android ikona (zelena `#3DDC84` i bijeli robot) | `res/drawable/ic_launcher_background.xml`, `res/drawable/ic_launcher_foreground.xml` | Ikona u paleti Pladnja (orah/porculan). ✅ korak 12 | P1 |
| Početni prozor je `android:Theme.Material.Light.NoActionBar`: u tamnoj temi pri hladnom pokretanju bljesne svijetla pozadina | `res/values/themes.xml:4` | `windowBackground` na papir/noć (`values-night`) ili DayNight roditelj. **Provjeriti na uređaju.** ✅ korak 12 | P2 |
| Traka sustavne navigacije: `darkScrim = BLACK`, `lightScrim = WHITE`, a `SystemBarStyle.auto` prati temu sustava, ne temu aplikacije | `MainActivity.kt:31-36` | Scrim u bojama pozadine i odabir prema `useDarkTheme`. **Provjeriti** na uređaju s navigacijom gumbima. ✅ korak 12 | P2 |
| `dynamicColor` parametar može zamijeniti cijelu paletu Material You bojama (zadano `false`, nitko ga ne postavlja) | `ui/theme/Theme.kt:80-88` | Ukloniti granu (vidi §5). | P2 |

### Prebačeni ekrani: sitnice
| Što ne valja | Redak | Prijedlog | Prioritet |
|---|---|---|---|
| Ikona vrste obroka u `ObrokRed` na `primaryContainer` (paprika) uz `onPrimaryContainer`: paprika kao ukras stanja, nije opisano ni u MASTER §8 ni u `povijest.md` | `ui/components/ObrokRed.kt:229-236` | `surfaceVariant`/`secondaryContainer` uz `onSurfaceVariant`, ili zapisati iznimku u MASTER. ✅ korak 11 | P2 |
| kcal s točkom tisućica („2.400") gdje MASTER kaže „kcal bez separatora (`1510`)" i `profil.md` piše „Cilj: 2400 kcal" | `screens/KakoSmoIzracunaliKartica.kt:38-40` (+ `GrafMase.kt:401`, `viewmodel/MjereViewModel.kt:69`, gdje `profil.md` izričito navodi „2.020") | Odlučiti jedno pravilo; za cilj u `KakoSmoIzracunali` `formatKcal`. ✅ korak 11 | P2 |
| „u %1$d dana" za razdoblje „Sve" daje „u 1 dana", „u 21 dana" | `res/values/strings.xml:428`, `screens/GrafMase.kt:165` | `plurals` („u 1 dan", „u 2 dana", „u 21 dan"). ✅ korak 11 | P2 |
| Kursor i ručice odabira teksta u zadanoj paprici u pretrazi, opisu i poljima filtera (samo `PoljeUnosa` postavlja `selectionColors`) | `screens/unosobroka/UnosObrokaSadrzaj.kt:472`, `screens/slikailiopis/SlikaIliOpisSadrzaj.kt:480`, `screens/unosobroka/FilterJela.kt:332` | `LocalTextSelectionColors` na tokenima (npr. u `MacroMenzaTheme`). ✅ korak 6 | P2 |
| Tekst snackbara „Obrok obrisan" / „Poništi" u kodu | `screens/DetaljDanaScreen.kt:181-182` | `strings.xml` (`kom_`), jer ga dijele Home, Povijest i Detalj dana. ✅ korak 11 | P2 |

---

## 4. Nedosljednosti između ekrana

| Uzorak | Načini u kodu | Prijedlog koji oblik ostaje |
|---|---|---|
| **Dijalog potvrde** | (a) `PotvrdaBrisanjaObrokaDialog`: zadani oblik, naslov `titleLarge`, „Obriši" kao `TextButton` u `error`, bez `skalaPritiska` (`ui/components/PotvrdaBrisanjaObroka.kt:26-56`); (b) Odjava: `MacroOblici.pladanj`, `headlineSmall`, `Button` u `error`, `skalaPritiska` (`screens/profil/ProfilSadrzaj.kt:563-600`); (c) Ručni unos mase: `pladanj`, `headlineSmall`, oba `TextButton` u `onSurface` (`screens/BrzoMjerenjeKartica.kt:303-330`); (d) Brisanje podataka: `Dialog` + `Surface` (`screens/PostavkeAplikacijeScreen.kt:477`); (e) Odbacivanje promjena: posve zadani M3 (`screens/UnosObrokaScreen.kt:407-427`) | Oblik (b) kao jedna dijeljena komponenta (`pladanj`, `surface`, `headlineSmall`, destruktivna potvrda `Button` u `error`, ostalo `TextButton` u `onSurface`, `skalaPritiska`); (a) i (e) prebaciti na nju. (d) ostaje zaseban jer ima polje i popise. ✅ korak 6: `DijalogPotvrde` (odjava, brisanje obroka, odbacivanje promjena); (c) i (d) već slijede pravilo gumba, bez izmjena |
| **Polje unosa** | `PoljeUnosa` (fokus `onSurface`, `utor`, `selectionColors`); pretraga, opis i polja filtera s fokusom `secondary` i vlastitim bojama (`UnosObrokaSadrzaj.kt:472`, `SlikaIliOpisSadrzaj.kt:480`, `FilterJela.kt:332`); Login/Register/Onboarding sa zelenim fokusom | `PoljeUnosa` za sva polja forme (filteri, auth, onboarding). Pretraga i opis smiju ostati zasebni (pilula, brojač), ali s istim bojama fokusa i odabira teksta. ✅ korak 6 (odabir teksta na temi; fokus i kursor pretrage i filtera u `onSurface`; opis zadržava fokus `secondary` po `slika-ili-opis.md`, kursor `onSurface`). Login/Register/Onboarding ostaju za korake 7 i 9 |
| **Odabir jedne opcije** | `IzborOpcije` (Uredi profil, Postavke ciljeva, Postavke aplikacije); `FilterChip` i kartice s `RadioButton` (Onboarding) | `IzborOpcije`. |
| **Primarni gumb** | `GumbSpremi` (Profil tab); `ExtendedFloatingActionButton` u paprici (Home); zeleni `Button` 58 dp s `Color.White` (Welcome, Login, Register, Onboarding, ObrokSpremljen); zeleni 46 dp (Sažetak), 52 dp (Detalj jela) | `GumbSpremi` za forme i pune širine, FAB samo na Home. |
| **Učitavanje** | Kostur (`KosturOblik` ili vlastiti `Kostur` s istim pulsom u `DnevniPregledSadrzaj.kt:571`, `PovijestSadrzaj.kt:632`, `ProfilSadrzaj.kt:604`, `DanSadrzaj.kt:257`, `SlikaIliOpisSadrzaj.kt:458`); spinner (Splash, Login, Register, Onboarding, Detalj jela) | Kostur. Pet vlastitih kostura izgledaju jednako (alfa 0,55 ↔ 1, `Trajanje.disanje`), pa ih ne treba dirati radi izgleda. |
| **Greška s „Pokušaj ponovno"** | Puna kartica s obrubljenim gumbom (Home, Povijest, Profil, unos obroka) i tihi red s linkom u `primary` (Moje mjere, Slika ili opis, unos obroka); oba su namjerna (`profil.md` § Stanje redizajna). Odstupaju: Detalj jela (zelena, `:102`) i Login/Register/Onboarding (obojena podloga, bez ponovnog pokušaja) | Zadržati dva namjerna oblika; Detalj jela prebaciti na tihi red. |
| **Zaglavlje podekrana** | `ZaglavljePodekrana` (5 ekrana Profila); vlastite strelice natrag u Slici ili opisu (`SlikaIliOpisSadrzaj.kt:313`, s podnaslovom po specu), unosu obroka (`UnosObrokaSadrzaj.kt:257`) i Detalju dana (`DanSadrzaj.kt:332`, s nadnaslovom po specu) | Ostaje kako je: razlike su opisane u specovima, a sve tri koriste istu ikonu, boju i 48 dp. |
| **Naziv vrste obroka** | `nazivTipa` (Composable, `ui/components/Tekstovi.kt:12`); privatni `nazivTipa(context, …)` (`UnosObrokaScreen.kt:68`, za snackbar izvan kompozicije); `formatTipObrokaSpremljen` s tekstom u kodu (`ObrokSpremljenScreen.kt:209`) | `Tekstovi.kt`; inačicu s `Context` premjestiti uz nju kad bude trebala još jednom ekranu, a `formatTipObrokaSpremljen` obrisati pri prebacivanju ObrokSpremljen. |
| **Zapis makronutrijenata** | `kcalIMakroKratko`/`makroKratko` „52P 38M 120U" (svi prebačeni ekrani); „x g" + „P/UH/M" u stupcima (`SazetakObrokaBar.kt:214-216`, `JeloDetaljSheet.kt:229-231`) | `Tekstovi.kt` zapis; u detalju jela smije ostati veći prikaz, ali s P/M/U i bojama iz `macroBoje`. |
| **Snackbar** | Svi koriste zadani M3 `SnackbarHost` iznad plutajuće navigacije; Home dodaje FAB (`HomeScreen.kt:158`), unos obroka sažetak (`UnosObrokaScreen.kt:388`) | Dosljedno; jedini problem je razmak u uređivanju obroka (§3). |
| **Ponovljeni stringovi** | „Pokušaj ponovno" ×6 (`dp_`, `uo_`, `sio_`, `pov_`, `prof_`, `mj_`), „Natrag" ×4 (`uo_`, `sio_`, `pov_`, `kom_`) u `res/values/strings.xml` | Ne utječe na izgled; spojiti u `kom_` tek kad se dira taj ekran. ✅ korak 11 |
| **Prefiks resursa kartice Unos** | `ko_` (5 stringova, `strings.xml:87-91`) uz `uo_` za unos obroka | Ostaje; samo zapisati u `meal-logging.md`. ✅ korak 11 |

---

## 5. Višak za brisanje

| Datoteka ili simbol | Zašto je višak | Dokaz | Sigurno za brisanje |
|---|---|---|---|
| `MacroBackground`, `MacroSurface`, `MacroDark`, `MacroDarkSurface`, `MacroText`, `MacroTextSecondary`, `MacroError` (`ui/theme/Color.kt:147-156`) | Stara paleta | `rg -w` nad `*.kt`: samo deklaracija | da — ✅ obrisano (korak 1) |
| `MacroLightGreen` (`ui/theme/Color.kt:145`) | Samo se uvozi, nigdje ne koristi | Pogoci samo u `import` (`LoginScreen.kt:26`, `RegisterScreen.kt:26`, `OnboardingScreen.kt:33`, `SplashScreen.kt:19`) | da (zajedno s ta četiri importa) — ✅ obrisano (korak 1) |
| `MacroGreen` (`ui/theme/Color.kt:144`) | Naslijeđena boja | Još 8 datoteka (§3) | ✅ obrisano (korak 10) |
| Sve boje u `res/values/colors.xml` (`purple_*`, `teal_*`, `black`, `white`) | Predložak Android Studija | Lint `UnusedResources` za svih 7; `themes.xml` ih ne koristi | da (cijela datoteka) — ✅ obrisano (korak 1) |
| `dp_bez_cilja` (`res/values/strings.xml:25`) | Nekorišten string | Lint `UnusedResources`; `rg` bez pogodaka | da — ✅ obrisano (korak 1) |
| `ScreenSpacing.Horizontal` (22 dp) (`ui/theme/ScreenPadding.kt:12`) | Stari bočni rub (sada `rubEkrana` 16 dp) | `rg ScreenSpacing.Horizontal`: samo deklaracija | da — ✅ obrisano (korak 1) |
| Parametar `dynamicColor` i grana s `dynamicLight/DarkColorScheme` (`ui/theme/Theme.kt:80-88`) | Nitko ga ne postavlja; uključen bi zamijenio Pladanj | `rg dynamicColor`: samo `Theme.kt` | da — ✅ obrisano (korak 1) |
| `spremiKonzumaciju` + model `KonzumacijaRequest` (`api/ApiService.kt:73-76`, `model/KonzumacijaRequest.kt`) | Stari unos po jelu (zamijenio ga `spremiObrok`) | `rg -w`: samo `ApiService.kt` | da — ✅ obrisano (korak 1) |
| `dohvatiUkupniUnos` + model `UkupniUnos` (`api/ApiService.kt:149-152`, `model/UkupniUnos.kt`) | Stari N+1 endpoint `GET /api/konzumacije/ukupno/{id}` | `rg -w`: samo `ApiService.kt` | da — ✅ obrisano (korak 1) |
| `filtrirajJela` (`api/ApiService.kt:120-130`) | Filter radi kroz `getJela` | `rg -w`: samo `ApiService.kt` | da — ✅ obrisano (korak 1) |
| `getMeni` (`api/ApiService.kt:138-141`) | Meniji dolaze listom `getMeniji` | `rg -w`: samo `ApiService.kt` | da — ✅ obrisano (korak 1) |
| `dodajProfil` (`api/ApiService.kt:213-216`) | Onboarding koristi `registracijaProfil` | `rg -w`: samo `ApiService.kt` | da (model `ProfilPodaci` ostaje, koristi se drugdje) — ✅ obrisano (korak 1) |
| `KarticaUnosa`, `_kartica`, `kartica`, `postaviKarticu` (`viewmodel/JelaViewModel.kt:93`, `:146-147`, `:253-256`) | Ostatak starih kartica Jela/Meniji | `rg -w` izvan `JelaViewModel.kt` bez pogodaka | provjeriti (`postaviKarticu` poziva `ucitajMenije`; potvrditi da se meniji učitavaju drugim putem) |
| `ExampleUnitTest.kt`, `ExampleInstrumentedTest.kt` (`app/src/test/…`, `app/src/androidTest/…`) | Predlošci (`assertEquals(4, 2 + 2)`) | sadržaj datoteka | provjeriti (s njima idu i `junit`/`espresso` ovisnosti) |
| `app/release/` (`app-release.apk`, `baselineProfiles/`, `output-metadata.json`), 12 MB | Izlaz builda u gitu | `git ls-files` | da — ✅ uklonjeno iz gita (korak 2) |
| `.kotlin/errors/*.log` (6 datoteka) | Logovi pada Kotlin daemona u gitu | `git ls-files` | da — ✅ uklonjeno iz gita (korak 2) |
| `.idea/` lokalne datoteke u gitu: `copilot.data.migration.ask2agent.xml`, `deviceManager.xml`, `deploymentTargetSelector.xml`, `appInsightsSettings.xml` | Postavke IDE-a za jedno računalo | `git ls-files .idea` | da — ✅ uklonjeno iz gita (korak 2); ostale `.idea` datoteke ostaju |
| `keystore/macromenza-release.jks` u gitu | Ključ za potpis ne pripada javnom repozitoriju | `git ls-files` | **provjeriti, ne brisati sada**: treba za potpis izdanja; spada u korak 18 (vjerodajnice) |
| `formatTipObrokaSpremljen` (`screens/ObrokSpremljenScreen.kt:209-219`) | Duplikat `nazivTipa` | usporedba s `ui/components/Tekstovi.kt:12-18` | da, u istom koraku kad se prebacuje ObrokSpremljen |
| `ProteinBlue`, `CarbsOrange`, `FatPurple` (`SazetakObrokaBar.kt:42-44`, `JeloDetaljSheet.kt:36-38`) | Stare makro boje, duplicirane u dvije datoteke | `rg` | da, u koraku prebacivanja tih datoteka |
| `macromenza/_citanje/` (izvan `frontend/`) | Privremena kopija `java/com/niko/macromenza` (136 stavki) | `ls` | obrisati ručno; nije u nijednom git repozitoriju |

**Nije višak (ne brisati):** `assets/licence/OFL-*.txt` (licence fontova moraju ići uz aplikaciju), `res/font/*.ttf`, `res/xml/file_paths.xml` (kamera), `ikonaTipa` i `PladanjLegenda` (koriste se unutar svojih datoteka). Ovisnosti u `app/build.gradle.kts` (`okhttp`, `exifinterface`, `datastore`, `material-icons-extended`, `navigation`) se koriste; nijedna nije sigurno nekorištena.

**Frontend i zastarjeli backend:** `api/ApiService.kt` ne poziva `/api/dashboard/{id}`, `tjedni-pregled` ni `povijest/dani`. Od N+1 poziva iz `CLAUDE.md` frontend još koristi `dohvatiPovijestZaDan` (`GET /api/konzumacije/povijest/{id}`, iz `viewmodel/JelaViewModel.kt` i `viewmodel/PovijestViewModel.kt`); to nije višak, nego posao za backend.

**Tekst u ViewModelima:** poruke grešaka su u kodu (`viewmodel/AuthViewModel.kt` 14, `viewmodel/SlikaIliOpisViewModel.kt` 15, `viewmodel/JelaViewModel.kt` 13, `viewmodel/PovijestViewModel.kt` 12 literala). Korisnik ih vidi, ali premještanje u `strings.xml` mijenja ViewModele, pa je to zaseban korak, a ne dio dizajna. ✅ korak 13: `util/UiTekst.kt` (`Resurs` / `Dinamicki`), poruke svih ViewModela u `strings.xml`; poruka s backenda ostaje doslovna.

---

## 6. Provjere

| Naredba | Rezultat |
|---|---|
| `.\gradlew.bat assembleDebug` (JDK 17) | **prošlo** (`BUILD` bez grešaka; task je bio `UP-TO-DATE`) |
| `.\gradlew.bat :app:compileDebugKotlin --rerun` | **prošlo**, 2 upozorenja: `LoginScreen.kt:118` zastarjeli `Icons.Filled.Login`; `viewmodel/AuthViewModelFactory.kt:13` neprovjereni cast |
| `.\gradlew.bat lint` | **pao na 1 grešci koja nije u kodu**: `frontend/local.properties:1` `PropertyEscape` (Windows putanja SDK-a bez escapea). Uz to 44 upozorenja i 1 savjet. Izvještaj: `frontend/app/build/reports/lint-results-debug.html` |

Lint upozorenja koja se tiču ovog pregleda:
- **Nekorišteni resursi:** 7 boja u `colors.xml`, `dp_bez_cilja` (§5).
- **Množina:** `PluralsCandidate` na `strings.xml:192` (`sio_brojac_opis`), `:246` (`pov_tjedan_opis`), `:275` (`pov_sazetak_dani_opis`), `:428` (`mj_u_n_dana`). Stvarna greška je samo `:428` (§3); ostala tri su genitiv iza „od N" i ispravni su.
- **Resursi:** `mipmap-anydpi-v26` je suvišna mapa jer je `minSdk` 26 (`ObsoleteSdkInt`); sadržaj premjestiti u `mipmap-anydpi`. `AndroidManifest.xml:21` suvišan `android:label` na aktivnosti. ✅ korak 11
- **Compose (nije izgled, samo za znanje):** `ModifierFactoryExtensionFunction` (`FilterJela.kt:78`, `PovijestSadrzaj.kt:382`, `UnosObrokaPokret.kt:89`), `ModifierParameter` (`JeloRed.kt:431`, `KakoSmoIzracunaliKartica.kt:34`), `AutoboxingStateCreation` (`JeloDetaljSheet.kt:55`).
- Ostalo su novije verzije ovisnosti i `UseTomlInstead`; ne tiču se teme ni viška.

---

## 7. Predloženi redoslijed popravaka

Svaki korak je jedna cjelina za jedan prompt. Dizajn i brisanje nisu u istom koraku.

1. **Brisanje sigurnog viška (bez promjene izgleda):** stare boje u `Color.kt` osim `MacroGreen`, `MacroLightGreen` s četiri importa, `colors.xml`, `dp_bez_cilja`, `ScreenSpacing.Horizontal`, `dynamicColor`, pet Retrofit metoda s `KonzumacijaRequest` i `UkupniUnos`.
2. **Čišćenje repozitorija:** `app/release/`, `.kotlin/errors/` i lokalne `.idea` datoteke iz gita, dopuna `.gitignore`; odluka o `keystore/` ostaje za korak 18. Obrisati `macromenza/_citanje/` ručno.
3. **Sažetak obroka i detalj jela na Pladanj (paket 2, dio 1):** `SazetakObrokaBar` i `JeloDetaljSheet` s page specom u `meal-logging.md` (makro boje, P/M/U, `PorcijaGlif`, koračnik 48 dp, paprika samo za „Spremi"/„Dodaj u obrok", višnja za prekoračenje, kostur, bez animiranja visine, `strings.xml`, previewi).
4. **Obrok spremljen na Pladanj (paket 2, dio 2):** `Tanjur` spremljenog obroka, rečenica bez roda i s pravim datumom, `nazivTipa`, jedna paprika; obrisati `formatTipObrokaSpremljen`.
5. **Plutajuća navigacija na tokene:** fiksne širine i indikator kroz `graphicsLayer`, tokeni za dimenzije i oblik, bez sjene, `Icons.Rounded`, semantika tabova, nazivi u `strings.xml`.
6. **Dijalozi i polja jednako:** jedna dijeljena komponenta dijaloga potvrde (Odjava kao uzor) za brisanje obroka i odbacivanje promjena; boje „U redu" u biračima; `LocalTextSelectionColors` na tokenima.
7. **Login i Register na Pladanj:** `PoljeUnosa` (s varijantom za lozinku), `GumbSpremi`, linkovi u `primary`, tihi red greške, bez ukrasnih krugova, `strings.xml`, previewi.
8. **Welcome i Splash na Pladanj:** bez krugova i zelene, naslov u Frauncesu, kostur ili tanjur umjesto spinnera, `strings.xml`, previewi.
9. **Onboarding na Pladanj:** sekcije bez kartica, `PoljeUnosa`, `IzborOpcije`, `GumbSpremi`, `strings.xml`, previewi.
10. **Brisanje `MacroGreen`:** nakon koraka 3, 4, 7, 8 i 9, kad ga više nitko ne koristi; ažurirati MASTER §1 i CLAUDE.md pravilo 8.
11. **Sitnice teksta i formata:** razmak u uređivanju obroka (`hasBottomNavigation`), `plurals` za „u N dana", jedno pravilo za kcal s točkom ili bez nje, snackbar „Obrok obrisan" u `strings.xml`, `ObrokRed` bez paprike na ikoni (ili iznimka u MASTER-u).
12. **Ikona aplikacije i početni prozor:** ikona u paleti Pladnja, `windowBackground` za tamnu temu, scrim sustavne navigacije prema temi aplikacije.
13. **Poruke ViewModela u `strings.xml`** (zaseban korak jer mijenja ViewModele).
