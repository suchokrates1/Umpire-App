---
title: Koniec meczu i powody zakończenia
tags: [aplikacja, ekran, sędzia]
aliases: [Match finished, Finish match, Walkover, Retirement]
---

# Koniec meczu i powody zakończenia

## Cel

Zakończenie meczu (naturalne lub ręczne), podsumowanie statystyk i wybór kolejnego meczu.

## A) Ręczne zakończenie — **Zakończ mecz**

Dialog listy: tytuł **Dlaczego kończysz ten mecz?** + opcje (bez `setMessage` — w AlertDialog wyklucza się z `setItems`).

| Opcja | Co robi |
|-------|---------|
| **Normalne zakończenie** | Zapis jako normalny wynik / koniec |
| **Wpis testowy** | Oznaczenie testowe (nie traktować jako oficjalny wynik turnieju) |
| **Krecz** | Następny dialog: **Który zawodnik ma kontuzję?** → wybór drużyny/gracza → koniec RETIREMENT |
| **Walkower** | **Który zawodnik wygrywa walkowerem?** → WALKOVER |
| **Nie** | Anuluj dialog |

UI: `FinishMatchDialogUITest` — lista powodów musi być widoczna; walkower → MATCH_FINISHED.

## B) Widok MATCH_FINISHED

Chrome meczu (**Cofnij** / **Zakończ mecz**) jest ukryty — nie pokazujemy dialogu powodów przy naturalnym końcu.

| Element | Znaczenie |
|---------|-----------|
| **Koniec meczu!** | Nagłówek |
| **Zwycięzca: …** | Wynik |
| Tabela **Statystyki** | Asy, Podwójne błędy, Zagrania wygrywające, Błędy niewymuszone, 1. serwis % |
| **Następny mecz (ten sam setup)** | Powrót do [[14 - Wybór zawodników]] z zachowanym `MatchConfig` |
| **Nowy mecz (inne ustawienia)** | Zawodnicy + ponowna [[15 - Konfiguracja meczu]] |

## Nawigacja

Po wyborze następnego meczu → aktywny mecz czyszczony → ekran zawodników.

## Powiązane

- [[16 - Mecz - scoreboard i chrome]]
- [[30 - Przyciski punktacji - słownik]]
