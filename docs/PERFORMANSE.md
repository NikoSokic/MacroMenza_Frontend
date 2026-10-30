# MacroMenza – performanse i specifikacije (Android)

Mjereno 29. 10. 2026. Kod u repozitoriju **nije mijenjan**. Release se u repozitoriju trenutno ne može složiti (vidi 2.), pa su release mjerenja napravljena na **kopiji projekta** s jednom ispravkom (`@OptIn` u `KosturBrojki`).

Oznake: **emulator** = AVD `MacroMenza_E2E` (slika iste vrste kao Pixel_3a, Android 16, 1536 MB RAM-a, release APK potpisan debug ključem, backend na laptopu). `Pixel_3a` AVD je 91 % pun pa nije korišten. **uređaj** = Nikin Samsung (Android 16, 1080x2340) s **debug** buildom koji je već bio instaliran. Na njemu se ništa nije instaliralo ni brisalo; mjereni su samo start i memorija.

## Sažetak

| Metrika | Izmjereno | Cilj za dobru aplikaciju | Ocjena |
|---|---|---|---|
| Hladni start (TotalTime, 5 mjerenja) | emulator, release: **1516 ms** (1690, 1568, 1417, 1404, 1502). Uređaj, debug: **672 ms** (953, 626, 587, 625, 569) | < 1,5 s na srednjem mobitelu | ⚠️ emulator na granici, uređaj ✅ |
| Topli start | emulator **103 ms**; uređaj **112 ms** | < 300 ms | ✅ |
| Janky frameovi (scroll) | emulator: Home **11,5 %**, Povijest **12,0 %**, Profil **12,4 %**. Medijan 17 ms, p99 17–32 ms | < 5 % | ⚠️ nepouzdano (vidi 3.) |
| Veličina APK-a (release, ne-minificirani) | **13,8 MB** | < 15 MB | ✅ (jedva) |
| Veličina APK-a uz R8 + shrinkResources (kopija) | **2,7 MB** | < 15 MB | ✅ |
| RAM u radu (PSS) | emulator, release: Home **89 MB**, Povijest **101 MB**, Profil **99 MB**, Unos 98 MB, Slika ili opis 92–96 MB. Uređaj, debug: **150 MB** | < 150 MB | ✅ emulator, ⚠️ uređaj (debug, 71 MB je mapirani kod) |
| Slika za Gemini | 1600 px po duljoj strani, JPEG 85. Sintetička slika 4000x3000 (1215 KB) → **283 KB** (procjena) | < 500 KB | ✅ |
| Release build prolazi | `assembleRelease` u repozitoriju **pada** | prolazi | ❌ |
| R8 / shrinkResources | **isključeni**, `proguard-rules.pro` je prazan | uključeni | ❌ |
| Rad u pozadini / baterija | ništa ne radi u pozadini | ništa | ✅ |
| Cache (OkHttp ili lokalni) | **nema**, a backend ne sažima odgovore | cache za statične liste, gzip | ❌ |

Ograničenja mjerenja:
- `TotalTime` je vrijeme do prvog prikazanog prozora, ne do podataka na Homeu. Aplikacija ne zove `reportFullyDrawn`, pa vrijeme do sadržaja nije izmjereno.
- Emulator je sporiji od mobitela i bez prave GPU-a. Postotak janky frameova na njemu nije mjerodavan. Medijan frame-a je 17 ms (vsync 16,7 ms), pa granični frameovi ulaze u janky. Profil ima p99 od samo 17 ms uz 12,4 % janky. Treba ponoviti na uređaju s **release** buildom.
- Ekran Unos nije izmjeren: gfxinfo je prijavio 0 frameova, pa swipe tamo nije skrolao.
- Veličinu slike za Gemini nisam mogao pročitati iz aplikacije (release nije debuggable, a datoteka se briše nakon slanja). Broj je dobiven istim parametrima (1600 px, JPEG 85) na sintetičkoj slici. Prava fotografija hrane s mobitela je tipično 250–450 KB.

## 1. Specifikacije

| Stavka | Vrijednost |
|---|---|
| minSdk / targetSdk / compileSdk | 26 / 36 / 36 (Android 8.0+; pokriva otprilike 97 % aktivnih uređaja, procjena) |
| Kotlin / AGP / Gradle | 2.0.21 / 8.13.2 / 8.13 (traži JDK 17; JDK 25 na PATH-u ruši build) |
| Compose BOM | 2024.09.00 (star, trenutni su znatno noviji). Kotlin 2.0.21 uključuje strong skipping |
| Dozvole | samo `INTERNET`. Potrebna. Kamera ide preko `TakePicture` intenta pa dozvola nije potrebna |
| Manifest | `usesCleartextTraffic="true"` (potrebno samo dok je backend na laptopu), `allowBackup="true"` |
| R8 / shrinkResources | `isMinifyEnabled = false`, bez `isShrinkResources`. `proguard-rules.pro` je neizmijenjeni predložak |
| Potpisivanje release | nema `signingConfigs`, pa je release **nepotpisan** (`frontend/keystore/` nisam dirao) |
| Baseline Profile | nema vlastitog (nema Macrobenchmark modula). U APK-u je samo generički `assets/dexopt/baseline.prof` (8 KB) iz Compose biblioteka |

## 2. Veličina (release, ne-minificirani)

Ukupno 13,8 MB. `assembleRelease` u repozitoriju pada s greškom `OdakleSuKalorijeScreen.kt:164: The API of this layout is experimental`. Funkciji `KosturBrojki` nedostaje `@OptIn(ExperimentalLayoutApi::class)` (susjedna `Brojke` ga ima).

| Dio | Veličina u APK-u |
|---|---|
| dex (3 datoteke) | 12,27 MB (raspakirano 46,5 MB) |
| resources.arsc | 0,65 MB |
| res (fontovi, ikone) | 0,32 MB |
| lib (4 ABI, `libandroidx.graphics.path`, DataStore counter) | 0,06 MB |
| META-INF, assets, ostalo | 0,06 MB |

5 najvećih stavki i što bi ih smanjilo:

| # | Stavka | Veličina | Što smanjuje |
|---|---|---|---|
| 1 | `classes.dex` | 7,37 MB | R8 (`isMinifyEnabled = true`). Uzrok je uglavnom `material-icons-extended` (tisuće ikona) |
| 2 | `classes2.dex` | 3,75 MB | isto |
| 3 | `classes3.dex` | 1,45 MB | isto |
| 4 | `resources.arsc` | 0,65 MB | `isShrinkResources = true` (uz R8: 0,51 MB) |
| 5 | `fraunces.ttf` (varijabilni) | 0,25 MB | Već je jedan varijabilni font (362 KB raspakirano). Dalje samo subsetting na latinicu i hrvatske znakove (pyftsubset). Mali dobitak. |

Izmjereno na kopiji s R8 + shrinkResources: **2,7 MB** (dex 1,6 MB). Fontovi, `publicsuffixes.gz` (41 KB) i ikone launchera nisu problem.

## 3. Brzina

Glatkoća (emulator, release, 6 swipeova gore i 6 dolje po ekranu):

| Ekran | Frameova | Janky | Medijan / p90 / p99 |
|---|---|---|---|
| Home | 226 | 11,5 % | 17 / 23 / 32 ms |
| Povijest | 192 | 12,0 % | 17 / 17 / 32 ms |
| Profil | 193 | 12,4 % | 17 / 17 / 17 ms |

Uzroci rekompozicija iz koda (nisam našao ozbiljan problem):
- **Stabilnost:** UI modeli ekrana (`DnevniPregledModel`, `PovijestModel`, ...) su `@Immutable`. Modeli iz `model/*.kt` nisu, ali Kotlin 2.0.21 ima strong skipping, pa to ne smeta.
- **LazyColumn:** `key` je zadan na Homeu, Povijesti (dan i tjedan) i Mojim mjerama. `JeloDetaljSheet.kt:217` nisam do kraja provjerio.
- `remember` ima ~220 mjesta, `derivedStateOf` 2.
- **Beskonačne animacije:** `Kostur` (Home, Dan) pulsira samo dok učitava i čita vrijednost u `graphicsLayer` (draw faza, bez rekompozicije). `Pladanj.kt:133` ima "disanje" praznih utora koje traje stalno dok je Home otvoren i dan prazan. Treba provjeriti čita li se `disanje` u draw fazi.
- **`collectAsState()` umjesto `collectAsStateWithLifecycle()`** na ~60 mjesta. Biblioteka `lifecycle-runtime-compose` je već dependency.

## 4. Memorija

Ispis iz `dumpsys meminfo`, emulator, release:

| Stanje | TOTAL PSS | Java heap | Native heap |
|---|---|---|---|
| Home | 89,1 MB | 9,6 MB | 11,5 MB |
| Povijest | 101,5 MB | 13,5 MB | 13,4 MB |
| Profil | 99,0 MB | 11,6 MB | 12,8 MB |
| Slika ili opis, prazno | 91,9 MB | 11,1 MB | 12,0 MB |
| Slika ili opis, s odabranom slikom (pregled) | 95,7 MB | 10,0 MB | 14,6 MB |
| Tijekom slanja slike | 94,6 MB | 9,2 MB | 14,1 MB |
| Nakon odgovora | 94,4 MB | 9,1 MB | 14,1 MB |

Uređaj, debug build, Home: **150 MB** (Java 15 MB, native 15 MB, Code 71 MB, Graphics 24 MB). Debug ima veći kod.

Slika: `SlikaUtil.kt` dekodira s `inSampleSize`, dovršava smanjivanje na najviše **1600 px**, poštuje EXIF, sprema JPEG kvalitete 85 i u `finally` zove `bitmapa.recycle()`. Poslana datoteka se briše nakon slanja. Za pregled na ekranu dekodira se zasebna bitmapa od 900 px (živi u `produceState`, oslobađa se s ekranom). Memorija nakon slanja se ne penje.

Leakovi: nisam našao. Jedini `AndroidViewModel` je `SlikaIliOpisViewModel`, i drži samo `Application`. Ostali ViewModeli ne drže Context. `UserSessionManager(context)` se stvara unutar kompozicije i ne preživljava ekran.

## 5. Mreža i podaci

Pozivi po ekranu (iz koda):

| Ekran | Pozivi |
|---|---|
| Home | 2 (`dan-pregled`, `obroci`) pri **svakom** ON_RESUME (`LifecycleResumeEffect`). Uključuje povratak iz galerije, kamere i pozadine |
| Povijest | 3 (`tjedan`, `povijest dana`, `obroci dana`) pri ulasku i pri svakoj promjeni dana |
| Profil | 4 **uzastopna** (`korisnik`, `profil`, `mjerenja` (sva), `preporuke`) |
| Unos obroka | `jela`, `meniji`, `kategorije`, `cesto`, `povijest dana` |

Rotacija ponovno stvara Activity (nema `configChanges`), ViewModel preživljava, ali ON_RESUME ponovno zove Home (nema dupliciranja, nego svjež dohvat).

Veličine odgovora (backend na laptopu, bez Authorization zaglavlja):

| Endpoint | Veličina | Uz gzip (procjena) |
|---|---|---|
| `/api/jela` | 61 035 B | ~10 KB |
| `/api/meniji` | 95 842 B | ~10 KB |
| `/api/jela/kategorije` | 658 B | |

Backend **ne sažima** odgovore (nema `server.compression`). Ispis zaglavlja potvrđuje da gzip nije uključen.

Timeouti i greške:
- Zadani `api`: OkHttp zadano **10 s** (connect/read/write), bez `callTimeout`.
- `prepoznavanjeApi` i `sporiApi` (isti klijent): connect 15 s, read/write 60 s, call 70 s.
- Supabase Auth Retrofit: zadani 10 s.
- Cold start servera od 30–60 s (npr. besplatni hosting iz koraka 20) probio bi 10 s na svakom ekranu. `HomeViewModel.ucitajHome` guta grešku (`catch (_: Exception) {}`), pa korisnik vidi samo stanje greške s gumbom "Ponovi", bez razlike između "nema interneta" i "server se budi". Specifične poruke (`Provjeri vezu s poslužiteljem`) postoje za unos obroka, detalj jela, spremanje i brisanje.
- **Cache:** nema OkHttp cachea ni lokalne baze. Svaki start dohvaća sve ispočetka, uključujući nepromjenjive liste (jela, meniji, kategorije).

## 6. Baterija

Nema WorkManagera, servisa, alarma, lokacije ni wakelocka (provjereno u kodu i manifestu). Beskonačne animacije su u Compose-u i staju kad aplikacija ode u pozadinu (frame clock staje). `collectAsState` bez lifecycle-a nastavlja prikupljati StateFlowove u pozadini, ali oni ne emitiraju bez poziva, pa je trošak zanemariv.

## Popis poboljšanja (po omjeru koristi i truda)

| # | Datoteka | Što promijeniti | Očekivani dobitak |
|---|---|---|---|
| 1 | `screens/OdakleSuKalorijeScreen.kt:162` | Dodaj `@OptIn(ExperimentalLayoutApi::class)` na `KosturBrojki`. | Release build ponovno prolazi (preduvjet za sve ostalo). 1 redak. |
| 2 | `app/build.gradle.kts`, `app/proguard-rules.pro` | `isMinifyEnabled = true`, `isShrinkResources = true`. Dodaj keep pravila za Gson modele (`model/**` i DTO klase iz `screens/**` koje se serijaliziraju, ili `@Keep`). Provjeri **ručno** prijavu, Home, Povijest i Slika ili opis. | APK 13,8 → 2,7 MB, manji dex, brži start. **Rizik:** bez keep pravila R8 preimenuje polja i Gson vraća prazne objekte. Runtime nisam testirao. |
| 3 | `app/build.gradle.kts` | Dodaj `signingConfigs.release` (keystore iz `frontend/keystore/`, lozinke iz env varijabli, ne iz koda) | Release se može instalirati na mobitel. Sada je nepotpisan. |
| 4 | backend `application.properties` | `server.compression.enabled=true`, `server.compression.mime-types=application/json` (nije vjerodajnica) | `/api/jela` i `/api/meniji` 61 / 96 KB → ~10 KB. Manje podataka i brže na mobilnoj mreži. |
| 5 | `viewmodel/ProfilViewModel.kt:42-58` | Pozive `korisnik`, `profil`, `mjerenja`, `preporuke` pokreni paralelno (`async`). Umjesto svih mjerenja traži samo zadnje. | Profil se učita ~3–4 puta brže (sada zbroj 4 odgovora, a ako je backend na internetu, zbroj 4 RTT-ova). |
| 6 | `api/RetrofitInstance.kt` | Zadani klijent: connect 15 s, read 30 s, `callTimeout` 40 s; dodaj `Cache` (10 MB) i `Cache-Control` za `jela`, `meniji`, `kategorije` | Nema ponovnog preuzimanja listi pri svakom otvaranju. Spor server više ne prekida zahtjev na 10 s. |
| 7 | `screens/*Screen.kt` (~60 mjesta) | `collectAsState()` → `collectAsStateWithLifecycle()` (`lifecycle-runtime-compose` je već u dependencyjima) | Prikupljanje staje kad ekran nije vidljiv. Manje rada u pozadini. |
| 8 | `viewmodel/HomeViewModel.kt:32`, `HomeScreen.kt` | Razlikuj grešku mreže od timeouta i prikaži poruku ("Poslužitelj se budi, pokušaj ponovno"). Ne dohvaćaj ponovno ako su podaci mlađi od ~30 s (osim nakon spremanja). | Jasna poruka umjesto praznog stanja. Manje poziva pri povratku iz galerije/kamere. |
| 9 | novi modul `baselineprofile` + `MainActivity` | Baseline Profile (Macrobenchmark) za start i skrol Homea, Povijesti, Unosa | Tipično 15–30 % brži hladni start i manje janka pri prvom skrolu. Veći trud (novi modul). |
| 10 | `app/build.gradle.kts` | Ukloni `material-icons-extended`; kopiraj samo korištene ikone kao `ImageVector` ili Material Symbols font | Manji dex i brži build. Uz R8 dobitak je manji, ali build ostaje velik bez toga. |
| 11 | `ui/components/Pladanj.kt:133` | Provjeri da se `disanje` čita u draw fazi (`graphicsLayer`/`drawBehind`), ne u kompoziciji | Bez rekompozicije svaki frame na praznom danu. |
| 12 | `gradle/libs.versions.toml` | Compose BOM 2024.09.00 → novija verzija | Manje popravke performansi Compose-a. Provjeri kompatibilnost, mali rizik. |
| 13 | `AndroidManifest.xml` | `usesCleartextTraffic` samo za debug (`src/debug/AndroidManifest.xml`) kad backend dobije HTTPS (korak 20). `allowBackup` razmisli isključiti. | Sigurnost, ne brzina. |

## Je li spremna za mobitel?

Aplikacija je brza i štedljiva: hladni start na mobitelu je ispod sekunde (debug), RAM je oko 90–100 MB, ništa ne radi u pozadini i slike za Gemini su ~300 KB. Za pokazivanje na mobitelu **obavezno** prvo ispravi `@OptIn` u `KosturBrojki`, dodaj potpisivanje releasea i uključi R8 (APK s 13,8 na 2,7 MB), zatim ručno prođi sve ekrane jer Gson modeli bez keep pravila mogu pasti. Prije javnog hostinga uključi gzip na backendu i postavi veće timeoute (cold start 30–60 s inače probija zadanih 10 s), a glatkoću (janky frameove) treba ponovno izmjeriti na pravom uređaju s release buildom.
