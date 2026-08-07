---
title: Wybór turnieju
tags: [aplikacja, ekran, sędzia]
aliases: [Tournament selection]
---

# Wybór turnieju

## Cel

Wybór aktywnego turnieju dnia, z którego pobierane są korty i zawodnicy.

## Kiedy się pojawia

- Po [[10 - Wybór języka]].
- Przy zmianie turnieju (powrót z [[12 - Wybór kortu]]).
- Auto-przejście do kortów, jeśli turniej na dziś jest już zapisany i nadal aktywny (chyba że wymuszono ponowny wybór).

## Elementy UI

| Kontrolka | Co robi | Efekt |
|-----------|---------|-------|
| Tytuł **Wybierz turniej** | — | — |
| Karta turnieju (nazwa, lokalizacja, daty) | Wybór | Zapis wyboru → [[12 - Wybór kortu]] |
| **Odśwież** | Ponowne pobranie listy | API aktywnych turniejów |
| Stan pusty **Brak aktywnych turniejów** | Informacja | Użyj Odśwież lub sprawdź Admin |
| Toast **Błąd wczytywania turniejów** | Błąd sieci/API | — |

## Dialogi / stany

- Loading podczas pobierania.
- W trybie wymuszonego wyboru (zmiana z kortów): wynik wraca do kortów zamiast pełnego restartu stosu.

## Nawigacja

- **Dalej:** stuknięcie turnieju → korty
- **Wstecz:** → [[10 - Wybór języka]] (wymuszony wybór) albo anulowanie w trybie force

## Powiązane

- [[12 - Wybór kortu]]
- [[01 - Szybki start]]
