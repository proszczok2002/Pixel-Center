# Historia zmian / Changelog

## 1.02 — 2026-11-13

### Polski

- Zintegrowano zmiany działania modułu Ostatnich z Pixelate 1.4.3 autora [@Pates2004](https://github.com/Pates2004/Pixelate/releases/tag/v1.4.3).
- Dodano ograniczone do 2,5 sekundy odzyskiwanie nakładki, gdy zdarzenie okna pojawi się przed gotowym drzewem dostępności.
- Starszy Pixel Launcher może zostać rozpoznany po widocznym kontenerze Ostatnich, a Android 9/10 otrzymał dłuższy odstęp przewijania starej karuzeli.
- Stan usługi odświeża się po połączeniu, a opisy dostępności odpowiadają stanowi przycisku.
- Zachowano własny wygląd Pixel Center: kolory, przezroczyste tło, ikonę, typografię, rozmiar i położenie przycisku nie zostały zmienione.

### English

- Integrated Recents behavior changes from [@Pates2004](https://github.com/Pates2004/Pixelate/releases/tag/v1.4.3)'s Pixelate 1.4.3.
- Added recovery retries limited to 2.5 seconds when a window event arrives before its accessibility tree is ready.
- Older Pixel Launcher versions can be recognized by an explicit visible Recents container, while Android 9/10 receives a longer delay for legacy carousel scrolling.
- The service state refreshes after connection and accessibility labels now match the button state.
- Preserved Pixel Center's own appearance: colors, transparent background, icon, typography, size, and placement are unchanged.

## 1.01 — 2026-10-31

### Polski

- Wydanie kontrolne potwierdzające działanie migracji updatera z poprzedniego serwera do GitHub Releases.
- Bez zmian w działaniu czterech głównych modułów Pixel Center względem 1.0.

### English

- Verification release for the updater migration from the previous server to GitHub Releases.
- No functional changes to Pixel Center's four main modules compared with 1.0.

## 1.0 — 2026-10-31

### Polski

- Pierwsze stabilne publiczne wydanie Pixel Center na GitHubie.
- Korektor audiobooków, cichy poziom nocny Bluetooth, sterowanie multimediami na zablokowanym ekranie i dostępne „Zamknij wszystkie aplikacje”.
- Updater przeniesiony z własnego serwera do GitHub Releases z weryfikacją integralności, pakietu, wersji i podpisu.
- Dodano dobrowolne wsparcie przez Revolut i PayPal w zakładce informacji i aktualizacji.

### English

- First stable public Pixel Center release on GitHub.
- Audiobook equalizer, quiet Bluetooth night volume, lock-screen media controls, and an accessible “Close all apps” action.
- Updater migrated from the previous server to GitHub Releases with integrity, package, version, and signing-certificate verification.
- Added voluntary Revolut and PayPal support links to the information and updates tab.
