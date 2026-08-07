---
title: Konfiguracja meczu
tags: [aplikacja, dialog, sędzia]
aliases: [Match setup, Match config]
---

# Konfiguracja meczu

## Cel

Ustalenie formatu meczu, sędziego, trybu punktacji (Basic/Advanced) i start meczu. **Wybór karty Basic lub Advanced startuje mecz.**

## Kiedy się pojawia

Po **Dalej** na [[14 - Wybór zawodników]], gdy nie ma zapisanego `match_config` do ponownego użycia.

## Elementy UI

| Kontrolka | Co robi | Efekt |
|-----------|---------|-------|
| Tytuł **Konfiguracja meczu** | — | — |
| Pole sędziego (**Imię i nazwisko lub inicjały…**) | Zapis nazwy sędziego | Może być puste |
| Blok **Debel mieszany** (tylko debel) | Status **Mikst** / **Debel** | Informacyjny (z doboru płci) |
| **Ręczna data i godzina** | Nadpisanie czasu startu | Domyślnie automatyczny „teraz” |
| **Ustaw** | DatePicker → TimePicker (24h) | Zapis `manualStartTime` |
| **Wyczyść** | Usuwa ręczną datę | Wraca do automatycznej |
| Switch **Tylko Tiebreak** | Mecz = sam TB zamiast setów | Ukrywa format gemów/setów; pokazuje TB do 7/10 |
| **Gemów do seta** `3\|4\|5\|6` | Format | Domyślnie **4** |
| **Setów do wygranej** `1\|2\|3` | Format | Domyślnie **2** |
| **Tiebreak do** `7\|10` | TB w secie | Domyślnie **7** |
| **Decydujący TB do** `7\|10` | Match TB / STB | Domyślnie **10** |
| Switch **Bez przewagi (Punkt decydujący)** | No-ad przy 40–40 | Włącza komunikat DP |
| Karta **Podstawowy** | Start w trybie Basic | `statsMode=BASIC` → mecz |
| Karta **Zaawansowany** | Start w trybie Advanced | `statsMode=ADVANCED` → mecz |

Teksty kart:
- Podstawowy: **WIN + FAULT / Szybko i prosto**
- Zaawansowany: **As, Winner, Błędy… / Pełne statystyki**

## Dialogi / stany

- Dialog jest cancelable (poza / wstecz) — bez przycisku OK; start wyłącznie przez kartę trybu.
- Tryb TB only: `setsToWin=1`, punkty TB z przełącznika 7/10.

## Nawigacja

- Po wyborze trybu → zapis aktywnego meczu → [[17 - Wybór serwującego]] (w [[16 - Mecz - scoreboard i chrome]])
- Anulowanie → pozostajesz na wyborze zawodników

## Powiązane

- [[18 - Tryb Advanced - serwis i wymiana]]
- [[19 - Tryb Basic]]
- [[32 - Singiel vs debel]]
