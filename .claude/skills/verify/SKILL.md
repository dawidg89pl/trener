---
name: verify
description: Pełna weryfikacja projektu — kompilacja, testy i sprawdzenie diffu. Uruchom przed commitem lub PR.
---

# Weryfikacja projektu

Wykonaj poniższe kroki w kolejności. Jeśli którykolwiek krok się nie powiedzie, zatrzymaj się i opisz problem.

## Krok 1 — Kompilacja

```bash
cd trener-ai-app && mvn compile -B
```

Jeśli kompilacja się nie powiedzie, napraw błędy i powtórz.

## Krok 2 — Testy

```bash
cd trener-ai-app && mvn test -B
```

Zgłoś liczbę testów uruchomionych, zaliczonych i niezaliczonych.

## Krok 3 — Sprawdzenie diffu

```bash
git diff --stat HEAD
```

Podsumuj zmiany: które pliki zostały zmodyfikowane, dodane lub usunięte.

## Krok 4 — Sprawdzenie sekretów

```bash
git diff HEAD -- '*.java' '*.yml' '*.xml' '*.html' '*.properties' | grep -iE '(password|secret|api.?key|token|credential).*=' | grep -v 'encrypted\|crypto\|test\|example\|placeholder\|change.this' || echo 'Brak podejrzanych sekretów w diffie.'
```

Jeśli wykryto coś podejrzanego, zgłoś to użytkownikowi.

## Podsumowanie

Na koniec podaj zwięzłe podsumowanie:
- Kompilacja: OK / FAILED
- Testy: X/Y zaliczonych
- Zmiany: lista plików
- Sekrety: czysto / UWAGA
