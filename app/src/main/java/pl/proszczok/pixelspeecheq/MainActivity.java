package pl.proszczok.pixelspeecheq;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.Locale;

import java.lang.ref.WeakReference;

public final class MainActivity extends Activity {
    private static final int COLOR_BACKGROUND = Color.rgb(12, 12, 14);
    private static final int COLOR_CARD = Color.rgb(29, 29, 34);
    private static final int COLOR_TEXT = Color.rgb(245, 245, 247);
    private static final int COLOR_SECONDARY = Color.rgb(190, 190, 200);
    private static final int COLOR_ACCENT = Color.rgb(129, 185, 255);
    private static final int COLOR_OK = Color.rgb(111, 220, 150);
    private static final int COLOR_WARNING = Color.rgb(255, 184, 105);
    private static final int TAB_AUDIO = 0;
    private static final int TAB_VOLUME = 1;
    private static final int TAB_MEDIA = 2;
    private static final int TAB_RECENTS = 3;
    private static final int TAB_INFO = 4;
    private static final long TIMEOUT_AUTO_SAVE_DELAY_MS = 900;
    private static WeakReference<MainActivity> resumedActivity = new WeakReference<>(null);

    private final int[] presetIds = new int[3];
    private TextView enabledState;
    private TextView accessState;
    private TextView selectedPreset;
    private TextView currentStatus;
    private TextView recentsState;
    private TextView mediaControlsState;
    private TextView volumeStepState;
    private Switch masterSwitch;
    private Switch mediaControlsSwitch;
    private Switch advancedVolumeSwitch;
    private Switch focusButtonSwitch;
    private RadioGroup presetGroup;
    private EditText timeoutInput;
    private SharedPreferences prefs;
    private SharedPreferences pixelatePrefs;
    private boolean volumePreferencesRegistered;
    private final SharedPreferences.OnSharedPreferenceChangeListener
            volumePreferenceListener = (preferences, key) ->
            runOnUiThread(this::refreshVolumeStepState);
    private boolean refreshing;
    private final Button[] tabButtons = new Button[5];
    private final View[] tabPages = new View[5];
    private final Handler preferencesHandler = new Handler(Looper.getMainLooper());
    private final Runnable timeoutSaveRunnable = () -> persistTimeout(false);
    private int selectedTab;
    private boolean updateDialogVisible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        prefs = getSharedPreferences(ScopedEqService.PREFS, MODE_PRIVATE);
        pixelatePrefs = PixelatePreferences.preferences(this);
        ScreenOffMediaService.sync(this);
        if (savedInstanceState != null) {
            selectedTab = savedInstanceState.getInt("selected_tab", TAB_AUDIO);
        }
        requestNotificationPermission();
        setContentView(buildImprovedUi());
        selectTab(selectedTab);
        preferencesHandler.postDelayed(this::refreshImprovedUi, 500);
        preferencesHandler.postDelayed(() -> checkForUpdates(false), 1200);
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumedActivity = new WeakReference<>(this);
        if (!volumePreferencesRegistered && pixelatePrefs != null) {
            pixelatePrefs.registerOnSharedPreferenceChangeListener(
                    volumePreferenceListener);
            volumePreferencesRegistered = true;
        }
        refreshImprovedUi();
    }

    @Override
    protected void onPause() {
        if (resumedActivity.get() == this) {
            resumedActivity.clear();
        }
        if (volumePreferencesRegistered && pixelatePrefs != null) {
            pixelatePrefs.unregisterOnSharedPreferenceChangeListener(
                    volumePreferenceListener);
            volumePreferencesRegistered = false;
        }
        persistTimeout(false);
        super.onPause();
    }

    static void onServiceConnectionChanged() {
        MainActivity activity = resumedActivity.get();
        if (activity != null) {
            activity.refreshImprovedUi();
        }
    }

    @Override
    protected void onDestroy() {
        preferencesHandler.removeCallbacks(timeoutSaveRunnable);
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("selected_tab", selectedTab);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == UpdateInstaller.REQUEST_UNKNOWN_SOURCES) {
            UpdateInstaller.onUnknownSourcesResult(this);
        }
    }

    private View buildImprovedUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COLOR_BACKGROUND);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(),
                    insets.getSystemWindowInsetBottom());
            return insets;
        });
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(14), dp(20), 0);

        addHeader(content);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            root.addView(content, matchWrap());
            FrameLayout host = new FrameLayout(this);
            tabPages[TAB_AUDIO] = buildAudioPage();
            tabPages[TAB_VOLUME] = buildVolumePage();
            tabPages[TAB_MEDIA] = buildMediaPage();
            tabPages[TAB_RECENTS] = buildRecentsPage();
            tabPages[TAB_INFO] = buildInfoPage();
            for (View page : tabPages) {
                host.addView(page, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
            }
            root.addView(host, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
            root.addView(buildBottomNavigation(), matchWrap());
            root.requestApplyInsets();
            return root;
        }
        addSectionTitle(content, "DŹWIĘK AUDIOBOOKÓW");
        addMainCard(content);
        addAccessCard(content);
        addPresetCard(content);
        addCurrentStatus(content);

        TextView note = text(
                "Korektor nie zwiększa głośności i nie wyłącza sprzętowej ochrony głośników telefonu.",
                13, Color.rgb(155, 155, 165));
        note.setPadding(0, dp(18), 0, 0);
        content.addView(note, matchWrap());

        addRecentsModule(content);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setClipToPadding(true);
        scrollView.addView(content);
        scrollView.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(
                    0,
                    insets.getSystemWindowInsetTop(),
                    0,
                    insets.getSystemWindowInsetBottom());
            return insets;
        });
        scrollView.requestApplyInsets();
        return scrollView;
    }

    private View buildBottomNavigation() {
        LinearLayout navigation = new LinearLayout(this);
        navigation.setOrientation(LinearLayout.VERTICAL);
        navigation.setBackgroundColor(COLOR_CARD);
        navigation.setPadding(dp(6), dp(5), dp(6), dp(5));
        navigation.setAccessibilityPaneTitle(tr("Główna nawigacja"));
        boolean largeText = getResources().getConfiguration().fontScale > 1.3f;
        String[] labels = {"Korektor", "Noc", "Przyciski", "Ostatnie", "Więcej"};
        int[] icons = {
                R.drawable.ic_nav_equalizer,
                R.drawable.ic_nav_night_volume,
                R.drawable.ic_nav_media,
                R.drawable.ic_nav_recents,
                R.drawable.ic_nav_info
        };
        int rowSize = largeText ? 3 : labels.length;
        LinearLayout row = null;
        for (int i = 0; i < labels.length; i++) {
            if (i % rowSize == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER);
                navigation.addView(row, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
            }
            final int tab = i;
            Button item = button(labels[i]);
            item.setMaxLines(2);
            item.setTextSize(largeText ? 14 : 11);
            item.setMinHeight(dp(largeText ? 62 : 66));
            item.setPadding(dp(4), dp(6), dp(4), dp(4));
            item.setGravity(Gravity.CENTER);
            item.setCompoundDrawablesRelativeWithIntrinsicBounds(0, icons[i], 0, 0);
            item.setCompoundDrawablePadding(dp(3));
            item.setCompoundDrawableTintList(ColorStateList.valueOf(COLOR_TEXT));
            item.setAccessibilityDelegate(new View.AccessibilityDelegate() {
                @Override
                public void onInitializeAccessibilityNodeInfo(
                        View host, AccessibilityNodeInfo info) {
                    super.onInitializeAccessibilityNodeInfo(host, info);
                    info.setClassName("android.app.ActionBar$Tab");
                    info.setSelected(host.isSelected());
                }
            });
            item.setOnClickListener(v -> selectTab(tab));
            tabButtons[i] = item;
            LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            itemParams.leftMargin = dp(2);
            itemParams.rightMargin = dp(2);
            row.addView(item, itemParams);
        }
        return navigation;
    }

    private View buildAudioPage() {
        LinearLayout content = page("Korektor audiobooków",
                "Włącz korekcję głosu, która uruchamia się sama tylko podczas odtwarzania audiobooka w obsługiwanej aplikacji.");
        addSectionTitle(content, "KOREKTOR AUDIOBOOKÓW");
        addMainCard(content);
        addAccessCard(content);
        addPresetCard(content);
        addCurrentStatus(content);
        return scrollPage(content, "Karta Korektor audiobooków");
    }

    private View buildMediaPage() {
        LinearLayout content = page("Sterowanie przyciskami",
                "Przy zablokowanym ekranie przytrzymaj przycisk głośności, aby sterować muzyką lub audiobookiem.");
        addScreenOffMediaControls(content);
        return scrollPage(content, "Karta Sterowanie przyciskami");
    }

    private View buildVolumePage() {
        LinearLayout content = page("Cichy tryb nocny",
                "Dodaje jeden cichszy poziom między wyciszeniem a pierwszym poziomem Androida. Działa na ekranie odblokowanym i zablokowanym.");
        addAdvancedVolumeControls(content);
        return scrollPage(content, "Karta Cichy tryb nocny");
    }

    private View buildRecentsPage() {
        LinearLayout content = page("Zamknij wszystkie aplikacje",
                "Dodaje łatwo dostępny przycisk „Zamknij wszystkie aplikacje” do ekranu ostatnich aplikacji Pixela.");
        addRecentsModule(content);
        return scrollPage(content, "Karta Zamknij wszystkie aplikacje");
    }

    private View buildInfoPage() {
        LinearLayout content = page("Informacje i aktualizacje",
                "Wersja aplikacji, prywatność oraz pobieranie nowych wydań Pixel Center.");
        addSectionTitle(content, "O APLIKACJI");
        LinearLayout appCard = card();
        TextView version = stateText();
        version.setText("PIXEL CENTER " + appVersion());
        appCard.addView(version, matchWrap());
        TextView about = text(
                "Cztery funkcje dla Pixela: korektor audiobooków, cichy poziom nocny, sterowanie muzyką na zablokowanym ekranie i przycisk Zamknij wszystkie aplikacje.",
                15, COLOR_SECONDARY);
        about.setPadding(0, dp(10), 0, 0);
        appCard.addView(about, matchWrap());
        content.addView(appCard, cardParams());

        addSectionTitle(content, "DZIAŁANIE I PRYWATNOŚĆ");
        LinearLayout detailsCard = card();
        TextView details = text(
                "Korektor działa tylko podczas audiobooka w obsługiwanej aplikacji. Cichy poziom nocny działa wyłącznie na słuchawkach Bluetooth, na ekranie odblokowanym i zablokowanym. Głośnik telefonu zachowuje fabryczne kroki Androida. Sterowanie muzyką reaguje tylko przy zablokowanym ekranie. Pixel Center nie zapisuje ani nie wysyła treści ekranu ani naciśnięć przycisków.",
                14, COLOR_SECONDARY);
        detailsCard.addView(details, matchWrap());
        TextView device = text(tf("Urządzenie: Google %1$s", Build.MODEL),
                14, COLOR_ACCENT);
        device.setPadding(0, dp(14), 0, 0);
        detailsCard.addView(device, matchWrap());
        content.addView(detailsCard, cardParams());

        addSectionTitle(content, "JĘZYK");
        LinearLayout languageCard = card();
        TextView languageState = stateText();
        languageState.setText(tf("JĘZYK APLIKACJI: %1$s",
                LocalizedText.languageName(this)));
        languageCard.addView(languageState, matchWrap());
        TextView languageDescription = text(
                "Pixel Center używa języka wybranego dla aplikacji w ustawieniach Androida. Dostępne są: polski, angielski, niemiecki, rosyjski, hiszpański, francuski i włoski.",
                14, COLOR_SECONDARY);
        languageDescription.setPadding(0, dp(8), 0, dp(12));
        languageCard.addView(languageDescription, matchWrap());
        Button languageButton = button("Zmień język aplikacji");
        languageButton.setOnClickListener(v -> openLanguageSettings());
        languageCard.addView(languageButton, matchWrap());
        content.addView(languageCard, cardParams());

        addSectionTitle(content, "AKTUALIZACJE");
        LinearLayout updateCard = card();
        TextView updateDescription = text(
                "Pixel Center sprawdza wydania na GitHubie, pobiera zweryfikowany APK wewnątrz aplikacji i przekazuje go bezpośrednio do instalatora Androida.",
                14, COLOR_SECONDARY);
        updateDescription.setPadding(0, 0, 0, dp(12));
        updateCard.addView(updateDescription, matchWrap());
        Button updateButton = button("Sprawd\u017a aktualizacje");
        updateButton.setOnClickListener(v -> checkForUpdates(true));
        updateCard.addView(updateButton, matchWrap());
        content.addView(updateCard, cardParams());

        addSectionTitle(content, "WSPARCIE PROJEKTU");
        LinearLayout supportCard = card();
        TextView supportDescription = text(
                "Jeżeli Pixel Center jest dla Ciebie przydatny, możesz dobrowolnie wesprzeć jego dalszy rozwój. Wsparcie nie wpływa na dostępność funkcji ani aktualizacji.",
                14, COLOR_SECONDARY);
        supportDescription.setPadding(0, 0, 0, dp(12));
        supportCard.addView(supportDescription, matchWrap());
        Button revolutButton = button("Wesprzyj przez Revolut");
        revolutButton.setOnClickListener(v ->
                openExternalLink("https://revolut.me/r_rosiejka"));
        supportCard.addView(revolutButton, matchWrap());
        Button paypalButton = button("Wesprzyj przez PayPal");
        LinearLayout.LayoutParams paypalParams = matchWrap();
        paypalParams.topMargin = dp(10);
        paypalButton.setOnClickListener(v ->
                openExternalLink("https://paypal.me/proszczok"));
        supportCard.addView(paypalButton, paypalParams);
        content.addView(supportCard, cardParams());
        return scrollPage(content, "Karta Informacje i aktualizacje");
    }

    private void openExternalLink(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        if (intent.resolveActivity(getPackageManager()) == null) {
            Toast.makeText(this, tr("Brak aplikacji do otwarcia linku."),
                    Toast.LENGTH_LONG).show();
            return;
        }
        startActivity(intent);
    }

    private void openLanguageSettings() {
        Intent intent;
        if (Build.VERSION.SDK_INT >= 33) {
            intent = new Intent(Settings.ACTION_APP_LOCALE_SETTINGS,
                    Uri.parse("package:" + getPackageName()));
        } else {
            intent = new Intent(Settings.ACTION_LOCALE_SETTINGS);
        }
        startActivity(intent);
    }

    private void checkForUpdates(boolean manual) {
        if (manual) {
            Toast.makeText(this, tr("Sprawdzam aktualizacje..."),
                    Toast.LENGTH_SHORT).show();
        }
        UpdateChecker.check(this, new UpdateChecker.Callback() {
            @Override
            public void onResult(UpdateChecker.UpdateInfo info) {
                if (isFinishing() || isDestroyed()) return;
                if (info.versionCode > UpdateChecker.installedVersionCode(MainActivity.this)) {
                    showUpdateDialog(info);
                } else if (manual) {
                    showCurrentVersionDialog();
                }
            }

            @Override
            public void onError(Exception error) {
                if (manual && !isFinishing() && !isDestroyed()) {
                    Toast.makeText(MainActivity.this,
                            tr("Nie udało się sprawdzić aktualizacji. Sprawdź internet i spróbuj ponownie."),
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void showUpdateDialog(UpdateChecker.UpdateInfo info) {
        if (updateDialogVisible) return;
        updateDialogVisible = true;
        String notes = TextUtils.isEmpty(info.changelog) ? "" : "\n\n" + info.changelog;
        String message = tf("Dostępna wersja: %1$s\nZainstalowana wersja: %2$s%3$s",
                info.versionName, appVersion(), notes);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(tr("Nowa wersja Pixel Center"))
                .setMessage(message)
                .setNegativeButton(tr("Później"), null)
                .setPositiveButton(tr("Pobierz i zainstaluj"), (ignored, which) ->
                        UpdateInstaller.downloadAndInstall(MainActivity.this, info))
                .create();
        dialog.setOnDismissListener(ignored -> updateDialogVisible = false);
        dialog.show();
    }

    private void showCurrentVersionDialog() {
        new AlertDialog.Builder(this)
                .setTitle(tr("Aktualizacje Pixel Center"))
                .setMessage(tf("Masz najnowszą wersję.\n\nZainstalowana wersja: %1$s",
                        appVersion()))
                .setPositiveButton("OK", null)
                .show();
    }

    private LinearLayout page(String titleValue, String descriptionValue) {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(12), dp(20), dp(32));
        content.setBackgroundColor(COLOR_BACKGROUND);
        TextView title = text(titleValue, 24, COLOR_TEXT);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setAccessibilityHeading(true);
        content.addView(title, matchWrap());
        TextView description = text(descriptionValue, 15, COLOR_SECONDARY);
        description.setPadding(0, dp(6), 0, dp(2));
        content.addView(description, matchWrap());
        return content;
    }

    private View scrollPage(LinearLayout content, String paneTitle) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(COLOR_BACKGROUND);
        scroll.setAccessibilityPaneTitle(tr(paneTitle));
        scroll.addView(content);
        return scroll;
    }

    private void selectTab(int requestedTab) {
        selectedTab = Math.max(TAB_AUDIO, Math.min(TAB_INFO, requestedTab));
        for (int i = 0; i < tabPages.length; i++) {
            boolean selected = i == selectedTab;
            tabPages[i].setVisibility(selected ? View.VISIBLE : View.GONE);
            tabButtons[i].setSelected(selected);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                tabButtons[i].setStateDescription(selected ? tr("Wybrano") : null);
            }
            tabButtons[i].setTextColor(selected
                    ? Color.rgb(8, 48, 95) : COLOR_TEXT);
            tabButtons[i].setCompoundDrawableTintList(ColorStateList.valueOf(
                    selected ? Color.rgb(8, 48, 95) : COLOR_TEXT));
            tabButtons[i].setBackground(tabBackground(selected));
        }
        tabPages[selectedTab].sendAccessibilityEvent(
                android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    private void addHeader(LinearLayout content) {
        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        ImageView icon = new ImageView(this);
        icon.setImageResource(R.mipmap.ic_launcher);
        icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        titleRow.addView(icon, new LinearLayout.LayoutParams(dp(46), dp(46)));

        TextView title = text("Pixel Center", 28, COLOR_TEXT);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setAccessibilityHeading(true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.leftMargin = dp(12);
        titleRow.addView(title, titleParams);
        titleRow.setPadding(0, 0, 0, dp(8));
        content.addView(titleRow, matchWrap());
    }

    private void addMainCard(LinearLayout content) {
        LinearLayout mainCard = card();
        enabledState = stateText();
        mainCard.addView(enabledState, matchWrap());

        masterSwitch = new Switch(this);
        masterSwitch.setText(tr("Automatyczny korektor"));
        masterSwitch.setTextSize(18);
        masterSwitch.setTextColor(COLOR_TEXT);
        masterSwitch.setPadding(0, dp(8), 0, dp(4));
        masterSwitch.setMinHeight(dp(56));
        masterSwitch.setShowText(false);
        masterSwitch.setOnCheckedChangeListener((button, checked) -> {
            if (refreshing) return;
            Intent intent = new Intent(this, ScopedEqService.class)
                    .setAction(checked
                            ? ScopedEqService.ACTION_ENABLE_GLOBAL
                            : ScopedEqService.ACTION_DISABLE);
            startService(intent);
            if (checked && !hasNotificationAccess()) {
                currentStatus.setText(tr(
                        "Włącz dostęp systemowy poniżej, aby aplikacja mogła wykrywać odtwarzanie."));
                currentStatus.setTextColor(COLOR_WARNING);
            }
            currentStatus.postDelayed(this::refreshImprovedUi, 350);
        });
        mainCard.addView(masterSwitch, matchWrap());

        TextView help = text(
                "Po włączeniu aplikacja sama uruchamia EQ przy audiobooku i wyłącza go po zatrzymaniu odtwarzania.",
                14, COLOR_SECONDARY);
        mainCard.addView(help, matchWrap());
        content.addView(mainCard, cardParams());
    }

    private void addAccessCard(LinearLayout content) {
        addSectionTitle(content, "DOSTĘP DO ODTWARZANIA");
        LinearLayout accessCard = card();
        accessState = stateText();
        accessState.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        accessCard.addView(accessState, matchWrap());

        TextView help = text(
                "Ten dostęp pozwala rozpoznać aktywny odtwarzacz dla automatycznego EQ. Sterowanie klawiszami w zakładce Sterowanie przyciskami go nie wymaga. Pixel Center nie odczytuje treści wiadomości.",
                14, COLOR_SECONDARY);
        help.setPadding(0, dp(8), 0, dp(12));
        accessCard.addView(help, matchWrap());

        Button accessButton = button("Otwórz ustawienia dostępu");
        accessButton.setContentDescription(tr(
                "Otwórz systemowe ustawienia dostępu do powiadomień"));
        accessButton.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        accessCard.addView(accessButton, matchWrap());
        content.addView(accessCard, cardParams());
    }

    private void addPresetCard(LinearLayout content) {
        addSectionTitle(content, "PROFIL DŹWIĘKU");
        LinearLayout presetCard = card();
        selectedPreset = stateText();
        selectedPreset.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        presetCard.addView(selectedPreset, matchWrap());

        presetGroup = new RadioGroup(this);
        presetGroup.setOrientation(RadioGroup.VERTICAL);
        String[] names = {"Lekki", "Średni", "Mocny"};
        String[] details = {
                "Delikatna korekcja",
                "Wyraźniejsze uspokojenie dźwięku",
                "Najsilniejsza korekcja mowy"
        };
        for (int i = 0; i < names.length; i++) {
            RadioButton radio = new RadioButton(this);
            presetIds[i] = View.generateViewId();
            radio.setId(presetIds[i]);
            radio.setText(tr(names[i]) + "\n" + tr(details[i]));
            radio.setTextSize(16);
            radio.setTextColor(new ColorStateList(
                    new int[][] {new int[] {android.R.attr.state_checked}, new int[] {}},
                    new int[] {COLOR_ACCENT, COLOR_TEXT}));
            radio.setButtonTintList(new ColorStateList(
                    new int[][] {new int[] {android.R.attr.state_checked}, new int[] {}},
                    new int[] {COLOR_ACCENT, COLOR_SECONDARY}));
            radio.setMinHeight(dp(64));
            radio.setPadding(dp(4), dp(5), dp(4), dp(5));
            presetGroup.addView(radio, matchWrap());
        }
        presetGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (refreshing) return;
            int preset = presetForId(checkedId);
            Intent intent = new Intent(this, ScopedEqService.class)
                    .setAction(ScopedEqService.ACTION_SET_PRESET)
                    .putExtra(ScopedEqService.EXTRA_PRESET, preset);
            startService(intent);
            selectedPreset.setText(tf("WYBRANY PROFIL: %1$s",
                    presetName(preset).toUpperCase(Locale.getDefault())));
            currentStatus.postDelayed(this::refreshImprovedUi, 250);
        });
        presetCard.addView(presetGroup, matchWrap());
        content.addView(presetCard, cardParams());
    }

    private void addCurrentStatus(LinearLayout content) {
        addSectionTitle(content, "BIEŻĄCY STAN");
        currentStatus = text("", 15, COLOR_ACCENT);
        currentStatus.setBackground(cardBackground());
        currentStatus.setPadding(dp(16), dp(16), dp(16), dp(16));
        currentStatus.setTextIsSelectable(true);
        currentStatus.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        content.addView(currentStatus, cardParams());
    }

    private void addScreenOffMediaControls(LinearLayout content) {
        addSectionTitle(content, "STEROWANIE PRZY WYGASZONYM EKRANIE");
        LinearLayout controlsCard = card();

        mediaControlsState = stateText();
        mediaControlsState.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        controlsCard.addView(mediaControlsState, matchWrap());

        mediaControlsSwitch = new Switch(this);
        mediaControlsSwitch.setText(tr("Sterowanie muzyką przyciskami głośności"));
        mediaControlsSwitch.setTextSize(17);
        mediaControlsSwitch.setTextColor(COLOR_TEXT);
        mediaControlsSwitch.setMinHeight(dp(56));
        mediaControlsSwitch.setOnCheckedChangeListener((button, checked) -> {
            if (refreshing) return;
            PixelatePreferences.setScreenOffMediaControlsEnabled(this, checked);
            ScreenOffMediaService.sync(this);
            refreshMediaControlsState();
            if (checked && !PixelateAccessibilityService.isConnected()) {
                Toast.makeText(this,
                        tr("Włącz usługę Pixel Center w ustawieniach dostępności."),
                        Toast.LENGTH_LONG).show();
            }
        });
        controlsCard.addView(mediaControlsSwitch, matchWrap());

        TextView help = text(
                "Wyłącznie gdy ekran jest zablokowany: przytrzymaj Głośność +, aby przejść do następnego utworu; przytrzymaj Głośność −, aby wrócić do poprzedniego; przytrzymaj oba przyciski, aby wstrzymać lub wznowić odtwarzanie. Na odblokowanym ekranie długie naciśnięcie nie steruje muzyką.",
                14, COLOR_SECONDARY);
        controlsCard.addView(help, matchWrap());

        Button accessibilityButton = button("Otwórz ustawienia dostępności");
        LinearLayout.LayoutParams accessibilityParams = matchWrap();
        accessibilityParams.topMargin = dp(12);
        accessibilityButton.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        controlsCard.addView(accessibilityButton, accessibilityParams);

        Button sessionsButton = button("Otwórz dostęp do odtwarzania");
        LinearLayout.LayoutParams sessionsParams = matchWrap();
        sessionsParams.topMargin = dp(8);
        sessionsButton.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        controlsCard.addView(sessionsButton, sessionsParams);

        TextView compatibility = text(
                "Dostęp do odtwarzania pozwala wysyłać komendy bezpośrednio do aktywnej aplikacji, co zwiększa zgodność z odtwarzaczami audiobooków.",
                13, COLOR_SECONDARY);
        compatibility.setPadding(0, dp(10), 0, 0);
        controlsCard.addView(compatibility, matchWrap());

        content.addView(controlsCard, cardParams());
    }

    private void addAdvancedVolumeControls(LinearLayout content) {
        addSectionTitle(content, "CICHY TRYB NOCNY");
        LinearLayout volumeCard = card();
        addAdvancedVolumeSwitch(volumeCard);
        content.addView(volumeCard, cardParams());
    }

    private void addAdvancedVolumeSwitch(LinearLayout volumeCard) {
        volumeStepState = stateText();
        volumeStepState.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        volumeCard.addView(volumeStepState, matchWrap());

        advancedVolumeSwitch = new Switch(this);
        advancedVolumeSwitch.setText(tr("Dodatkowy cichy poziom nocny"));
        advancedVolumeSwitch.setTextSize(17);
        advancedVolumeSwitch.setTextColor(COLOR_TEXT);
        advancedVolumeSwitch.setMinHeight(dp(56));
        advancedVolumeSwitch.setOnCheckedChangeListener((button, checked) -> {
            if (refreshing) return;
            PixelatePreferences.setAdvancedVolumeEnabled(this, checked);
            ScreenOffMediaService.sync(this);
            refreshVolumeStepState();
        });
        volumeCard.addView(advancedVolumeSwitch, matchWrap());

        TextView help = text(
                "Po włączeniu słuchawki Bluetooth mają jeden dodatkowy poziom: 0 → cichy poziom nocny → pierwszy poziom Androida. Na głośniku telefonu głośność działa fabrycznie. Na dodatkowym poziomie czytnik mówi „Cichy poziom nocny”.",
                14, COLOR_SECONDARY);
        help.setPadding(0, dp(4), 0, 0);
        volumeCard.addView(help, matchWrap());
    }

    private void addRecentsModule(LinearLayout content) {
        addSectionTitle(content, "OSTATNIE APLIKACJE");

        LinearLayout serviceCard = card();
        recentsState = stateText();
        recentsState.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        serviceCard.addView(recentsState, matchWrap());

        TextView device = text(tf("Telefon: %1$s", Build.MODEL), 14, COLOR_ACCENT);
        device.setPadding(0, dp(8), 0, 0);
        serviceCard.addView(device, matchWrap());

        TextView help = text(
                "Usługa dostępności pokazuje przycisk „Zamknij wszystkie aplikacje” na ekranie Ostatnich. Działa tylko z Pixel Launcherem i nie wysyła ani nie zapisuje zawartości ekranu.",
                14, COLOR_SECONDARY);
        help.setPadding(0, dp(10), 0, dp(12));
        serviceCard.addView(help, matchWrap());

        Button accessibilityButton = button("Otwórz ustawienia dostępności");
        accessibilityButton.setContentDescription(
                tr("Otwórz ustawienia dostępności i włącz usługę Pixel Center"));
        accessibilityButton.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        serviceCard.addView(accessibilityButton, matchWrap());

        Button testButton = button("Otwórz Ostatnie i sprawdź");
        LinearLayout.LayoutParams testParams = matchWrap();
        testParams.topMargin = dp(8);
        testButton.setOnClickListener(v -> testRecents());
        serviceCard.addView(testButton, testParams);
        content.addView(serviceCard, cardParams());

        addSectionTitle(content, "USTAWIENIA OSTATNICH");
        LinearLayout settingsCard = card();

        focusButtonSwitch = new Switch(this);
        focusButtonSwitch.setText(tr("Ustaw fokus czytnika na przycisku"));
        focusButtonSwitch.setTextSize(17);
        focusButtonSwitch.setTextColor(COLOR_TEXT);
        focusButtonSwitch.setMinHeight(dp(56));
        focusButtonSwitch.setChecked(
                PixelatePreferences.shouldFocusClearButton(this));
        focusButtonSwitch.setOnCheckedChangeListener((button, checked) -> {
            if (!refreshing) {
                PixelatePreferences.setFocusClearButton(this, checked);
            }
        });
        settingsCard.addView(focusButtonSwitch, matchWrap());

        TextView focusHelp = text(
                "Zalecane dla TalkBacka i Jieshuo. Po otwarciu Ostatnich czytnik przejdzie bezpośrednio na „Zamknij wszystkie aplikacje”.",
                14, COLOR_SECONDARY);
        focusHelp.setPadding(0, 0, 0, dp(14));
        settingsCard.addView(focusHelp, matchWrap());

        TextView timeoutLabel = text("Limit szukania przycisku w sekundach", 16, COLOR_TEXT);
        timeoutInput = new EditText(this);
        timeoutInput.setId(View.generateViewId());
        timeoutLabel.setLabelFor(timeoutInput.getId());
        timeoutInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        timeoutInput.setImeOptions(EditorInfo.IME_ACTION_DONE);
        timeoutInput.setSingleLine(true);
        timeoutInput.setSelectAllOnFocus(true);
        timeoutInput.setText(String.valueOf(
                PixelatePreferences.getSearchTimeoutSeconds(this)));
        timeoutInput.setTextSize(18);
        timeoutInput.setTextColor(COLOR_TEXT);
        timeoutInput.setHintTextColor(COLOR_SECONDARY);
        timeoutInput.setContentDescription(
                tr("Limit czasu szukania systemowego przycisku w sekundach, od 0 do 300"));
        timeoutInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(
                    CharSequence value, int start, int count, int after) { }
            @Override public void onTextChanged(
                    CharSequence value, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable value) {
                if (refreshing) return;
                preferencesHandler.removeCallbacks(timeoutSaveRunnable);
                preferencesHandler.postDelayed(
                        timeoutSaveRunnable, TIMEOUT_AUTO_SAVE_DELAY_MS);
            }
        });
        timeoutInput.setOnFocusChangeListener((view, hasFocus) -> {
            if (!hasFocus) persistTimeout(true);
        });
        timeoutInput.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) persistTimeout(true);
            return false;
        });
        settingsCard.addView(timeoutLabel, matchWrap());
        settingsCard.addView(timeoutInput, matchWrap());

        TextView timeoutHelp = text(
                "Domyślnie 20 sekund. Wartość 0 oznacza brak limitu.",
                14, COLOR_SECONDARY);
        timeoutHelp.setPadding(0, dp(4), 0, dp(12));
        settingsCard.addView(timeoutHelp, matchWrap());

        content.addView(settingsCard, cardParams());

        TextView privacy = text(
                "Moduł nie używa internetu, alarmów ani blokady wybudzenia. Po zmianie okna może przez maksymalnie 2,5 sekundy ponawiać wykrywanie Ostatnich; nie skanuje stale zwykłych aplikacji. Jeśli włączysz sterowanie muzyką, reaguje też na przyciski głośności przy wygaszonym ekranie.",
                13, Color.rgb(155, 155, 165));
        privacy.setPadding(0, dp(16), 0, 0);
        content.addView(privacy, matchWrap());
    }

    private void refreshImprovedUi() {
        if (masterSwitch == null) return;
        boolean enabled = prefs.getBoolean(ScopedEqService.PREF_ENABLED, false);
        boolean access = hasNotificationAccess();
        int preset = Math.max(0, Math.min(2, prefs.getInt(ScopedEqService.PREF_PRESET, 1)));

        refreshing = true;
        masterSwitch.setChecked(enabled);
        presetGroup.check(presetIds[preset]);
        if (mediaControlsSwitch != null) {
            mediaControlsSwitch.setChecked(
                    PixelatePreferences.areScreenOffMediaControlsEnabled(this));
        }
        if (advancedVolumeSwitch != null) {
            boolean advanced = PixelatePreferences.isAdvancedVolumeEnabled(this);
            advancedVolumeSwitch.setChecked(advanced);
        }

        enabledState.setText(tr(enabled ? "KOREKTOR WŁĄCZONY" : "KOREKTOR WYŁĄCZONY"));
        enabledState.setTextColor(enabled ? COLOR_OK : COLOR_WARNING);
        accessState.setText(tr(access ? "DOSTĘP NADANY" : "BRAK DOSTĘPU"));
        accessState.setTextColor(access ? COLOR_OK : COLOR_WARNING);
        selectedPreset.setText(tf("WYBRANY PROFIL: %1$s",
                presetName(preset).toUpperCase(Locale.getDefault())));
        selectedPreset.setTextColor(COLOR_ACCENT);
        refreshRecentsState();
        refreshMediaControlsState();
        refreshVolumeStepState();

        if (focusButtonSwitch != null) {
            focusButtonSwitch.setChecked(
                    PixelatePreferences.shouldFocusClearButton(this));
        }
        if (timeoutInput != null && !timeoutInput.hasFocus()) {
            timeoutInput.setText(String.valueOf(
                    PixelatePreferences.getSearchTimeoutSeconds(this)));
        }
        refreshing = false;

        String saved = prefs.getString(ScopedEqService.PREF_STATUS,
                "Korektor czeka na obsługiwany odtwarzacz.");
        if (!LocalizedText.language(this).equals(prefs.getString("status_language", "pl"))) {
            saved = "Korektor czeka na obsługiwany odtwarzacz.";
        }
        if (!access) {
            currentStatus.setText(tr("Wymagany jest dostęp systemowy. Naciśnij „Otwórz ustawienia dostępu” i zezwól aplikacji Pixel Center."));
            currentStatus.setTextColor(COLOR_WARNING);
        } else if (!enabled) {
            currentStatus.setText(tr("Korektor jest wyłączony. Włącz przełącznik „Automatyczny korektor”."));
            currentStatus.setTextColor(COLOR_WARNING);
        } else {
            currentStatus.setText(tr(saved));
            currentStatus.setTextColor(COLOR_ACCENT);
        }
    }

    private void refreshMediaControlsState() {
        if (mediaControlsState == null) return;
        boolean enabled = PixelatePreferences.areScreenOffMediaControlsEnabled(this);
        if (!enabled) {
            mediaControlsState.setText(tr("STEROWANIE WYŁĄCZONE"));
            mediaControlsState.setTextColor(COLOR_WARNING);
        } else if (!ScreenOffMediaService.isRunning()) {
            mediaControlsState.setText(tr("URUCHAMIANIE USŁUGI MUZYKI"));
            mediaControlsState.setTextColor(COLOR_WARNING);
        } else if (!hasNotificationAccess()) {
            mediaControlsState.setText(tr("WŁĄCZONE — NADAJ DOSTĘP DO ODTWARZANIA"));
            mediaControlsState.setTextColor(COLOR_WARNING);
        } else {
            mediaControlsState.setText(tr("STEROWANIE WŁĄCZONE"));
            mediaControlsState.setTextColor(COLOR_OK);
        }
    }

    private void refreshRecentsState() {
        if (recentsState == null) return;
        boolean centerEnabled = isAccessibilityComponentEnabled(
                new ComponentName(this, PixelateAccessibilityService.class));
        boolean legacyEnabled = isAccessibilityComponentEnabled(
                new ComponentName("pl.pixelate", "pl.pixelate.PixelateAccessibilityService"));
        boolean connected = PixelateAccessibilityService.isConnected();

        if (centerEnabled && legacyEnabled) {
            recentsState.setText(tr("WYŁĄCZ STARĄ USŁUGĘ PIXELATE"));
            recentsState.setTextColor(COLOR_WARNING);
        } else if (connected) {
            recentsState.setText(tr("USŁUGA OSTATNICH DZIAŁA"));
            recentsState.setTextColor(COLOR_OK);
        } else if (centerEnabled) {
            recentsState.setText(tr("USŁUGA WŁĄCZONA — OCZEKUJE NA POŁĄCZENIE"));
            recentsState.setTextColor(COLOR_WARNING);
        } else if (legacyEnabled) {
            recentsState.setText(tr("STARA PIXELATE DZIAŁA — PRZEŁĄCZ USŁUGĘ"));
            recentsState.setTextColor(COLOR_WARNING);
        } else {
            recentsState.setText(tr("USŁUGA OSTATNICH WYŁĄCZONA"));
            recentsState.setTextColor(COLOR_WARNING);
        }
    }

    private boolean isAccessibilityComponentEnabled(ComponentName expected) {
        String enabled = Settings.Secure.getString(
                getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (TextUtils.isEmpty(enabled)) return false;
        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
        splitter.setString(enabled);
        while (splitter.hasNext()) {
            ComponentName component = ComponentName.unflattenFromString(splitter.next());
            if (expected.equals(component)) return true;
        }
        return false;
    }

    private void testRecents() {
        boolean legacyEnabled = isAccessibilityComponentEnabled(
                new ComponentName("pl.pixelate", "pl.pixelate.PixelateAccessibilityService"));
        boolean centerEnabled = isAccessibilityComponentEnabled(
                new ComponentName(this, PixelateAccessibilityService.class));
        if (legacyEnabled && centerEnabled) {
            Toast.makeText(this,
                    tr("Najpierw wyłącz starą usługę Pixelate, aby nie pojawiły się dwa przyciski."),
                    Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            return;
        }
        if (!PixelateAccessibilityService.isConnected()) {
            Toast.makeText(this,
                    tr("Najpierw włącz usługę Pixel Center w ustawieniach dostępności."),
                    Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            return;
        }
        if (!PixelateAccessibilityService.openRecentsForTest()) {
            Toast.makeText(this,
                    tr("Nie udało się otworzyć Ostatnich. Spróbuj ponownie."),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void persistTimeout(boolean showEmptyError) {
        if (timeoutInput == null) return;
        preferencesHandler.removeCallbacks(timeoutSaveRunnable);
        String raw = timeoutInput.getText().toString().trim();
        if (raw.isEmpty()) {
            if (showEmptyError) {
                timeoutInput.setError(tr("Wpisz pełną liczbę sekund."));
            }
            return;
        }
        int timeout;
        try {
            timeout = Integer.parseInt(raw);
        } catch (NumberFormatException error) {
            timeoutInput.setError(tr("Wpisz pełną liczbę sekund."));
            return;
        }
        if (timeout < 0 || timeout > PixelatePreferences.MAX_SEARCH_TIMEOUT_SECONDS) {
            timeoutInput.setError(tr("Dozwolony zakres to od 0 do 300."));
            return;
        }
        PixelatePreferences.setSearchTimeoutSeconds(this, timeout);
        timeoutInput.setError(null);
    }

    private void refreshVolumeStepState() {
        if (volumeStepState == null) return;
        boolean enabled = PixelatePreferences.isAdvancedVolumeEnabled(this);
        if (enabled) {
            AudioManager manager = getSystemService(AudioManager.class);
            int min = manager == null ? 0
                    : manager.getStreamMinVolume(AudioManager.STREAM_MUSIC);
            int max = manager == null ? 25
                    : manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int range = Math.max(1, max - min);
            int firstPercent = Math.max(1, Math.round(100f / range));
            int currentFine = PixelatePreferences.getFineVolumePercent(this);
            boolean nightActive = currentFine > 0 && currentFine < firstPercent;
            volumeStepState.setText(nightActive
                    ? tr("CICHY POZIOM NOCNY AKTYWNY")
                    : tr("CICHY TRYB NOCNY WŁĄCZONY"));
        } else {
            volumeStepState.setText(tr("CICHY TRYB NOCNY WYŁĄCZONY"));
        }
        volumeStepState.setTextColor(enabled ? COLOR_OK : COLOR_SECONDARY);
    }

    private int presetForId(int checkedId) {
        for (int i = 0; i < presetIds.length; i++) {
            if (presetIds[i] == checkedId) return i;
        }
        return 1;
    }

    private String presetName(int preset) {
        if (preset == 0) return tr("Lekki");
        if (preset == 2) return tr("Mocny");
        return tr("Średni");
    }

    private void addSectionTitle(LinearLayout parent, String title) {
        TextView heading = text(title, 13, COLOR_SECONDARY);
        heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        heading.setAccessibilityHeading(true);
        heading.setPadding(0, dp(22), 0, dp(8));
        parent.addView(heading, matchWrap());
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackground(cardBackground());
        return card;
    }

    private GradientDrawable cardBackground() {
        GradientDrawable background = new GradientDrawable();
        background.setColor(COLOR_CARD);
        background.setCornerRadius(dp(14));
        background.setStroke(dp(1), Color.rgb(52, 52, 60));
        return background;
    }

    private RippleDrawable tabBackground(boolean selected) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(selected ? COLOR_ACCENT : COLOR_CARD);
        shape.setCornerRadius(dp(24));
        if (!selected) {
            shape.setStroke(dp(1), Color.rgb(70, 70, 78));
        }
        int ripple = selected
                ? Color.argb(50, 0, 0, 0)
                : Color.argb(45, 255, 255, 255);
        return new RippleDrawable(ColorStateList.valueOf(ripple), shape, null);
    }

    private TextView stateText() {
        TextView view = text("", 15, COLOR_ACCENT);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(tr(value));
        view.setTextSize(size);
        view.setTextColor(color);
        view.setLineSpacing(0, 1.12f);
        return view;
    }

    private LinearLayout.LayoutParams cardParams() {
        LinearLayout.LayoutParams params = matchWrap();
        params.bottomMargin = dp(2);
        return params;
    }

    private boolean hasNotificationAccess() {
        return getSystemService(android.app.NotificationManager.class)
                .isNotificationListenerAccessGranted(
                        new android.content.ComponentName(this, PlayerMonitorService.class));
    }

    private Button button(String text) {
        Button button = new Button(this);
        button.setText(tr(text));
        button.setAllCaps(false);
        button.setMinHeight(dp(48));
        return button;
    }

    private String tr(String value) {
        return LocalizedText.text(this, value);
    }

    private String tf(String value, Object... arguments) {
        return LocalizedText.format(this, value, arguments);
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String appVersion() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException ignored) {
            return "";
        }
    }

    private void requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] { Manifest.permission.POST_NOTIFICATIONS }, 17);
        }
    }
}
