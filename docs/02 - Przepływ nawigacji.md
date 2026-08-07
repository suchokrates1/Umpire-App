---
title: Przepływ nawigacji
tags: [aplikacja, nawigacja]
aliases: [Mapa ekranów]
---

# Przepływ nawigacji

```mermaid
flowchart TD
  Lang[Wybór języka] --> Tour[Wybór turnieju]
  Tour --> Court[Wybór kortu]
  Court -->|PIN OK| Players[Wybór zawodników]
  Court --> Settings[Ustawienia]
  Settings --> History[Historia meczów]
  History --> Detail[Szczegóły meczu]
  Players --> Config[Konfiguracja meczu]
  Config --> Match[Mecz]
  Match -->|Następny ten sam setup| Players
  Match -->|Nowy setup| Players
  Court -->|Wstecz| Tour
  Tour -->|Wstecz| Lang
```

## Skrót przejść

| Z | Do | Jak |
|---|-----|-----|
| Start aplikacji | Język lub Turniej | Skip języka, jeśli już wybrany |
| Turniej | Korty | Stuknięcie turnieju |
| Kort | PIN → Zawodnicy | PIN 4 cyfry |
| Zawodnicy | Konfiguracja → Mecz | **Dalej** lub auto po pełnym wyborze |
| Mecz | Zawodnicy | Po zakończeniu: ten sam / nowy setup |
| Korty | Ustawienia | Menu (zębatka) |
| Ustawienia | Historia | Karta Historii meczów |

## Powiązane

- [[01 - Szybki start]]
- [[00 - Aplikacja sędziowska]]
