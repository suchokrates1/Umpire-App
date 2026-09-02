# TODO — aplikacja sędziowska

## Zdalne sterowanie z reżyserki (P0)

Pełny opis: `wyniki-live/docs/TODO.md`.

Admin klika w reżyserce → tablet sędziego **od razu** dostaje nowy kort, nazwiska, wynik i zasady. Samo przeniesienie w bazie/overlayu nie wystarczy: apka trzyma `courtId` i nadpisuje eventami.

Vilnius 2026: dwa tablety na korcie 2; González–Schmidt przeniesiony na 8 w bazie, telefon dalej pisał na 2.

Wymaga: push/kanał serwer → apka (nie tylko PUT z tabletu).

Pełny plan portu sędziego do przeglądarki (PWA 1:1) + Etap 6 reżyserki: w vaultcie Vest Media `notes/areas/vest-media/PWA-sedzia-plan-wdrozenia.md` ([[areas/vest-media/PWA-sedzia-plan-wdrozenia]]). Ten TODO zostaje źródłem **incydentu Vilnius**, nie drugim planem produktu.
