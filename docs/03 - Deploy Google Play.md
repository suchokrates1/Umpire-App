---
title: Deploy Google Play
tags: [aplikacja, deploy, play]
aliases: [Play Store deploy, Upload AAB]
---

# Deploy Google Play — Blind Tennis Referee

Jak zbudować AAB i wrzucić na wszystkie tory Play z **dowolnego PC**.

## Wymagania

- Git clone: https://github.com/suchokrates1/Umpire-App (`android-tennis-referee`)
- JDK 17 (np. Eclipse Temurin)
- Android SDK + `local.properties` z `sdk.dir=...`
- Python 3.10+ z `google-api-python-client`, `google-auth`
- Dostęp do Vaultwarden: wpis **`Google Play Blind Tennis Referee (deploy)`** (folder *APIs i Tokeny*)
- Skrypt `deploy.py` z katalogu nadrzędnego Vest Tennis **albo** ten sam workflow ręcznie

## Sekrety z Vaultwarden

```bash
# Windows (vault.bat → SSH minipc) albo Linux z vault CLI
vault "Google Play Blind Tennis Referee (deploy)" --field service_account_json > play-service-account.json
vault "Google Play Blind Tennis Referee (deploy)" --field keystore_jks_base64 > keystore.b64

# Linux / Git Bash:
base64 -d keystore.b64 > release.jks
# PowerShell:
# [IO.File]::WriteAllBytes("release.jks", [Convert]::FromBase64String((Get-Content keystore.b64 -Raw)))

vault "Google Play Blind Tennis Referee (deploy)" --field KEYSTORE_STORE_PASSWORD
vault "Google Play Blind Tennis Referee (deploy)" --field KEYSTORE_KEY_PASSWORD
vault "Google Play Blind Tennis Referee (deploy)" --field KEYSTORE_KEY_ALIAS
```

Umieść pliki lokalnie (poza gitem):

| Plik | Gdzie |
|------|--------|
| `play-service-account.json` | `%USERPROFILE%\.config\vest-tennis\play-service-account.json` **lub** `PLAY_SERVICE_ACCOUNT_JSON` |
| `release.jks` | `android-tennis-referee/keystore/release.jks` |

`local.properties` (gitignored):

```properties
sdk.dir=C:\\Users\\<you>\\AppData\\Local\\Android\\Sdk
KEYSTORE_FILE=../keystore/release.jks
KEYSTORE_KEY_ALIAS=blindtennis
KEYSTORE_STORE_PASSWORD=<z vault>
KEYSTORE_KEY_PASSWORD=<z vault>
```

Albo zmienne środowiskowe: `KEYSTORE_FILE`, `KEYSTORE_KEY_ALIAS`, `KEYSTORE_STORE_PASSWORD`, `KEYSTORE_KEY_PASSWORD`.

## Wersja

W `app/build.gradle` przed buildem:

- `vBuild` — +1 przy każdym uploadzie (Play odrzuca ten sam `versionCode`)
- `vQualifier` — zwykle `"dev"` na wszystkie tory w tym projekcie (dev.N na internal/alpha/beta/production)

`versionCode = MAJOR*100000 + MINOR*10000 + PATCH*1000 + BUILD` → np. `1.0.0-dev.28` = `100028`.

## Build + upload (zalecane)

Z katalogu **Vest Tennis** (rodzic zawierający `deploy.py` i `android-tennis-referee/`):

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
$env:PLAY_SERVICE_ACCOUNT_JSON = "$env:USERPROFILE\.config\vest-tennis\play-service-account.json"

python deploy.py status
python deploy.py build
python deploy.py upload all -n "1.0.0-dev.N — opis zmian"
```

`upload all` = **internal + alpha + beta + production**.

Same build bez uploadu:

```powershell
cd android-tennis-referee
.\gradlew.bat assembleRelease bundleRelease
# AAB: app\build\outputs\bundle\release\app-release.aab
```

## Promote bez rebuild

Gdy AAB jest już w Play (np. tylko na internal):

```powershell
python deploy.py promote 100028 all -n "Promocja na wszystkie tory"
```

## Checklist nowego PC

1. [ ] `git clone` Umpire-App + (opcjonalnie) Vest Tennis z `deploy.py`
2. [ ] `vault --list` działa (SSH do minipc / Tailscale)
3. [ ] Pobrane SA JSON + keystore z vault (tabela wyżej)
4. [ ] JDK 17, Android SDK, `pip install google-api-python-client google-auth`
5. [ ] `python deploy.py status` — widać tory
6. [ ] Build + `upload all`

## Powiązane

- [[00 - Aplikacja sędziowska]]
- Vaultwarden: https://vault.dawidsuchodolski.pl
- Play Console: https://play.google.com/console
- Package: `pl.vestmedia.tennisreferee`
