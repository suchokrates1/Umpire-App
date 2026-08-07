---
title: Mecz - scoreboard i chrome
tags: [aplikacja, ekran, sędzia]
aliases: [MatchActivity, Scoreboard]
---

# Mecz — scoreboard i chrome

## Cel

Wspólna ramka ekranu meczu: wynik, timer, synchro, Cofnij, Zakończ. Wewnątrz zmieniają się widoki punktacji.

## Kiedy się pojawia

Po starcie z [[15 - Konfiguracja meczu]]. Ekran pozostaje aktywny do wyjścia lub zakończenia.

## Elementy zawsze widoczne

| Kontrolka | Co robi | Efekt |
|-----------|---------|-------|
| **↶ Cofnij** | Potwierdzenie → cofnięcie ostatniej akcji | Undo; nieaktywne (przygaszone), gdy brak historii |
| **Zakończ mecz** | Dialog powodów | → [[21 - Koniec meczu i powody zakończenia]] |
| Podtytuł ActionBar | Status sync | [[31 - Synchronizacja i diagnostyka]] |
| Keep-screen-on | Ekran nie gaśnie | — |
| System Back | Jeśli mecz w toku | Dialog **Opuścić mecz?** |

### Scoreboard (ukryty przy wyborze serwującego)

| Element | Znaczenie |
|---------|-----------|
| Timer `HH:MM:SS` | Czas trwania meczu |
| Meta: **Singiel / Debel / Mikst** • **Sędzia: …** • opcjonalna data ręczna | Kontekst |
| Baner TB / Match TB | Gdy trwa tie-break |
| Wiersze zawodników/par | Flagi, ikona serwisu, punkty, Set 1, Set 2 |
| Nagłówki **Zawodnik**, **Punkty**, **Set 1**, **Set 2** | Kolumny |

## Dialogi chrome

| Dialog | Przyciski | Efekt |
|--------|-----------|-------|
| **Opuścić mecz?** | **Tak** / **Nie** | Tak = wyjście z Activity (mecz może zostać w stanie lokalnym) |
| **Cofnij** — **Cofnąć ostatnią akcję?** | **Tak** / **Nie** | Tak = undo |
| **Informacja o meczu** (inne grupy / sparing) | **OK** | Tylko informacja, nieanulowalny |

## Nawigacja wewnętrzna (widoki)

| Widok | Notatka |
|-------|---------|
| SERVER_SELECTION | [[17 - Wybór serwującego]] |
| SERVE / RALLY | [[18 - Tryb Advanced - serwis i wymiana]] |
| BASIC_SCORING | [[19 - Tryb Basic]] |
| ANNOUNCEMENT | [[20 - Komunikaty (zmiana stron, TB, DP)]] |
| MATCH_FINISHED | [[21 - Koniec meczu i powody zakończenia]] |

## Powiązane

- [[30 - Przyciski punktacji - słownik]]
- [[31 - Synchronizacja i diagnostyka]]
