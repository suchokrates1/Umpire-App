---
title: Komunikaty (zmiana stron, TB, DP)
tags: [aplikacja, ekran, sędzia]
aliases: [Announcements, Change sides, Deciding point]
---

# Komunikaty (zmiana stron, TB, DP)

## Cel

Wymuszenie uwagi sędziego przy zdarzeniach meczowych: zmiana stron, start TB/STB, punkt decydujący (no-ad).

## Kiedy się pojawia

Inline na ekranie meczu (nie AlertDialog), gdy logika meczu wymaga potwierdzenia przed kolejnym punktem.

## Typy komunikatów

| Typ | Tytuł | Treść | Extra |
|-----|-------|-------|-------|
| Zmiana stron | **Zmiana stron!** | **Zawodnicy zamieniają się stronami kortu.** | Przycisk **Nie zmieniaj stron** |
| Tie-break | **Tie-break!** | Info o gemach/punktach | — |
| Match Tie-break | **Match Tie-break!** | Info o formacie | — |
| Punkt decydujący | **Punkt decydujący!** | Przy 40–40 (no-ad); odbiorca wybiera stronę wg reguł | — |

## Elementy UI

| Kontrolka | Co robi | Efekt |
|-----------|---------|-------|
| **Dalej** | Kontynuacja | `continueFromAnnouncement` — przy zmianie stron zwykle stosuje swap |
| **Nie zmieniaj stron** | Tylko przy side_change | `skipSideChange` — bez zamiany wyświetlania |

## Nawigacja

Po **Dalej** → powrót do punktacji (Basic/Advanced) lub kolejny stan meczu.

## Powiązane

- [[15 - Konfiguracja meczu]] (no-ad, TB)
- [[16 - Mecz - scoreboard i chrome]]
