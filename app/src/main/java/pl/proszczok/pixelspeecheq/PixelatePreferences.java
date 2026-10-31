package pl.proszczok.pixelspeecheq;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

final class PixelatePreferences {
    static final int DEFAULT_SEARCH_TIMEOUT_SECONDS = 20;
    static final int MAX_SEARCH_TIMEOUT_SECONDS = 300;
    static final int DEFAULT_VOLUME_STEP_PERCENT = 10;
    static final int MIN_VOLUME_STEP_PERCENT = 1;
    static final int MAX_VOLUME_STEP_PERCENT = 50;
    static final String THEME_SYSTEM = "system";
    static final String THEME_LIGHT = "light";
    static final String THEME_DARK = "dark";

    private static final String FILE_NAME = "pixelate_preferences";
    private static final String KEY_FOCUS_CLEAR_BUTTON = "focus_clear_button";
    private static final String KEY_SEARCH_TIMEOUT_SECONDS = "search_timeout_seconds";
    private static final String KEY_APP_THEME = "app_theme";
    private static final String KEY_SCREEN_OFF_MEDIA_CONTROLS = "screen_off_media_controls";
    private static final String KEY_ADVANCED_VOLUME_ENABLED = "advanced_volume_enabled";
    private static final String KEY_VOLUME_STEP_PERCENT = "volume_step_percent";
    private static final String KEY_FINE_VOLUME_PERCENT = "fine_volume_percent";
    private static final String KEY_FINE_VOLUME_PHYSICAL = "fine_volume_physical";

    private PixelatePreferences() {
    }

    static boolean shouldFocusClearButton(Context context) {
        return preferences(context).getBoolean(KEY_FOCUS_CLEAR_BUTTON, true);
    }

    static int getSearchTimeoutSeconds(Context context) {
        int value = preferences(context).getInt(
                KEY_SEARCH_TIMEOUT_SECONDS, DEFAULT_SEARCH_TIMEOUT_SECONDS);
        return Math.max(0, Math.min(MAX_SEARCH_TIMEOUT_SECONDS, value));
    }

    static String getAppTheme(Context context) {
        String value = preferences(context).getString(KEY_APP_THEME, THEME_SYSTEM);
        if (THEME_LIGHT.equals(value) || THEME_DARK.equals(value)) {
            return value;
        }
        return THEME_SYSTEM;
    }

    static boolean shouldUseDarkTheme(Context context) {
        String theme = getAppTheme(context);
        if (THEME_DARK.equals(theme)) {
            return true;
        }
        if (THEME_LIGHT.equals(theme)) {
            return false;
        }
        return (context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    static boolean areScreenOffMediaControlsEnabled(Context context) {
        return preferences(context).getBoolean(KEY_SCREEN_OFF_MEDIA_CONTROLS, false);
    }

    static boolean isAdvancedVolumeEnabled(Context context) {
        return preferences(context).getBoolean(KEY_ADVANCED_VOLUME_ENABLED, false);
    }

    static int getVolumeStepPercent(Context context) {
        int value = preferences(context).getInt(
                KEY_VOLUME_STEP_PERCENT, DEFAULT_VOLUME_STEP_PERCENT);
        return Math.max(MIN_VOLUME_STEP_PERCENT,
                Math.min(MAX_VOLUME_STEP_PERCENT, value));
    }

    static void setFocusClearButton(Context context, boolean focusClearButton) {
        preferences(context).edit()
                .putBoolean(KEY_FOCUS_CLEAR_BUTTON, focusClearButton)
                .apply();
    }

    static void setSearchTimeoutSeconds(Context context, int timeoutSeconds) {
        int safeTimeout = Math.max(0, Math.min(MAX_SEARCH_TIMEOUT_SECONDS, timeoutSeconds));
        preferences(context).edit()
                .putInt(KEY_SEARCH_TIMEOUT_SECONDS, safeTimeout)
                .apply();
    }

    static void setAppTheme(Context context, String appTheme) {
        String safeTheme = THEME_LIGHT.equals(appTheme) || THEME_DARK.equals(appTheme)
                ? appTheme : THEME_SYSTEM;
        preferences(context).edit()
                .putString(KEY_APP_THEME, safeTheme)
                .apply();
    }

    static void setScreenOffMediaControlsEnabled(Context context, boolean enabled) {
        preferences(context).edit()
                .putBoolean(KEY_SCREEN_OFF_MEDIA_CONTROLS, enabled)
                .apply();
    }

    static void setAdvancedVolumeEnabled(Context context, boolean enabled) {
        preferences(context).edit()
                .putBoolean(KEY_ADVANCED_VOLUME_ENABLED, enabled)
                .apply();
    }

    static void setVolumeStepPercent(Context context, int percent) {
        int safePercent = Math.max(MIN_VOLUME_STEP_PERCENT,
                Math.min(MAX_VOLUME_STEP_PERCENT, percent));
        preferences(context).edit()
                .putInt(KEY_VOLUME_STEP_PERCENT, safePercent)
                .apply();
    }

    static int getFineVolumePercent(Context context) {
        return preferences(context).getInt(KEY_FINE_VOLUME_PERCENT, -1);
    }

    static int getFineVolumePhysicalLevel(Context context) {
        return preferences(context).getInt(KEY_FINE_VOLUME_PHYSICAL, -1);
    }

    static void saveFineVolumeState(Context context, int percent, int physicalLevel) {
        preferences(context).edit()
                .putInt(KEY_FINE_VOLUME_PERCENT, percent)
                .putInt(KEY_FINE_VOLUME_PHYSICAL, physicalLevel)
                .apply();
    }

    static void clearFineVolumeState(Context context) {
        preferences(context).edit()
                .remove(KEY_FINE_VOLUME_PERCENT)
                .remove(KEY_FINE_VOLUME_PHYSICAL)
                .apply();
    }

    static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
    }
}
