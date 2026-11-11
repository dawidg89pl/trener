---
name: status
description: "Szybki przegląd stanu projektu: git, build, Docker, ostatnie zmiany."
---

# Status projektu

Wyświetl zwięzły przegląd stanu projektu. Wykonaj wszystkie kroki i przedstaw wynik w jednym podsumowaniu.

## Krok 1 — Git

```bash
echo "--- Gałąź ---" && git branch --show-current && echo "--- Ostatnie commity ---" && git log --oneline -5 && echo "--- Niezacommitowane zmiany ---" && git status --short
```

## Krok 2 — Build

```bash
mvn compile -q -B 2>&1 && echo "Kompilacja: OK" || echo "Kompilacja: FAILED"
```

## Krok 3 — Docker

```bash
docker compose ps 2>/dev/null || echo "Docker Compose nie działa lub nie jest dostępny."
```

## Krok 4 — Wersja aplikacji

```bash
grep '<version>' pom.xml | head -1
```

## Podsumowanie

Przedstaw wynik w formacie:

```
Projekt: Trener AI
Wersja:  X.Y.Z
Gałąź:   nazwa
Build:   OK / FAILED
Docker:  działa / nie działa
Ostatnie commity: (lista 5 ostatnich)
Niezacommitowane: (lista plików lub "brak")
```
