---
title: Wybór kortu
tags: [aplikacja, ekran, sędzia]
aliases: [Court selection]
---

# Wybór kortu

## Cel

Wybór fizycznego kortu, na którym sędzia będzie sędziować. Dostęp chroniony PIN-em.

## Kiedy się pojawia

Po wyborze turnieju. Wymaga zapisanego języka i turnieju dnia — inaczej przekierowuje wstecz.

## Elementy UI

| Kontrolka | Co robi | Efekt |
|-----------|---------|-------|
| Tytuł **Wybierz kort** | — | — |
| Podtytuł (nazwa turnieju) | Kontekst | — |
| Karta kortu (nazwa + **Dostępny** / **Zajęty**) | Wybór kortu | Otwiera [[13 - Autoryzacja PIN]] |
| **Odśwież** | Przeładowanie listy kortów | API kortów turnieju |
| Menu **Ustawienia** (zębatka) | Otwiera ustawienia | → [[22 - Ustawienia]] |
| Stan pusty **Brak dostępnych kortów** | Informacja | — |

> [!tip] Kolory statusu
> Status **Dostępny** / **Zajęty** jest informacyjny — PIN i tak wymagany przy wejściu na kort.

## Dialogi / stany

- Progress podczas ładowania.
- Dialog PIN — osobna notatka [[13 - Autoryzacja PIN]].

## Nawigacja

- **Dalej:** PIN OK → [[14 - Wybór zawodników]]
- **Wstecz / Up:** → [[11 - Wybór turnieju]] (wymuszony wybór)

## Powiązane

- [[13 - Autoryzacja PIN]]
- [[22 - Ustawienia]]
