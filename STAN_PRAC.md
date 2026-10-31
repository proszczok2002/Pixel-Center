# Stan prac — Pixel Center

## Aktualny stan — migracja na GitHub, 2026-10-31

- Publiczne repozytorium: `https://github.com/proszczok2002/Pixel-Center`.
- Wydano pierwszą stabilną wersję GitHub 1.0 (`versionCode=28`) i przygotowano aktualizację kontrolną 1.01 (`versionCode=29`).
- Updater odczytuje manifest z `raw.githubusercontent.com` i przyjmuje wyłącznie APK z wydań `proszczok2002/Pixel-Center`.
- Zachowano sprawdzanie SHA-256, pakietu, kodu wersji i certyfikatu oraz instalację zatwierdzaną przez system Android.
- Dodano w aplikacji dobrowolne wsparcie przez Revolut i PayPal.
- Pełny build `clean testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug` zakończył się powodzeniem osobno dla 1.0 i 1.01, a po przeniesieniu lokalnej dystrybucji Gradle został ponownie wykonany z sukcesem przez dołączony `gradlew.bat`.
- APK 1.0: 2 719 126 bajtów; SHA-256 `247D9D048364A52B9338AA027F30DA3F9BD4CDB2605B799686900426C21D47EE`.
- APK 1.01: 2 719 130 bajtów; SHA-256 `022F4F99256169F992E20B91AF831B5BA951975941B8EB20B8B3047D8170CE1B`.
- Pakiet: `pl.proszczok.pixelspeecheq`; podpis jest identyczny z 1.20: certyfikat SHA-256 `C033826456D8A38F662306C5D3253EF0A7B8B4D19582DD50408D0861CD4CC41E`.
- Telefon nie był podłączony i zgodnie z decyzją użytkownika nie był potrzebny do tej migracji. Test urządzeniowy pozostaje zakresem wcześniejszych wydań.
- Wersja 1.01 służy do praktycznego sprawdzenia ścieżki aktualizacji 1.0 → 1.01 przez GitHub Releases.
- Jedynym publicznym kanałem aktualizacji jest GitHub. Manifest i APK Pixel Center zostały usunięte ze starego serwera `proszczok.pl`; znajdujące się niżej stare adresy są wyłącznie zapisem historycznym i nie są już aktywnym kanałem.
- W aktywnym projekcie pozostawiono źródła, Git, Gradle Wrapper, dokumentację, narzędzia, manifest GitHub i tylko najnowszy lokalny `Pixel Center 1.01.apk`. Starsze APK, checkpoint, ZIP źródłowy Pixelate i wyniki kompilacji przeniesiono do `F:\pixel center\niepotrzebne`, który nie jest wymagany do dalszej pracy i może zostać usunięty.
- Autor modułu źródłowego Pixelate został oznaczony jako [@Pates2004](https://github.com/Pates2004), z bezpośrednim linkiem do [repozytorium Pixelate](https://github.com/Pates2004/Pixelate).

Poniżej zachowano historyczny dziennik poprzednich wydań.

---

Data punktu kontrolnego: 2026-10-07

## Porządkowanie katalogu projektu — 2026-10-07

- Wszystkie stare, zduplikowane i wygenerowane elementy przeniesiono odwracalnie do `F:\pixel center\hasiok`; niczego nie usunięto.
- Hasiok zachowuje pierwotną hierarchię katalogów i zawiera 1293 pliki o łącznym rozmiarze 462 897 693 bajtów.
- Aktywny projekt zawiera 43 potrzebne pliki: źródła, konfigurację, dokumentację, narzędzie testowe, bieżące APK i manifest oraz jeden checkpoint.
- W aktywnym katalogu pozostawiono kompletne `app/src`, konfigurację Gradle, `local.properties`, Android SDK, rozpakowany Gradle 9.4.1, narzędzie testowe, dokumentację, najnowszy APK 1.20, bieżący katalog `server` oraz jeden końcowy checkpoint 1.20.
- W katalogu głównym pozostawiono `najnowszykod.zip` jako źródło kodu autora Pixelate 1.4.2 użytego w bieżącej wersji.
- Stare APK, zrzuty ekranu, drzewa UI, wcześniejsze checkpointy, przekazywane kopie źródeł, katalogi Pixelate, analiza Volumee oraz duplikaty i archiwa instalacyjne narzędzi znajdują się w hasioku.
- Po przeniesieniu wykonano od zera `clean testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug` przy użyciu pozostawionego `gradle-dist/gradle-9.4.1` i `android-sdk`: build zakończył się powodzeniem.
- APK utworzony po sprzątaniu miał `versionName=1.20`, `versionCode=27` i SHA-256 identyczny z zachowanym wydaniem: `8FB2770BC09CA819FA1B257EDC105077070407E8DBD8871D066441FE6345CFCE`.
- Katalogi `.gradle`, `build` i `app/build` wygenerowane podczas kontroli ponownie przeniesiono do `hasiok/weryfikacja-po-sprzataniu-20261007`.
- Instrukcja dalszej pracy i publikacji znajduje się w `KONTYNUACJA.md`.

---

## Najnowsze wydanie — 1.20

- Wersja: 1.20
- versionCode: 27
- Pakiet: `pl.proszczok.pixelspeecheq`
- Kod źródłowy autora: `F:\pixel center\najnowszykod.zip`, Pixelate 1.4.2.
- Punkt przywracania sprzed integracji przeniesiony do archiwum: `F:\pixel center\hasiok\Pixel Center\backup\Pixel-Center-1.19-before-Pixelate-1.4.2-integration-20260914.zip`.
- Końcowy punkt kontrolny 1.20: `backup/Pixel-Center-1.20-checkpoint-20260914.zip`.
- Lokalny APK: `Pixel Center 1.20.apk`
- APK dystrybucyjny: `server/Pixel-Center-1.20-20260914.apk`
- Rozmiar APK: 2 716 750 bajtów.
- SHA-256 APK: `8FB2770BC09CA819FA1B257EDC105077070407E8DBD8871D066441FE6345CFCE`
- Build `clean testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug`: zakończony powodzeniem.
- Wersja 1.20 jest zainstalowana na Pixelu 10 z Androidem 17 i opublikowana na serwerze aktualizacji.

### Integracja logiki Pixelate 1.4.2

- Do `ClearAllController` i `OverviewDetector` przeniesiono najnowsze działanie autora dotyczące Ostatnich aplikacji.
- Przed przewijaniem wykonywana jest jedna próba `ACTION_SHOW_ON_SCREEN` na niewidocznym systemowym węźle `clear_all`.
- Po przyjęciu akcji aplikacja czeka 180 ms, ponownie pobiera świeże drzewo dostępności i dopiero wtedy naciska widoczny systemowy przycisk. Niewidoczny węzeł nigdy nie jest klikany.
- Warunki opuszczenia Ostatnich i limit czasu są sprawdzane przed wykonaniem akcji na ekranie.
- Jeśli launcher odrzuci akcję, tylko pozornie ją przyjmie albo jej nie udostępni, pozostaje zachowanie awaryjne autora: przewijanie semantyczne, a następnie gest.
- `OverlayController` nie został zmieniony. Zachowano nasz płaski wygląd zgodny z nakładką Pixela, ikonę siatki, pełną etykietę `Zamknij wszystkie aplikacje`, rozmiar i położenie przycisku.

### Testy na Pixelu 10

- Pełny build, APK testowe i Android Lint zakończyły się powodzeniem; brak błędów kompilacji i lint.
- Wykonano trzy pełne cykle z wieloma kartami aplikacji przy aktywnym Jieshuo. Każdy cykl użył `ACTION_SHOW_ON_SCREEN`, znalazł systemowy przycisk po 0 krokach przewijania i wyczyścił Ostatnie.
- Czasy od uruchomienia operacji do kliknięcia systemowego przycisku wyniosły około 391 ms, 326 ms i 390 ms.
- Po każdej próbie nakładka znikała, a w systemowej liście pozostawał wyłącznie ekran główny.
- `Crashed services: {}`; Pixel Center, Jieshuo i AutoGłośnik pozostały związane.
- Zachowane ustawienia użytkownika: tryb nocnej głośności wyłączony, zapamiętane 28%/7, sterowanie multimediami przy wygaszonym ekranie włączone, limit wyszukiwania 20 s.
- Pakiet AndroidTest usunięto po testach, a telefon pozostawiono na ekranie głównym.

### Publikacja 1.20

- Publiczny APK: https://proszczok.pl/YT%20Downloader%20by%20Proszczok/Pixel-Center-1.20-20260914.apk
- Publiczny manifest: https://proszczok.pl/YT%20Downloader%20by%20Proszczok/pixel-center-update.json
- APK pobrany przez publiczny HTTPS ma 2 716 750 bajtów i zgodny SHA-256 `8FB2770BC09CA819FA1B257EDC105077070407E8DBD8871D066441FE6345CFCE`.
- Manifest publiczny odpowiada kodem HTTP 200 i wskazuje `versionName=1.20`, `versionCode=27`, datę `2026-09-14` oraz zgodny SHA-256.
- Zastąpiony APK 1.19 usunięto z lokalnego katalogu `server` i katalogu publicznego; jego publiczny adres odpowiada kodem 404.
- W obu katalogach dystrybucyjnych pozostały wyłącznie `Pixel-Center-1.20-20260914.apk` oraz `pixel-center-update.json`.
- Stała zasada publikacji pozostaje bez zmian: na serwerze ma być zawsze tylko najnowszy APK i manifest aktualizacji.

---

## Poprzednie wydanie — 1.19

- Wersja: 1.19
- versionCode: 26
- Pakiet: `pl.proszczok.pixelspeecheq`
- Lokalny APK: `Pixel Center 1.19.apk`
- APK dystrybucyjny: `server/Pixel-Center-1.19-20260906.apk`
- SHA-256 APK: `3E5EAF06901EA94ACBB74F22DBE78B9C442C934F0399390789A151A9DF93225D`
- Build `clean assembleDebug assembleDebugAndroidTest lintDebug`: zakończony powodzeniem.
- Wersja 1.19 jest zainstalowana na Pixelu 10 z Androidem 17 i opublikowana na serwerze aktualizacji.

### Pasek głośności w cichym trybie nocnym

- Zmiana poziomu wykonywana przez silnik nocny przekazuje teraz `AudioManager.FLAG_SHOW_UI`.
- Panel Androida pojawia się również przy przejściu pomiędzy 4% i 2%, gdy poziom fizyczny pozostaje ten sam, a zmienia się tylko tłumienie cyfrowe.
- Sonda na urządzeniu pokazała systemowy panel głośności i po teście przywróciła poziom 13/25 oraz wcześniejsze dane 28%/7.

### Zamknij wszystkie aplikacje

- Semantyczne przewijanie Launchera nie kończy się już po 20 próbach, więc długa lista kart nie przechodzi przedwcześnie na gest awaryjny.
- Gest awaryjny został odwrócony na lewo-prawo, zgodnie z położeniem starszych kart i systemowego przycisku na Pixel Launcherze.
- Test na telefonie znalazł i nacisnął systemowy przycisk po 10 krokach, bez timeoutu i bez awarii usługi.
- Przycisk nakładki jest płaski, neutralny, z ikoną siatki i pełną jednowierszową etykietą; wygląd sprawdzono obok systemowych akcji „Zrzut ekranu” i „Wybierz”.

### Pauza oboma przyciskami w Pixel AudioPlayerze

- Odtworzono błąd: sesja Media3 Pixel AudioPlayera przyjmowała `TransportControls.pause()`, ale pozostawała w stanie `PLAYING`.
- Dla pauzy/wznowienia Pixel Center wysyła teraz jawny klawisz `MEDIA_PAUSE` albo `MEDIA_PLAY` do wybranej sesji, a dotychczasowe transport controls pozostają fallbackiem.
- Test przy wygaszonym ekranie potwierdził `PLAYING → PAUSED → PLAYING`; oba polecenia trafiły do `pl.proszczok.player`, a ekran pozostał w stanie `Dozing`.
- Po testach Pixel AudioPlayer pozostawiono na pauzie przy pozycji 11,608 s (34 ms od pozycji 11,574 s sprzed testu).

### Publikacja 1.19

- Publiczny APK: https://proszczok.pl/YT%20Downloader%20by%20Proszczok/Pixel-Center-1.19-20260906.apk
- Publiczny manifest: https://proszczok.pl/YT%20Downloader%20by%20Proszczok/pixel-center-update.json
- APK pobrany przez publiczny HTTPS ma 2 878 063 bajty i zgodny SHA-256 `3E5EAF06901EA94ACBB74F22DBE78B9C442C934F0399390789A151A9DF93225D`.
- Manifest publiczny odpowiada kodem HTTP 200 i wskazuje `versionName=1.19`, `versionCode=26` oraz zgodny SHA-256.
- Stała zasada publikacji: w lokalnym katalogu `server` i katalogu publicznym zawsze pozostają tylko najnowszy APK oraz manifest aktualizacji. Po wydaniu 1.19 plik APK 1.18 usunięto z obu miejsc.
- Końcowa kontrola publikacji: publiczny APK 1.19 odpowiada kodem HTTP 200, usunięty APK 1.18 kodem 404, a oba katalogi dystrybucyjne zawierają wyłącznie `Pixel-Center-1.19-20260906.apk` i `pixel-center-update.json`.

---

## Poprzednie wydanie — 1.18

- Wersja: 1.18
- versionCode: 25
- Pakiet: `pl.proszczok.pixelspeecheq`
- Lokalny APK: `Pixel Center 1.18.apk`
- APK dystrybucyjny: `server/Pixel-Center-1.18-20260906.apk`
- SHA-256 APK: `FD174BE119F8BF461436B1F14F9EFB266D2749EBB6450658BE6394D9254FE151`
- Czysty build `clean assembleDebug assembleDebugAndroidTest lintDebug`: zakończony powodzeniem.

### Naprawa przycisków na zablokowanym ekranie

- Błąd odtworzono z YouTube Music: po wznowieniu odtwarzania przy wygaszonym ekranie odtwarzacz przejmował priorytet klawiszy i oba długie naciśnięcia przestawały trafiać do Pixel Center.
- Pixel Center obserwuje teraz zmianę aktywnych sesji i zmianę stanu odtwarzania. Po wznowieniu lub zmianie odtwarzacza odzyskuje klawisze bez odblokowania telefonu.
- Odzyskanie priorytetu nie wymienia już MediaSession ani VolumeProvider. Dzięki temu puszczenie klawisza zawsze kończy ten sam gest.
- Przy zatrzymanym odtwarzaniu wybierana jest sesja, do której Android kieruje klawisze multimedialne. Usunięto wpływ starej zapamiętanej aplikacji.
- Automatyczny test YouTube Music: 3 pełne cykle, 12/12 komend (następny, poprzedni, pauza i wznowienie), wszystkie trafiły do YouTube Music.
- Przez cały test ekran pozostał w stanie `Dozing`, a token sesji Pixel Center nie zmienił się.
- Krótkie naciśnięcia na głośniku nadal używają fabrycznego kroku Androida; sprawdzono `1/25 → 2/25 → 1/25`.
- Użytkownik potwierdził działanie finalnej 1.18 na telefonie przed publikacją.

### Nazwa przycisku Ostatnich

- Widoczna i odczytywana nazwa to dokładnie `Zamknij wszystkie aplikacje`.
- Usunięto dodatkowy opis `Zamknij wszystkie ostatnie aplikacje` i dodatkowy tytuł panelu przypisany do przycisku.

### Publikacja 1.18

- Publiczny APK: https://proszczok.pl/YT%20Downloader%20by%20Proszczok/Pixel-Center-1.18-20260906.apk
- Publiczny manifest: https://proszczok.pl/YT%20Downloader%20by%20Proszczok/pixel-center-update.json
- APK pobrany przez publiczny HTTPS ma zgodny SHA-256 `FD174BE119F8BF461436B1F14F9EFB266D2749EBB6450658BE6394D9254FE151`.
- Manifest publiczny wskazuje `versionName=1.18`, `versionCode=25` i zgodny SHA-256.
- W lokalnym katalogu `server` i katalogu publicznym pozostawiono APK 1.18; zastąpiony APK 1.17 usunięto.

---

## Poprzednie wydanie — 1.17

- Wersja: 1.17
- versionCode: 24
- Pakiet: `pl.proszczok.pixelspeecheq`
- Lokalny APK: `Pixel Center 1.17.apk`
- APK dystrybucyjny: `server/Pixel-Center-1.17-20260906.apk`
- SHA-256 APK: `B2420FB9A8C4F568B389846C47007759D6213009A0811D0289929C197E2C08B4`
- Build `clean assembleDebug assembleDebugAndroidTest lintDebug`: zakończony powodzeniem.

### Cichy poziom tylko na słuchawkach Bluetooth

- Nowy `AudioOutputRouting` sprawdza rzeczywistą trasę multimediów przez `AudioManager.getAudioDevicesForAttributes` na Androidzie 13 i nowszym.
- Cichy poziom nocny włącza się tylko dla wyjść Bluetooth A2DP, Bluetooth SCO, BLE Headset i aparatów słuchowych Bluetooth.
- Na wbudowanym głośniku Pixel Center nie przechwytuje głośności przy odblokowanym ekranie. Android reguluje ją fabrycznie.
- Przy zablokowanym ekranie i włączonym sterowaniu multimediami krótkie naciśnięcie na głośniku używa `AudioManager.adjustStreamVolume`, czyli zwykłego kroku Androida.
- Test na głośniku przy blokadzie potwierdził dokładnie jeden krok `7/25 → 8/25`, a końcowa próba `8/25 → 7/25`.
- Sesja Pixel Center na głośniku ma zakres systemowy `max=25`, a nie wirtualne `max=100`.
- Aplikacja nasłuchuje zdarzeniowo zmian urządzeń i trasy odtwarzania. Po odłączeniu Bluetooth cyfrowe tłumienie jest wyłączane bez pętli i odpytywania.
- Podczas finalnych testów słuchawki były odłączone (`Connected devices: 0`), więc potwierdzono stronę głośnika i wybór trasy; wcześniejszy silnik poziomu nocnego pozostaje niezmieniony po wykryciu aktywnego Bluetooth.
- Opisy ustawienia i powiadomienia jasno mówią, że tryb nocny jest przeznaczony dla słuchawek Bluetooth.

### Naprawa kolejnych gestów na blokadzie

- Błąd odtworzono na 1.16: z czterech długich naciśnięć jedno nie wysłało komendy.
- Przyczyna: Pixel Center wymieniał swoją MediaSession około 120 ms po komendzie, czasami jeszcze przed odebraniem puszczenia przycisku (`ADJUST_SAME`). Następny gest pozostawał oznaczony jako już obsłużony.
- W 1.17 odzyskiwanie priorytetu sesji zaczyna się dopiero po puszczeniu klawisza. Jest też ograniczone zabezpieczenie czasowe na wypadek braku zdarzenia puszczenia.
- Ta sama synchronizacja jest wykonywana po geście odebranym przez usługę dostępności oraz po geście odebranym przez VolumeProvider.
- Test po poprawce: seria 8/8 gestów, 8/8 komend do Audioteki i 8/8 odzyskań priorytetu.
- Finalny czysty build: seria 6/6 gestów, 6/6 komend i 6/6 odzyskań priorytetu.
- Pozycję Audioteki zmienioną testem przywrócono dokładnie do `464308 ms`; odtwarzanie pozostało wstrzymane.

### Stan telefonu i publikacja

- Na Pixelu 10 zainstalowano finalne 1.17/code 24.
- Jieshuo, AutoGłośnik i Pixel Center są włączone; `Crashed services: {}`.
- Pakiet AndroidTest usunięto. Ekran pozostawiono w `Dozing`.
- Głośnik pozostawiono na wcześniejszym poziomie 7/25. Aktywna sesja blokady ma `RELATIVE max=25 current=7`.
- Publiczny APK: https://proszczok.pl/YT%20Downloader%20by%20Proszczok/Pixel-Center-1.17-20260906.apk
- Publiczny manifest: https://proszczok.pl/YT%20Downloader%20by%20Proszczok/pixel-center-update.json
- APK pobrany przez publiczny HTTPS ma zgodny SHA-256 `B2420FB9A8C4F568B389846C47007759D6213009A0811D0289929C197E2C08B4`.
- Manifest publiczny wskazuje `versionName=1.17`, `versionCode=24` oraz zgodny SHA-256.
- Na Sambie i w lokalnym katalogu `server` pozostawiono tylko APK 1.17 i manifest; zastąpiony APK 1.16 usunięto.

---

## Poprzednie wydanie — 1.16

- Wersja: 1.16
- versionCode: 23
- Pakiet: `pl.proszczok.pixelspeecheq`
- Lokalny APK: `Pixel Center 1.16.apk`
- APK dystrybucyjny: `server/Pixel-Center-1.16-20260905.apk`
- SHA-256 APK: `037B389B2D4E18415FB93F16850D860AA90E1437225EF061599F2F0DC819FA48`
- Build `clean assembleDebug assembleDebugAndroidTest lintDebug`: zakończony powodzeniem.

### Naprawa podwójnego naciśnięcia

- Log z rzeczywistego użycia wykazał, że jeden klawisz bywał obsługiwany najpierw przez usługę dostępności Pixel Center, a około pół sekundy później drugi raz przez system lub aktywny odtwarzacz.
- Przy zejściu `4% → 2%` opóźnione zdarzenie fizyczne `1/25 → 0/25` było błędnie liczone jako drugie naciśnięcie i kończyło na 0%.
- Przy wejściu `2% → 4%` opóźnione `1/25 → 2/25` powodowało błędny skok do 8%.
- Pixel Center zapamiętuje teraz przez 1200 ms wyłącznie dokładnie pasujące echo po przejściu pomiędzy 4% i poziomem nocnym. Echo jest cofane do oczekiwanego poziomu i nie wywołuje drugiego komunikatu czytnika.
- Każde następne prawdziwe zdarzenie z usługi dostępności zastępuje albo kasuje oczekiwane echo, dlatego szybkie kolejne kliknięcie nadal działa.
- Obsługa pozostaje zdarzeniowa: brak pętli, alarmu i odpytywania, więc poprawka nie zwiększa zużycia baterii.

### Dokumentacja i testy

- Sprawdzono oficjalną dokumentację Androida dla `AccessibilityService.onKeyEvent`, `FLAG_REQUEST_FILTER_KEY_EVENTS`, `MediaSession.setPlaybackToRemote` oraz względnego `VolumeProvider.onAdjustVolume`.
- Test regresji odtworzył opóźnione echo po 550 ms. `4% → 2%` pozostało na 2% mimo systemowego `1 → 0`, a `2% → 4%` pozostało na 4% mimo systemowego `1 → 2`.
- Dwa szybkie kliknięcia co 450 ms zostały policzone oddzielnie: `4% → 2% → 0%` i `0% → 2% → 4%`.
- Test na zablokowanym ekranie (`Dozing`) potwierdził `4% → 2%`; fizycznie pozostało 1/25, a tłumienie wyniosło `-6.0206 dB`.
- Finalnie na Pixelu 10 jest 1.16/code 23. Poziom pozostawiono na 2%, ekran w `Dozing`, pakiet AndroidTest usunięto.
- Jieshuo, AutoGłośnik i Pixel Center są włączone; `Crashed services: {}`.

### Publikacja 1.16

- Publiczny APK: https://proszczok.pl/YT%20Downloader%20by%20Proszczok/Pixel-Center-1.16-20260905.apk
- Publiczny manifest: https://proszczok.pl/YT%20Downloader%20by%20Proszczok/pixel-center-update.json
- APK pobrany przez publiczny HTTPS ma SHA-256 `037B389B2D4E18415FB93F16850D860AA90E1437225EF061599F2F0DC819FA48`.
- Manifest publiczny wskazuje `versionName=1.16`, `versionCode=23` oraz zgodny SHA-256.
- Na Sambie i w lokalnym katalogu `server` pozostawiono tylko APK 1.16 oraz manifest; zastąpiony APK 1.15 usunięto.
- Changelog w updaterze jest krótki i opisuje problem prostymi słowami.

---

## Poprzednie wydanie — 1.15

- Wersja: 1.15
- versionCode: 22
- Pakiet: `pl.proszczok.pixelspeecheq`
- Lokalny APK: `Pixel Center 1.15.apk`
- APK dystrybucyjny: `server/Pixel-Center-1.15-20260905.apk`
- SHA-256 APK: `BD0322A8EB3824A4A1ED362048C93D7FEC1B7DAF21919A0E9BF2B85D52E8E760`
- Build `clean assembleDebug assembleDebugAndroidTest lintDebug`: zakończony powodzeniem.

### Naprawa cichego trybu nocnego

- Odtworzono błąd z normalnego użycia: gdy aktywny odtwarzacz znajdował się przed MediaSession Pixel Center, sprzętowy przycisk omijał aplikację i wykonywał `4% → 0%`.
- Testy syntetyczne 1.14 tworzyły świeżą MediaSession na pierwszym miejscu, dlatego wcześniej nie wykryły tego przypadku.
- 1.15 obserwuje systemowe zdarzenie faktycznej zmiany STREAM_MUSIC i koryguje zmianę niezależnie od tego, który odtwarzacz ma priorytet klawiszy.
- Gdy odtwarzacz wykona `1/25 → 0/25`, Pixel Center zamienia pierwsze zejście na poziom nocny: fizyczne 1/25, wirtualne 2%, tłumienie `-6.0206 dB`.
- Drugie naciśnięcie w dół dopiero ustawia 0%. W górę działa `0% → 2% nocne → 4%`.
- Wejście na poziom nocny wysyła zdarzenie `Cichy poziom nocny` przez związaną usługę dostępności Pixel Center.
- Usługa dostępności nadal może obsłużyć klawisz bezpośrednio, a obserwator zmiany głośności stanowi niezależne zabezpieczenie dla CSR, TalkBacka i odtwarzaczy przejmujących przyciski.

### Test odpowiadający zgłoszeniu użytkownika

- Audioteka została uruchomiona i jej `PlaybackService` znalazł się przed `PixelCenterScreenOffVolume`.
- Po naciśnięciu Głośność − nie było callbacku VolumeProvider; Android naprawdę spróbował zmienić fizyczny poziom `1 → 0`.
- Ostateczny build 1.15 wykrył tę zewnętrzną zmianę, przywrócił 1/25, zapisał 2% i włączył tłumienie `-6.0206 dB`.
- Log potwierdził przekazanie komunikatu `Cichy poziom nocny` do usługi dostępności.
- Sprawdzono cały ciąg przy przejętych klawiszach: `4% → 2% nocne → 0% → 2% nocne → 4%`.
- Audioteka po teście została ponownie wstrzymana.
- Na telefonie pozostawiono zainstalowane 1.15/code 22, włączony tryb nocny i poziom 2% (fizycznie 1/25).
- Końcowa MediaSession: `RELATIVE max=100 current=2`; usługa działa jako foreground.
- Jieshuo, AutoGłośnik i Pixel Center są związane; `Crashed services: {}`.
- Pakiet AndroidTest nie jest zainstalowany, a ekran pozostawiono w stanie `Dozing`.

### Bateria

- Nie ma pętli odzyskującej priorytet MediaSession.
- Naprawa reaguje tylko na systemowe zdarzenie zmiany głośności.
- Powtarzane przez Audiotekę co pół sekundy zdarzenia tego samego stanu odtwarzania są ignorowane przez monitor EQ.
- Cyfrowe tłumienie pozostaje aktywne tylko na jednym poziomie nocnym.

### Publikacja 1.15

- Publiczny APK: https://proszczok.pl/YT%20Downloader%20by%20Proszczok/Pixel-Center-1.15-20260905.apk
- Publiczny manifest: https://proszczok.pl/YT%20Downloader%20by%20Proszczok/pixel-center-update.json
- Publicznie pobrany APK ma SHA-256 `BD0322A8EB3824A4A1ED362048C93D7FEC1B7DAF21919A0E9BF2B85D52E8E760`.
- Manifest publiczny wskazuje `versionName=1.15`, `versionCode=22` i zgodny SHA-256.
- Na Sambie i w lokalnym katalogu `server` pozostawiono tylko APK 1.15 i manifest; zastąpiony APK 1.14 usunięto.

---

## Poprzednie wydanie — 1.14 (historia)

- Wersja: 1.14
- versionCode: 21
- Pakiet: `pl.proszczok.pixelspeecheq`
- Lokalny APK: `Pixel Center 1.14.apk`
- APK dystrybucyjny: `server/Pixel-Center-1.14-20260905.apk`
- SHA-256 APK: `C71A820EC2471B4319DD363B1AC700CC14B03BF230F0BE21F65EBEC7BA2CB251`
- Build `clean assembleDebug assembleDebugAndroidTest` i `lintDebug`: zakończone powodzeniem.

### Cichy tryb nocny

- Usunięto pole do wpisywania procentów oraz przycisk zapisywania kroku.
- Jest jeden przełącznik `Dodatkowy cichy poziom nocny` na osobnej karcie `Cichy tryb nocny`.
- Przy włączonej funkcji kolejność wynosi: `0 → cichy poziom nocny → pierwszy poziom Androida → dalsze zwykłe poziomy Androida`.
- Na Pixelu 10 pierwszy poziom Androida to 4%, a dodatkowy poziom nocny to 2%.
- Poziom nocny utrzymuje fizyczne 1/25 i dodaje cyfrowe tłumienie `-6.0206 dB`; przy 0% efekt jest wyłączony.
- Powyżej 4% aplikacja zmienia głośność zwykłymi stopniami 4%, 8%, 12% itd.
- Po wyłączeniu przełącznika stare ustawienie procentowe jest ignorowane i obowiązuje zwykły krok systemowy.
- Tłumienie jest na globalnym miksie multimediów, więc nie jest przypisane do konkretnego wyjścia i działa zarówno dla głośnika, jak i słuchawek.
- Funkcja działa na ekranie odblokowanym i zablokowanym.
- Na dodatkowym poziomie aplikacja wysyła czytnikowi prosty komunikat `Cichy poziom nocny`.
- Stare zapisane wartości po aktualizacji są automatycznie sprowadzane do 0, poziomu nocnego albo pełnego poziomu systemowego.

### Testy na Pixelu 10 / Androidzie 17

- Odblokowany ekran: potwierdzone `4% → 2% → 0%` i `0% → 2% → 4%`.
- Zablokowany ekran (`Dozing`): potwierdzone `2% → 0% → 2%`.
- Prawdziwe zdarzenia przycisków na blokadzie: potwierdzone `2% → 4% → 2%` oraz komunikat czytnika.
- Powyżej poziomu nocnego: potwierdzone `4% → 8%`, fizycznie 1/25 → 2/25, bez cyfrowego tłumienia.
- Regresja sterowania na blokadzie: `MEDIA_NEXT` i `MEDIA_PREVIOUS` trafiły bezpośrednio do `com.audioteka`, a Pixel Center po obu komendach odzyskał priorytet przycisków.
- Końcowo zapisano poziom nocny 2%, fizyczny STREAM_MUSIC 1/25 i aktywne tłumienie `-6.0206 dB`.
- Pakiet testowy AndroidTest został usunięty.
- Końcowa MediaSession: `RELATIVE max=100 current=2`.
- Pixel Center 1.14, Jieshuo i AutoGłośnik są włączone i związane; `Crashed services: {}`.
- Android po instalacji wyłączył usługę dostępności Pixel Center, więc została ponownie włączona i sprawdzona.
- Telefon pozostawiono z wygaszonym ekranem w stanie `Dozing`.

### Interfejs, opisy i bateria

- Karta i nagłówki noszą prostą nazwę `Cichy tryb nocny`.
- Opis wprost podaje kolejność poziomów oraz działanie na głośniku i słuchawkach.
- Opis aplikacji nadal wymienia cztery funkcje, ale nie ma już dodatkowego opisu nad zakładkami.
- Changelog aktualizacji jest krótki i napisany prostym językiem.
- Brak pętli i cyklicznego odpytywania; efekt działa tylko na jednym poziomie nocnym i jest wyłączany na pełnych stopniach oraz przy 0%.
- Sterowanie muzyką na blokadzie zachowuje poprawkę odzyskiwania priorytetu MediaSession z wydania 1.13.

### Publikacja 1.14

- Publiczny APK: https://proszczok.pl/YT%20Downloader%20by%20Proszczok/Pixel-Center-1.14-20260905.apk
- Publiczny manifest: https://proszczok.pl/YT%20Downloader%20by%20Proszczok/pixel-center-update.json
- Publicznie pobrany APK ma SHA-256 `C71A820EC2471B4319DD363B1AC700CC14B03BF230F0BE21F65EBEC7BA2CB251`.
- Manifest publiczny wskazuje `versionName=1.14`, `versionCode=21` oraz zgodny SHA-256.
- Na Sambie i w lokalnym katalogu `server` pozostawiono tylko APK 1.14 i manifest; zastąpiony APK 1.13 usunięto.

---

## Poprzednie wydanie — 1.13 (historia)

- Wersja: 1.13
- versionCode: 20
- Pakiet: `pl.proszczok.pixelspeecheq`
- Lokalny APK: `Pixel Center 1.13.apk`
- APK dystrybucyjny: `server/Pixel-Center-1.13-20260905.apk`
- SHA-256 APK: `F0412BFABBF2EE7F53D77F2117E9DEA5DCE20D967D309CE0EA899EB1BD3FE48D`
- Build `clean assembleDebug assembleDebugAndroidTest lintDebug`: zakończony powodzeniem.

### Prawdziwa głośność nocna

- Poprzednie sumowanie kroków zostało zastąpione silnikiem `FineVolumeEngine`.
- MediaSession udostępnia teraz zakres 0–100 zamiast fizycznego 0–25.
- Poziomy pomiędzy sprzętowymi stopniami są realizowane przez globalny `DynamicsProcessing` z ujemnym input gain.
- Test możliwości Pixela: session 0, `hasControl=true`, `enabled=true`, 2 kanały, poprawnie przyjęte `-12 dB`.
- Test 4% → 2%: fizyczny poziom pozostał 1/25, a cyfrowe tłumienie wyniosło `-6.0206 dB`.
- Test 2% → 0%: fizyczny poziom zmienił się 1/25 → 0/25, a efekt został wyłączony.
- Test 0% → 2%: sesja pokazała `current=2/max=100`, fizycznie 1/25, tłumienie aktywne.
- Poziom 1%, 2% lub 3% jest teraz rzeczywiście cichszy od systemowego 4%, co realizuje tryb nocnego słuchania.
- Poziom wirtualny i odpowiadający mu poziom fizyczny są zapisywane oraz odtwarzane po restarcie usługi.
- Ekran ustawień aktualizuje napis `TERAZ` zdarzeniowo, bez pętli i odpytywania. Test UI potwierdził natychmiastową zmianę 2% → 4% → 2%.
- Efekt cyfrowy jest aktywny tylko wtedy, gdy poziom leży pomiędzy fizycznymi stopniami; na pełnym stopniu lub 0% jest wyłączany.

### Niezawodność przycisków na blokadzie

- Ustalona przyczyna: po pierwszej komendzie aktywny odtwarzacz przejmował priorytet klawiszy od MediaSession Pixel Center.
- Pixel Center odzyskuje teraz priorytet 120 ms po bezpośredniej komendzie.
- Usługa jest dodatkowo synchronizowana po związaniu usługi dostępności i listenera powiadomień, więc wraca także po ponownym uruchomieniu procesu.
- Przerwa po globalnym fallbacku została skrócona z 900 ms do 180 ms.
- Wybór awaryjny preferuje ostatnio zapamiętany odtwarzacz, a następnie sesję z najświeższym czasem aktualizacji.
- Test przed poprawką: 1 rozpoznany gest na 4.
- Test po poprawce: 4 rozpoznane gesty na 4, 4 komendy do Audioteki i 4 odzyskania priorytetu.
- Osobny test na blokadzie: `MEDIA_NEXT=1` i `MEDIA_PREVIOUS=1`, oba wysłane do `com.audioteka`.
- Sterowanie multimediami nadal jest wyłączone na odblokowanym ekranie.

### Nazwa przycisku Ostatnich

- Widoczny przycisk nazywa się teraz `Zamknij wszystkie aplikacje`.
- Zmieniono także opis dla czytnika ekranu, komunikat `Zamykanie aplikacji…`, komunikat sukcesu i komunikat braku aplikacji.
- Techniczne rozpoznawanie tekstu systemowego `Wyczyść wszystko` pozostaje w `OverviewDetector`, ponieważ tak Pixel Launcher nazywa własny ukryty element.
- Test okna potwierdził aktywną nakładkę `Pixel Center — Ostatnie aplikacje` o granicach `120,2112–960,2293`; nowa dłuższa nazwa mieści się w przycisku.

### Bateria

- Brak pętli, alarmów i cyklicznego odpytywania.
- Cyfrowe tłumienie działa w systemowym procesorze audio i jest wyłączane na fizycznych stopniach.
- Usługa przycisków działa tylko przy włączonej zaawansowanej głośności lub sterowaniu na blokadzie.
- Wake-lock pozostaje ograniczony do maksymalnie 2 sekund podczas awaryjnej obsługi gestu.
- Usługa EQ jest uruchamiana tylko dla faktycznie odtwarzającej obsługiwanej aplikacji; identyczne zdarzenia tego samego odtwarzacza nie uruchamiają jej ponownie.

### Stan telefonu

- Pixel 10 `frankel_beta`, Android 17, ADB `58071FDCR00200`.
- Zainstalowano finalny APK 1.13/code 20.
- Ustawienia końcowe: zaawansowana głośność włączona, krok 2%, bieżący poziom nocny 2%, sterowanie na blokadzie włączone.
- Sesja końcowa: `ABSOLUTE max=100 current=2`.
- Fizyczny STREAM_MUSIC: 1/25; cyfrowe tłumienie: `-6.0206 dB`, aktywne.
- Pixel Center, Jieshuo i AutoGłośnik są włączone i związane; `Crashed services: {}`.
- Pakiet AndroidTest został usunięty.
- Telefon pozostawiono w stanie `Dozing`.

### Publikacja 1.13

- Publiczny APK: https://proszczok.pl/YT%20Downloader%20by%20Proszczok/Pixel-Center-1.13-20260905.apk
- Publiczny manifest: https://proszczok.pl/YT%20Downloader%20by%20Proszczok/pixel-center-update.json
- Publicznie pobrany APK ma SHA-256 `F0412BFABBF2EE7F53D77F2117E9DEA5DCE20D967D309CE0EA899EB1BD3FE48D`.
- Manifest publiczny wskazuje `versionName=1.13`, `versionCode=20` oraz zgodny SHA-256.
- Na Sambie i w lokalnym katalogu `server` pozostawiono tylko APK 1.13 i manifest; zastąpiony APK 1.12 usunięto.

### Najważniejsze nowe pliki i zmiany

- `app/src/main/java/pl/proszczok/pixelspeecheq/FineVolumeEngine.java`
- `app/src/main/java/pl/proszczok/pixelspeecheq/ScreenOffMediaService.java`
- `app/src/main/java/pl/proszczok/pixelspeecheq/MediaCommandDispatcher.java`
- `app/src/main/java/pl/proszczok/pixelspeecheq/PixelateAccessibilityService.java`
- `app/src/main/java/pl/proszczok/pixelspeecheq/PlayerMonitorService.java`
- `app/src/main/java/pl/proszczok/pixelspeecheq/PixelatePreferences.java`
- `app/src/main/java/pl/proszczok/pixelspeecheq/MainActivity.java`
- `app/src/main/res/values/strings.xml`
- `app/src/androidTest/java/pl/proszczok/pixelspeecheq/ScreenOffGestureInstrumentation.java`
- `app/build.gradle.kts`
- `server/pixel-center-update.json`

---

## Poprzednie wydanie — 1.12 (historia)

- Wersja: 1.12
- versionCode: 19
- Pakiet: `pl.proszczok.pixelspeecheq`
- Lokalny APK: `Pixel Center 1.12.apk`
- APK dystrybucyjny: `server/Pixel-Center-1.12-20260905.apk`
- SHA-256 APK: `1273A0F7FC4D27C9790BFA05D4959E74E410E869D0A68B2F7D540A8EEBDFBE50`
- Build `clean assembleDebug assembleDebugAndroidTest lintDebug`: zakończony powodzeniem.
- Brak repozytorium Git; ten plik i archiwum w katalogu `backup` są punktem kontrolnym.

## Zakres wydania 1.12

- Zakładki mają pełne, jednoznaczne nazwy:
  - Korektor audiobooków
  - Zaawansowana głośność
  - Sterowanie przyciskami
  - Zamknij wszystkie aplikacje
  - Informacje i aktualizacje
- Usunięto opis aplikacji znad zakładek. Została tylko ikona i nazwa Pixel Center.
- Pasek zakładek przewija się poziomo, ma naturalną wysokość i szerokość przycisków oraz nie ściska tekstu przy dużej czcionce.
- Opisy stron i funkcji zostały uproszczone i mówią wprost, kiedy oraz jak dana funkcja działa.
- Naprawiono własny krok głośności. Poprzednio każdy mały procent był wymuszany do minimum 1/25, więc 1%, 2% i podobne zachowywały się jak 4%.
- Nowy `VolumeStepController` sumuje ułamkowe części pomiędzy kliknięciami. Dla zakresu Pixela 0–25:
  - 2% daje sekwencję 4%, 0%, 4%, 0% — średnio dokładnie 2% na kliknięcie,
  - 10% daje sekwencję 12%, 8%, 12%, 8% — średnio dokładnie 10% na kliknięcie.
- Sterowanie muzyką długim naciśnięciem nadal działa tylko na zablokowanym ekranie. Krótkie naciśnięcia własnej głośności działają na ekranie odblokowanym i zablokowanym.
- Ograniczono ponowne uruchamianie usługi EQ: ten sam aktywny odtwarzacz nie wybudza już wielokrotnie `ScopedEqService` przy kolejnych identycznych zdarzeniach.

## Zużycie baterii

- Obsługa przycisków i odtwarzaczy jest zdarzeniowa; nie ma pętli, alarmów ani cyklicznego odpytywania.
- Usługa przycisków działa tylko wtedy, gdy włączona jest zaawansowana głośność lub sterowanie na blokadzie. Po wyłączeniu obu funkcji jest zatrzymywana.
- Wake-lock usługi dostępności jest awaryjny, używany tylko podczas gestu przy wygaszonym ekranie i ma limit 2 sekund.
- Korektor działa jako foreground service wyłącznie podczas rzeczywistego odtwarzania w obsługiwanej aplikacji i jest zwalniany po zatrzymaniu.

## Updater

- Aktualizacja jest pobierana wewnątrz Pixel Center, bez Chrome.
- Przed instalacją sprawdzane są SHA-256, identyfikator pakietu, versionCode, podpis i limit rozmiaru.
- APK trafia bezpośrednio do systemowego instalatora Google przez prywatny, tylko-do-odczytu `UpdateFileProvider`.
- Pełny przepływ pobrania i instalacji został sprawdzony wcześniej na przejściu 1.10 → 1.11.
- Publiczny manifest 1.12 i publicznie pobrany APK 1.12 zostały ponownie sprawdzone; wersja, URL i SHA-256 są zgodne.

## Testy na Pixelu 10 / Androidzie 17

- Urządzenie: Pixel 10 `frankel_beta`, Android 17, ADB `58071FDCR00200`.
- Skala czcionki użytkownika: 150%; rozdzielczość: 1080 × 2424.
- UI hierarchy potwierdziła pełne nazwy i poprawne granice zakładek oraz treści bez nakładania.
- Test 2%, cztery krótkie naciśnięcia Głośność−: `25 → 24 → 24 → 23 → 23`.
- Test 10%, cztery krótkie naciśnięcia Głośność−: `23 → 20 → 18 → 15 → 13`.
- Długie naciśnięcie 850 ms na odblokowanym ekranie zmieniło wyłącznie głośność; nie wysłało komendy multimedialnej.
- Długie naciśnięcie na zablokowanym ekranie wysłało `MEDIA_NEXT` bezpośrednio do aktywnej sesji YouTube Music.
- Wcześniejsze testy potwierdziły bezpośrednie `MEDIA_NEXT` i `MEDIA_PREVIOUS` dla Audioteki oraz zgodność transport controls/media keys z Pixel Audio Player.
- Finalnie na telefonie jest 1.12/code 19. Pakiet AndroidTest został usunięty.
- Ustawienia użytkownika zachowane: zaawansowana głośność włączona, średni krok 10%, sterowanie na blokadzie włączone.
- Usługi dostępności Pixel Center, Jieshuo i AutoGłośnik są włączone i związane; `Crashed services: {}`.
- Dostęp do powiadomień jest zachowany. Telefon pozostawiono w stanie Dozing.

## Publikacja

- Publiczny APK: https://proszczok.pl/YT%20Downloader%20by%20Proszczok/Pixel-Center-1.12-20260905.apk
- Publiczny manifest: https://proszczok.pl/YT%20Downloader%20by%20Proszczok/pixel-center-update.json
- Publiczny APK pobrano ponownie przez HTTPS i zweryfikowano SHA-256.
- Manifest publiczny wskazuje `versionName=1.12`, `versionCode=19` i właściwy SHA-256.
- Na Sambie i w lokalnym katalogu `server` pozostawiono tylko APK 1.12 oraz manifest aktualizacji; zastąpiony APK 1.11 usunięto.

## Główne zmienione pliki

- `app/src/main/java/pl/proszczok/pixelspeecheq/MainActivity.java`
- `app/src/main/java/pl/proszczok/pixelspeecheq/VolumeStepController.java`
- `app/src/main/java/pl/proszczok/pixelspeecheq/ScreenOffMediaService.java`
- `app/src/main/java/pl/proszczok/pixelspeecheq/PixelateAccessibilityService.java`
- `app/src/main/java/pl/proszczok/pixelspeecheq/PlayerMonitorService.java`
- `app/src/androidTest/java/pl/proszczok/pixelspeecheq/ScreenOffGestureInstrumentation.java`
- `app/build.gradle.kts`
- `server/pixel-center-update.json`
