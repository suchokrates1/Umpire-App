# Tennis Referee App - Aplikacja dla Sędziów Tenisowych

Aplikacja Android umożliwiająca sędziom tenisowym wprowadzanie wyników meczów w czasie rzeczywistym i synchronizację ich z systemem score.vestmedia.pl oraz overlayami na transmisjach online.

## 🎯 Funkcjonalności

### Zaimplementowane (v1.0)
- ✅ **Wybór kortu** - Wyświetlanie listy kortów pobranych z serwera
- ✅ **Wybór zawodników** - Lista graczy z serwera, tryb singiel/debel
- ✅ **Prowadzenie meczu** - Pełna funkcjonalność sędziego
- ✅ **Scoreboard** - Profesjonalny wyświetlacz wyniku (punkty, gemy, sety)
- ✅ **Wybór serwującego** - Duże przyciski dla każdego gracza
- ✅ **Serwis** - Ace, Fault (1. i 2. serwis), Ball in play
- ✅ **Wymiana** - Winner, Forced Error, Unforced Error
- ✅ **Logika tenisowa** - 0-15-30-40, Deuce, Advantage
- ✅ **Tie-break** - Standardowy (do 7) i Super tie-break (do 10)
- ✅ **Statystyki** - Aces, Double Faults, Winners, Errors, % 1st serve
- ✅ Komunikacja z API (score.vestmedia.pl)
- ✅ Architektura MVVM z Repository pattern
- ✅ Obsługa błędów i loading states
- ✅ Nawigacja między ekranami z przekazywaniem danych

### Do zaimplementowania
- ⏳ Real-time aktualizacja wyników na serwerze (WebSocket/REST)
- ⏳ Wysyłanie statystyk do API po zakończeniu meczu
- ⏳ Zapisywanie meczu lokalnie (Room database)
- ⏳ Historia meczów
- ⏳ Możliwość cofnięcia ostatniego punktu
- ⏳ Timer meczu
- ⏳ Obsługa debla (4 graczy)

## 🏗️ Architektura

```
app/
├── data/
│   ├── api/          # Retrofit API service
│   ├── model/        # Data models
│   │   ├── Court, Player, Match, Score
│   │   ├── MatchState - Stan meczu podczas rozgrywki
│   │   └── MatchStatistics - Statystyki graczy
│   └── repository/   # Repository pattern
├── ui/
│   ├── courtselection/     # Ekran wyboru kortu
│   │   ├── CourtSelectionActivity
│   │   ├── CourtSelectionViewModel
│   │   └── CourtAdapter
│   ├── playerselection/    # Ekran wyboru zawodników
│   │   ├── PlayerSelectionActivity
│   │   ├── PlayerSelectionViewModel
│   │   └── PlayerAdapter
│   └── match/              # Ekran prowadzenia meczu
│       ├── MatchActivity
│       ├── MatchViewModel
│       └── MatchView (enum)
└── TennisRefereeApp
```

## 📦 Technologie

- **Język:** Kotlin
- **Minimum SDK:** 24 (Android 7.0)
- **Target SDK:** 34 (Android 14)
- **Architektura:** MVVM
- **Biblioteki:**
  - Retrofit 2.9.0 - Komunikacja z API
  - Coroutines - Asynchroniczne operacje
  - LiveData & ViewModel - Zarządzanie stanem
  - Material Design Components
  - ViewBinding

## 🚀 Instalacja

### Wymagania
- Android Studio Hedgehog (2023.1.1) lub nowszy
- JDK 17
- Android SDK

### Kroki instalacji

1. Otwórz projekt w Android Studio:
   ```
   File -> Open -> Wybierz folder android-tennis-referee
   ```

2. Poczekaj na synchronizację Gradle

3. Skonfiguruj URL serwera (jeśli potrzebne):
   Edytuj `RetrofitClient.kt`:
   ```kotlin
   private const val BASE_URL = "https://score.vestmedia.pl/"
   ```

4. Uruchom aplikację:
   - Podłącz urządzenie Android lub uruchom emulator
   - Kliknij "Run" (lub Shift+F10)

## 🔌 API Endpoints

Aplikacja komunikuje się z następującymi endpointami:

```
GET  /api/courts              - Pobierz listę kortów
GET  /api/players             - Pobierz listę zawodników
GET  /api/matches/{id}        - Pobierz szczegóły meczu
POST /api/matches             - Utwórz nowy mecz
PUT  /api/matches/{id}        - Aktualizuj wynik meczu
POST /api/matches/{id}/finish - Zakończ mecz
```

### Przykładowa struktura danych - Kort
```json
{
  "courts": [
    {
      "id": 1,
      "name": "Kort 1",
      "is_available": true,
      "current_match_id": null
    }
  ],
  "total_count": 3
}
```

### Przykładowa struktura danych - Zawodnicy
```json
{
  "players": [
    {
      "id": 1,
      "first_name": "Jan",
      "last_name": "Kowalski",
      "full_name": "Jan Kowalski",
      "country": "POL",
      "ranking": 42,
      "photo_url": null
    }
  ],
  "total_count": 15
}
```

## 📱 Ekrany

### 1. Wybór Kortu (Zaimplementowany)
- Wyświetla gridową listę dostępnych kortów (2 kolumny)
- Korty dostępne są oznaczone na zielono
- Korty zajęte są oznaczone na czerwono
- Przycisk odświeżania listy
- Loading indicator podczas pobierania danych
- Obsługa błędów
- Po wyborze kortu przejście do ekranu wyboru zawodników

### 2. Wybór Zawodników (Zaimplementowany)
- Wyświetla informację o wybranym korcie
- Checkbox "Debel" - przełącza tryb między singleem (2 graczy) a deblem (4 graczy)
- Lista wszystkich dostępnych zawodników z serwera
- Wyświetla nazwisko, kraj i ranking zawodnika
- Wybrani gracze są oznaczeni zielonym tłem i checkmarkiem
- Dynamiczny licznik wybranych graczy
- Przycisk "Dalej" aktywny tylko gdy wybrano odpowiednią liczbę graczy
- Przycisk "Wstecz" do powrotu do wyboru kortu

### 3. Konfiguracja Meczu (Zaimplementowany)
- **Scoreboard** na górze z nazwiskami graczy i wynikami (początkowo zerowe)
- Wyświetla punkty, gemy, Set 1, Set 2
- Wskaźnik który gracz serwuje (żółta kropka)
- **Duże przyciski wyboru:** "[Gracz 1] Serwuje" i "[Gracz 2] Serwuje"
- Po wyborze rozpoczyna się mecz z liczeniem czasu

### 4. Widok Serwisu (Zaimplementowany)
- Przyciski pojawiają się po stronie serwującego gracza
- **ACE** (zielony) - punkt bezpośrednio z serwisu
- **FAULT** (czerwony) - nieudany serwis (1. lub 2.)
- **BALL IN PLAY** (żółty) - piłka w grze, przejście do wymiany
- Informacja "1. Serwis" lub "2. Serwis"
- Drugi Fault = Double Fault = punkt dla przeciwnika

### 5. Widok Wymiany (Zaimplementowany)
- Przyciski po obu stronach dla każdego gracza
- **WINNER** (zielony) - uderzenie kończące wymianę
- **FORCED ERROR** (pomarańczowy) - wymuszony błąd
- **UNFORCED ERROR** (czerwony) - niewymuszony błąd
- Po każdej akcji automatyczna aktualizacja wyniku

### 6. Logika Meczu (Zaimplementowana)
- **Punkty:** 0 → 15 → 30 → 40
- **Deuce:** Przy 40:40
- **Advantage:** Przewaga po deuce
- **Gemy:** Do 4 punktów z przewagą 2
- **Sety:** Do 6 gemów z przewagą 2
- **Tie-break:** Przy 6:6 w gemach (do 7 punktów)
- **Super Tie-break:** Przy 1:1 w setach (do 10 punktów)
- **Mecz:** Do 2 wygranych setów

### 7. Statystyki (Zaimplementowane)
- Aces
- Double Faults
- Winners
- Forced Errors
- Unforced Errors
- Procent skuteczności 1. serwisu
- Wyświetlanie po zakończeniu meczu

## 🎨 Kolory i Styling

- **Główny kolor:** Zielony (#2E7D32) - nawiązanie do kortów tenisowych
- **Kort dostępny:** #4CAF50 (zielony)
- **Kort zajęty:** #F44336 (czerwony)
- **Zawodnik wybrany:** #C8E6C9 (jasny zielony)
- **Zawodnik niewybrany:** #FFFFFF (biały)
- **Akcentowy:** #FFC107 (żółty)

## 🔧 Konfiguracja

### Permissions
Aplikacja wymaga następujących uprawnień:
- `INTERNET` - Komunikacja z serwerem
- `ACCESS_NETWORK_STATE` - Sprawdzanie stanu połączenia

## 📝 TODO - Następne kroki

1. **Ekran meczu:**
   - Layout z wynikami (sety, gemy, punkty)
   - Przyciski do dodawania punktów dla każdego gracza
   - Wyświetlanie historii setów
   
2. **Logika tenisowa:**
   - Obliczanie punktów (0, 15, 30, 40, Advantage, Deuce)
   - Zliczanie gemów i setów
   - Określanie zwycięzcy

3. **Real-time updates:**
   - WebSocket dla live updates
   - Automatyczna synchronizacja z serwerem
   
4. **Dodatkowe funkcje:**
   - Zapisywanie meczu w lokalnej bazie danych
   - Tryb offline z synchronizacją po powrocie połączenia
   - Statystyki meczów

## 🐛 Debugowanie

W przypadku problemów z połączeniem do serwera:
1. Sprawdź czy URL w `RetrofitClient.kt` jest poprawny
2. Upewnij się że serwer jest dostępny
3. Sprawdź logi w Android Studio (Logcat)
4. Dla HTTP (nie HTTPS) upewnij się że `usesCleartextTraffic="true"` w Manifest

## 👥 Autor

Vest Media - Tennis Scoring System

## 📄 Licencja

Proprietary - Wszystkie prawa zastrzeżone
