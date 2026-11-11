# Trener AI

Aplikacja do planowania treningów kolarskich z AI. Synchronizuje aktywności ze Stravy, generuje 7-dniowe plany treningowe i eksportuje je do ZWO (Zwift) i FIT (Garmin).

## Stack

Java 21, Spring Boot 3.4, Spring Security, Spring Data JPA, PostgreSQL 17, Flyway, Thymeleaf (SSR), vanilla JS/CSS. REST API z dokumentacją OpenAPI (springdoc). Docker Compose, GitHub Actions CI/CD, VPS Mikrus.

## Struktura projektu

```
trener-ai-app/                  # jedyny moduł aplikacji
├── src/main/java/pl/trenerai/
│   ├── api/                    # REST API: kontrolery, DTO, obsługa błędów
│   ├── config/                 # Security, OpenAPI, bootstrap użytkownika
│   ├── model/                  # encje JPA
│   ├── repository/             # Spring Data repozytoria
│   ├── service/                # logika biznesowa
│   └── web/                    # kontrolery Thymeleaf (SSR)
├── src/main/resources/
│   ├── db/migration/           # migracje Flyway (V{N}__opis.sql)
│   ├── templates/              # szablony Thymeleaf
│   └── static/                 # CSS, JS, favicon
└── src/test/                   # testy (JUnit 5, MockMvc, H2)
docker-compose.yml              # stack: app + PostgreSQL
.github/workflows/ci-cd.yml     # test → build GHCR → deploy SSH
```

## Komendy

```bash
# Testy
cd trener-ai-app && mvn test

# Pełna weryfikacja (testy + analiza)
cd trener-ai-app && mvn verify

# Build (bez testów, np. do budowy obrazu)
cd trener-ai-app && mvn package -DskipTests -B

# Docker — lokalnie
docker compose up -d

# Docker — produkcja
docker compose -f docker-compose.yml pull app && docker compose -f docker-compose.yml up -d --remove-orphans
```

## Code Style

- **Język UI**: cały interfejs, komunikaty błędów i opisy API po polsku. Kod (nazwy klas, metod, zmiennych) po angielsku.
- **Records do DTO i value objects**: `record WorkoutDto(...)`, `record SyncResult(...)`. Nie używaj klas z getterami/setterami do transportu danych.
- **Brak Lombok**: projekt celowo nie używa Lomboka. Gettery, konstruktory i metody piszemy ręcznie.
- **Encje JPA**: chroniony bezargumentowy konstruktor (`protected Entity() {}`), publiczny konstruktor z wymaganymi polami, metody biznesowe na encji (np. `markDeleted()`).
- **Kontrolery REST vs Web**: REST (`@RestController`) w pakiecie `api/` z adnotacjami Swagger (`@Operation`, `@Tag`). Kontrolery Thymeleaf (`@Controller`) w pakiecie `web/` — nie mieszaj warstw.
- **Walidacja**: `jakarta.validation` na DTO w REST API. W kontrolerach Thymeleaf walidacja ręczna z `RedirectAttributes` (flash message).
- **Testy**: JUnit 5 + `@SpringBootTest` + `@AutoConfigureMockMvc` dla integracyjnych, H2 in-memory zamiast Testcontainers. Testy jednostkowe bez kontekstu Springa.

## Zakazy

### Nigdy nie przechowuj sekretów w plaintexcie
Klucze API, tokeny Stravy i inne sekrety szyfruj przez `SecretCrypto.encrypt()` (AES-256-GCM) przed zapisem do bazy. Odczyt przez `SecretCrypto.decrypt()`. **Dlaczego**: kompromitacja bazy nie ujawnia kluczy użytkowników.

### Nigdy nie wystawiaj portów bazy na zewnątrz
PostgreSQL w Compose używa `expose`, nie `ports`. Mapowanie `127.0.0.1:5432:5432` tylko w konfiguracji deweloperskiej. **Dlaczego**: Docker omija UFW — `ports` oznacza port publiczny mimo firewalla.

### Nigdy nie parsuj XML bez ochrony przed XXE
Każde użycie `DocumentBuilderFactory` musi wyłączać DTD i zewnętrzne encje (patrz `FitWorkoutService.secureDocumentBuilderFactory()`). **Dlaczego**: pliki ZWO przychodzą od zewnętrznego dostawcy AI — XML injection jest realnym wektorem.

### Nigdy nie commituj plików .env ani sekretów
Plik `.env` jest w `.gitignore`. Sekrety na serwerze tworzone ręcznie. Sekret, który trafił do historii Git, traktuj jako skompromitowany. **Dlaczego**: repozytorium jest na GitHubie — każdy push upublicznia historię.

### Nie dodawaj zależności bez uzasadnienia
Projekt celowo ma mało zależności. Garmin FIT SDK jest jedyną niestandardową biblioteką. Przed dodaniem nowej zależności sprawdź, czy Spring Boot lub JDK nie oferują wbudowanego rozwiązania (np. `java.net.http.HttpClient` zamiast zewnętrznego klienta HTTP).

## Konwencje

- **Dostawcy AI**: nowi dostawcy dodawani w `AiProviderCatalog` (lista `PROVIDERS` + `MODELS`). Komunikacja przez OpenAI-compatible API (`/chat/completions`). Nie twórz osobnych klientów per dostawca.
- **Migracje Flyway**: plik `V{N}__opis_zmiany.sql` w `src/main/resources/db/migration/`. Flyway uruchamia się automatycznie przy starcie aplikacji. `ddl-auto: validate` — Hibernate tylko weryfikuje schemat, nie tworzy tabel.
- **Eventy użytkownika**: operacje logowane przez `UserEventService.record()` z typem (`STRAVA_SYNC`, `WORKOUTS_GENERATED` itp.). Timeline widoczny na dashboardzie.
- **Soft delete**: treningi oznaczane jako `deleted = true`, nie usuwane fizycznie z bazy.
- **Autoryzacja**: `CurrentUserService.get()` zwraca zalogowanego użytkownika. Każde query do bazy filtruje po `userId` — nie polegaj na samym Spring Security.

## CI/CD

Pipeline w `.github/workflows/ci-cd.yml`:
1. **test** — `mvn verify` na każdym pushu i PR do `main`
2. **build-and-push** — buduje obraz Docker, pushuje do GHCR (`ghcr.io/dawidg89pl/trener-ai:latest`) — tylko push do `main`
3. **deploy** — SSH na VPS Mikrus, `git pull` + `docker compose pull app` + `docker compose up -d`

Sekrety w GitHub: `MIKRUS_HOST`, `MIKRUS_USER`, `MIKRUS_SSH_KEY`, `MIKRUS_SSH_PORT`.
