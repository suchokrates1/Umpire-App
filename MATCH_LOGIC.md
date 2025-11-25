# Logika Meczu Tenisowego - Dokumentacja

## 📋 Przebieg Meczu

### 1. Konfiguracja
- Wybór kortu
- Wybór 2 zawodników
- Wybór pierwszego serwującego

### 2. Rozpoczęcie Meczu
- Timer startuje po wyborze serwującego
- Wynik: 0:0, 0:0, 0-0

## 🎾 Punktacja

### Punkty w Gemie
```
0 punktów = "0"
1 punkt   = "15"
2 punkty  = "30"
3 punkty  = "40"
```

### Szczególne Sytuacje
- **40:40** = Deuce (równowaga)
- **Przewaga** = Advantage (ADV) - gracz musi wygrać 2 punkty z rzędu
- **Wygrany gem** = Minimum 4 punkty + przewaga 2 punktów

### Przykład Gema:
```
0-0 → 15-0 → 15-15 → 30-15 → 40-15 → GEM
0-0 → 15-0 → 15-15 → 15-30 → 30-30 → 40-30 → 40-40 (Deuce)
→ ADV-40 → 40-40 → 40-ADV → GEM
```

## 🎯 Gemy i Sety

### Standardowy Set
- **Wygrany set:** 6 gemów + przewaga min. 2 gemy
- Przykłady: 6:4, 6:3, 6:2, 6:1, 6:0
- Przy **6:5** gramy kolejny gem:
  - 7:5 = wygrany set
  - 6:6 = tie-break

### Tie-break (przy 6:6)
- Gramy do **7 punktów** z przewagą 2
- Punkty liczone jako: 0, 1, 2, 3... (nie 15-30-40)
- Zmiana serwującego co 2 punkty
- Przykłady zakończenia: 7:5, 8:6, 10:8, 12:10

### Mecz
- **Format:** Do 2 wygranych setów
- Możliwe wyniki:
  - 2:0 (6:4, 6:3)
  - 2:1 (6:4, 3:6, 6:2)

### Super Tie-break (przy 1:1 w setach)
- Gramy do **10 punktów** z przewagą 2
- Zamiast 3. pełnego seta
- Przykłady: 10:8, 11:9, 12:10

## 🔄 Przepływ Gry

### 1. Serwis
**Pierwszy serwis:**
- ✅ **ACE** → Punkt dla serwującego, koniec wymiany
- ❌ **FAULT** → Przejście do drugiego serwisu
- 🎾 **BALL IN PLAY** → Wymiana

**Drugi serwis:**
- ❌ **FAULT** → Double Fault = punkt dla przeciwnika
- 🎾 **BALL IN PLAY** → Wymiana

### 2. Wymiana (Rally)
Każdy gracz może:
- ✅ **WINNER** → Punkt dla tego gracza
- ⚠️ **FORCED ERROR** → Punkt dla przeciwnika (błąd wymuszony)
- ❌ **UNFORCED ERROR** → Punkt dla przeciwnika (błąd własny)

### 3. Po Zakończeniu Punktu
System automatycznie:
1. Dodaje punkt odpowiedniemu graczowi
2. Sprawdza czy gem został wygrany
3. Jeśli tak - dodaje gema, resetuje punkty
4. Zmienia serwującego
5. Sprawdza czy set został wygrany
6. Jeśli tak - dodaje seta, resetuje gemy
7. Sprawdza warunki tie-breaku lub super tie-breaku
8. Sprawdza czy mecz się zakończył (2 wygrane sety)

## 📊 Statystyki

### Zbierane podczas meczu:
- **Aces** - punkty bezpośrednio z serwisu
- **Double Faults** - dwa nieudane serwisy z rzędu
- **Winners** - uderzenia kończące wymianę
- **Forced Errors** - wymuszone błędy
- **Unforced Errors** - niewymuszone błędy
- **First Serve %** - procent skuteczności 1. serwisu
- **Second Serve %** - procent skuteczności 2. serwisu

### Zliczanie serwisów:
```kotlin
// Pierwszy serwis
ACE lub BALL IN PLAY → firstServesIn++, firstServesTotal++
FAULT → firstServesTotal++

// Drugi serwis
BALL IN PLAY → secondServesIn++, secondServesTotal++
FAULT (double) → secondServesTotal++
```

## 🎮 Przykładowy Przebieg Meczu

```
WYBÓR SERWUJĄCEGO: Gracz A

SET 1:
━━━━━━━━━━━━━━━━━━━━━━━━
Gem 1: Gracz A serwuje
  0-0 → ACE → 15-0
  15-0 → Winner A → 30-0
  30-0 → Fault → Fault (Double) → 30-15
  30-15 → Ball in play → Winner A → 40-15
  40-15 → ACE → GEM dla A
  Wynik: A: 1-0 B: 0-0

Gem 2: Gracz B serwuje
  0-0 → Ball in play → Unforced Error B → 0-15
  0-15 → Fault → Ball in play → Winner B → 15-15
  15-15 → ACE → 30-15
  30-15 → Ball in play → Forced Error A → 40-15
  40-15 → ACE → GEM dla B
  Wynik: A: 1-1 B: 1-1

[...ciąg dalszy gemów...]

Wynik końcowy Set 1: A: 6-4 B: 4-6
Gracz A wygrywa Set 1

SET 2:
━━━━━━━━━━━━━━━━━━━━━━━━
[...mecz trwa...]

Wynik 6:6 → TIE-BREAK
  A: 1, B: 0
  A: 1, B: 1
  A: 2, B: 1
  [...]
  A: 7, B: 5
  Gracz A wygrywa tie-break i Set 2

MECZ ZAKOŃCZONY!
Zwycięzca: Gracz A (6:4, 7:6)
```

## 🔧 Implementacja w Kodzie

### MatchState
Przechowuje:
- Aktualny wynik (punkty, gemy, sety)
- Historię setów
- Stan gry (kto serwuje, czy tie-break)
- Statystyki obu graczy

### MatchViewModel
Obsługuje:
- `handleAce()` - as serwisowy
- `handleFault()` - nieudany serwis
- `handleBallInPlay()` - piłka w grze
- `handleWinner()` - winner
- `handleForcedError()` - wymuszony błąd
- `handleUnforcedError()` - niewymuszony błąd

### Automatyka
- Sprawdzanie wygranego gema
- Sprawdzanie wygranego seta
- Wykrywanie tie-breaku (6:6)
- Wykrywanie super tie-breaku (1:1 w setach)
- Zakończenie meczu (2 wygrane sety)
- Zmiana serwującego po gemie
