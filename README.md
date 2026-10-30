# MacroMenza

Android aplikacija za praćenje kalorija i makronutrijenata obroka iz studentske menze. Kalorije se ne upisuju ručno:
računaju se iz službenih normativa menze (NN 120/2013) i tablice nutritivnih vrijednosti namirnica. Nastala je kao
završni rad na FESB-u.

Projekt ima dva repozitorija:

| Repozitorij | Sadržaj |
|---|---|
| [macromenza_backend](https://github.com/NikoSokic/macromenza_backend) | Spring Boot REST API, SQL migracije i izvorni podaci |
| [MacroMenza_Frontend](https://github.com/NikoSokic/MacroMenza_Frontend) | Android aplikacija (Kotlin, Jetpack Compose) |

## Što aplikacija radi

- **Unos obroka:** pretraga 311 jela iz menze po nazivu i kategoriji, filtri po makronutrijentima, „Često jedeš“
  i gotovi meniji menze (84). Količina ide u koracima od pola porcije, a obrok ima datum, vrijeme i tip
  (doručak, ručak, užina, večera).
- **Slika ili opis:** fotografija tanjura, screenshot Iksice ili računa iz menze, ili kratak opis pretvara se u
  prijedlog obroka (Gemini). Prijedlog se prije spremanja može urediti, a slika se u aplikaciju može i
  podijeliti iz druge aplikacije.
- **Detalj jela:** sastojci iz normativa s gramažom i doprinosom kalorijama.
- **Početni ekran:** prsten dnevnih kalorija po obrocima, makronutrijenti prema cilju i savjet dana.
- **Povijest:** listanje po tjednima, graf s linijom cilja, broj dana u cilju (±10 %) i prosjek makronutrijenata.
- **Masa:** brzo mjerenje s korakom od 0,1 kg i graf mase. Preporuka se nakon mjerenja ponovno izračuna.
- **Personalizirani cilj:** Mifflin-St Jeor BMR × razina aktivnosti, uz korekciju za cilj. Proteini su 2 g/kg,
  masti 25 % kalorija, a ugljikohidrati ostatak. Izračun je korak po korak prikazan u onboardingu.
- **Transparentnost:** ekran „Odakle su kalorije“ s izvorima podataka i napomenom da su vrijednosti okvirne.
- **Primjer tjedna** za isprobavanje, **brisanje svih podataka** uz potvrdu, svijetla i tamna tema.

## Arhitektura

```
Android (Compose, MVVM)  ──HTTPS + JWT──▶  Spring Boot API  ──JDBC──▶  PostgreSQL (Supabase)
        │                                      │
        └──── prijava i registracija ──▶ Supabase Auth          └──▶ Gemini API (slika ili opis)
```

- **Android:** Jetpack Compose i Material 3, MVVM (ViewModel i StateFlow), Retrofit i Gson, DataStore za sesiju
  i temu, Navigation Compose. Dizajn „Pladanj“ ima vlastiti sustav tokena (boje, tipografija, razmaci i pokret
  u `ui/theme`).
- **Backend:** Spring Boot 4 (Java 17), Spring Data JPA, Spring Security (OAuth2 resource server), springdoc-openapi.
- **Baza:** PostgreSQL na Supabaseu. Shema se mijenja isključivo numeriranim SQL skriptama (`db/migracije`).

### Izračun nutritivnih vrijednosti

Vrijednosti jela računaju se u bazi, u SQL viewu `v_jelo_nutritivno`. To je zbroj po sastojcima iz `jelo_sastojak`:
vrijednost namirnice na 100 g × grami / 100. Kod alternativa u normativu („ili“) uzima se prva opcija, a artikli
na komad koriste masu jednog komada. Meniji i obroci zbrajaju se iz istih viewova, pa postoji jedan izvor istine.
Sve vrijednosti su decimalne.

### Sigurnost

- Registracija i prijava idu preko Supabase Auth. Lozinke se ne spremaju u bazu aplikacije.
- Aplikacija uz svaki zahtjev šalje Supabase access token, a pri isteku ga sama osvježi refresh tokenom.
- Backend provjerava potpis tokena javnim ključevima projekta (JWKS, ES256), pa ne treba nikakvu tajnu.
  Korisnik se određuje iz tokena, a pristup tuđim podacima vraća 403.
- Vjerodajnice (baza, Gemini ključ) dolaze iz env varijabli i nisu u repozitoriju.
- Greške imaju ispravne HTTP statuse (400, 401, 403, 404, 405, 409, 500) i tijelo `{"poruka": "..."}`.

## Glavni endpointi

Svi endpointi osim `/api/ping` traže zaglavlje `Authorization: Bearer <token>`.

| Metoda i putanja | Namjena |
|---|---|
| `GET /api/jela`, `/api/jela/kategorije`, `/api/jela/cesto`, `/api/jela/{id}/detalj` | Katalog jela, kategorije, često jedeno, sastojci |
| `GET /api/meniji` | Meniji menze |
| `POST /api/obroci`, `GET/PUT/DELETE /api/obroci/{id}` | Obrok kao cjelina (jedan poziv za sve stavke) |
| `GET /api/obroci/dan-pregled`, `/api/obroci/tjedan` | Početni ekran i Povijest (totali u jednom upitu) |
| `POST/DELETE /api/obroci/primjer-tjedna` | Primjer tjedna |
| `GET /api/mjerenja`, `POST /api/mjerenja/brzo` | Graf mase i brzo mjerenje |
| `POST /api/preporuke/pregled` | Izračun preporuke korak po korak (ništa se ne sprema) |
| `POST /api/prepoznavanje` | Prijedlog obroka iz slike ili opisa |
| `GET /api/izvori-podataka` | Izvori i broj namirnica, jela i menija |
| `POST /korisnici/registracija-profil`, `GET /korisnici/{id}/onboarding-status` | Registracija profila i onboarding |
| `DELETE /api/korisnici/{id}/podaci?potvrda=OBRISI` | Brisanje svih podataka korisnika |
| `GET /api/ping` | Provjera rada (bez tokena) |

## Pokretanje

### Backend

U `backend/` napravi `.env` po uzoru na `.env.example` (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `GEMINI_API_KEY`),
pa pokreni:

```powershell
cd backend
.\mvnw.cmd spring-boot:run      # http://localhost:8080
```

### Android

U `frontend/local.properties` postavi adresu backenda:

```properties
macromenza.baseUrl=http://10.0.2.2:8080/      # emulator -> laptop; za mobitel IP laptopa ili adresa na Renderu
```

```powershell
cd frontend
.\gradlew.bat assembleDebug     # treba JDK 17
```

APK je u `app/build/outputs/apk/debug/app-debug.apk`.

### Deploy

Backend je na Renderu (Docker, `render.yaml`). Novi servis se stvara kao Blueprint iz ovog repozitorija, a
vjerodajnice se upisuju u Environment. Besplatni servis se uspava nakon 15 minuta, pa ga vanjski cron
svakih 10 minuta poziva na `/api/ping`.

## Testiranje

- **Backend:** unit testovi servisa i integracijski testovi svih kontrolera na pravoj bazi. Svaki test radi u
  transakciji koja se poništava. Pokreće se s `.\mvnw.cmd test`.
- **Android:** ViewModeli, ugovor sa stvarnim JSON odgovorima backenda, pravila dizajna i snimke svih ekrana
  (Roborazzi). Pokreće se s `.\gradlew.bat testDebugUnitTest`.

Detalji su u [`docs/TESTIRANJE.md`](https://github.com/NikoSokic/macromenza_backend/blob/master/docs/TESTIRANJE.md).

## Autor

**Niko Sokić**
Fakultet elektrotehnike, strojarstva i brodogradnje (FESB), Sveučilište u Splitu
