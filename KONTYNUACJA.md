# Kontynuacja prac nad Pixel Center

Aktualny kod, dokumentacja i historia zmian znajdują się w tym repozytorium. Publiczne wydania są publikowane w `https://github.com/proszczok2002/Pixel-Center/releases`, a aktywny manifest updatera jest w `server/pixel-center-update.json` na gałęzi `main`.

## Środowisko

- JDK 17.
- Android SDK 37.
- Gradle Wrapper dołączony do repozytorium.
- Lokalny `local.properties` może wskazywać SDK, ale nie jest śledzony przez Git.

## Pełna weryfikacja

```powershell
.\gradlew.bat clean testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug
```

Wynik: `app/build/outputs/apk/debug/app-debug.apk`.

## Zasady wydawania

- Zwiększ `versionCode`; `versionName` ma odpowiadać nazwie wydania.
- Używaj tego samego klucza podpisującego, ponieważ updater odrzuca inny certyfikat.
- Najpierw opublikuj i niezależnie sprawdź APK w GitHub Releases, a manifest na `main` zmień na końcu.
- Potwierdź publiczny manifest, rozmiar, SHA-256, pakiet, wersję i podpis pobranego APK.
- Stary manifest na `proszczok.pl` aktualizuj tylko jako most dla wersji sprzed migracji.

## Porządek lokalny

W aktywnym projekcie zostają źródła, dokumentacja, konfiguracja, narzędzia, manifest updatera i tylko najnowszy lokalny APK. Stare APK, archiwa kontrolne oraz katalogi wynikowe należy przenosić odwracalnie do sąsiedniego folderu `niepotrzebne`, zachowując ścieżki względne. Folder `niepotrzebne` nie jest wymagany do dalszej pracy i może zostać usunięty przez użytkownika.

Kod źródłowy Pixelate 1.4.2 użyty przy module ostatnich aplikacji został dostarczony osobno i zintegrowany za zgodą autora. Interfejs modułu pozostaje interfejsem Pixel Center.
