---
title: Wybór zawodników
tags: [aplikacja, ekran, sędzia]
aliases: [Player selection]
---

# Wybór zawodników

## Cel

Wybór graczy do meczu (singiel 2 / debel 4), opcjonalnie z sugestii z planu turnieju, oraz dodanie nowego gracza ad-hoc.

## Kiedy się pojawia

Po poprawnym PIN-ie kortu. Także po zakończeniu meczu przy „następnym meczu”.

## Elementy UI

| Kontrolka | Co robi | Efekt |
|-----------|---------|-------|
| Etykieta **Kort: …** | Kontekst | — |
| Tytuł **Wybierz zawodników** | — | — |
| Checkbox **Debel** | Przełącza singiel ↔ debel | Wymaga 2 lub 4 graczy; czyści powiązanie z sugestią planu |
| Typ gry **Singiel (2 graczy)** / **Debel (4 graczy)** (+ **• Mikst**) | Feedback | Mikst = obie pary mają M+K (informacyjnie) |
| **Wybrano: X/Y** | Licznik | — |
| Pole **Szukaj zawodników** | Filtr listy | Lokalne filtrowanie |
| FAB / **+ Dodaj nowego gracza** | Nowy zawodnik | Dialog dodawania (niżej) |
| Karta zawodnika (flaga, imię, płeć, kategoria B1–B4) | Zaznaczenie / odznaczenie | Toggle; czyści scheduleId |
| **Wstecz** | Wyjście | `finish()` → korty |
| **Dalej** | Dalej gdy liczba OK | → [[15 - Konfiguracja meczu]] (lub od razu mecz, jeśli jest zapisany `match_config`) |

### Karta sugerowanego meczu (gdy plan ma trafienie)

| Kontrolka | Co robi | Efekt |
|-----------|---------|-------|
| **Sugerowany mecz z planu** | Info: gracze, czas, kategoria, faza | — |
| **Użyj meczu** | Preselektuje graczy (ścieżka singles) | Toast **Wybrano sugerowany mecz**, zapis `scheduleId` |
| **Wybiorę ręcznie** | Odrzuca sugestię | Ukrywa kartę, czyści `scheduleId` |

## Dialog: Dodaj gracza

| Pole / przycisk | Co robi | Walidacja / efekt |
|-----------------|---------|-------------------|
| **Imię** | Imię | Wymagane |
| **Nazwisko** | Nazwisko | Wymagane |
| **Kraj** | Lista krajów | Domyślnie Polska; kody flag |
| **Kategoria** | B1 / B2 / B3 / B4 | — |
| OK | Wysyła gracza na API | Dodaje do listy i turnieju |
| Anuluj | Zamyka | — |

> [!note] Prefill z wyszukiwarki
> Jeśli w polu szukaj jest tekst, dialog może podzielić go na imię/nazwisko.

## Stany / błędy

| Sytuacja | Komunikat / zachowanie |
|----------|------------------------|
| Pusta lista | **Brak dostępnych zawodników** + hint o **+** |
| Brak wyników filtra | **Nie znaleziono zawodnika** |
| Dalej przy złej liczbie | Toast **Wybierz odpowiednią liczbę graczy** |
| Brak court_id | Toast **Błąd: Brak danych kortu**, zamknięcie |
| Pełny wybór (2/4) | Po ~300 ms auto-przejście jak **Dalej** |

## Nawigacja

- **Dalej / auto** → [[15 - Konfiguracja meczu]]
- **Wstecz** → [[12 - Wybór kortu]]
- Po meczu z wynikiem „ten sam setup” — ponowny wybór graczy z zachowanym configiem

## Powiązane

- [[15 - Konfiguracja meczu]]
- [[32 - Singiel vs debel]]
