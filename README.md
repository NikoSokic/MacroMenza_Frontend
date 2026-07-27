# MacroMenza - Frontend

MacroMenza je Android aplikacija razvijena s ciljem praćenja dnevnog unosa hrane i makronutrijenata kod studenata koji koriste studentsku menzu. Korisnicima omogućuje izračun personaliziranih prehrambenih preporuka, evidenciju konzumiranih obroka te pregled prehrambenih navika kroz jednostavno korisničko sučelje.

## Funkcionalnosti

- Registracija i prijava korisnika putem Supabase autentikacije s potvrdom e-mailom
- Personalizirani onboarding prilikom prve prijave
- Izračun preporučenog dnevnog unosa kalorija i makronutrijenata
- Evidencija doručka, ručka i večere te pregled dnevno konzumiranih obroka
- Tjedni pregled unosa hranjivih vrijednosti
- Profil korisnika s prehrambenim ciljevima
- Automatsko pamćenje prijavljenog korisnika

## Arhitektura sustava

Aplikacija je razvijena prema MVVM arhitekturnom uzorku i komunicira s backendom putem REST API-ja:

1. **UI sloj (Jetpack Compose)** — prikazuje korisničko sučelje i reagira na promjene stanja
2. **ViewModel sloj** — upravlja stanjem zaslona, obrađuje korisničke akcije i poziva repozitorije
3. **Repozitorijski sloj (Retrofit)** — komunicira sa Spring Boot backendom putem HTTP zahtjeva
4. **Spring Boot** — obrađuje poslovnu logiku i vraća podatke aplikaciji

## Autentikacija

Za autentikaciju korisnika koristi se Supabase Authentication:

1. Korisnik se registrira koristeći e-mail adresu i lozinku
2. Supabase šalje poruku za potvrdu računa
3. Korisnik potvrđuje registraciju putem e-maila
4. Korisnik se prijavljuje u aplikaciju
5. Supabase vraća jedinstveni identifikator korisnika (UID)
6. Spring Boot backend pomoću tog identifikatora dohvaća ili kreira korisnika u vlastitoj bazi
7. Podaci o prijavljenom korisniku spremaju se lokalno — korisnik ostaje prijavljen i nakon ponovnog pokretanja aplikacije

Lozinke se ne spremaju u bazu podataka aplikacije.

## Zasloni

- Welcome Screen
- Prijava i registracija
- Onboarding
- Početni zaslon
- Dodavanje obroka
- Povijest konzumacije
- Tjedni pregled
- Profil korisnika

## Korištene tehnologije

### Mobilna aplikacija
- Kotlin
- Jetpack Compose
- MVVM arhitektura
- Retrofit
- Kotlin Coroutines
- StateFlow

## Autor

**Niko Sokić**  
Fakultet elektrotehnike, strojarstva i brodogradnje (FESB)  
Sveučilište u Splitu
