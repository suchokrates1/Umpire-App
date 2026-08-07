---
title: Autoryzacja PIN
tags: [aplikacja, dialog, sędzia]
aliases: [Court PIN, PIN kortu]
---

# Autoryzacja PIN

## Cel

Potwierdzenie, że sędzia ma prawo prowadzić mecz na wybranym korcie (PIN ustawiany w Admin → Korty).

## Kiedy się pojawia

Po stuknięciu karty kortu na [[12 - Wybór kortu]].

## Elementy UI

| Kontrolka | Co robi | Efekt |
|-----------|---------|-------|
| Tytuł **Autoryzacja kortu** | — | — |
| Komunikat **Wprowadź PIN dla kortu: …** | Kontekst | — |
| 4 pola cyfr | Wpis PIN | Auto-przeskok do kolejnego pola; po 4. cyfrze auto-wysłanie |
| **Anuluj** | Zamknięcie dialogu | Powrót do listy kortów |
| ProgressBar | Trwa weryfikacja | Pola zablokowane na czas requestu |

## Dialogi / stany / błędy

| Sytuacja | Zachowanie |
|----------|------------|
| PIN poprawny | Dialog znika → [[14 - Wybór zawodników]] (court_id, court_name) |
| PIN błędny | Toast **Nieprawidłowy PIN**, pola czyszczone, fokus na pierwszą cyfrę |
| Backspace | Fokus wraca do poprzedniego pola |

> [!warning] Bezpieczeństwo
> PIN nie jest zapamiętywany w UI. Każde wejście na kort wymaga ponownego wpisania.

## Nawigacja

- **Dalej:** sukces weryfikacji → zawodnicy
- **Anuluj / poza dialog:** pozostajesz na wyborze kortu

## Powiązane

- [[12 - Wybór kortu]]
- [[31 - Synchronizacja i diagnostyka]]
