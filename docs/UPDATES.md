# Aktualizacje przez GitHub / GitHub updates

## Polski

Pixel Center odczytuje publiczny manifest z gałęzi `main`:

`https://raw.githubusercontent.com/proszczok2002/Pixel-Center/main/server/pixel-center-update.json`

Bieżący manifest wskazuje Pixel Center 1.02 (`versionCode 30`) i plik `Pixel-Center-1.02-20261113.apk` z wydania [v1.02](https://github.com/proszczok2002/Pixel-Center/releases/tag/v1.02).

Manifest zawiera `appId`, `versionCode`, `versionName`, `apkUrl`, opis zmian, datę publikacji i SHA-256. Klient akceptuje tylko HTTPS, właściwy identyfikator aplikacji, 64-znakowy skrót SHA-256 oraz APK z `github.com/proszczok2002/Pixel-Center/releases/download/`. Parametr czasowy i nagłówki bez cache zapobiegają odczytaniu starego manifestu tuż po publikacji.

Pobrany plik jest sprawdzany przed instalacją: skrót SHA-256, nazwa pakietu, kod wersji oraz certyfikat podpisujący muszą być zgodne. Instalację zawsze zatwierdza użytkownik w systemowym instalatorze Androida.

Bezpieczna kolejność wydania:

1. Zbuduj i przetestuj APK, oblicz SHA-256.
2. Utwórz tag i wydanie GitHuba oraz prześlij APK.
3. Niezależnie pobierz publiczny plik i potwierdź jego wersję, podpis i skrót.
4. Dopiero na końcu zaktualizuj manifest na `main`.

Jedynym publicznym kanałem aktualizacji jest GitHub. Poprzedni manifest i APK na `proszczok.pl` zostały wycofane i nie powinny być ponownie publikowane. Wersję sprzed migracji trzeba najpierw zaktualizować ręcznie z GitHub Releases.

## English

Pixel Center reads a public manifest from the `main` branch at the URL shown above. The manifest contains the app ID, version code and name, APK URL, changelog, publication date, and SHA-256. The client accepts only HTTPS, the expected app ID, a valid 64-character SHA-256 value, and APKs under this repository's GitHub Releases download path. A timestamp query and no-cache headers prevent a stale manifest immediately after a release.

The current manifest points to Pixel Center 1.02 (`versionCode 30`) and `Pixel-Center-1.02-20261113.apk` from release [v1.02](https://github.com/proszczok2002/Pixel-Center/releases/tag/v1.02).

Before installation, the downloaded file's SHA-256, package name, version code, and signing certificate must match. Installation still requires explicit confirmation in Android's system package installer.

Publish the release asset first, verify the public download independently, and update the `main` manifest last. GitHub is the only public update channel. The legacy manifest and APK on `proszczok.pl` have been retired and must not be republished; pre-migration installations require one manual update from GitHub Releases.
