# Audyt dostępności / Accessibility audit — Pixel Center 1.03

Data: 2026-11-16. Urządzenie: Google Pixel 11, Android 17 / API 37, 1080×2424, 420 dpi. TalkBack był włączony, związany z systemem, z aktywnym eksplorowaniem dotykiem. Pomocnicza instrumentacja używa `FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES`, aby nie wyłączać czytnika podczas odczytu drzewa.

## Wynik i poprawki

W 1.02 pasek kart na górze wymagał przewijania poziomego; ostatnie karty były poza ekranem. W 1.03 pięć kart jest stale dostępnych na dole. Przy skali czcionki ponad 130% nawigacja ma dwa rzędy. Przyciski mają role zakładek, zaznaczenie oraz opisy paneli w języku aplikacji. Nagłówki i etykiety formularzy pozostają semantyczne. Ikony ozdobne nie powielają tekstu. W poziomie treść uwzględnia boczny pasek systemowy. Standardowe przyciski mają minimum 48 dp wysokości.

| Obszar | Weryfikacja |
| --- | --- |
| Karty i języki | 35 przejść: 5 kart × PL/EN/DE/RU/ES/FR/IT. Każde drzewo: 5 widocznych zakładek, dokładnie 1 zaznaczona. |
| Duży tekst | 20 przejść przy 150%: wszystkie karty w PL/DE/RU/IT; dwa rzędy nawigacji, przewijalna treść. |
| Końcowy APK | Dodatkowe 10 przejść PL/EN i kontrola poziomu po poprawce marginesów. Zainstalowany plik ma identyczny SHA-256 z lokalnym wydaniem. |
| Kontrolki | Włączanie/wyłączanie EQ, nocnej głośności i sterowania multimediami; wybór profilu średniego i powrót do lekkiego; fokus dostępności i zdarzenia ze stanem przełącznika. |
| Ostatnie | Pełna etykieta i fokus nakładki; czyszczenie zakończone przez natywny przycisk po 0 krokach przewijania, ok. 352 ms. W Ostatnich po ponownym otwarciu zostały Pixel Center i ekran główny. |
| Teksty | 142 wpisy interfejsu × 7 języków; test kompletności i argumentów formatowania, lokalizowane zasoby usług i nakładki. |
| Build | 5 testów jednostkowych bez błędów; APK główne i AndroidTest; Lint: 0 błędów, 18 ostrzeżeń. AndroidTest został zbudowany, nie uruchomiono całej instrumentacji gestów sprzętowych. |

Kontrast obliczony dla stałych kolorów aplikacji: tekst podstawowy na karcie 15,42:1, tekst pomocniczy 9,10:1, niebieski tekst 8,25:1, tekst zaznaczonej zakładki 6,45:1. Stan jest oznaczony tekstem i semantyką, także bez rozróżniania koloru. Są to obliczenia dla kolorów z kodu, nie certyfikacja całej aplikacji.

## Zakres i ograniczenia

To audyt kodu, zrzutów, drzew dostępności i interakcji wywoływanych przez automat przy działającym TalkBacku. Potwierdzono zdarzenie `TYPE_VIEW_ACCESSIBILITY_FOCUSED` z etykietą i stanem przełącznika. Nie wykonano odsłuchu syntezy mowy ani ręcznego, dotykowego przejścia osoby niewidomej. Poprawność głosów, wymowy i gestów fizycznych w siedmiu językach wymaga takiego odbioru. Testy nie zastępują próby audio z konkretnymi słuchawkami Bluetooth i każdym odtwarzaczem ani pomiarów baterii. Nie deklarujemy zgodności WCAG na podstawie samego drzewa.

W trakcie testów zachowano/odtworzono ustawienia użytkownika: EQ włączony, profil lekki, nocna głośność wyłączona, sterowanie multimediami włączone, limit 20 s, automatyczny fokus Ostatnich wyłączony, język aplikacji zgodny z systemem, czcionka 100%, pion i obrót automatyczny wyłączony. TalkBack, AutoGłośnik i Pixel Center pozostają aktywne; końcowy stan usług należy sprawdzić w `STAN_PRAC.md`.

## Powtarzanie testu

Moduł `audit-driver` jest narzędziem developerskim, poza produkcyjnym APK. Pochodzi z pomocnika użytego w Androidowym YouTube Downloader by Proszczok; zachowuje jego przestrzeń nazw, więc nie instaluj dwóch różnych wersji pomocnika jednocześnie. Nie publikujemy surowych zrzutów systemu i logów telefonu w GitHubie.

```powershell
.\gradlew.bat -PincludeAuditDriver=true :audit-driver:assembleDebug
# Zainstaluj pomocnik na urządzeniu testowym, otwórz Pixel Center i włącz TalkBack.
# W PowerShell 5 odczytaj UTF-8 jawnie:
$auditScript = Get-Content -Raw -Encoding utf8 tools/Invoke-AccessibilityAudit.ps1
& ([scriptblock]::Create($auditScript)) -Adb 'C:\Android\platform-tools\adb.exe'
```

Skrypt przywraca poprzedni język aplikacji. Skala czcionki i orientacja są osobnymi ustawieniami testu; po próbie trzeba odtworzyć ich wartości. Pomocnik obsługuje również `focus`, `scroll`, `click` i `setText`; dopasowanie `targetPackage` chroni przed naciśnięciem systemowego przycisku o tej samej nazwie. Po audycie usuń pomocnik z urządzenia.

## English

Tested on Pixel 11, Android 17/API 37 with TalkBack bound and touch exploration enabled. All 35 language/tab combinations passed; 20 large-text combinations were checked at 150%, followed by final PL/EN smoke tests and landscape verification. Navigation exposes five visible tabs and one selected tab. Contrast ratios for the app's fixed colors range from 6.45:1 for selected-tab text to 15.42:1 for body text. Unit tests passed; Lint reported zero errors and 18 warnings.

Coverage consists of code review, screenshots, accessibility trees, focus events, and automated actions while TalkBack remains active. Audible speech, physical touch gestures, pronunciation in seven languages, Bluetooth audio hardware, every media player, and battery use were not fully tested. This is not a WCAG certification. The optional audit helper is excluded from the shipped APK, and raw phone evidence remains local. Device preferences are restored after testing.
