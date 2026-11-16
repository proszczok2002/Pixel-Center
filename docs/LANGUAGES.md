# Języki / Languages

Pixel Center 1.03: Polski (pl), English (en), Deutsch (de), Русский (ru), Español (es), Français (fr), Italiano (it).

- Polski: Więcej → Zmień język aplikacji → wybierz język.
- English: More → Change app language → choose a language.
- Deutsch: Mehr → App-Sprache ändern → Sprache auswählen.
- Русский: Ещё → Изменить язык приложения → выберите язык.
- Español: Más → Cambiar idioma de la aplicación → elige un idioma.
- Français : Plus → Changer la langue de l’application → choisissez une langue.
- Italiano: Altro → Cambia lingua dell’app → scegli una lingua.

Android 13+ udostępnia wybór języka dla samej aplikacji. Android 9–12 używa języka systemu. Nieobsługiwany język korzysta z angielskiego. Język głosu TalkBacka zależy od ustawień czytnika i zainstalowanego syntezatora.

Android 13+ provides per-app language selection. Android 9–12 follows the system language. Unsupported languages fall back to English. TalkBack's spoken voice depends on the screen reader settings and installed speech engine.

Teksty programowego interfejsu są w `LocalizedText.java`, a etykiety usług i nakładki w `app/src/main/res/values-*/strings.xml`. Każdy wpis wymaga siedmiu wersji. Test `LocalizedTextTest` sprawdza kompletność i zgodność argumentów formatowania. Manifest aktualizacji może zawierać `changelogI18n` z kluczami języków; `changelog` pozostaje zgodnym polem dla wersji 1.0–1.02.

Programmatic interface translations live in `LocalizedText.java`; service and overlay labels live in `app/src/main/res/values-*/strings.xml`. Each entry requires seven versions. `LocalizedTextTest` checks completeness and formatting arguments. Update manifests may include `changelogI18n` language keys; `changelog` remains the compatible field for versions 1.0–1.02.
