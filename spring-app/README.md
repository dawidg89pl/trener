# Trener AI — Spring Boot

Wieloużytkownikowa wersja aplikacji. Każdy użytkownik ma własne konto, ustawienia, token Stravy, historię analiz i pliki treningów zapisane w PostgreSQL.

## Konfiguracja

W pliku `src/main/resources/application.yml` podaj dane PostgreSQL lub ustaw zmienne `DB_URL`, `DB_USERNAME` i `DB_PASSWORD`. Przed wdrożeniem ustaw także długi losowy `APP_CRYPTO_KEY` oraz `APP_PUBLIC_URL` (np. `https://trener.example.pl`).

Przy pierwszym uruchomieniu tworzony jest użytkownik startowy `dawid` z hasłem ustawionym w `app.seed-user.password`. Po zalogowaniu zmień to hasło albo ustaw `SEED_USER_ENABLED=false`.

```bash
createdb trener_ai
mvn spring-boot:run
```

If the existing database reports `Large Objects may not be used in auto-commit mode`, run [db/migrate-lobs-to-text.sql](db/migrate-lobs-to-text.sql) once. New installations do not need it.

Aplikacja będzie dostępna na porcie 8080. W Strava ustaw **Authorization Callback Domain** na domenę aplikacji (bez protokołu i bez ścieżki), np. `trener.example.pl`. W `APP_PUBLIC_URL` ustaw pełny, publiczny adres tej samej aplikacji, np. `https://trener.example.pl`. Podczas pracy lokalnej oba adresy to odpowiednio `localhost` oraz `http://localhost:8080`.
