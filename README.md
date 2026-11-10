# Trener AI

Aplikacja webowa do planowania treningów kolarskich z wykorzystaniem sztucznej inteligencji. Synchronizuje aktywności ze Stravy, analizuje dane treningowe i generuje spersonalizowane 7-dniowe plany treningowe z eksportem do formatów ZWO (Zwift) i FIT (Garmin).

## Funkcjonalności

- **Synchronizacja ze Stravą** — OAuth2, import aktywności z pełnymi danymi (moc, tętno, kadencja)
- **Generowanie planów treningowych** — AI dobiera treningi na podstawie historii aktywności, FTP i wybranego celu
- **Obsługa wielu dostawców AI** — OpenAI, Google Gemini, Groq (Llama)
- **Eksport treningów** — pliki ZWO (Zwift) i FIT (Garmin) gotowe do wgrania na urządzenie
- **Analiza stref mocy** — rozkład czasu w strefach Z1–Z7 dla każdego treningu
- **Kalendarz treningowy** — widok tygodniowy z aktywnościami i zaplanowanymi treningami
- **REST API** — pełne API z dokumentacją OpenAPI/Swagger

## Stack technologiczny

| Warstwa | Technologia |
|---------|------------|
| Backend | Java 21, Spring Boot 3.4, Spring MVC, Spring Security, Spring Data JPA |
| Baza danych | PostgreSQL 17, Flyway (migracje) |
| Frontend | Thymeleaf (SSR), vanilla CSS/JS |
| API | REST + springdoc-openapi (Swagger UI) |
| Integracje | Strava API (OAuth2), Garmin FIT SDK |
| Infrastruktura | Docker Compose, multi-stage Dockerfile, GitHub Actions CI/CD |
| Testy | JUnit 5, Spring Boot Test, MockMvc, H2 (in-memory) |

## Architektura

```
pl.trenerai
├── api/                  # REST API — kontrolery, DTO, obsługa błędów
├── config/               # Security, OpenAPI, bootstrap użytkownika
├── model/                # Encje JPA (AppUser, Activity, GeneratedWorkout, ...)
├── repository/           # Spring Data repozytoria
├── service/              # Logika biznesowa
│   ├── WorkoutGenerationService   — generowanie planu (wywołanie AI, parsowanie, ZWO)
│   ├── WorkoutSummaryService      — analiza stref mocy i kroków treningu
│   ├── FitWorkoutService          — eksport do formatu Garmin FIT
│   ├── StravaService              — synchronizacja aktywności ze Stravy
│   └── SecretCrypto               — szyfrowanie kluczy API (AES-GCM)
└── web/                  # Kontrolery Thymeleaf (dashboard, auth, settings)
```

## Uruchomienie

### Wymagania
- Docker i Docker Compose

### Start

```bash
git clone <repo-url>
cd trener
cp .env.example .env      # uzupełnij dane Strava i klucz kryptograficzny
docker compose up -d
```

Aplikacja: http://localhost:20190
Swagger UI: http://localhost:20190/swagger-ui.html

### Zmienne środowiskowe (.env)

| Zmienna | Opis |
|---------|------|
| `POSTGRES_DB` | Nazwa bazy danych |
| `POSTGRES_USER` | Użytkownik bazy |
| `POSTGRES_PASSWORD` | Hasło bazy |
| `STRAVA_CLIENT_ID` | Client ID z Strava API |
| `STRAVA_CLIENT_SECRET` | Client Secret z Strava API |
| `APP_CRYPTO_KEY` | Klucz szyfrowania (min. 32 znaki) |

## REST API

| Metoda | Endpoint | Opis |
|--------|----------|------|
| `GET` | `/api/activities` | Lista aktywności |
| `GET` | `/api/activities/{id}` | Szczegóły aktywności |
| `GET` | `/api/workouts` | Aktywny plan treningowy |
| `POST` | `/api/workouts/generate` | Generuj 7-dniowy plan |
| `DELETE` | `/api/workouts/{id}` | Usuń trening |
| `DELETE` | `/api/workouts/plan` | Anuluj cały plan |
| `GET` | `/api/workouts/{id}/download/zwo` | Pobierz plik ZWO |
| `GET` | `/api/workouts/{id}/download/fit` | Pobierz plik FIT |
| `GET` | `/api/settings` | Ustawienia użytkownika |
| `PUT` | `/api/settings/ai` | Zmień dostawcę/model AI |
| `PUT` | `/api/settings/ftp` | Ustaw FTP |
| `GET` | `/api/settings/ai/providers` | Dostępni dostawcy AI |

Pełna dokumentacja: `/swagger-ui.html`

## Testy

```bash
cd trener-ai-app
mvn test
```

61 testów: unit (szyfrowanie, parsowanie ZWO, strefy mocy, FIT, katalog AI) + integracyjne (security, rejestracja, dashboard, REST API).

## CI/CD

GitHub Actions automatycznie:
- **CI** — uruchamia testy (`mvn verify`) na każdym pushu i pull requeście
- **CD** — po pushu do `main`: łączy się z serwerem przez SSH, pobiera zmiany i przebudowuje kontenery

Wymagane sekrety w repozytorium (`Settings → Secrets → Actions`):

| Sekret | Opis |
|--------|------|
| `MIKRUS_HOST` | Adres serwera (np. `srvXXX.mikr.us`) |
| `MIKRUS_USER` | Użytkownik SSH |
| `MIKRUS_SSH_KEY` | Klucz prywatny SSH |
| `MIKRUS_SSH_PORT` | Port SSH (domyślnie 10022 na Mikrusie) |

## Migracje bazy danych

Schemat zarządzany przez Flyway. Migracje w `trener-ai-app/src/main/resources/db/migration/`.

Dodanie nowej migracji:
```bash
# Utwórz plik z kolejnym numerem wersji
touch trener-ai-app/src/main/resources/db/migration/V2__opis_zmiany.sql
```

Flyway automatycznie wykona nowe migracje przy starcie aplikacji.
