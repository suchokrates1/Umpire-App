---
title: Ustawienia
tags: [aplikacja, ekran, sędzia]
aliases: [Settings]
---

# Ustawienia

## Cel

Język, motyw, dostęp do historii lokalnej oraz diagnostyka połączenia z backendem.

## Kiedy się pojawia

Menu (zębatka) na [[12 - Wybór kortu]].

## Elementy UI

| Sekcja / kontrolka | Co robi | Efekt |
|--------------------|---------|-------|
| **Wybierz język** + radio 6 języków | Zmiana locale | Zapis + `recreate()` Activity |
| **Wygląd** / **Motyw**: **Jasny** / **Ciemny** / **Systemowy** | Motyw UI | Natychmiastowa zmiana |
| Karta **Tutorial sędziego** / **Start tutorial** | Mecz demo z dymkami | → [[24 - Tutorial sędziego]] |
| Karta **Historia meczów** / **Otwórz historię meczów** | Lista lokalna | → [[23 - Historia i szczegóły meczu]] |
| **Informacje** | Nazwa app, **Wersja …**, opis | — |
| **Diagnostyka** | Wersja, URL backendu, urządzenie, locale, strefa, status sync / błąd | Podgląd |
| **Kopiuj diagnostykę** | Schowek | Toast **Skopiowano diagnostykę** |

## Nawigacja

- Up / wstecz → powrót do wyboru kortu
- Historia → osobna Activity

## Powiązane

- [[10 - Wybór języka]]
- [[31 - Synchronizacja i diagnostyka]]
- [[23 - Historia i szczegóły meczu]]
