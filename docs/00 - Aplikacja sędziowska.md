---
title: Aplikacja sędziowska
tags: [aplikacja, moc, sędzia]
aliases: [Blind Tennis Referee, MOC Aplikacja]
---

# Aplikacja sędziowska — Blind Tennis Referee

> [!info] Vault Obsidian
> Otwórz folder `android-tennis-referee/docs/` jako vault **albo** dodaj go do wspólnego vaultu. Linki wewnętrzne używają `[[wikilinków]]` — działają w obrębie jednego vaultu.

Kompletny katalog funkcji i instrukcja użycia aplikacji Android dla sędziów tenisa niewidomych. Każdy ekran, przycisk i dialog jest opisany osobną notatką.

## Szybki dostęp

- [[01 - Szybki start]] — typowy dzień turnieju krok po kroku
- [[02 - Przepływ nawigacji]] — mapa ekranów
- [[03 - Deploy Google Play]] — build AAB + upload na wszystkie tory (też z innego PC)

## Ekrany

1. [[10 - Wybór języka]]
2. [[11 - Wybór turnieju]]
3. [[12 - Wybór kortu]]
4. [[13 - Autoryzacja PIN]]
5. [[14 - Wybór zawodników]]
6. [[15 - Konfiguracja meczu]]
7. [[16 - Mecz - scoreboard i chrome]]
8. [[17 - Wybór serwującego]]
9. [[18 - Tryb Advanced - serwis i wymiana]]
10. [[19 - Tryb Basic]]
11. [[20 - Komunikaty (zmiana stron, TB, DP)]]
12. [[21 - Koniec meczu i powody zakończenia]]
13. [[22 - Ustawienia]]
14. [[23 - Historia i szczegóły meczu]]

## Referencje

- [[30 - Przyciski punktacji - słownik]]
- [[31 - Synchronizacja i diagnostyka]]
- [[32 - Singiel vs debel]]
- [[TODO]] — zdalne sterowanie tabletem z reżyserki

## Poza tym vaultem

- Logika punktacji (techniczna): plik `MATCH_LOGIC.md` w katalogu głównym repo aplikacji
- Debel (technicznie): `DOUBLES_SUPPORT.md`
- Deploy / Play Store: `DEPLOYMENT.md` w `Vest Tennis`

## Produkt

| Pole | Wartość |
|------|---------|
| Nazwa | Blind Tennis Referee |
| Package | `pl.vestmedia.tennisreferee` |
| Backend | score.vestmedia.pl |
| Języki UI | PL, EN, DE, ES, FR, IT |
