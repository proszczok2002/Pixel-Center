# Pixel Center

Pixel Center to bezpłatny zestaw dodatkowych funkcji dla telefonów Google Pixel. Aplikacja łączy narzędzia dźwiękowe i ułatwienia obsługi, których brakuje w standardowym systemie, zachowując wygląd zbliżony do nakładki Pixela.

## Funkcje

- Korektor przeznaczony do audiobooków, uruchamiany tylko dla obsługiwanych odtwarzaczy: Audioteka, Smart AudioBook Player i Lissen.
- Cichy poziom nocny — dokładniejsze, bardzo niskie poziomy głośności na słuchawkach Bluetooth, również przy zablokowanym ekranie. Głośnik telefonu zachowuje fabryczne kroki Androida.
- Sterowanie muzyką przyciskami głośności na zablokowanym ekranie: przytrzymanie głośniej przechodzi do następnego utworu, przytrzymanie ciszej do poprzedniego, a jednoczesne naciśnięcie obu przycisków wstrzymuje lub wznawia odtwarzanie. Krótkie naciśnięcia nadal regulują głośność.
- Dostępny przycisk „Zamknij wszystkie aplikacje” na ekranie ostatnich aplikacji, dopasowany wizualnie do Pixela i przygotowany z myślą o TalkBacku oraz Czytniku ekranu Jieshuo.
- Wbudowany, bezpieczny updater oparty na wydaniach GitHuba. Aplikacja sprawdza identyfikator pakietu, wersję, SHA-256 i podpis APK przed przekazaniem pliku systemowemu instalatorowi Androida.

## Wymagania i prywatność

Pixel Center wymaga Androida 9 lub nowszego (`minSdk 28`) i jest rozwijany dla telefonów Google Pixel. Część funkcji korzysta z usługi ułatwień dostępu, dostępu do powiadomień lub uprawnień wymaganych przez Androida. Aplikacja nie zawiera reklam, analityki ani telemetrii i nie wysyła treści ekranu ani naciśnięć przycisków. Szczegóły znajdują się w [polityce prywatności](PRIVACY.md).

## Aktualizacje

Najnowsze wydanie jest dostępne w sekcji [Releases](https://github.com/proszczok2002/Pixel-Center/releases/latest). Format manifestu i procedurę publikacji opisuje [docs/UPDATES.md](docs/UPDATES.md).

## Kompilacja

Wymagane są JDK 17 oraz Android SDK 37. Po ustawieniu `ANDROID_HOME` lub utworzeniu lokalnego `local.properties` uruchom:

```powershell
.\gradlew.bat clean testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug
```

Gotowy APK znajdzie się w `app/build/outputs/apk/debug/app-debug.apk`. Materiały podpisujące nie są przechowywane w repozytorium. Oficjalne wydania są podpisywane tym samym kluczem co wcześniejsze wersje Pixel Center.

## Wsparcie

- Revolut: https://revolut.me/r_rosiejka
- PayPal: https://paypal.me/proszczok

Wsparcie jest dobrowolne i nie wpływa na dostępność funkcji ani aktualizacji.

## Podziękowania, autorstwo i kod

Szczególne podziękowania dla autora projektu **Pixelate 1.4.2**, którego logika stanowi podstawę modułu „Zamknij wszystkie aplikacje”. Kod został zintegrowany za zgodą autora, a Pixel Center zachowuje własny interfejs tego modułu.

Repozytorium zawiera kod Pixel Center i dokumentację projektu. Brak pliku licencji oznacza, że nie udzielono automatycznie prawa do redystrybucji ani tworzenia własnych wydań poza uprawnieniami wynikającymi z prawa lub odrębnej zgody właścicieli kodu.

---

## English

Pixel Center is a free collection of extra features for Google Pixel phones. It combines audio tools and accessibility improvements missing from stock Android while keeping a visual style close to the Pixel interface.

### Features

- An audiobook equalizer activated only for supported players: Audioteka, Smart AudioBook Player, and Lissen.
- Quiet night volume — finer, very low Bluetooth-headphone volume levels, including while the screen is locked. The phone speaker keeps Android's stock volume steps.
- Lock-screen media controls using the volume buttons: hold volume up for next, hold volume down for previous, and press both buttons to pause or resume. Short presses still adjust volume.
- An accessible, Pixel-styled “Close all apps” button on the recent-apps screen, designed with TalkBack and Jieshuo Screen Reader in mind.
- A secure in-app updater backed by GitHub Releases. Before handing an APK to Android's system installer, the app verifies its package ID, version, SHA-256 hash, and signing certificate.

### Requirements and privacy

Pixel Center requires Android 9 or newer (`minSdk 28`) and is developed for Google Pixel phones. Some features use an accessibility service, notification access, or other Android permissions. The app contains no ads, analytics, or telemetry and does not transmit screen contents or button presses. See the bilingual [privacy policy](PRIVACY.md) for details.

### Updates and building

Download the latest version from [GitHub Releases](https://github.com/proszczok2002/Pixel-Center/releases/latest). The update format and release procedure are documented in [docs/UPDATES.md](docs/UPDATES.md).

Building requires JDK 17 and Android SDK 37. Set `ANDROID_HOME` or create a local `local.properties`, then run the Gradle command shown above. Signing material is deliberately excluded from this repository.

Voluntary support: [Revolut](https://revolut.me/r_rosiejka) or [PayPal](https://paypal.me/proszczok). Support does not affect access to features or updates.

Special thanks to the author of **Pixelate 1.4.2**, whose logic forms the basis of the “Close all apps” module. The code was integrated with the author's permission while retaining Pixel Center's own interface.
