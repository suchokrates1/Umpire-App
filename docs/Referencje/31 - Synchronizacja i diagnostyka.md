---
title: Synchronizacja i diagnostyka
tags: [aplikacja, referencja, sync]
aliases: [Sync status, Offline queue]
---

# Synchronizacja i diagnostyka

## Statusy na ekranie meczu (podtytuł)

| Tekst | Znaczenie |
|-------|-----------|
| **Synchronizacja...** | Trwa wysyłanie stanu |
| **Zsynchronizowano** | Ostatni update OK |
| **Błąd synchronizacji** | Błąd API — sprawdź sieć / PIN / backend |
| **Oczekuje na sieć — w kolejce synchronizacji** | Outbox offline; wyśle po powrocie sieci |
| (brak napisu) | Idle |

## Diagnostyka w Ustawieniach

Sekcja pokazuje: wersję app, URL backendu, model urządzenia, locale, strefę czasową, ostatni status sync, czas aktualizacji, treść błędu.

**Kopiuj diagnostykę** — wklej do zgłoszenia / czatu z adminem.

> [!tip] Offline
> Aplikacja kolejkuję zmiany lokalnie. Nie wyłączaj aplikacji w trakcie długiego braku sieci bez potrzeby — kolejka jest po to, by dogonić serwer później.

## Powiązane

- [[16 - Mecz - scoreboard i chrome]]
- [[22 - Ustawienia]]
- [[TODO]] — push stanu z reżyserki na tablet (kort, nazwiska, wynik, zasady)
- Plan PWA + Etap 6: `notes/areas/vest-media/PWA-sedzia-plan-wdrozenia.md`
