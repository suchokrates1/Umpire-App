---
title: Tutorial sędziego
tags: [aplikacja, ekran, sędzia]
aliases: [Tutorial]
---

# Tutorial sędziego

## Cel

Prowadzony mecz demo: kort, PIN, zawodnicy, strony, punktacja, zmiana stron, set, TB, Undo, krecz. Bez API i outboxa.

## Kiedy się pojawia

- [[22 - Ustawienia]] → karta Start tutorial
- Banner przy pierwszym starcie (język / turniej / kort). **Później** tylko odkłada; Settings zawsze ponawia.

## Przebieg

1. Kort 1 + PIN `1234`
2. Dwóch zawodników demo, setup Basic
3. Swap stron i wybór serwisu
4. **Na żywo** (ten sam silnik co mecz, bez skoku snapshotem):
   - WIN
   - 2. serwis
   - podwójny błąd
   - Undo — od razu, bez dialogu potwierdzenia
5. Potem skoki snapshotem: nowy serwisujący, zmiana stron, set-break, TB, Finish / krecz
6. Ekran końca — powrót do ustawień albo kortów

## Sandbox

`TutorialSession` + `TutorialCatalog`. Brak `verifyCourtPin`, sync, director poll, `ActiveMatchStore`.
