# Kontynuacja prac nad Pixel Center

Aktualny kod, dokumentacja i historia zmian znajdują się w tym repozytorium. Publiczne wydania są publikowane w `https://github.com/proszczok2002/Pixel-Center/releases`, a aktywny manifest updatera jest w `server/pixel-center-update.json` na gałęzi `main`.

## Bieżące wydanie 1.03

- Kod: `versionName=1.03`, `versionCode=31`.
- Lokalny instalator: `Pixel Center 1.03.apk`, SHA-256 `5BDAEDC4CD0228221D6AC770C879DC689176F57B7E3DEE25FFC133CF8FA19C17`, 3 016 562 bajty, dotychczasowy podpis.
- Dolne zakładki, duża czcionka, poziom, PL/EN/DE/RU/ES/FR/IT; logika Pixelate 1.4.3 pozostaje zintegrowana.
- Build, 5 testów jednostkowych, APK AndroidTest i Lint (0 błędów, 18 ostrzeżeń) przeszły 2026-11-16.
- Testy na Pixelu 11 z TalkBackiem oraz zakres nieprzetestowany opisuje `docs/ACCESSIBILITY-1.03.md`. Końcowy APK na telefonie ma identyczny hash z wydaniem. Należy jeszcze wykonać odsłuch i ręczny odbiór gestów.
- Docelowe wydanie: `https://github.com/proszczok2002/Pixel-Center/releases/tag/v1.03`; stan publikacji i weryfikacji jest w najnowszym wpisie `STAN_PRAC.md`.

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
