---
title: Wybór serwującego
tags: [aplikacja, ekran, sędzia]
aliases: [Server selection, Who serves]
---

# Wybór serwującego

## Cel

Ustalenie, kto serwuje pierwszy, oraz opcjonalna zamiana stron na scoreboardzie.

## Kiedy się pojawia

Na początku meczu, przed pierwszym punktem. Scoreboard jest wtedy ukryty.

## Elementy UI

| Kontrolka | Co robi | Efekt |
|-----------|---------|-------|
| **Kto serwuje pierwszy?** | Nagłówek | — |
| **⇄ Zamień strony** | Zamiana stron wyświetlania | Animacja + `swapSides` (nie zmienia jeszcze serwisu) |
| Duże przyciski graczy (2 w singlu, 4 w deblu) | Wybór pierwszego serwującego | Start timera i przejście do punktacji |

Przyciski pokazują imię (+ płeć w deblu); zaznaczenie wizualne prefixem.

## Dialogi / stany

Brak dodatkowych dialogów.

## Nawigacja

- Po wyborze serwującego → [[18 - Tryb Advanced - serwis i wymiana]] lub [[19 - Tryb Basic]] (zależnie od configu)
- Zamiana stron możliwa też w trakcie dalszej gry przy komunikatach — [[20 - Komunikaty (zmiana stron, TB, DP)]]

## Powiązane

- [[32 - Singiel vs debel]]
- [[16 - Mecz - scoreboard i chrome]]
