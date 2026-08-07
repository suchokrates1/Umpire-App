# Blind Tennis Referee

Aplikacja Android dla sędziów tenisa niewidomych — wynik na żywo, sync z [score.vestmedia.pl](https://score.vestmedia.pl) i overlayami transmisji.

**Package:** `pl.vestmedia.tennisreferee`

## Przewodnik użytkownika (Obsidian)

Pełny katalog każdego ekranu, przycisku i dialogu:

➡️ **[docs/00 - Aplikacja sędziowska.md](docs/00%20-%20Aplikacja%20sędziowska.md)**

Otwórz folder `docs/` jako vault w Obsidianie (wikilinki, callouty, MOC).

Szybki start dnia turnieju: [docs/01 - Szybki start.md](docs/01%20-%20Szybki%20start.md)

## Funkcje (skrót)

- Język (PL/EN/DE/ES/FR/IT) → turniej → kort + PIN → zawodnicy (singiel/debel) → konfiguracja meczu
- Punktacja Basic (WIN/FAULT) lub Advanced (As, Winner, błędy…)
- Tie-break, match TB, no-ad (punkt decydujący), komunikaty zmiany stron
- Undo, timer, finish (normal / test / krecz / walkower)
- Sync + kolejka offline; historia lokalna; motyw jasny/ciemny/systemowy

## Stack

- Kotlin, MVVM, ViewBinding (nie Compose)
- minSdk 24 / targetSdk 36
- Retrofit, Coroutines, LiveData, Room, Material

## Instalacja (dev)

1. Otwórz folder `android-tennis-referee` w Android Studio.
2. Zsynchronizuj Gradle (JDK z Android Studio JBR).
3. Backend domyślnie: `https://score.vestmedia.pl/`
4. Uruchom na urządzeniu / emulatorze.

Build / Play Store: zobacz `../DEPLOYMENT.md` (katalog Vest Tennis).

## Dokumentacja techniczna (repo)

| Plik | Temat |
|------|--------|
| [MATCH_LOGIC.md](MATCH_LOGIC.md) | Reguły punktacji |
| [DOUBLES_SUPPORT.md](DOUBLES_SUPPORT.md) | Debel / rotacja serwisu |
| [DARK_MODE_GUIDE.md](DARK_MODE_GUIDE.md) | Motyw |
| [API_EXAMPLES.md](API_EXAMPLES.md) | Przykłady API |

## Uprawnienia

- `INTERNET`, `ACCESS_NETWORK_STATE`

## Autor

Vest Media — Tennis Scoring System (Proprietary)
