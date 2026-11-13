# Kontynuacja prac nad Pixel Center

Aktualny kod, dokumentacja i historia zmian znajdują się w tym repozytorium. Publiczne wydania są publikowane w `https://github.com/proszczok2002/Pixel-Center/releases`, a aktywny manifest updatera jest w `server/pixel-center-update.json` na gałęzi `main`.

## Bieżące wydanie 1.02

- Kod ma `versionName=1.02` i `versionCode=30`.
- Lokalny instalator: `Pixel Center 1.02.apk`, SHA-256 `A3C34A8F372C45B161961B0135B44BFBC6AEE9A260BD6AE647F7D6994EFB6F22`.
- Zintegrowano działanie Pixelate 1.4.3 bez zmiany grafiki nakładki Pixel Center.
- Pełna kompilacja, 4 testy jednostkowe, APK AndroidTest i Lint przeszły 2026-11-13.
- Wydanie publiczne: `https://github.com/proszczok2002/Pixel-Center/releases/tag/v1.02`; aktywny manifest wskazuje 1.02/code 30.
- Publiczny APK został niezależnie pobrany i sprawdzony pod kątem rozmiaru, SHA-256, pakietu, wersji i certyfikatu.
- Telefon nie był podłączony, więc fizyczny test pojawienia się przycisku, czyszczenia długiej listy oraz braku nakładki na ekranie głównym nadal pozostaje do wykonania.

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
- Jedynym publicznym kanałem aktualizacji jest GitHub. Nie publikuj manifestu ani APK Pixel Center na starym serwerze `proszczok.pl`.

## Porządek lokalny

W aktywnym projekcie zostają źródła, dokumentacja, konfiguracja, narzędzia, manifest updatera i tylko najnowszy lokalny APK. Stare APK, archiwa kontrolne oraz katalogi wynikowe należy przenosić odwracalnie do sąsiedniego folderu `niepotrzebne`, zachowując ścieżki względne. Folder `niepotrzebne` nie jest wymagany do dalszej pracy i może zostać usunięty przez użytkownika.

Kod źródłowy [Pixelate 1.4.3](https://github.com/Pates2004/Pixelate/releases/tag/v1.4.3), commit `76344f11a72fa060c0b37dedb7bc229985d2164a`, autorstwa [@Pates2004](https://github.com/Pates2004), użyty przy module ostatnich aplikacji, został zintegrowany za zgodą autora. Interfejs modułu pozostaje interfejsem Pixel Center; przy kolejnych integracjach nie zastępuj `OverlayController` wersją autora, tylko przenoś zmiany działania i dostępności bez zmiany grafiki.
